package de.mpicbg.ms.model.data;

import org.apache.commons.lang3.Range;
import org.apache.commons.math3.util.Precision;

import java.util.TreeMap;

/**
 * EstSample contains the estimation values ( Isomer and Position estimation ).
 *
 * Author: HongKee Moon (moon@mpi-cbg.de), Scientific Computing Facility
 * Organization: MPI-CBG Dresden
 * Date: March 2017
 */
public class EstSample
{
	private final String name;
	private final String group;
	private final String specie;
	private final String mz;
	private final Float ce;

	private Float cFAI, cCOI, faCoRatio;
	private Range<Float> isomer;
	private Range<Float> position;

	// First iteration
	private Range<Float> cf;
	private Range<Float> rel_FAI;
	private Range<Float> secondaryPosition;

	// Second iteration
	private Range<Float> secondaryRel_FAI;
	private Range<Float> secondaryCF;
	private Range<Float> thirdPosition;

	// TX.CF factor
	private Float txcf;

	// Isomer based CF map
	private TreeMap<Float, Float> cfMap;

	public EstSample( String name, String group, String specie, String mz, Float ce, Float txcf )
	{
		this.name = name;
		this.group = group;
		this.specie = specie;
		this.mz = mz;
		this.ce = ce;
		this.txcf = txcf;

		isomer = Range.between( 0f, 0f );

		// First iteration
		position = Range.between( 0f, 1f );
		rel_FAI = Range.between( 0f, 0f );

		// Second iteration
		secondaryPosition = Range.between( 0f, 1f );
		secondaryRel_FAI = Range.between( 0f, 0f );

		// Third iteration
		thirdPosition = Range.between( 0f, 1f );
	}

	private Range<Float> addRangeValue( Range<Float> rangeSet, Float addedValue )
	{
		if( null == rangeSet)
			rangeSet = Range.between( addedValue, addedValue );
		if( rangeSet.isBefore( addedValue ) )
			rangeSet = Range.between( rangeSet.getMinimum(), addedValue );
		else if( rangeSet.isAfter( addedValue ) )
			rangeSet = Range.between( addedValue, rangeSet.getMaximum() );

		return rangeSet;
	}

	private Range<Float> convert( Range<Float> floatRange, int scale )
	{
		return Range.between( Precision.round( floatRange.getMinimum(), scale ), Precision.round( floatRange.getMaximum(), scale ) );
	}

	public String getName()
	{
		return name;
	}

	public String getKey()
	{
		return name + " - " + mz;
	}

	public String getGroup()
	{
		return group;
	}

	public String getSpecie()
	{
		return specie;
	}

	public String getMz()
	{
		return mz;
	}

	public Float getCe()
	{
		return ce;
	}

	public Float getCorrectedFAI()
	{
		return cFAI;
	}

   public Float getTxCorrectedFAI()
   {
      return cFAI * txcf;
   }

	public void setcFAI( Float cFAI )
	{
		this.cFAI = cFAI;
	}

	public Float getCorrectedCOI()
	{
		return cCOI;
	}

	public void setcCOI( Float cCOI )
	{
		this.cCOI = cCOI;
	}

	public Float getFaCoRatio()
	{
		return faCoRatio;
	}

	public void setFaCoRatio( Float faCoRatio )
	{
		this.faCoRatio = faCoRatio;
	}

	public Range< Float > getIsomer()
	{
		return isomer;
	}

	public void setIsomer( Range< Float > isomer )
	{
		this.isomer = isomer;
	}

	public Range< Float > getPosition()
	{
		return position;
	}

	public void setPosition( Range< Float > position )
	{
		this.position = position;
	}

	public Range< Float > getCF()
	{
		if( null == cf )
			return Range.between( 1f, 1f );
		return convert( cf, 3 );
	}

	public void setCF( Range< Float > cf )
	{
		this.cf = convert( cf, 3 );
	}

	public void addCFRange( Float cfValue )
	{
		this.cf = addRangeValue( cf, cfValue );
	}

	public Range< Float > getCFCorrectedFAI()
	{
		return convert( Range.between( cFAI * getCF().getMinimum(), cFAI * getCF().getMaximum() ), 3 );
	}
   public Range< Float > getCFTxCorrectedFAI()
   {
      return Range.between( txcf * cFAI * getCF().getMinimum(), txcf * cFAI * getCF().getMaximum() );
   }


   public Range< Float > getRel_FAI()
	{
		return rel_FAI;
	}

	public void setRel_FAI( Range< Float > rel_FAI )
	{
		this.rel_FAI = convert( rel_FAI, 3 );
	}

	public void addRelFAIRange( Float relFAIValue )
	{
		this.rel_FAI = addRangeValue( rel_FAI, relFAIValue );
	}

	public Range< Float > getSecondaryPosition()
	{
		return secondaryPosition;
	}

	public void setSecondaryPosition( Range< Float > secondaryPosition )
	{
		this.secondaryPosition = convert( secondaryPosition, 2 );
	}

	public void addSecondaryPositionRange( Float secondaryPositionValue )
	{
		this.secondaryPosition = addRangeValue( secondaryPosition, secondaryPositionValue );
	}

	public Range< Float > getSecondaryCF()
	{
		if(null == secondaryCF )
			return Range.between( 1f, 1f );

		return convert( secondaryCF, 3 );
	}

	public void setSecondaryCF( Range< Float > secondaryCF )
	{
		this.secondaryCF = convert( secondaryCF, 3 );
	}

	public Range< Float > getSecondCFCorrectedFAI()
	{
		return Range.between( cFAI * getSecondaryCF().getMinimum(), cFAI * getSecondaryCF().getMaximum() );
	}

	public Range< Float > get2ndCFTxCorrectedFAI()
	{
		return Range.between( txcf * cFAI * getSecondaryCF().getMinimum(), txcf * cFAI * getSecondaryCF().getMaximum() );
	}

	public void setSecondaryRel_FAI( Range< Float > secondaryRel_FAI )
	{
		this.secondaryRel_FAI = convert( secondaryRel_FAI, 3 );
	}

	public Range< Float > getSecondaryRel_FAI()
	{
		return secondaryRel_FAI;
	}

	public Range< Float > getThirdPosition()
	{
		return thirdPosition;
	}

	public void setThirdPosition( Range< Float > thirdPosition )
	{
		this.thirdPosition = convert( thirdPosition, 3 );
	}

	public Range<Float> getFinalFAI()
	{
		Range<Float> secondCFAI = getSecondCFCorrectedFAI();
		Range<Float> thirdPos = getThirdPosition();
		return Range.between( txcf * secondCFAI.getMinimum() * thirdPos.getMinimum(), txcf * secondCFAI.getMaximum() * thirdPos.getMaximum() );
	}

	public Range<Float> getNormFAI( Range<Float> basis )
	{
		Range<Float> thirdPos = getThirdPosition();
		return Range.between( basis.getMinimum() * thirdPos.getMinimum(), basis.getMaximum() * thirdPos.getMaximum() );
	}

	public Float getNormFAI( Float basis )
	{
		Range<Float> thirdPos = getThirdPosition();
		return basis * thirdPos.getMaximum();
	}

	public Float getTxCF()
	{
		return txcf;
	}

	public void addCF( Float isomer, Float cf )
	{
		if( cfMap == null) cfMap = new TreeMap<>(  );

		cfMap.put( isomer, cf );
	}

	public TreeMap<Float, Float> getCfMap()
	{
		return cfMap;
	}

	@Override public String toString()
	{
		return group + '\t' + specie + '\t' + mz + '\t' + name + '\t' + ce + '\t' + cFAI + '\t' + cCOI +
				'\t' + faCoRatio + '\t' + isomer + '\t' + position + '\t' + getCF() + '\t' + getCFCorrectedFAI() +
				'\t' + rel_FAI + '\t' + secondaryPosition + '\t' + getSecondaryCF() + '\t' + getSecondCFCorrectedFAI() +
				'\t' + secondaryRel_FAI + '\t' + thirdPosition + '\t' + getTxCF() + '\t' + getCFTxCorrectedFAI();
	}
}
