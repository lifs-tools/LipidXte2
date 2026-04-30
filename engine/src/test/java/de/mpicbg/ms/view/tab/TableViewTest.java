package de.mpicbg.ms.view.tab;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeSet;

/**
 * Author: HongKee Moon (moon@mpi-cbg.de), Scientific Computing Facility
 * Organization: MPI-CBG Dresden
 * Date: May 2017
 */
public class TableViewTest
{
	@Test
	public void createColumns()
	{
		boolean bNCE = true;
		//sampleQuantTableView = TableViewUtil.createDataView(new String[]{ "Species", "Mspecies" });

		ArrayList<String> list = new ArrayList<>(  );
		list.add( "Species" );

		TreeSet<String> sampleDataSet = new TreeSet();
		sampleDataSet.add( "M1_01" );
		sampleDataSet.add( "M1_02" );

		for( String sample : sampleDataSet )
			list.add( "PRI_" + sample );

		// Mspecieis
		list.add( "Mspecies" );

		if( bNCE )
		{
			list.add( "NCE" );
		}

		for( String sample : sampleDataSet )
			list.add( "FAI.FC_" + sample );

		String[] samples = list.toArray( new String[] { } );

		//TableView tv = TableViewUtil.createDataView(new String[]{ "Species", "Mspecies" });

		//TableViewUtil.addContextMenu( sampleQuantTableView );

		//setMasterNode( sampleQuantTableView );
		System.out.println( Arrays.asList( samples ) );
	}
}
