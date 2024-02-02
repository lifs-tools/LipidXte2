package de.mpicbg.ms.model.data;

import de.mpicbg.ms.view.treecell.NamedBoolean;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import org.apache.commons.math3.util.Precision;

/**
 * Author: HongKee Moon (moon@mpi-cbg.de), Scientific Computing Facility
 * Organization: MPI-CBG Dresden
 * Date: January 2017
 */
public class BARow
{
	NamedBoolean title;
	String name;

	Double mass;

	SimpleIntegerProperty carbon, db;

	SimpleBooleanProperty co;

	Float isomer = 0f;
	Float unspecifiedIsomer = 0f;
	static public boolean isomerCheck = false;

	public BARow( String title )
	{

	}

	private BARow( String title, Double mass, int carbon, int db)
	{
		this.title = new NamedBoolean( title );
		this.mass = mass;
		this.carbon = new SimpleIntegerProperty( carbon );
		this.db = new SimpleIntegerProperty( db );
	}

	public BARow( PR pr )
	{
		this(pr.getSpecie(), pr.getMass(), pr.getCarbon(), pr.getDoubleBonds());
	}

	public BARow( FA fa, int i )
	{
		this("FA-" + i, fa.getMass(), fa.getCarbon(), fa.getDoubleBonds());

		if( null != fa.getCO() )
			this.co = new SimpleBooleanProperty( fa.getCO().validCO() );
	}

	public String getTitle()
	{
		return title.getName();
	}

	public NamedBoolean titleProperty()
	{
		return title;
	}

	public String getName()
	{
		return name;
	}

	public String getMassString()
	{
		if(mass == null) return "";
		return mass.toString();
	}

	public Double getMass()
	{
		return mass;
	}

	public int getCarbon()
	{
		return carbon.get();
	}

	public SimpleIntegerProperty carbonProperty()
	{
		return carbon;
	}

	public int getDb()
	{
		return db.get();
	}

	public SimpleIntegerProperty dbProperty()
	{
		return db;
	}

	public boolean isCoValid()
	{
		if(null == co) return false;
		return co.get();
	}

	public String getCoValidString()
	{
		if(null == co) return "";
		return co.get() + "";
	}

	public SimpleBooleanProperty coProperty()
	{
		return co;
	}

	public Float getIsomer()
	{
		return isomer;
	}

	public void setIsomer( Float isomer )
	{
		this.isomer = Precision.round( isomer, 1 );
	}

	public void setUnspecifiedIsomer( Float unspecifiedIsomer )
	{
		this.unspecifiedIsomer = unspecifiedIsomer;
	}

	@Override
	public String toString()
	{
		if( isomer.equals( 0f ) && unspecifiedIsomer.equals( 0f ))
			return String.format( "%2d:%d", getCarbon(), getDb() );
		else
		{
			if( unspecifiedIsomer.equals( 0f ) )
			{
				if(isomerCheck) {
					if(getDb() < 3) {
						return String.format( "%2d:%d", getCarbon(), getDb() );
					} else {
						if ( isomer == isomer.intValue() )
							return String.format( "%2d:%d (%.0fz)", getCarbon(), getDb(), isomer );
						else
							return String.format( "%2d:%d (%.1fz)", getCarbon(), getDb(), isomer );
					}
				}
				else {
					if ( isomer == isomer.intValue() )
						return String.format( "%2d:%d (%.0fz)", getCarbon(), getDb(), isomer );
					else
						return String.format( "%2d:%d (%.1fz)", getCarbon(), getDb(), isomer );
				}
			}
			else
			{
				if(isomerCheck)
				{
					if ( getDb() < 3 )
					{
						return String.format( "%2d:%d", getCarbon(), getDb() );
					}
					else
					{
						return String.format( "%2d:%d (%.0fz-%.0fz)", getCarbon(), getDb(), isomer, unspecifiedIsomer );
					}
				}
				else
				{
					return String.format( "%2d:%d (%.0fz-%.0fz)", getCarbon(), getDb(), isomer, unspecifiedIsomer );
				}
			}
		}
	}
}
