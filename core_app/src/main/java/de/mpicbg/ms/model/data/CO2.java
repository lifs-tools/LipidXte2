package de.mpicbg.ms.model.data;

import de.mpicbg.ms.model.Fragment;

/**
 * Created by moon on 6/21/16.
 */
public class CO2
{
	Double mass;
	Integer CO2Carbon;
	Integer CO2DoubleBonds;
	Fragment fragment;

	public CO2()
	{

	}

	public CO2(double mass)
	{
		this.mass = mass;
	}

	public Double getMass()
	{
		return mass;
	}

	@Override public String toString()
	{
		return mass.toString();
	}
}
