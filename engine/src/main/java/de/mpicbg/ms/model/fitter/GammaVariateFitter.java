package de.mpicbg.ms.model.fitter;

import org.apache.commons.math3.exception.MathInternalError;
import org.apache.commons.math3.fitting.AbstractCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresBuilder;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresProblem;
import org.apache.commons.math3.linear.DiagonalMatrix;

import java.util.Collection;

/**
 * Created by moon on 6/2/16.
 */
public class GammaVariateFitter extends AbstractCurveFitter
{
   private static final GammaVariateFunction.Parametric FUNCTION = new GammaVariateFunction.Parametric();

   /**
    * Initial guess.
    */
   private final double[] initialGuess;

   private GammaVariateFitter()
   {
      initialGuess = new double[] { 10, 10, 10, 10 };
   }

   public static GammaVariateFitter create()
   {
      return new GammaVariateFitter();
   }

   @Override protected LeastSquaresProblem getProblem( Collection< WeightedObservedPoint > observations )
   {
      final int len = observations.size();
      final double[] target = new double[ len ];
      final double[] weights = new double[ len ];

      int i = 0;
      //		double startX = 0;
      //		double maxX = 0;
      //		double maxY = 0;
      for ( WeightedObservedPoint obs : observations )
      {

         //			if(i == 0) startX = obs.getX();
         //
         //			if( FastMath.max( maxY, obs.getY() ) == obs.getY() )
         //			{
         //				maxY = obs.getY();
         //				maxX = obs.getX();
         //			}

         target[ i ] = obs.getY();
         weights[ i ] = obs.getWeight();
         ++i;
      }

      //		double ab = maxX - startX;
      //		initialGuess[0] = startX;
      //		initialGuess[2] = FastMath.sqrt( ab );
      //		initialGuess[3] = FastMath.sqrt( ab );
      //		initialGuess[1] = maxY / ( FastMath.pow( ab, initialGuess[2] ) * FastMath.exp( - ab / initialGuess[3] ) );

      final AbstractCurveFitter.TheoreticalValuesFunction model = new AbstractCurveFitter.TheoreticalValuesFunction( FUNCTION, observations );

      if ( initialGuess == null )
      {
         throw new MathInternalError();
      }

      // Return a new least squares problem set up to fit a polynomial curve to the
      // observed points.
      return new LeastSquaresBuilder().maxEvaluations( Integer.MAX_VALUE ).maxIterations( Integer.MAX_VALUE ).start( initialGuess ).target( target ).weight( new DiagonalMatrix( weights ) ).model( model.getModelFunction(), model.getModelFunctionJacobian() ).build();
   }
}
