package de.mpicbg.ms.view.tab;

import de.mpicbg.ms.view.pipeline.calibration.CalibrationTab;
import junit.framework.TestCase;

public class CalibrationTabTest extends TestCase
{
	public void testCompareSN() throws Exception
	{
		assertEquals( CalibrationTab.compareSN( "PC120120" ), 0);
		assertEquals( CalibrationTab.compareSN( "PC130120" ), 1);
		assertEquals( CalibrationTab.compareSN( "PC120150" ), -1);

		assertEquals( CalibrationTab.compareSN( "PCE120120" ), 0);
		assertEquals( CalibrationTab.compareSN( "PCE130120" ), 1);
		assertEquals( CalibrationTab.compareSN( "PCE120150" ), -1);

		assertEquals( CalibrationTab.compareSN( "PC12012011z" ), 0);
		assertEquals( CalibrationTab.compareSN( "PC13012011z" ), 1);
		assertEquals( CalibrationTab.compareSN( "PC12015011z" ), -1);

		assertEquals( CalibrationTab.compareSN( "PC1201209z" ), 0);
		assertEquals( CalibrationTab.compareSN( "PC1301209z" ), 1);
		assertEquals( CalibrationTab.compareSN( "PC1201509z" ), -1);

		assertEquals( CalibrationTab.compareSN( "PCE12012011z" ), 0);
		assertEquals( CalibrationTab.compareSN( "PCE13012011z" ), 1);
		assertEquals( CalibrationTab.compareSN( "PCE12015011z" ), -1);

		assertEquals( CalibrationTab.compareSN( "PCE1201209z" ), 0);
		assertEquals( CalibrationTab.compareSN( "PCE1301209z" ), 1);
		assertEquals( CalibrationTab.compareSN( "PCE1201509z" ), -1);
	}

	public void testSplitFAIndexes() throws Exception
	{
		Float[] arr = CalibrationTab.splitFAIndexes( ",,,, 0.35, 0.0" );
		assertEquals( 0.35f, arr[0] );
		assertEquals( 0.0f, arr[1] );
	}
}