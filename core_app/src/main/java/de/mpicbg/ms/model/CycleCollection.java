package de.mpicbg.ms.model;

import org.apache.commons.lang3.Range;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.tools.mzxml_parser.mzxml.model.Scan;

import java.util.DoubleSummaryStatistics;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Collection based on collision energy based cycles
 */
public class CycleCollection
{
	private final Logger logger = LoggerFactory.getLogger( this.getClass() );
	private boolean removeIncompleteCycle = true;

	LinkedList<Cycle> cycles = new LinkedList<>();
	Range<Double> range;
	TreeSet<Float> rangeSet;
	Float step;

	public TreeSet< Float > getRangeSet()
	{
		return rangeSet;
	}

	public void clear()
	{
		cycles.parallelStream().forEach( c -> c.clear() );
		cycles.clear();
	}

	public void addScanCollection(ScanCollection scans)
	{
//		checkRange(scans);

		Cycle cycle = null;
		for(Scan scan : scans.getScans())
		{
//			System.out.println( scan.getCollisionEnergy() );

			// Check the range
			// If it is the minimum, create a cycle.
			if ( rangeSet.first().equals( scan.getCollisionEnergy() ) )
			{
				cycle = new Cycle();
			}

			if( rangeSet.contains( scan.getCollisionEnergy() ) )
			{
				if (cycle == null) cycle = new Cycle();
				cycle.addScan( scan.getCollisionEnergy(), scan );
			}

			if ( rangeSet.last().equals( scan.getCollisionEnergy() ))
			{
				cycles.add( cycle );
			}
		}

		if(!removeIncompleteCycle)
		{
			cycles.add( cycle );
		}

		scans.dispose();

		final int[] i = { 1 };
		cycles.forEach( c -> c.setId( i[ 0 ]++) );
	}

	public boolean isRemoveIncompleteCycle()
	{
		return removeIncompleteCycle;
	}

	public void setRemoveIncompleteCycle( boolean removeIncompleteCycle )
	{
		this.removeIncompleteCycle = removeIncompleteCycle;
	}

	public LinkedList< Cycle > getCycles()
	{
		return cycles;
	}

	public void setCycles( LinkedList< Cycle > cycles )
	{
		this.cycles = cycles;
	}

	public void rangeCheck( ScanCollection scans )
	{
		Range<Double> range;
		TreeSet<Float> rangeSet;

		DoubleSummaryStatistics summary = scans.getScans().parallelStream().mapToDouble( i -> i.getCollisionEnergy() ).summaryStatistics();
		rangeSet = scans.getScans().parallelStream().map( i -> i.getCollisionEnergy() ).collect( Collectors.toCollection( TreeSet::new ) );

		//			for(Float f : rangeSet)
		//			{
		//				System.out.print( f + ", " );
		//			}

		Float min = rangeSet.first();
		Float max = rangeSet.last();


		if( this.range == null || min > this.range.getMinimum() || max < this.range.getMaximum() )
		{
			step = rangeSet.higher( min ) - min;
			range = Range.between( summary.getMin(), summary.getMax() );

			// Remove unnecessary collisionEnergy
			rangeSet.clear();
			for ( Float i = min; i <= max; i += step )
			{
				rangeSet.add( i );
			}

			logger.info( "Collision Energy Range: " + range );
			System.out.println( "Collision Energy Range: " + range );
			System.out.println( "Collision Energy Step: " + step );

			this.range = range;
			this.rangeSet = rangeSet;
		}
	}

	public class Cycle
	{
		private int id;

		public int getId()
		{
			return id;
		}

		public void setId( int id )
		{
			this.id = id;
		}

		final TreeMap<Float, Scan> cycle = new TreeMap<>();

		public void addScan(Float collisionEnergy, Scan scan)
		{
			cycle.put(collisionEnergy, scan);
		}

		public int size()
		{
			return cycle.size();
		}

		public Scan getScan(Float collisionEnergy)
		{
			return cycle.get( collisionEnergy );
		}

		public Set<Float> getKeys()
		{
			return cycle.keySet();
		}

		public void clear()
		{
			cycle.clear();
		}
	}
}
