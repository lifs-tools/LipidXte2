package de.mpicbg.ms.util;

import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.util.FastMath;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Author: HongKee Moon (moon@mpi-cbg.de), Scientific Computing Facility
 * Organization: MPI-CBG Dresden
 * Date: March 2018
 */
public class Validation
{
	public static Double computeRsquared( HashMap< Float, Float > map, PolynomialFunction function )
	{
		double average = map.values().stream().mapToDouble(c -> c).average().getAsDouble();
		double rSquared = 0d;
		double fitted = 0d;
		for( Float cq : map.keySet() )
		{
			double intensity = function.value( cq );
			double target = map.get( cq );

			rSquared += FastMath.pow( target - intensity, 2d );
			fitted += FastMath.pow( intensity - average, 2d );
		}

		rSquared = 1 - (rSquared / fitted);

		return rSquared;
	}

	public static Double computeRsquared( ArrayList< Float > sourceList, ArrayList< Float > targetList )
	{
		double rSquared = 0d;
		double fitted = 0d;
		double average = sourceList.stream().mapToDouble(c -> c).average().getAsDouble();

		for( int i = 0; i < sourceList.size(); i++ )
		{
			rSquared += FastMath.pow(sourceList.get(i) - targetList.get(i), 2d);
			fitted += FastMath.pow( sourceList.get(i) - average, 2d );
		}

		rSquared = 1 - (rSquared / fitted);

		return rSquared;
	}

	public static Double computeRsquared( List< Float > sourceList, Float target )
	{
//		double rSquared = 0d;
//		double fitted = 0d;
//		double average = sourceList.stream().mapToDouble(c -> c).average().getAsDouble();
//
//		for( int i = 0; i < sourceList.size(); i++ )
//		{
//			rSquared += FastMath.pow(sourceList.get(i) - target, 2d);
//			fitted += FastMath.pow( sourceList.get(i) - average, 2d );
//		}
//
//		rSquared = 1 - (rSquared / fitted);
//
//		return rSquared;
		double rSquared = 0d;

		for( int i = 0; i < sourceList.size(); i++ )
		{
			rSquared += FastMath.pow( sourceList.get(i) - target, 2d );
		}

		rSquared = 1.0 - FastMath.sqrt( rSquared / (sourceList.size() - 1.0) );

		return rSquared;
	}
}
