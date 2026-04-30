package de.mpicbg.ms.model;

import org.junit.Test;

import java.util.TreeMap;
import java.util.TreeSet;

import static de.mpicbg.ms.model.regression.SimpleRegression.computeRegressionParameters;

/**
 * Author: HongKee Moon (moon@mpi-cbg.de), Scientific Computing Facility
 * Organization: MPI-CBG Dresden
 * Date: May 2017
 */
public class IsomerSplitTest
{
	@Test
	public void SplitIsomer()
	{
		TreeMap<Float, Float> fa199 = new TreeMap<>(  );
		fa199.put( 25f, 748244.6f );
		fa199.put( 30f, 830750.7f );
		fa199.put( 35f, 832926.3f );

		TreeMap<Float, Float> fa213 = new TreeMap<>(  );
		fa213.put( 25f, 1989476f );
		fa213.put( 30f, 2083753f );
		fa213.put( 35f, 1984795f );

		TreeMap< Float, Float > isomers = new TreeMap<>(  );

		isomers.put( 5f, 1f );
		isomers.put( 8f, 0f );


		double[] params = computeRegressionParameters( true, isomers );

		System.out.println( 6.9f * params[1] + params[0] );
		System.out.println( 7.2f * params[1] + params[0] );
	}
}
