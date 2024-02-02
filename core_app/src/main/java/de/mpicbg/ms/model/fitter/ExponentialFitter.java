package de.mpicbg.ms.model.fitter;

import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.util.FastMath;

import java.util.List;

/**
 * Exponential Fitter
 */
public class ExponentialFitter
{
	private double slope;
	private double intercept;

	public ExponentialFitter( double[] ce, double[] mz )
	{
		findParameter( ce, mz );
	}

	public ExponentialFitter( List< WeightedObservedPoint > weightedObservedPoints )
	{
		double[] ce = new double[ weightedObservedPoints.size() ];
		double[] mz = new double[ weightedObservedPoints.size() ];

		for ( int i = 0; i < ce.length; i++ )
		{
			ce[i] = weightedObservedPoints.get(i).getX();
			mz[i] = weightedObservedPoints.get(i).getY();
		}

		findParameter( ce, mz );
	}

	public double value(double x)
	{
		return intercept * FastMath.exp( x * slope );
	}

	void findParameter(double[] ce, double[] mz)
	{
		double ceMean = StatUtils.mean( ce );

		double[] logMz = new double[ mz.length ];

		for ( int i = 0; i < mz.length; i++ )
		{
			logMz[ i ] = FastMath.log( mz[ i ] );
		}

		double logMzMean = StatUtils.mean( logMz );

		double sumSlope1 = 0;
		double sumSlope2 = 0;
		double[] intercepts = new double[ mz.length ];

		for ( int i = 0; i < mz.length; i++ )
		{
			sumSlope1 += ( ce[ i ] - ceMean ) * ( logMz[ i ] - logMzMean );
			sumSlope2 += FastMath.pow( ce[ i ] - ceMean, 2 );
		}

		slope = sumSlope1 / sumSlope2;

		for ( int i = 0; i < mz.length; i++ )
		{
			intercepts[ i ] = FastMath.exp( logMz[ i ] - ce[ i ] * slope );
		}

		intercept = StatUtils.mean( intercepts );

//		System.out.println( "A = " + intercept + ", B = " + slope );
	}
}
