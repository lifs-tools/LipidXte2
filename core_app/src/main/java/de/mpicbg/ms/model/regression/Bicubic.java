package de.mpicbg.ms.model.regression;

import org.apache.commons.math3.analysis.BivariateFunction;
import org.apache.commons.math3.analysis.interpolation.BicubicInterpolator;
import org.apache.commons.math3.analysis.interpolation.BivariateGridInterpolator;

/**
 * Author: HongKee Moon (moon@mpi-cbg.de), Scientific Computing Facility
 * Organization: MPI-CBG Dresden
 * Date: April 2017
 */
public class Bicubic
{
   public static BivariateFunction createBivariateFunction( double[] xval, double[] yval, double[][] xyfunc )
   {
      BivariateGridInterpolator interpolator = new BicubicInterpolator();

      BivariateFunction function = interpolator.interpolate( xval, yval, xyfunc );

      return function;
   }

   public static void main( final String[] args )
   {
      double[] xval = new double[] { 0, 0.5, 1 };
      double[] yval = new double[] { 5, 6.5, 8 };

      double[][] ce25 = new double[][] { { 1.0192133, 0.94556016, 0.91545737 }, { 1.0871681, 1.0158457, 0.9726076 },
              { 1.124115, 1.0563594, 1.0053717 } };

      BivariateFunction p = createBivariateFunction( xval, yval, ce25 );

      System.out.println( "SN 0, 5z -> " + p.value( 0, 5 ) );
      System.out.println( "SN 0.95, 5.2z -> " + p.value( 0.95, 5.2 ) );
   }
}
