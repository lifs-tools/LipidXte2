package de.mpicbg.ms.model.fitter;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.analysis.MultivariateFunction;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Author: HongKee Moon (moon@mpi-cbg.de), Scientific Computing Facility
 * Organization: MPI-CBG Dresden
 * Date: July 2022
 */
public class NewtonPolynomialFunction implements MultivariateFunction
{
   List<Double> coeffs;
   List<Integer[]> exponents;
   double [][] genPoints;

   double [][] prodPlaceholder;

   int[] maxExponents;

   double[] lowerBounds;
   double[] upperBounds;

   double[] xvals = new double[] { 10.0, 11.0, 12.0, 13.0, 14.0, 15.0, 16.0, 17.0, 18.0, 19.0, 20.0, 21.0, 22.0, 23.0, 24.0, 25.0, 26.0, 27.0, 28.0, 29.0, 30.0, 31.0, 32.0, 33.0, 34.0, 35.0, 36.0, 37.0, 38.0, 39.0, 40.0, 41.0, 42.0, 43.0, 44.0, 45.0, 46.0, 47.0, 48.0, 49.0, 50.0, 51.0, 52.0, 53.0, 54.0, 55.0, 56.0, 57.0, 58.0, 59.0, 60.0, 61.0, 62.0, 63.0, 64.0, 65.0, 66.0, 67.0, 68.0, 69.0, 70.0 };

   public double[] getXvals()
   {
      return xvals;
   }

   public NewtonPolynomialFunction() {
   }

   int getMaxExponents(Integer[][] exponents) {
      maxExponents = new int[exponents[0].length];

      for ( Integer[] item : exponents )
      {
         for ( int j = 0; j < item.length; j++ )
         {
            maxExponents[ j ] = Math.max( maxExponents[ j ], item[ j ] );
         }
      }

      int max = 0;
      for ( int maxExponent : maxExponents )
      {
         max = Math.max( max, maxExponent );
      }

      return max;
   }

   public double[] generateChebychev2ndOrder(int polyDegree)
   {
      if (polyDegree == 0) {
         return new double[] { 1 };
      } else if (polyDegree == 1) {
         return new double[] { -1, 1 };
      } else {
         double[] res = new double[polyDegree];
         for(int i = 0; i < res.length; i++) {
            res[i] = Math.cos( i * Math.PI / (polyDegree - 1) );
         }
//         System.out.println( Arrays.toString(res));
         return res;
      }
   }

   public double[] generateChebychev2ndOrderLejaOrdered(int polyDegree)
   {
      double [] points1 = generateChebychev2ndOrder( polyDegree + 1 );
      ArrayUtils.reverse( points1 );

      double [] points2 = points1;
      int [] ord = new int[polyDegree];
      for (int i = 0; i < polyDegree; i++) {
         ord[i] = i + 1;
      }

      int [] lj = new int[polyDegree + 1];
      double m = 0;

      // https://github.com/casus/minterpy/blob/main/src/minterpy/utils.py
      //      for k in range(0, n):
      //         jj = 0
      //         for i in range(0, n - k):
      //            P = 1
      //            for j in range(k + 1):
      //               idx_pts = int(lj[0, j])
      //               P = P * (points1[idx_pts] - points1[ord[i]])
      //            P = np.abs(P)
      //            if P >= m:
      //               jj = i
      //               m = P
      //         m = 0
      //         lj[0, k + 1] = ord[jj]
      //         ord = np.delete(ord, jj)
      //
      //      leja_points = np.zeros([n + 1, 1])
      //      for i in range(n + 1):
      //         leja_points[i, 0] = points2[int(lj[0, i])]
      //      return leja_points
      for(int k = 0; k < polyDegree; k++) {
         int jj = 0;
         for(int i = 0; i < polyDegree - k; i++) {
            double P = 1;
            for(int j = 0; j < k + 1; j++) {
               int idx_pts = lj[j];
               P = P * (points1[idx_pts] - points1[ord[i]]);
            }
            P = Math.abs( P );
            if (P >= m) {
               jj = i;
               m = P;
            }
         }
         m = 0;
         lj[k + 1] = ord[jj];
         ord = ArrayUtils.remove( ord, jj );
      }

      double[] lejaPoints = new double[polyDegree + 1];
      for(int i = 0; i < polyDegree + 1; i++) {
         lejaPoints[i] = points2[lj[i]];
      }

//      System.out.println(Arrays.toString( lejaPoints ));

      return lejaPoints;
   }

   public double[][] generatePointsFromValues(int spartialDims, double[] generatingValues) {

//      generating_points = np.tile(generating_values, (1, spatial_dimension))
//      generating_points[:, ::2] *= -1

//      System.out.println(Arrays.toString( generatingValues ));
      double[][] res = new double[generatingValues.length][spartialDims];

      for(int i = 0; i < res.length; i++) {
         for(int j = 0; j < spartialDims; j++) {
            res[i][j] = generatingValues[i] * ((j % 2 == 0) ? -1.0 : 1.0);
         }
//         System.out.println(Arrays.toString( res[i] ));
      }

      return res;
   }

   public double[][] generatePoints(int spartialDims, int polyDegree) {
      return generatePointsFromValues( spartialDims,  generateChebychev2ndOrderLejaOrdered(polyDegree));
   }

   public void loadParameters(String resourceName) {
      InputStream is = getClass().getResourceAsStream(resourceName);
      if (is == null) {
         throw new NullPointerException("Cannot find resource file " + resourceName);
      }

      JSONTokener tokener = new JSONTokener(is);
      JSONObject object = new JSONObject(tokener);

      // Loading coefficients
      JSONObject obj = object.getJSONObject( "coeffs" );
      JSONArray array = obj.getJSONArray( "__ndarray__" );
      coeffs = new ArrayList<>();
      for(int i = 0; i < array.length(); i++) {
         coeffs.add( Double.parseDouble(array.get(i).toString()) );
      }

//      System.out.println(coeffs);

      obj = object.getJSONObject( "exponents" );
      array = obj.getJSONArray( "__ndarray__" );
      exponents = new ArrayList<>();
      for(int i = 0; i < array.length(); i++) {
         JSONArray exp = array.getJSONArray( i );

         Integer[] exponent = new Integer[exp.length()];
         for(int j = 0; j < exp.length(); j++) {
            exponent[j] = exp.getInt( j );
         }

         exponents.add( exponent );
      }

//      System.out.println( exponents );

      // Loading exponents with finding max-exponents
      int maxExponent = getMaxExponents( exponents.toArray(new Integer[0][0]) );
//      System.out.println( Arrays.toString(maxExponents) );
      int spatialDims = maxExponents.length;


      obj = object.getJSONObject( "lower_bounds" );
      array = obj.getJSONArray( "__ndarray__" );
      lowerBounds = new double[array.length()];

      for(int i = 0; i < array.length(); i++) {
         lowerBounds[i] = array.getDouble( i );
      }

//      System.out.println( Arrays.toString(lowerBounds) );

      obj = object.getJSONObject( "upper_bounds" );
      array = obj.getJSONArray( "__ndarray__" );
      upperBounds = new double[array.length()];
      for(int i = 0; i < array.length(); i++) {
         upperBounds[i] = array.getDouble( i );
      }

//      System.out.println( Arrays.toString(upperBounds) );

      // Create gen points
      genPoints = generatePoints( spatialDims, maxExponent );
      // Create product place holder
      prodPlaceholder = new double[maxExponent + 1][spatialDims];
   }

   public double[] evalNewtonPolynomials( double[] point ) {
//      System.out.println(Arrays.toString( xvals ));

      double[] ret = new double[xvals.length];
      for(int i = 0; i < xvals.length; i++) {
         point[0] = xvals[i];
         double val = value(point);
         ret[i] = Math.pow( 2, val );
      }

//      System.out.println(Arrays.toString( ret ));

      return ret;
   }

   @Override public double value( double[] input )
   {
      double[] point = input.clone();

      // Normalize input points
      for(int i = 0; i < point.length; i++) {
         point[i] = (((point[i] - lowerBounds[i]) / (upperBounds[i] - lowerBounds[i])) * 2) - 1.0;
      }

//      System.out.println( Arrays.toString(point) );

      int m = point.length;

      for(int i = 0; i < m; i++) {
         int maxExpInDim = maxExponents[i];

         double Xi = point[i];
         double prod = 1.0;

         for(int j = 0; j < maxExpInDim; j++) {
            double Pij = genPoints[j][i];
            prod *= Xi - Pij;
            int exponent = j + 1;
            prodPlaceholder[exponent][i] = prod;
         }
      }

      double[] monomialValsPlaceholder = new double[exponents.size()];

      // evaluate all Newton polynomials. O(Nm)
      for(int j = 0; j < exponents.size(); j++) {
         double newtMonVal = 1.0;

         for(int i = 0; i < m; i++)
         {
            int exp = exponents.get(j)[i];

            if(exp > 0) {
               newtMonVal *= prodPlaceholder[exp][i];
            }
         }

         monomialValsPlaceholder[j] = newtMonVal;
      }

      double res = 0;
      for(int j = 0; j < coeffs.size(); j++) {
         res += coeffs.get(j) * monomialValsPlaceholder[j];
      }

      return res;
   }

   public int[] getMaxExponents()
   {
      return maxExponents;
   }

   public double[] getLowerBounds() { return lowerBounds; }

   public double[] getUpperBounds() { return upperBounds; }
}
