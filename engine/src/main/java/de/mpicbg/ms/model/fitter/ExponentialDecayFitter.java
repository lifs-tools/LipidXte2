package de.mpicbg.ms.model.fitter;

import org.apache.commons.math3.exception.MathInternalError;
import org.apache.commons.math3.fitting.AbstractCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresBuilder;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresProblem;
import org.apache.commons.math3.linear.DiagonalMatrix;

import java.util.Collection;

/**
 * Author: HongKee Moon (moon@mpi-cbg.de), Scientific Computing Facility
 * Organization: MPI-CBG Dresden
 * Date: May 2017
 */
public class ExponentialDecayFitter extends AbstractCurveFitter
{
   private static final ExponentialDecayFunction.Parametric FUNCTION = new ExponentialDecayFunction.Parametric();

   /**
    * Initial guess.
    */
   private final double[] initialGuess;

   private ExponentialDecayFitter()
   {
      initialGuess = new double[] { 1000, 80 };
   }

   public static ExponentialDecayFitter create()
   {
      return new ExponentialDecayFitter();
   }

   @Override protected LeastSquaresProblem getProblem( Collection< WeightedObservedPoint > observations )
   {
      final int len = observations.size();
      final double[] target = new double[ len ];
      final double[] weights = new double[ len ];

      int i = 0;
      for ( WeightedObservedPoint obs : observations )
      {
         target[ i ] = obs.getY();
         weights[ i ] = obs.getWeight();
         ++i;
      }

      final AbstractCurveFitter.TheoreticalValuesFunction model = new AbstractCurveFitter.TheoreticalValuesFunction( FUNCTION, observations );

      if ( initialGuess == null )
      {
         throw new MathInternalError();
      }

      return new LeastSquaresBuilder().maxEvaluations( Integer.MAX_VALUE ).maxIterations( Integer.MAX_VALUE ).start( initialGuess ).target( target ).weight( new DiagonalMatrix( weights ) ).model( model.getModelFunction(), model.getModelFunctionJacobian() ).build();
   }
}
