package de.mpicbg.ms.model;

import java.util.Map;

import uk.ac.ebi.pride.tools.mzxml_parser.MzXMLFile;
import uk.ac.ebi.pride.tools.mzxml_parser.MzXMLParsingException;
import uk.ac.ebi.pride.tools.mzxml_parser.mzxml.model.Peaks;
import uk.ac.ebi.pride.tools.mzxml_parser.mzxml.model.Scan;

/**
* This class provides conversions from the jmzreader data model
*/
public class MzXMLConverter {

	public static double[] extractMzValues(Scan scan, double[] array) {

		if(scan.getPeaks().size() > 1)
		{
			throw new ParsingRuntimeException( "Peaks size > 1. Please, contact to Scientific Computing Facility." );
		}

		Peaks peaks = scan.getPeaks().get( 0 );
		Map<Double, Double> jmzreaderPeakList = null;
		try
		{
			jmzreaderPeakList = MzXMLFile.convertPeaksToMap( peaks );
		}
		catch ( MzXMLParsingException e )
		{
			e.printStackTrace();
		}

		// Allocate space for the data points
		if ((array == null) || (array.length < jmzreaderPeakList.size()))
			array = new double[jmzreaderPeakList.size()];

		// Copy the actual data point values
		int newIndex = 0;
		for (Double mz : jmzreaderPeakList.keySet()) {
			array[newIndex] = mz.doubleValue();
			newIndex++;
		}

		return array;
	}

	public static float[] extractIntensityValues(Scan scan, float[] array) {

		if(scan.getPeaks().size() > 1)
		{
			throw new ParsingRuntimeException( "Peaks size > 1. Please, contact to Scientific Computing Facility." );
		}

		Peaks peaks = scan.getPeaks().get( 0 );

		Map<Double, Double> jmzreaderPeakList = null;
		try
		{
			jmzreaderPeakList = MzXMLFile.convertPeaksToMap( peaks );
		}
		catch ( MzXMLParsingException e )
		{
			e.printStackTrace();
		}

		// Allocate space for the data points
		if ((array == null) || (array.length < jmzreaderPeakList.size()))
			array = new float[jmzreaderPeakList.size()];

		// Copy the actual data point values
		int newIndex = 0;
		for (Double mz : jmzreaderPeakList.keySet()) {
			array[newIndex] = jmzreaderPeakList.get(mz).floatValue();
			newIndex++;
		}

		return array;
	}

}
