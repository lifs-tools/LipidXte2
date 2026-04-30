package de.mpicbg.ms.model;

import org.apache.commons.lang3.Range;
import org.junit.Assert;
import org.junit.Test;
import uk.ac.ebi.pride.tools.mzxml_parser.mzxml.model.Scan;

import java.io.File;

/**
 * Created by moon on 2/24/16.
 */
public class CycleCollectionTest
{

	private static final String TEST_DATA_PATH = "src/test/resources/";

	@SuppressWarnings("null")
	@Test
	public void testRange()
	{
		CycleCollection cycleCollection = new CycleCollection();
//		cycleCollection.setRemoveIncompleteCycle( false );

		File inputFile1 = new File(TEST_DATA_PATH + "20150129_sn12_PC140-180_MSMS_01_1.mzXML");
		MzXMLFileImportMethod importer1 = new MzXMLFileImportMethod(inputFile1);

		File inputFile2 = new File(TEST_DATA_PATH + "20150129_sn12_PC140-180_MSMS_01_2.mzXML");
		MzXMLFileImportMethod importer2 = new MzXMLFileImportMethod(inputFile2);

		cycleCollection.rangeCheck( importer1.executeForMs2() );

		cycleCollection.rangeCheck( importer2.executeForMs2() );

		cycleCollection.addScanCollection( importer1.executeForMs2() );

		cycleCollection.addScanCollection( importer2.executeForMs2() );

		for(CycleCollection.Cycle cycle : cycleCollection.getCycles())
		{
			Assert.assertEquals(cycle.size() , 51);
			Scan scan = cycle.getScan( 10f );

			double mzValues[] ;
			float intensityValues[];

			mzValues = MzXMLConverter.extractMzValues( scan, null );
			intensityValues = MzXMLConverter.extractIntensityValues( scan, null );
			int numOfDataPoints = scan.getPeaksCount().intValue();


			MzIntCollection collection = new MzIntCollection( mzValues, intensityValues );

			collection.sort( MzIntCollection.SortingProperty.MZ, MzIntCollection.SortingDirection.ASCENDING );

			// Get the m/z range
			Range<Double> mzRange = collection.getMzRange();
			Assert.assertEquals( mzRange.getMinimum(), collection.get( 0 ).getMz() );

			collection.sort( MzIntCollection.SortingProperty.MZ, MzIntCollection.SortingDirection.DESCENDING );
			Assert.assertEquals( mzRange.getMaximum(), collection.get( 0 ).getMz() );

			// Get the intensity range
			Range<Float> intensityRange = collection.getIntRange();
			collection.sort( MzIntCollection.SortingProperty.INTENSITY, MzIntCollection.SortingDirection.ASCENDING );
			Assert.assertEquals( intensityRange.getMinimum(), collection.get( 0 ).getIntensity() );

			collection.sort( MzIntCollection.SortingProperty.INTENSITY, MzIntCollection.SortingDirection.DESCENDING );
			Assert.assertEquals( intensityRange.getMaximum(), collection.get( 0 ).getIntensity() );

//			for(MzInt mi : collection)
//			{
//				System.out.println(mi.getMz());
//			}

			System.out.println("Mz Range: [" + mzRange.getMinimum() + ", " + mzRange.getMaximum() + "]");
			System.out.println("Int Range: [" + intensityRange.getMinimum() + ", " + intensityRange.getMaximum() + "]");

		}
	}
}
