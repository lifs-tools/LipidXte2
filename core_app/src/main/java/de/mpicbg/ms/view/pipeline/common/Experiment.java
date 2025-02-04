package de.mpicbg.ms.view.pipeline.common;

import de.mpicbg.ms.model.data.BA;
import de.mpicbg.ms.model.data.BARow;
import de.mpicbg.ms.model.data.CO;
import de.mpicbg.ms.model.data.FA;
import de.mpicbg.ms.model.data.FAAnion;
import de.mpicbg.ms.model.data.Header;
import de.mpicbg.ms.model.data.PR;
import de.mpicbg.ms.util.Data;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.math3.util.Precision;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Author: HongKee Moon (moon@mpi-cbg.de), Scientific Computing Facility
 * Organization: MPI-CBG Dresden
 * Date: September 2018
 */
public class Experiment
{
	public static void process(String outputPath, File loadingFile, String expName,
			TreeItem<BARow> root, final ObservableList<String> samples, final HashMap<TreeItem< BARow >, BA> baMap,
			final ObservableList<PRIRef> priRefs, HashSet<StandardReference> defaultPriRefs,
			ObservableList< FAAnion > mFaAnionsList, boolean isomerCheck)
	{
		String[] nceStringArray = null;

		if( null != outputPath )
		{
			File machinePerformanceFile = new File(outputPath + "machine_performance.tsv");

			if(machinePerformanceFile.exists())
			{
				String firstLine = Data.getFirstLine( machinePerformanceFile );
				//				System.out.println(firstLine);

				String[] split = firstLine.split( "\t" );

				if( !split[2].equals( "-Infinity" ) && !split[2].equals( "null" ) && split[0].equals( "BEST" ) )
				{
					System.out.println( split[1] );
					//					System.out.println( split[1].split( "\\[|\\]|,|\\s" ).length );
					String withBracket = split[1].replaceAll( "[\\p{Ps}\\p{Pe}]", "" );
					nceStringArray = withBracket.split( ",\\s" );
					//					System.out.println( Arrays.toString( nceStringArray ));
				}
			}
		}

		root.getChildren().clear();
		samples.clear();
		baMap.clear();

		TreeSet<String> sampleSet = new TreeSet<>(  );

		Data data = new Data( loadingFile );

		List<String> headers = data.getHeaders();

		TreeMap<Double, PR> experiment = new TreeMap<>(  );

		boolean pccocoPresent = false;
		boolean co2present = false;
		String nce = "25";
		String clazz = "";

		int nceIndex = 0;
		for( CSVRecord record : data.getRecords() )
		{
			if(record.get( Header.PRM ).isEmpty() )
				continue;
			else if( record.get(Header.EC).endsWith( "FAS" ) )
			{
				co2present = false;
				pccocoPresent = false;
				clazz = record.get(Header.EC).replace( "FAS", "" );
				continue;
			}
         else if( record.get(Header.EC).endsWith( "fa" ) )
         {
            co2present = false;
            pccocoPresent = false;
            clazz = record.get(Header.EC).replace( "fa", "" );
            continue;
         }
			else if(record.get(Header.PRM).equals( "#NCE" ))
			{
				nce = record.get( Header.EC );

				if(null != nceStringArray) {
					nce = nceStringArray[nceIndex++];
				}
				continue;
			}
			else if(!record.get(Header.EC).startsWith( "PEO" ) &&
					!record.get(Header.EC).startsWith( "PCO" ) &&
					record.get(Header.EC).endsWith( "COCO" ))
			{
				//				System.out.println("### PCCOCO");
				pccocoPresent = true;
				//				co2present = true;
				continue;
			}
			else if(record.get(Header.EC).endsWith( "CO" ))
			{
				//				System.out.println("### PCCO");
				co2present = true;
				continue;
			}

//			System.out.println( record.get(Header.EC) );

			if( !co2present && !pccocoPresent )
			{
				// Create Precursor
				Double prm = Double.parseDouble( record.get(Header.PRM) );
				if( !experiment.containsKey( prm ) )
				{
					String className = record.get(Header.CLASS);
					String speciesName = record.get(Header.SPECIE);

               if (speciesName.contains( "d5" )) {
                  speciesName = speciesName.replace( "d5", "" );
               }

					// Handling PCO and PEO classes
					// The internal names are PCO and PEO while the correct names are "PC O-" and "PE O-"
					if(className.equals( "PC O-" )) {
						className = "PCO";
						speciesName = speciesName.replace( "PC O-", "PCO" );
					}
					else if (className.equals( "PE O-" )) {
						className = "PEO";
						speciesName = speciesName.replace( "PE O-", "PEO" );
					}

					experiment.put( prm, new PR(prm,
							className,
							speciesName,
							parseInt( record, Header.PRC ),
							parseInt( record, Header.PRDB )
					) );
				}

				PR precursor = experiment.get( prm );

				// Create FA-Anion
				FA faanion1 = null;
				FA faanion2 = null;

				if( record.isMapped( Header.FA1M + "" ) )
				{
					Double faM = parseDouble( record, Header.FA1M );
					if ( !precursor.containFA( faM ) )
					{
						precursor.addFA( faM, new FA( faM,
								parseInt( record, Header.FA1C ),
								parseInt( record, Header.FA1DB ) ) );
					}

					faanion1 = precursor.getFA( faM );
				}

				if( record.isMapped( Header.FA2M + "" ) )
				{
					Double faM = parseDouble( record, Header.FA2M );
					if ( !precursor.containFA( faM ) )
					{
						precursor.addFA( faM, new FA( faM,
								parseInt( record, Header.FA2C ),
								parseInt( record, Header.FA2DB ) ) );
					}

					faanion2 = precursor.getFA( faM );
				}

				for(String s : headers)
				{
					if(s.startsWith( "PRI:" ) ||
							s.startsWith( "FA1I:" ) || s.startsWith( "FA2I:" ) ||
							s.startsWith( "FACO1I:" ) || s.startsWith( "FACO2I:" ))
					//|| s.startsWith( "FAOI:" ) )
					{

						String sampleName;
						BA element = null;

						if( s.startsWith( "FACO1I:" ) || s.startsWith( "FACO2I:" ) )
							sampleName = s.substring( 7 );
						else if( s.startsWith( "FA1I:" ) || s.startsWith( "FA2I:" ) )
							sampleName = s.substring( 5 );
						else
							sampleName = s.substring( 4 );

						if(s.startsWith( "PRI:" ))
							element = precursor;
						else if( s.startsWith( "FA1I:" ) )
							element = faanion1;
						else if( s.startsWith( "FA2I:" ) )
							element = faanion2;
						else if(s.startsWith( "FACO1I:" ) )
							element = faanion1.getCO();
						else if(s.startsWith( "FACO2I:" ))
							element = clazz.equals( "PCO" ) ? faanion1.getCO() : faanion2.getCO();

						final String sample = sampleName.replace( expName, "" );

						final String sampleId = sample;
						final Float ce = Float.parseFloat( nce );

						if( !record.get( s ).trim().isEmpty() && !record.get( s ).trim().equals( "None" ) )
						{
							sampleSet.add( sampleId );
							element.putSampleIntensity( sampleId, ce, Float.parseFloat( record.get( s ) ) );
						}

						// s : header
						// parse the header!
						// get the value
						//											System.out.println( tokens[0] + ":"  + tokens[1] + "=" + record.get( s ) );
					}
				}
			}
			else if(co2present)
			{
				// Process two CO2Losses
				// Create Precursor
				Double prm = Double.parseDouble( record.get(Header.PRM) );

				PR precursor = experiment.get( prm );

				// Create FA-Anion
				FA faanion = null;
				String co = null;

				if( record.isMapped( Header.FA2M + "" ) )
				{
					Double faM = parseDouble( record, Header.FA2M );
					faanion = precursor.getFA( faM );

					if(faanion.getCO() == null && !record.get(Header.FACO2M).equals( "None" ) )
						faanion.setCO( new CO( parseDouble( record, Header.FACO2M ) ) );

					co = "FACO2I:";
				} else if( clazz.equals( "PCO" ) || clazz.equals( "PEO" ) ) {
					Double faM = parseDouble( record, Header.FA1M );
					faanion = precursor.getFA( faM );

					if(faanion.getCO() == null && !record.get(Header.FACO1M).equals( "None" ) )
						faanion.setCO( new CO( parseDouble( record, Header.FACO1M ) ) );

					co = "FACO1I:";
				}

				for(String s : headers)
				{
					if( s.startsWith( co ) )
					{
						String sampleName = s.substring( 7 );
						BA element = faanion.getCO();

						final String sample = sampleName.replace( expName, "" );

						final String sampleId = sample;
						final Float ce = Float.parseFloat( nce );

						if( !record.get( s ).trim().isEmpty() && !record.get( s ).trim().equals( "None" ) )
						{
							sampleSet.add( sampleId );
							element.putSampleIntensity( sampleId, ce, Float.parseFloat( record.get( s ) ) );
						}
					}
				}
			}
			else if(pccocoPresent)
			{
				// Process CO2Loss
				// Create Precursor
				Double prm = Double.parseDouble( record.get(Header.PRM) );

				PR precursor = experiment.get( prm );

				// Create FA-Anion
				FA faanion1 = null;
				FA faanion2 = null;

				if( record.isMapped( Header.FA1M + "" ) )
				{
					Double faM = parseDouble( record, Header.FA1M );
					faanion1 = precursor.getFA( faM );

					if(faanion1.getCO() == null && record.isMapped( Header.FACO1M + "" ) )
						faanion1.setCO( new CO( parseDouble( record, Header.FACO1M ) ) );
				}

				if( record.isMapped( Header.FA2M + "" ) )
				{
					Double faM = parseDouble( record, Header.FA2M );
					faanion2 = precursor.getFA( faM );

					if(faanion2.getCO() == null && record.isMapped( Header.FACO2M + "" ) )
						faanion2.setCO( new CO( parseDouble( record, Header.FACO2M ) ) );
				}

				for(String s : headers)
				{
					BA element = null;

					if( s.startsWith( "FACO1I:" ) )
						element = faanion1.getCO();
					else if( s.startsWith( "FACO2I:" ) )
						element = faanion2.getCO();

					if( element != null )
					{
						String sampleName = s.substring( 7 );

						final String sample = sampleName.replace( expName, "" );

						final String sampleId = sample;
						final Float ce = Float.parseFloat( nce );

						if( !record.get( s ).trim().isEmpty() && !record.get( s ).trim().equals( "None" ) )
						{
							sampleSet.add( sampleId );
							element.putSampleIntensity( sampleId, ce, Float.parseFloat( record.get( s ) ) );
						}
					}
				}
			}

			//			if(faanion.validCO())
			//				System.out.println(faanion.getMass() + " has CO");
			//			else
			//				System.out.println(faanion.getMass() + " does not have CO");
		}

		samples.addAll( sampleSet );

		//		priRefs.clear();
		//
		// Data validation
		for( PR precursor : experiment.values() )
		{
			// remove if it contains inconsistent NCE data
			int len = precursor.getKeys().size();

			ArrayList<Double> toRemove = new ArrayList<>(  );

			for ( FA faanion : precursor.getFAs() )
			{
				if(len != faanion.getKeys().size())
				{
					System.err.println( "Add " + faanion.getMass() + " toRemove list");
					toRemove.add( faanion.getActualMass() );
					// Check complement fa and remove as well
					toRemove.add( getComplement(precursor, faanion) );
				}
				else if( faanion.getCO() != null )
				{
					if(len != faanion.getCO().getKeys().size())
					{
//						System.err.println( "Set " + faanion.getMass() + "'s CO2loss null due to insufficient data");
						faanion.setCO( null );
					}
				}
			}

			for ( Double mass : toRemove )
				precursor.removeFA( mass );

			// adjust mass precision based on FAAnionList
			for( PR pr : PR.splitPR( precursor ) )
			{
				TreeMap<Double, FA> toUpdate = new TreeMap<>(  );

				for ( FA faanion : pr.getFAs() )
				{
					Double mass = faanion.getMass();
					int carbon = faanion.getCarbon();
					int db = faanion.getDoubleBonds();

					Optional<FAAnion> faAnion = mFaAnionsList.stream().filter( c ->
							c.getMass().equals( mass ) &&
									c.getFACarbon().equals( carbon ) &&
									c.getFADoubleBonds().equals( db ) ).findFirst();

					if(!faAnion.isPresent())
					{
						System.err.println( mass + ":carbon - " + carbon + ":db - " + db + " => Not found in FAAnion List!" );
						Double newMass = Precision.round( mass - 0.01d, 2 );

						Optional<FAAnion> newFaAnion = mFaAnionsList.stream().filter( c ->
								c.getMass().equals( newMass ) &&
										c.getFACarbon().equals( carbon ) &&
										c.getFADoubleBonds().equals( db ) ).findFirst();

						if(newFaAnion.isPresent())
						{
							System.out.println("Found with " + newMass);
							faanion.setMass( newMass );

							toUpdate.put( mass, faanion);
						}
					}
				}

				for ( Double mass : toUpdate.keySet() )
				{
					pr.removeFA( mass );
					pr.addFA( toUpdate.get(mass).getMass(), toUpdate.get(mass) );
				}
			}
		}

		BARow.isomerCheck = isomerCheck;

		// Fill up the GUI with the precursor data
		for( PR precursor : experiment.values() )
		{
			// Add PRI reference item into the ListView of PRI
			PRIRef priRef = new PRIRef( precursor.getSpecie(), false );
			if( null != defaultPriRefs && !defaultPriRefs.contains( priRef ) )
			{
				priRefs.add( priRef );
			}

			if(clazz.equals( "PCO" ) || clazz.equals( "PEO" ) ) {
				PR pr = precursor;
				for ( FA faanion : pr.getFAs() )
				{
					// Create a PRI tree item
					final TreeItem< BARow > prItem = new TreeItem<>( new BARow( pr ) );
					baMap.put( prItem, pr );

					final TreeItem< BARow > faItem = new TreeItem<>( new BARow( faanion, 1 ) );
					baMap.put( faItem, faanion );

					prItem.getChildren().add( faItem );

					root.getChildren().add( prItem );
				}
			}
			else
			{
				for( PR pr : PR.splitPR( precursor ) )
				{
					// Create a PRI tree item
					final TreeItem< BARow > prItem = new TreeItem<>( new BARow( pr ) );
					baMap.put( prItem, pr );

					int i = 1;
					for ( FA faanion : pr.getFAs() )
					{
						final TreeItem< BARow > faItem = new TreeItem<>( new BARow( faanion, i++ ) );
						baMap.put( faItem, faanion );

						prItem.getChildren().add( faItem );
					}

					root.getChildren().add( prItem );
				}
			}
		}
	}

	private static Double getComplement( PR precursor, FA firstFA )
	{
		int preCarbon = precursor.getCarbon() - firstFA.getCarbon();
		int preDoubleBonds = precursor.getDoubleBonds() - firstFA.getDoubleBonds();
		double actualMass = 0;

		for(FA fa : precursor.getFAs()) {
			if(fa.equals( firstFA )) continue;

			if( preCarbon - fa.getCarbon() == 0 && preDoubleBonds - fa.getDoubleBonds() == 0) {
				actualMass = fa.getActualMass();
				break;
			}
		}

		return actualMass;
	}

	static Double parseDouble( CSVRecord record, Header header )
	{
		return Precision.round( Double.parseDouble( record.get( header) ), 3 );
	}

	static Integer parseInt( CSVRecord record, Header header )
	{
		return (int) Double.parseDouble(record.get( header));
	}

	public static class PRIRef {
		private final StringProperty name = new SimpleStringProperty();
		private final BooleanProperty on = new SimpleBooleanProperty();

		public PRIRef(String name, boolean on) {
			setName(name);
			setOn(on);
		}

		public final StringProperty nameProperty() {
			return this.name;
		}

		public final String getName() {
			return this.nameProperty().get();
		}

		public final void setName(final String name) {
			this.nameProperty().set(name);
		}

		public final BooleanProperty onProperty() {
			return this.on;
		}

		public final boolean isOn() {
			return this.onProperty().get();
		}

		public final void setOn(final boolean on) {
			this.onProperty().set(on);
		}

		@Override
		public String toString() {
			return getName();
		}

		@Override
		public boolean equals( Object o )
		{
			if( o != null )
				return o.hashCode() == hashCode();
			else return false;
		}

		@Override
		public int hashCode()
		{
			return getName().hashCode();
		}
	}

	public static class StandardReference extends PRIRef
	{
		public String specie;
		public String molSpecie;
		public Float quantity;

		public StandardReference( String specie, String molSpecie, Float quantity )
		{
			super( specie, true );

			this.specie = specie;
			this.molSpecie = molSpecie;
			this.quantity = quantity;
		}

		public String getSpecie()
		{
			return specie;
		}

		public void setSpecie( String specie )
		{
			this.specie = specie;
		}

		public String getMolSpecie()
		{
			return molSpecie;
		}

		public void setMolSpecie( String molSpecie )
		{
			this.molSpecie = molSpecie;
		}

		public Float getQuantity()
		{
			return quantity;
		}

		public void setQuantity( Float quantity )
		{
			this.quantity = quantity;
		}

		@Override
		public String toString() {
			return getName() + " - Qty:" + quantity;
		}
	}
}
