package de.mpicbg.ms.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ModifiableObservableListBase;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LipidClass Collection class
 */
public class LipidClassCollection extends ModifiableObservableListBase< LipidClass >
{
	final ObservableList< LipidClass > list = FXCollections.observableArrayList();

	final static LipidClassCollection collection = new LipidClassCollection();

	private LipidClassCollection()
	{
		parseClassText();
	}

	private void parseClassText()
	{
		String lipidClass = null;
		try
		{
			lipidClass = IOUtils.toString( getClass().getResourceAsStream( "lipidClass.txt" ) );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}

		StringTokenizer st = new StringTokenizer( lipidClass, "\n" );

		Pattern pattern = Pattern.compile("([^\\t]+)\\t([^\\t]+)\\t([^\\t]+)\\t([^\\t]+)\\t([^\\t]+)\\t([^\\t]+)");

		while(st.hasMoreTokens())
		{
			String line = st.nextToken();

			Matcher matcher = pattern.matcher( line );
			matcher.find();

			String lipid = matcher.group( 1 ).trim();
			if(lipid.equals( "Lipid class" )) continue;

			String aliphaticMoiety = matcher.group( 2 ).trim();
			String faAnions = matcher.group( 3 ).trim();
			String sn2 = matcher.group( 4 ).trim();
			String sym = matcher.group( 5 ).trim();
			String sn1 = matcher.group( 6 ).trim();

			list.add( new LipidClass( lipid, AliphaticMoiety.valueOf( aliphaticMoiety ),
					Integer.parseInt( faAnions ),
					Integer.parseInt( sn2 ),
					Integer.parseInt( sym ),
					Integer.parseInt( sn1 ) ) );
		}
	}

	@Override public LipidClass get( int index )
	{
		return list.get( index );
	}

	@Override public int size()
	{
		return list.size();
	}

	@Override protected void doAdd( int index, LipidClass element )
	{
		list.add( index, element );
	}

	@Override protected LipidClass doSet( int index, LipidClass element )
	{
		return list.set( index, element );
	}

	@Override protected LipidClass doRemove( int index )
	{
		return list.remove( index );
	}

	public static LipidClassCollection get()
	{
		return collection;
	}

	public static boolean isSn2( String clazz )
	{
		Optional<LipidClass> lipidClass = collection.stream().filter( c -> c.toString().equals( clazz ) ).findFirst();

		if(lipidClass.isPresent())
			return lipidClass.get().isSn2();
		else
			return false;
	}

	public static boolean isSym( String clazz )
	{
		Optional<LipidClass> lipidClass = collection.stream().filter( c -> c.toString().equals( clazz ) ).findFirst();

		if(lipidClass.isPresent())
			return lipidClass.get().isSym();
		else
			return false;
	}

	public static boolean isSn1( String clazz )
	{
		Optional<LipidClass> lipidClass = collection.stream().filter( c -> c.toString().equals( clazz ) ).findFirst();

		if(lipidClass.isPresent())
			return lipidClass.get().isSn1();
		else
			return false;
	}
}
