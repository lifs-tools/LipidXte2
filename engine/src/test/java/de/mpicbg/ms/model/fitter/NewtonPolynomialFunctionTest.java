package de.mpicbg.ms.model.fitter;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

/**
 * Author: HongKee Moon (moon@mpi-cbg.de), Scientific Computing Facility
 * Organization: MPI-CBG Dresden
 * Date: August 2022
 */
public class NewtonPolynomialFunctionTest
{

   @Test
   public void evalNewtonPolynomials()
   {
      NewtonPolynomialFunction newt = new NewtonPolynomialFunction();
      newt.loadParameters( "PC/sym_FA_poly.json" );

      double[] point = new double[] { 10,  22., 6., 4. };

      double[] ret = newt.evalNewtonPolynomials( point );

//      System.out.println( Arrays.toString(ret));

      double[] target = new double[] {1.3335242721824894, 1.8661924324578365, 2.6847398050166364, 3.8793548393490496, 5.533265908756339, 7.693136910194959, 10.33602814346558, 13.345050238030119, 16.506922192043053, 19.538382400551985, 22.13690867521664, 24.04054579226977, 25.07779031104221, 25.19320755159571, 24.444522602851944, 22.976839889707406, 20.98524055163485, 18.677273506704072, 16.243406309443078, 13.8388607558677, 11.576401336076437, 9.527431589027493, 7.72810972852683, 6.187586966551686, 4.896316796249146, 3.8332610253987354, 2.9715131131038177, 2.2823127960299714, 1.7376671564839874, 1.3118833223411135, 0.9823174917349292, 0.729600309634434, 0.5375397126637769, 0.3928456507652938, 0.28477384262977046, 0.20474975276782084, 0.14600838550824488, 0.10326830610680965, 0.07244739635337769, 0.05042136568579029, 0.03482249784525945, 0.023874437211866512, 0.016258257114874337, 0.011005107728993541, 0.0074111015561854135, 0.00497057591330929, 0.0033243747598524446, 0.0022202695543040744, 0.001483073438573145, 9.923924580839873E-4, 6.663047009073463E-4, 4.495666972603833E-4, 3.052184036994252E-4, 2.0869459131645475E-4, 1.4375200897390523E-4, 9.968927629039945E-5, 6.947201280230959E-5, 4.848220874315781E-5, 3.369203952319694E-5, 2.3122304525805776E-5, 1.548846622420405E-5};

      Assert.assertArrayEquals(ret, target, 1e-3);
   }

   @Test
   public void loadParameters()
   {
      NewtonPolynomialFunction newt = new NewtonPolynomialFunction();
      newt.loadParameters( "PC/sym_FA_poly.json" );
   }

   @Test
   public void checkPCsymMaxExponents()
   {
      NewtonPolynomialFunction newt = new NewtonPolynomialFunction();
      newt.loadParameters( "PC/sym_FA_poly.json" );

      int [] expected = new int[]{ 9, 2, 2, 1 };
      int [] actual = newt.getMaxExponents();

      Assert.assertArrayEquals(actual, expected);
   }

   @Test
   public void checkPCsymCO2MaxExponents()
   {
      NewtonPolynomialFunction newt = new NewtonPolynomialFunction();
      newt.loadParameters( "PC/sym_CO2_poly.json" );

      int [] expected = new int[]{ 4, 2, 2, 1 };
      int [] actual = newt.getMaxExponents();

      Assert.assertArrayEquals(actual, expected);
   }

   @Test
   public void checkPCsymLowerBounds()
   {
      NewtonPolynomialFunction newt = new NewtonPolynomialFunction();
      newt.loadParameters( "PC/sym_FA_poly.json" );

      double [] expected = new double []{ 9.0, 12.0, 0.0, 0.0 };
      double [] actual = newt.getLowerBounds();

      Assert.assertArrayEquals(actual, expected, 0.1);
   }

   @Test
   public void checkPCsymUpperBounds()
   {
      NewtonPolynomialFunction newt = new NewtonPolynomialFunction();
      newt.loadParameters( "PC/sym_FA_poly.json" );

      double [] expected = new double []{ 72.0, 33.0, 7.0, 15.0 };
      double [] actual = newt.getUpperBounds();

      Assert.assertArrayEquals(actual, expected, 0.1);
   }

   @Test
   public void checkChebychev2ndOrder() {
      NewtonPolynomialFunction newt = new NewtonPolynomialFunction();
      double [] expected = new double[]{1.00000000e+00,  9.23879533e-01,  7.07106781e-01,  3.82683432e-01,
              6.12323400e-17, -3.82683432e-01, -7.07106781e-01, -9.23879533e-01, -1.00000000e+00};
      double [] actual = newt.generateChebychev2ndOrder( 9 );

      Assert.assertArrayEquals(actual, expected, 0.0001);
   }

   @Test
   public void checkGeneratingValues() {
      NewtonPolynomialFunction newt = new NewtonPolynomialFunction();

      double [] expected = new double []{ -1, 1, 0.17364818, -0.5, 0.76604444, -0.76604444, 0.5, -0.17364818, -0.93969262, 0.93969262 };
      double [] actual = newt.generateChebychev2ndOrderLejaOrdered( 9 );

      Assert.assertArrayEquals(actual, expected, 0.0001);
   }

   @Test
   public void checkGeneratePoints() {
      NewtonPolynomialFunction newt = new NewtonPolynomialFunction();

      double [][] actual = new double[][] {
              { 1.0,        -1.0,         1.0,        -1.0       },
              {-1.0,         1.0,        -1.0,         1.0       },
              {-0.17364818,  0.17364818, -0.17364818,  0.17364818},
              { 0.5,        -0.5,         0.5,        -0.5       },
              {-0.76604444,  0.76604444, -0.76604444,  0.76604444},
              { 0.76604444, -0.76604444,  0.76604444, -0.76604444},
              {-0.5,         0.5,        -0.5,         0.5       },
              { 0.17364818, -0.17364818,  0.17364818, -0.17364818},
              { 0.93969262, -0.93969262,  0.93969262, -0.93969262},
              {-0.93969262,  0.93969262, -0.93969262,  0.93969262}};

      double [][] expected = newt.generatePoints( 4, 9 );

      for(int i = 0; i < actual.length; i++) {
         Assert.assertArrayEquals(actual[i], expected[i], 0.0001);
      }
   }

   @Test
   public void checkMaxDegree() {
      NewtonPolynomialFunction newt = new NewtonPolynomialFunction();

      newt.loadParameters( "PI/sym_CO2_poly.json" );

//      System.out.println(Arrays.toString( newt.maxExponents ));

      int [] actual = new int[] {4, 2, 2, 1};

      Assert.assertArrayEquals( actual, newt.maxExponents );
   }

   @Test
   public void checkParams() {
      NewtonPolynomialFunction newt = new NewtonPolynomialFunction();

      newt.loadParameters( "PI/sym_CO2_poly.json" );

//      System.out.println(newt.coeffs);
      for(Integer[] arr : newt.exponents) {
         System.out.println(Arrays.deepToString(arr));
      }

//      System.out.println(Arrays.deepToString(newt.genPoints));
//      System.out.println(Arrays.deepToString(newt.prodPlaceholder));
   }
}
