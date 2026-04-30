package de.mpicbg.ms.model;

import java.io.File;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import uk.ac.ebi.pride.tools.mzxml_parser.mzxml.model.Scan;

public class MzXMLFileImportMethodTest {

	private static final String TEST_DATA_PATH = "src/test/resources/";

	@SuppressWarnings("null")
	@Test
	public void test20150129_sn12_PC140_77() throws ParsingRuntimeException {

		// Import the file
		File inputFile = new File(TEST_DATA_PATH + "20150129_sn12_PC140-180_MSMS_01_1.mzXML");
		Assert.assertTrue(inputFile.canRead());
		MzXMLFileImportMethod importer = new MzXMLFileImportMethod(inputFile);
		ScanCollection scanCollection = importer.executeForMs2();
		Assert.assertNotNull(scanCollection);
		Assert.assertEquals(1.0, importer.getFinishedPercentage(), 0.0001);

		List<Scan> scans = scanCollection.getScans();
		Assert.assertNotNull(scans);
		Assert.assertEquals(314, scans.size());

		Scan scan = scanCollection.getScan( 77 );

		System.out.println( "Scan number :" + scan.getNum() );
		System.out.println( "Ms Level :" + scan.getMsLevel() );
		System.out.println( "PolarityType :" + scan.getPolarity() );
		System.out.println( scan.getScanType() );
		System.out.println( scan.getFilterLine() );
		System.out.println( scan.getRetentionTime() );
		System.out.println( "Collision Energy: " + scan.getCollisionEnergy() );

		if(scan.getPrecursorMz().size() == 1)
		{
			System.out.println("Precursor Mz: " + scan.getPrecursorMz().get( 0 ).getValue());
			System.out.println("Precursor Intensity: " + scan.getPrecursorMz().get( 0 ).getPrecursorIntensity());
			System.out.println("Activation Method: " + scan.getPrecursorMz().get( 0 ).getActivationMethod());
		}

		Assert.assertEquals( new Long( 77 ), scan.getNum() );
		Assert.assertEquals( new Float( 13 ), scan.getCollisionEnergy() );

		scanCollection.dispose();
	}

	@SuppressWarnings("null")
	@Test
	public void testCycles() throws ParsingRuntimeException {

		// Import the file
		File inputFile = new File(TEST_DATA_PATH + "20150129_sn12_PC140-180_MSMS_01_1.mzXML");
		MzXMLFileImportMethod importer = new MzXMLFileImportMethod(inputFile);
		ScanCollection scanCollection = importer.executeForMs2();

		List<Scan> scans = scanCollection.getScans();
		for(Scan scan: scans)
		{
			System.out.println(scan.getCollisionEnergy());
		}

		scanCollection.dispose();

		inputFile = new File(TEST_DATA_PATH + "20150129_sn12_PC140-180_MSMS_01_2.mzXML");
		importer = new MzXMLFileImportMethod(inputFile);
		scanCollection = importer.executeForMs2();

		scans = scanCollection.getScans();
		for(Scan scan: scans)
		{
			System.out.println(scan.getCollisionEnergy());
		}

		scanCollection.dispose();
	}

}
