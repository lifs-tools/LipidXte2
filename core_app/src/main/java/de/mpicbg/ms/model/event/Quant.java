package de.mpicbg.ms.model.event;

/**
 * Author: HongKee Moon (moon@mpi-cbg.de), Scientific Computing Facility
 * Organization: MPI-CBG Dresden
 * Date: May 2017
 */
public class Quant
{
	public enum Option
	{
		Intensity,
		Profile,
		Quantity
	}

	public enum Output
	{
		All,
		Sum,
		Mspecies
	}

	public enum AdditionalOption
	{
		RemoveReference,
		SummarizeNCE,
		NoCorrection,
		GroupOnly,
		MergeUnspecifiedIsomer,
		MergeGlobalHomogeneous,
		IntensityCheckRemove,
		RemoveIsomerInfo
		//ApplyTXCFinSummary
	}
}
