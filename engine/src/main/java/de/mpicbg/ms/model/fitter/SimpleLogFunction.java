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
public class SimpleLogFunction implements UnivariateDifferentiableFunction
{
   private final double coefficients[];

   public SimpleLogFunction( double c[] )
   {
      MathUtils.checkNotNull( c );
      int n = c.length;
      if ( n != 3 )
      {
         throw new NoDataException( LocalizedFormats.ARGUMENT_OUTSIDE_DOMAIN );
      }

      this.coefficients = new double[ 3 ];
      System.arraycopy( c, 0, this.coefficients, 0, 3 );
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
      double c = coefficients[ 2 ];

      return FastMath.log( a, argument ) * b + c;
   }

   @Override public DerivativeStructure value( DerivativeStructure t ) throws DimensionMismatchException
   {
      double a = coefficients[ 0 ];
      double b = coefficients[ 1 ];
      double c = coefficients[ 2 ];

      DerivativeStructure logBase = new DerivativeStructure( t.getFreeParameters(), t.getOrder(), t.getValue() );

      DerivativeStructure result = new DerivativeStructure( t.getFreeParameters(), t.getOrder(), t.getValue() );

      logBase = logBase.multiply( a ).log();

      result = result.log().divide( logBase ).multiply( b ).add( c );

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
         return SimpleLogFunction.evaluate( doubles, v );
      }

      @Override public double[] gradient( double v, double... doubles )
      {
         final double[] gradient = new double[ doubles.length ];

         double a = doubles[ 0 ];
         double b = doubles[ 1 ];
         double c = doubles[ 2 ];

         gradient[ 0 ] = -( b * FastMath.log( v ) ) / ( a * FastMath.pow( FastMath.log( a ), 2 ) );
         gradient[ 1 ] = FastMath.log( v ) / FastMath.log( a );
         gradient[ 2 ] = 1;

         return gradient;
      }
   }
}
