package de.mpicbg.ms.model.data;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.math3.util.Precision;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mass class holds type and lipid class
 */
public class Mass
{
	Double mass;
	String type;
	String clazz;
	SimpleBooleanProperty validProperty;

	public Mass( Double mass, String type, String clazz )
	{
		this.mass = mass;
		this.type = type;
		this.clazz = clazz;
		this.validProperty = new SimpleBooleanProperty( true );
	}

	public Mass( Double mass )
	{
		this.mass = mass;
	}

	@Override public boolean equals( Object obj )
	{
		return mass.equals( obj );
	}

	@Override public int hashCode()
	{
		return mass.hashCode();
	}

	@Override public String toString()
	{
		return getMass().toString();
	}

	public Double getMass()
	{
		return mass;
	}

	public void setMass( Double mass )
	{
		this.mass = mass;
	}

	public String getType()
	{
		return type;
	}

	public void setType( String type )
	{
		this.type = type;
	}

	public String getClazz()
	{
		return clazz;
	}

	public void setClazz( String clazz )
	{
		this.clazz = clazz;
	}

	public boolean getValidProperty()
	{
		return validProperty.get();
	}

	public void setValidProperty( boolean val )
	{
		validProperty.set( val );
	}

	public SimpleBooleanProperty validPropertyProperty()
	{
		return validProperty;
	}

	public static void parseTextArea( ObservableList<CharSequence> lines, final int mmu,
			TreeMap<Double, Mass> massClassTreeMap, TreeSet<String> classSet )
	{
		int digit = 3 - (int) Math.log10(mmu);

		LinkedList<String> queryList = new LinkedList<>();
		Pattern pattern = Pattern.compile("\\s*([^,]+),\\s*([^,]*),\\s*([^,]*)");

		for(CharSequence cs : lines)
		{
			if(cs.toString().isEmpty()) continue;

			Matcher matcher = pattern.matcher(cs);
			matcher.find();

			String mz = matcher.group(1).trim();
			if(mz.equals( "mz" )) continue;

			String type = matcher.group(2).trim();
			String clazz = matcher.group(3).trim();
			if(!clazz.isEmpty())
				classSet.add( clazz );

			Double fragment = Double.parseDouble( mz );
			Double roundedValue = Precision.round( fragment, digit );
			queryList.add( roundedValue + "" );

			massClassTreeMap.put( roundedValue, new Mass( roundedValue, type, clazz ) );
			System.out.println( cs + " -> " + roundedValue + "(" + type + "/" + clazz + ")" );
		}

		if(queryList.size() == 0)
		{
			System.err.println( "No fragment is given." );
			return;
		}
	}

	public static List<Mass> tryParseMzInput( String contents, final int mmu )
	{
		final int digit = 3 - (int) Math.log10(mmu);
		final ArrayList<Mass> arrayList = new ArrayList<>();
		StringReader reader = new StringReader( contents );
		CSVParser parser = null;

		try
		{
			parser = CSVFormat.TDF.withNullString( "" ).parse( reader );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}

		if ( parser != null )
		{
			parser.forEach( c ->
			{
				//			System.out.println(  + "-" + c.get("mz") + "-" + c.get("FA_C") + c.get("FA_DB") + c.get("FA_iso") );

				Double fragment = Double.parseDouble( c.get(0) );
				Double roundedValue = Precision.round( fragment, digit );

				System.out.println(roundedValue);
				arrayList.add( new Mass( roundedValue ) );
			} );
		}

		return arrayList;
	}

}
