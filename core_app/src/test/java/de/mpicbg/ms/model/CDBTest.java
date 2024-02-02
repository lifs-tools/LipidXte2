package de.mpicbg.ms.model;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.function.BiConsumer;

/**
 * Author: HongKee Moon (moon@mpi-cbg.de), Scientific Computing Facility
 * Organization: MPI-CBG Dresden
 * Date: May 2017
 */
public class CDBTest
{
	@Test
	public void testCarbonDoubleBond()
	{
		int prc = 36;
		int prdb = 4;

		ArrayList<int[]> candidates = new ArrayList<>(  );
		candidates.add( new int[] { 16, 0 } );
		candidates.add( new int[] { 20, 4 } );
		candidates.add( new int[] { 18, 2 } );

		candidates.add( new int[] { 30, 2 } );
		candidates.add( new int[] { 6, 2 } );

		HashMap<Integer, HashSet<int[]> > splitMap = new HashMap<>(  );

		// Test symmetric
		for( int[] frag : candidates )
		{
			if( frag[0] == prc / 2 && frag[1] == prdb / 2 )
			{
				System.out.println( Arrays.toString( frag ) + " : symmetric");
				int idx = splitMap.size();
				splitMap.put( idx, new HashSet<>() );
				splitMap.get( idx ).add( frag );
			}
			else
			{
				// search complement item if there is
				int key = searchComplement( prc, prdb, splitMap, frag );
				splitMap.get( key ).add( frag );
			}
		}

		splitMap.forEach( (k, v) ->
		{
			System.out.println( k + " : "  );
			v.forEach( t -> System.out.println( Arrays.toString( t ) ) );
		});
	}

	public Integer searchComplement(int prc, int prdb, HashMap<Integer, HashSet<int[]> > splitMap, int[] frag)
	{
		final int[] idx = { -1 };

		splitMap.forEach( (k, v) ->
		{
			int[] sum = new int[]{ 0, 0 };
			v.forEach( c -> {
				sum[0] += c[0];
				sum[1] += c[1];
			}  );

			sum[0] += frag[0];
			sum[1] += frag[1];

			if( prc == sum[0] && prdb == sum[1] )
				idx[ 0 ] = k;
		});

		if( idx[0] == -1 )
		{
			idx[0] = splitMap.size();
			splitMap.put( idx[0], new HashSet<>(  ) );
		}

		return idx[0];
	}
}
