package de.mpicbg.ms.model.fitter;

import org.apache.commons.math3.analysis.ParametricUnivariateFunction;
import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.analysis.differentiation.UnivariateDifferentiableFunction;
import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.exception.NoDataException;
import org.apache.commons.math3.exception.NullArgumentException;
import org.apache.commons.math3.exception.util.LocalizedFormats;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.MathUtils;

/**
 * Author: HongKee Moon (moon@mpi-cbg.de), Scientific Computing Facility
 * Organization: MPI-CBG Dresden
 * Date: May 2017
 */
public class SimpleExponentialFunction implements UnivariateDifferentiableFunction
{
   private final double coefficients[];

   public SimpleExponentialFunction( double c[] )
   {
      MathUtils.checkNotNull( c );
      int n = c.length;
      if ( n != 2 )
      {
         throw new NoDataException( LocalizedFormats.ARGUMENT_OUTSIDE_DOMAIN );
      }

      this.coefficients = new double[ 2 ];
      System.arraycopy( c, 0, this.coefficients, 0, 2 );
   }

   protected static double evaluate( double[] coefficients, double argument ) throws NullArgumentException, NoDataException
   {
      MathUtils.checkNotNull( coefficients );
      int n = coefficients.length;
      if ( n == 0 )
      {
         throw new NoDataException( LocalizedFormats.EMPTY_POLYNOMIALS_COEFFICIENTS_ARRAY );
      }

      double a = coefficients[ 0 ];
      double b = coefficients[ 1 ];

      return a * FastMath.pow( argument, b );
   }

   @Override public DerivativeStructure value( DerivativeStructure t ) throws DimensionMismatchException
   {
      double a = coefficients[ 0 ];
      double b = coefficients[ 1 ];

      DerivativeStructure result = new DerivativeStructure( t.getFreeParameters(), t.getOrder(), t.getValue() );

      result = result.pow( b ).multiply( a );

      return result;
   }

   @Override public double value( double x )
   {
      return evaluate( coefficients, x );
   }

   public static class Parametric implements ParametricUnivariateFunction
   {
      @Override public double value( double v, double... doubles )
      {
         return SimpleExponentialFunction.evaluate( doubles, v );
      }

      @Override public double[] gradient( double v, double... doubles )
      {
         final double[] gradient = new double[ doubles.length ];

         double a = doubles[ 0 ];
         double b = doubles[ 1 ];

         gradient[ 0 ] = FastMath.pow( v, b );
         gradient[ 1 ] = a * FastMath.pow( v, b ) * FastMath.log( v );

         return gradient;
      }
   }
}
