package de.mpicbg.ms.model.regression;

import org.apache.commons.math3.util.Precision;

/**
 * Author: HongKee Moon (moon@mpi-cbg.de), Scientific Computing Facility
 * Organization: MPI-CBG Dresden
 * Date: March 2017
 */
public class Percent
{
	public static double toFivePercentUnit( double a )
	{
		if( a < 0 )
			return 0d;
		else if( a > 1 )
			return 1d;
		else
		{
			return Precision.round( a * 2, 1 ) / 2d;
		}
	}

	public static void main( final String[] args )
	{
		System.out.println( toFivePercentUnit(0.97d) );
	}
}
