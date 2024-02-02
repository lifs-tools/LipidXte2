package de.mpicbg.ms.model.regression;

import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

import java.util.TreeMap;

/**
 * Author: HongKee Moon (moon@mpi-cbg.de), Scientific Computing Facility
 * Organization: MPI-CBG Dresden
 * Date: February 2017
 */
public class SimpleRegression
{
	public static double[] computeRegressionParameters( boolean verbose, TreeMap< Float, Float > samples )
	{
		final int dataSize = samples.size();
		double[] params = null;

		OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();

		if( dataSize == 3 )
		{
			double[] data = new double[ dataSize * 3 ];
			int itemIdx = 0;

			for ( Float ce : samples.keySet() )
			{
				data[ itemIdx++ ] = samples.get( ce );
				data[ itemIdx++ ] = ce;
				data[ itemIdx++ ] = ce * ce;
			}

			regression.newSampleData( data, dataSize, 2 );
			params = regression.estimateRegressionParameters();
			if(verbose)
				System.out.println( String.format( ">> %6.2E * x^2 + %6.2E * x + %6.2E", params[2] , params[1], params[0] ) );
		}
		else if( dataSize == 2 )
		{
			double[] data = new double[ dataSize * 2 ];
			int itemIdx = 0;

			for ( Float ce : samples.keySet() )
			{
				data[ itemIdx++ ] = samples.get( ce );
				data[ itemIdx++ ] = ce;
			}

			regression.newSampleData( data, dataSize, 1 );
			params = regression.estimateRegressionParameters();
			if(verbose)
				System.out.println( String.format( ">> %6.2E * x + %6.2E", params[1], params[0] ) );
		}
		else
		{
			System.err.println( "Only linear and 2nd degree regression are supported." );
		}

		return params;
	}

	public static double[] computeRegressionParametersForChart( TreeMap< Float, Float > samples )
	{
		final int dataSize = samples.size();
		double[] params = null;

		OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();

		if( dataSize == 3 )
		{
			double[] data = new double[ dataSize * 3 ];
			int itemIdx = 0;

			for ( Float key : samples.keySet() )
			{
				data[ itemIdx++ ] = key * 100;
				data[ itemIdx++ ] = samples.get( key );
				data[ itemIdx++ ] = samples.get( key ) * samples.get( key );
			}

			regression.newSampleData( data, dataSize, 2 );
			params = regression.estimateRegressionParameters();
		}
		else if( dataSize == 2 )
		{
			double[] data = new double[ dataSize * 2 ];
			int itemIdx = 0;

			for ( Float key : samples.keySet() )
			{
				data[ itemIdx++ ] = key * 100;
				data[ itemIdx++ ] = samples.get( key );
			}

			regression.newSampleData( data, dataSize, 1 );
			params = regression.estimateRegressionParameters();
		}
		else
		{
			System.err.println( "Only linear and 2nd degree regression are supported." );
		}

		return params;
	}

	public static void main( final String[] args )
	{
		TreeMap< Float, Float > isomers = new TreeMap<>(  );

		isomers.put( 5f, 0.09378f );
		isomers.put( 6.5f, 0.05772f );
		isomers.put( 8f, 0.02978f );

		computeRegressionParameters( true, isomers );
	}
}
