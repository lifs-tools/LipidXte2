package de.mpicbg.ms.util;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Data object provides a basic way of handling csv file
 */
public class Data
{
	List< String > headers;
	List< CSVRecord > records = null;

	public Data( InputStream inputStream )
	{
		try
		{
			Reader in = new InputStreamReader( inputStream );
			records = CSVFormat.EXCEL.parse( in ).getRecords();
		}
		catch ( FileNotFoundException e )
		{
			e.printStackTrace();
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}

		for ( CSVRecord record : records )
		{
			// Get the column headers
			for ( int i = 0; i < record.size(); i++ )
			{
				if ( i == 0 )
					headers = new ArrayList<>( record.size() );

				headers.add( record.get( i ) );
				//System.out.println( headers[i] );
			}
			break;
			//			String colEnergy = record.get(6);
			//			System.out.println(colEnergy);
		}
	}

	public Data( String fileName )
	{
		try
		{
			Reader in = new FileReader( fileName );
			records = CSVFormat.EXCEL.parse( in ).getRecords();
		}
		catch ( FileNotFoundException e )
		{
			e.printStackTrace();
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}

		for ( CSVRecord record : records )
		{
			// Get the column headers
			for ( int i = 0; i < record.size(); i++ )
			{
				if ( i == 0 )
					headers = new ArrayList<>( record.size() );

				headers.add( record.get( i ) );
				//System.out.println( headers[i] );
			}
			break;
			//			String colEnergy = record.get(6);
			//			System.out.println(colEnergy);
		}
	}

	public Data( File file )
	{
		final InputStream is;
		try
		{
			is = new FileInputStream( file );
			final Reader in = new InputStreamReader( is );
			CSVParser parser = CSVFormat.EXCEL.withHeader().parse( in );

			records = parser.getRecords();
			headers = parser.getHeaderMap().keySet().stream().collect( Collectors.toList() );
		}
		catch ( FileNotFoundException e )
		{
			e.printStackTrace();
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}

	public static Set<String> getHeaders( File file )
	{
		final InputStream is;
		try
		{
			is = new FileInputStream( file );
			final Reader in = new InputStreamReader( is );
			CSVParser parser = CSVFormat.EXCEL.withHeader().parse( in );
			return parser.getHeaderMap().keySet();
		}
		catch ( FileNotFoundException e )
		{
			e.printStackTrace();
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}

		return null;
	}


	public List< String > getHeaders()
	{
		return headers;
	}

	public static String getFirstLine( File file )
	{
		String ret = "";
		try
		{
			final BufferedReader in = new BufferedReader( new FileReader( file ) );

			ret = in.readLine();

			in.close();
		}
		catch ( FileNotFoundException e )
		{
			e.printStackTrace();
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}

		return ret;
	}

	public ArrayList< Double > getValues( int i )
	{
		ArrayList< Double > result = new ArrayList<>();

		for ( CSVRecord record : records )
		{
			if ( record.getRecordNumber() == 1 )
				continue;
			if ( StringUtils.isEmpty( record.get( i ) ) )
				result.add( 0d );
			else
				result.add( Double.parseDouble( record.get( i ) ) );
		}

		return result;
	}

	public List< CSVRecord > getRecords()
	{
		return records;
	}
}
