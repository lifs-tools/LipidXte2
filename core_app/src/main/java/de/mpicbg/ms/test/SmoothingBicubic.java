package de.mpicbg.ms.test;

import org.apache.commons.math3.analysis.BivariateFunction;
import org.apache.commons.math3.analysis.interpolation.BicubicInterpolator;
import org.apache.commons.math3.analysis.interpolation.BivariateGridInterpolator;
import org.apache.commons.math3.analysis.interpolation.SmoothingPolynomialBicubicSplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;

/**
 * Author: HongKee Moon (moon@mpi-cbg.de), Scientific Computing Facility
 * Organization: MPI-CBG Dresden
 * Date: April 2017
 */
public class SmoothingBicubic
{
   public static void main( final String[] args )
   {
      BivariateGridInterpolator interpolator = new BicubicInterpolator();

      double[] xval = new double[] { 0 };
      double[] yval = new double[] { 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30,
              31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56,
              57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70 };

      double[][] avg = new double[][]
              {
                      { 0, 1.5111464, 1.9002304, 2.5411408, 3.5182884, 4.870805, 6.620148, 8.623651, 11.28017,
                              14.522267, 18.011074, 21.870022, 25.967356, 30.47247, 34.295227, 35.685665, 36.413315,
                              38.058636, 39.702408, 40.466038, 39.5851, 38.402206, 37.923225, 37.32405, 36.312977,
                              35.202923, 34.0791, 32.340588, 30.32681, 28.828495, 27.452282, 25.978271, 24.611061,
                              23.14795, 21.583155, 19.983849, 18.562841, 17.339558, 15.771154, 14.126713, 12.803677,
                              11.417684, 9.948368, 8.538523, 7.2880487, 6.428468, 5.7770934, 5.04504, 4.345248,
                              3.7635937, 3.2358582, 2.7603586, 2.4575226, 2.21808, 1.8447853, 1.4559416, 1.1681387,
                              0.9910564, 0.85143244, 0.69602025, 0 }
              };

      BivariateFunction p = interpolator.interpolate( xval, yval, avg );

      for ( int i = 0; i < yval.length; i++ )
         System.out.println( p.value( 0, yval[ i ] ) );
   }
}
