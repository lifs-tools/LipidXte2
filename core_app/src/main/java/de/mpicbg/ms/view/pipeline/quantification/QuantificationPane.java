package de.mpicbg.ms.view.pipeline.quantification;

import de.mpicbg.ms.db.MasterDatabase;
import de.mpicbg.ms.model.data.EstSample;
import de.mpicbg.ms.model.event.ProcessEvent;
import de.mpicbg.ms.model.SampleEstimation;
import de.mpicbg.ms.model.data.BA;
import de.mpicbg.ms.model.data.BARow;
import de.mpicbg.ms.model.data.FAAnion;
import de.mpicbg.ms.model.event.Quant;
import de.mpicbg.ms.util.TableViewUtil;
import de.mpicbg.ms.view.pipeline.validation.SampleValidationPane;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.chart.LineChart;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Skin;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;

import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.Range;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static de.mpicbg.ms.model.SampleEstimation.createEstSamples;
import static de.mpicbg.ms.model.SampleEstimation.createGroupData;
import static de.mpicbg.ms.model.SampleEstimation.processEstSample;
import static de.mpicbg.ms.util.TableViewUtil.autoFitTable;
import static de.mpicbg.ms.view.pipeline.validation.SampleValidationPane.collectIntensityRatio;

/**
 * Author: HongKee Moon (moon@mpi-cbg.de), Scientific Computing Facility
 * Organization: MPI-CBG Dresden
 * Date: May 2017
 */
public class QuantificationPane extends BorderPane
{
	private TableView<String[]> sampleQuantTableView;

	final private TreeMap<String, LinkedHashMap< SampleEstimation.FASample, SampleEstimation.FASample[] > > sampleData;
	final private TreeMap<String, SampleEstimation.FASample> priMap;
	final private TreeMap< String, EstSample > estSampleTreeMap;
	final private HashSet< String > sampleIdSet;
	final private TreeMap< String, TreeMap< String, EstSample > > groupSampleTreeMap;

	final SampleValidationPane sampleValidationPane;

	final SimpleDoubleProperty progressProperty;
	final SimpleDoubleProperty sizeProperty;

	public QuantificationPane( SampleValidationPane sampleValidationPane, LineChart< Number, Number > chart )
	{
		sampleData = new TreeMap<>(  );
		priMap = new TreeMap<>(  );
		estSampleTreeMap = new TreeMap<>(  );
		sampleIdSet = new HashSet<>(  );
		groupSampleTreeMap = new TreeMap<>(  );

		this.sampleValidationPane = sampleValidationPane;

		progressProperty = new SimpleDoubleProperty( 0d );
		sizeProperty = new SimpleDoubleProperty( 0d );

		// PRM Table
		//sampleQuantTableView = TableViewUtil.createDataView(new String[]{ "Sample", "PRM", "Ratio" });
		sampleQuantTableView = TableViewUtil.createDataView(new String[]{ "Species", "Mspecies" });

		setCenter( sampleQuantTableView );

		addEventHandler( ProcessEvent.QUANTIFICATION_RESET, new EventHandler< ProcessEvent >()
		{
			@Override public void handle( ProcessEvent event )
			{
				Event.fireEvent( sampleValidationPane, new ProcessEvent( ProcessEvent.VALIDATION_GROUPING, "Reset" ) );

				sampleData.clear();
				priMap.clear();
				estSampleTreeMap.clear();
				sampleIdSet.clear();
				groupSampleTreeMap.clear();

				sampleQuantTableView.getItems().clear();
			}
		} );

		addEventHandler( ProcessEvent.QUANTIFICATION_PROCESS, new EventHandler< ProcessEvent >()
		{
			@Override public void handle( ProcessEvent event )
			{
				estSampleTreeMap.clear();
				sampleIdSet.clear();
				groupSampleTreeMap.clear();

				Object[] params = event.getParam();

				TreeMap< String, ArrayList<String> > groupMap = (TreeMap< String, ArrayList<String> >) params[0];
				//ArrayList<String> samples = (ArrayList) params[0];

				ObservableList<TreeItem<BARow>> species = (ObservableList<TreeItem<BARow>>) params[1];

				HashMap<TreeItem< BARow >, BA> baMap = (HashMap<TreeItem< BARow >, BA>) params[2];

				Map<String, Float> refPRIMap = (Map<String, Float>) params[3];

				ObservableList< FAAnion > mFaAnionsList = (ObservableList< FAAnion >) params[4];

				Quant.Option processOption = (Quant.Option) params[5];

				Quant.Output outputOption = (Quant.Output) params[6];

				EnumSet<Quant.AdditionalOption> additionalOptions = (EnumSet<Quant.AdditionalOption>) params[7];

				int size = 0;

				ObservableList<String> samples = FXCollections.observableArrayList();

				for ( String groupKey : groupMap.keySet() )
					for ( String sampleId : groupMap.get( groupKey ) ) {
						size++;
						samples.add( sampleId );
					}

				final ProgressIndicator pi = (ProgressIndicator) params[8];
				pi.progressProperty().unbind();
				pi.progressProperty().bind( progressProperty );
				setSize( size * 2 );

				final File loadingFile = (File) params[9];
				final File outputFile = (File) params[10];

				System.out.println( processOption );
				System.out.println( additionalOptions );
				System.out.println( outputOption );

//            System.out.println( loadingFile );
            boolean txCorrect = true;
            if (loadingFile.getParentFile().toString().contains( "slens10" )) txCorrect = false;
            System.out.println( "Tx correction: " + txCorrect );
//            System.out.println( outputFile );

            boolean finalTxCorrect = txCorrect;
            new Thread( () ->
				{
					final MasterDatabase masterDatabase = new MasterDatabase();
					masterDatabase.connect();

					if(additionalOptions.contains( Quant.AdditionalOption.IntensityCheckRemove ))
						checkIntensityRatio( species, baMap, mFaAnionsList, samples, masterDatabase );

					int count = 0;

					for ( String groupKey : groupMap.keySet() )
					{
						for ( String sampleId : groupMap.get( groupKey ) )
						{
							count++;
							setProgress( count );

							TreeMap< String, EstSample > estSampleTreeMap = createEstSamples( masterDatabase, groupKey,
									sampleId, species, baMap, mFaAnionsList, true );

							groupSampleTreeMap.put( groupKey + "-" + sampleId, estSampleTreeMap );
						}
					}

               // Removed the below for now
               // This doesn't mean because we use 1st CF instead of 2nd CF for technical reason
//					updateEstSamples( masterDatabase, mFaAnionsList, baMap, species, groupMap, groupSampleTreeMap );

					masterDatabase.close();

					StringBuilder sb = new StringBuilder(  );

					sb.append( "Group\tSpecie\tMz\tSample\tCE\tC.FAI\tC.COI\tFA-CO.ratio\tFA_Isomer\t1st Pos\t1st CF\t1st C.FAI\t1st rel_FAI\t2nd Pos\t2nd CF\t2nd C.FAI\t2nd rel_FAI\t3rd Pos\tTX.CF\n" );

					for ( String groupKey : groupMap.keySet() )
					{
						for ( String sampleId : groupMap.get( groupKey ) )
						{
							count++;
							setProgress( count );

							processEstSample( sampleId, species, baMap, refPRIMap, mFaAnionsList, finalTxCorrect,
									additionalOptions.contains( Quant.AdditionalOption.NoCorrection ), groupSampleTreeMap.get( groupKey + "-" + sampleId ),
									( priReferenceMap, computedPriFragmentMap, faSampleMap, sampleTreeMap ) ->
									{
										sampleData.put( sampleId, faSampleMap );

										//final String key = specieName + "-" + faMz + ce;
										for ( String key : sampleTreeMap.keySet() )
										{
//											System.out.println( sampleId + "-" + key );
											// e.g. PC 43:6:0-327.2335.0
											estSampleTreeMap.put( sampleId + "-" + key, sampleTreeMap.get( key ) );
//                                 System.out.println(sampleTreeMap.get( key ).toString());
											sb.append( sampleTreeMap.get( key ).toString() + "\n" );
										}
									} );
						}
					}

					File parent = loadingFile.getParentFile();

					try
					{
						FileUtils.writeStringToFile( new File( parent, loadingFile.getName() + "-inter.tsv"), sb.toString() );
					}
					catch ( IOException e )
					{
						e.printStackTrace();
					}

					// sampleIdSet used for sending them to validation pane
					sampleIdSet.addAll( sampleData.keySet() );

					createGroupData( groupMap, sampleData,
							additionalOptions.contains( Quant.AdditionalOption.GroupOnly ),
							additionalOptions.contains( Quant.AdditionalOption.MergeGlobalHomogeneous ),
							additionalOptions.contains( Quant.AdditionalOption.NoCorrection ),
							mFaAnionsList );

					refreshQuantTable( groupMap, refPRIMap, processOption, outputOption, additionalOptions );

					if(outputFile != null)
					{
						try
						{
							FileUtils.writeStringToFile( outputFile, TableViewUtil.exportToString( sampleQuantTableView ) );
						}
						catch ( IOException e )
						{
							e.printStackTrace();
						}
						Platform.exit();
					}
				}).start();
			}
		} );
	}

	public void checkIntensityRatio( ObservableList< TreeItem< BARow > > species, HashMap< TreeItem< BARow >, BA > baMap, ObservableList< FAAnion > mFaAnionsList, ObservableList< String > samples, MasterDatabase masterDatabase )
	{
		LinkedHashSet< TreeItem< BARow > > baSet = new LinkedHashSet<>(  );

		for(TreeItem< BARow > pre : species)
		{
			if(pre.getChildren().size() == 2) {

				baSet.add( pre );
			}
		}

		final TreeMap< String, TreeMap< String, EstSample > > groupSampleMapForIntensityCheck = new TreeMap<>(  );

		TreeMap< String, ArrayList<String> > groupMapForIntensityCheck = new TreeMap<>(  );
		groupMapForIntensityCheck.put( "", new ArrayList<>(  ) );

		final TreeMap< String, TreeMap< String, EstSample > > groupSampleTreeMap = new TreeMap<>(  );

		for( String sampleId : samples )
		{
			groupMapForIntensityCheck.get("").add( sampleId );

			TreeMap< String, EstSample > estSampleTreeMap = createEstSamples( masterDatabase, "",
					sampleId, baSet, baMap, mFaAnionsList, true );

			groupSampleMapForIntensityCheck.put( "-" + sampleId, estSampleTreeMap );
		}

		updateEstSamples( masterDatabase, mFaAnionsList, baMap, baSet, groupMapForIntensityCheck, groupSampleMapForIntensityCheck );

		HashMap<TreeItem< BARow >, Float> ratioCheckMap = new HashMap<>(  );

		HashMap<Float, Float[]> minMaxXml = new HashMap<>(  );

		collectIntensityRatio( baSet, baMap, samples, masterDatabase, 1.25f, groupSampleMapForIntensityCheck, ratioCheckMap, minMaxXml );

		float total = minMaxXml.size() * samples.size();

		TreeItem< BARow >[] keys = ratioCheckMap.keySet().toArray( new TreeItem[ 0 ] );

		Arrays.sort( keys, Comparator.comparing( o -> o.getValue().getMassString() ) );

		for( TreeItem< BARow > baRow : keys ) {
			final Double priMass = baRow.getValue().getMass();
			final Float count = ratioCheckMap.get(baRow);

			String fais = null;

			for( TreeItem< BARow > child : baRow.getChildren() )
			{
				BARow childBaRow = child.getValue();

				if ( null == fais )
				{
					fais = childBaRow.getMassString() + ",";
				}
				else
				{
					fais += childBaRow.getMassString();
				}
			}

			if( count / total < 0.5) {
				System.err.println( priMass + "(" + fais + ") = " + count / total );
				species.remove( baRow );
			} else {
				System.out.println( priMass + "(" + fais + ") = " + count / total );
			}
		}
	}

	public static void updateEstSamples( MasterDatabase masterDatabase, ObservableList< FAAnion > mFaAnionsList, HashMap< TreeItem< BARow >, BA > baMap, Collection< TreeItem< BARow > > species, TreeMap< String, ArrayList< String > > groupMap, TreeMap< String, TreeMap< String, EstSample > > groupSampleTreeMap )
	{
		// Update isomer based on other samples' isomer
		for ( String groupKey : groupMap.keySet() )
		{
			for ( TreeItem< BARow > baRowTreeItem : species )
			{
				final String specieName = baRowTreeItem.getValue().getTitle();

				BA fa = baMap.get( baRowTreeItem );
				Set<Float> keySet = fa.getKeys();
				final String clazz = specieName.split( " " )[ 0 ];

				HashSet<String> faKeySet = new HashSet<>();

				for ( TreeItem< BARow > faTreeItem : baRowTreeItem.getChildren() )
					faKeySet.add( specieName + "-" + faTreeItem.getValue().getMassString() );

				for ( TreeItem< BARow > faTreeItem : baRowTreeItem.getChildren() )
				{
					final String faMz = faTreeItem.getValue().getMassString();
					final String key = specieName + "-" + faMz;

					final int db = faTreeItem.getValue().getDb();

					if( db > 2 )
					{
						for ( Float ce : keySet )
						{
							int total = groupMap.get( groupKey ).size();
							int cnt = 0;
							Float min = Float.MAX_VALUE, max = 0f;

							for ( String sampleId : groupMap.get( groupKey ) )
							{
								final TreeMap< String, EstSample > estSampleTreeMap = groupSampleTreeMap.get( groupKey + "-" + sampleId );

								if ( !estSampleTreeMap.get( key + ce ).getCorrectedCOI().equals( 0f ) )
								{
									EstSample estSample = estSampleTreeMap.get( key + ce );
									min = Float.min( min, estSample.getIsomer().getMinimum() );
									max = Float.max( max, estSample.getIsomer().getMaximum() );
									cnt++;
								}
							}

							if ( total != cnt && ((float) cnt / total) > 0.5f )
							{
								ArrayList< TreeMap< String, EstSample > > estSampleList = new ArrayList< TreeMap< String, EstSample > >(  );
								final double mass = Double.parseDouble( faMz );
								final int carbon = faTreeItem.getValue().getCarbon();

								for ( String sampleId : groupMap.get( groupKey ) )
								{
									final TreeMap< String, EstSample > estSampleTreeMap = groupSampleTreeMap.get( groupKey + "-" + sampleId );
									estSampleList.add( estSampleTreeMap );

									if ( estSampleTreeMap.get( key + ce ).getCorrectedCOI().equals( 0f ) )
										estSampleTreeMap.get( key + ce ).setIsomer( Range.between( min, max ) );
								}

//                        updateSecondaryDBCF( masterDatabase, mFaAnionsList, estSampleList, keySet, clazz, key, mass, carbon, db );

//                        for ( String sampleId : groupMap.get( groupKey ) )
//                        {
//                           final TreeMap< String, EstSample > estSampleTreeMap = groupSampleTreeMap.get( groupKey + "-" + sampleId );
//
//                           if ( faKeySet.size() > 1 && estSampleTreeMap.get( key + ce ).getCorrectedCOI().equals( 0f ) )
//                              estimate2ndPosition( estSampleTreeMap, clazz, keySet, faKeySet );
//                        }
                     }
						}
					}
				}
			}
		}
	}

	private void setSize( double value )
	{
		sizeProperty.set( value );
	}

	private void setProgress( double value )
	{
		progressProperty.set( value / sizeProperty.doubleValue() );
	}

	static Range<Float> addRange( Range<Float> a, Range<Float> b )
	{
		return Range.between( a.getMinimum() + b.getMinimum(), a.getMaximum() + b.getMaximum() );
	}

	private void refreshQuantTable( TreeMap< String, ArrayList< String > > groupMap, Map< String, Float > refPRIMap, Quant.Option processOption, Quant.Output outputOption, EnumSet< Quant.AdditionalOption > additionalOptions )
	{
		final boolean noCorrection = additionalOptions.contains( Quant.AdditionalOption.NoCorrection );
		final boolean groupOnly = additionalOptions.contains( Quant.AdditionalOption.GroupOnly );
		sampleQuantTableView.getItems().clear();
		priMap.clear();

		boolean bNCE = !additionalOptions.contains( Quant.AdditionalOption.SummarizeNCE );
		//boolean applyTXCFinSummary = additionalOptions.contains( Quant.AdditionalOption.ApplyTXCFinSummary );

		HashMap<String, TreeMap<String, Float> > priSumMap = new HashMap<>();
		HashMap<String, TreeMap<String, Float> > priRefSumMap = new HashMap<>();

		HashMap<String, TreeMap<String, Range<Float> > > priGroupSumMap = new HashMap<>();
		HashMap<String, TreeMap<String, Range<Float> > > priGroupRefSumMap = new HashMap<>();

		for( String sampleId : sampleData.keySet() )
		{
			LinkedHashMap< SampleEstimation.FASample, SampleEstimation.FASample[] > map = sampleData.get( sampleId );

			if( sampleId.startsWith( "Group-" ) )
			{
				TreeMap<String, Range<Float> > sum = new TreeMap<>();
            TreeMap<String, Range<Float> > refSum = new TreeMap<>();
            TreeMap<String, Float> qtyValue = new TreeMap<>();

				for ( SampleEstimation.FASample pri : map.keySet() )
				{
					String priKey = pri.getKey();

               final String clazz = priKey.split( " " )[ 0 ];

               if (! sum.containsKey( clazz ) ) {
                  sum.put(clazz, Range.between( 0f, 0f ));
                  refSum.put(clazz, Range.between( 0f, 0f ));
                  qtyValue.put(clazz, 0f);
               }

					if(priKey.startsWith( "PC O-" )) {
						priKey = priKey.replace( "PC O-", "PCO" );
					} else if(priKey.startsWith( "PE O-" )) {
						priKey = priKey.replace( "PE O-", "PEO" );
					}

					if ( refPRIMap.containsKey( priKey ) )
					{
                  qtyValue.put(clazz, qtyValue.get(clazz) + refPRIMap.get( priKey ));
						if(refPRIMap.get( priKey ) != 0f)
                  {
                     refSum.put(clazz, addRange( refSum.get(clazz), pri.getIntensityRange() ));
                  }

						if ( !additionalOptions.contains( Quant.AdditionalOption.RemoveReference ) ) {
                     sum.put(clazz, addRange( sum.get(clazz), pri.getIntensityRange() ) );
                  }
					}
					else
					{
                  sum.put(clazz, addRange( sum.get(clazz), pri.getIntensityRange() ));
					}
				}

				priGroupSumMap.put( sampleId, sum );

            for ( String clazz : qtyValue.keySet() ) {
               Range<Float> range = refSum.get(clazz);
               Float refValue = qtyValue.get(clazz);
               refSum.put( clazz,  Range.between( range.getMinimum() / refValue, range.getMaximum() / refValue ) );
            }

				priGroupRefSumMap.put( sampleId, refSum );
			}
			else
			{
				TreeMap<String, Float> sum = new TreeMap<>();
            TreeMap<String, Float> refSum = new TreeMap<>();
            TreeMap<String, Float> qtyValue = new TreeMap<>();

				for ( SampleEstimation.FASample pri : map.keySet() )
				{
					String priKey = pri.getKey();

               final String clazz = priKey.split( " " )[ 0 ];

               if (! sum.containsKey( clazz ) ) {
                  sum.put(clazz, 0f);
                  refSum.put(clazz, 0f);
                  qtyValue.put(clazz, 0f);
               }

               if(priKey.startsWith( "PC O-" )) {
						priKey = priKey.replace( "PC O-", "PCO" );
					} else if(priKey.startsWith( "PE O-" )) {
						priKey = priKey.replace( "PE O-", "PEO" );
					}

					if ( refPRIMap.containsKey( priKey ) )
					{
						Float qty = refPRIMap.get( priKey );
                  qtyValue.put(clazz, qtyValue.get(clazz) + qty);

						if(qty != 0f)
                     refSum.put(clazz, refSum.get(clazz) + pri.getIntensity());

						if ( !additionalOptions.contains( Quant.AdditionalOption.RemoveReference ) )
                     sum.put(clazz, sum.get(clazz) + pri.getIntensity());
					}
					else
					{
                  sum.put(clazz, sum.get(clazz) + pri.getIntensity());
					}
				}

				priSumMap.put( sampleId, sum );

            for( String clazz: qtyValue.keySet() ) {
               refSum.put(clazz, refSum.get(clazz) / qtyValue.get(clazz));
            }

				priRefSumMap.put( sampleId, refSum );
			}
		}

		HashMap<String, Float> priValueMap = new HashMap<>();
		HashMap<String, SampleEstimation.FASample[]> priSampleMap = new HashMap<>();
		HashMap<String, HashMap<String, SampleEstimation.FASample> > faSampleMap = new HashMap<>();
		HashMap<String, LinkedHashSet<String> > speciesNameMap = new HashMap<>();

		// Holds "Sum" part information in MSpecie
		HashMap<String, TreeMap<Float, Range<Float> > > faiSumMap = new HashMap<>(  );

//		System.out.println( Arrays.toString( sampleData.keySet().toArray() ) );

		for( String sampleId : sampleData.keySet() )
		{
			LinkedHashMap< SampleEstimation.FASample, SampleEstimation.FASample[] > map = sampleData.get( sampleId );

			for( SampleEstimation.FASample pri : map.keySet() )
			{
				String priKey = pri.getFullKey();
				priMap.put( priKey, pri );

//				System.out.println(priKey + sampleId);
            final String clazz = priKey.split( " " )[ 0 ];

				if( !sampleId.startsWith( "Group-" ) )
				{
					switch ( processOption )
					{
						case Intensity:
							priValueMap.put( priKey + sampleId, pri.getIntensity() );
							break;
						case Profile:
							priValueMap.put( priKey + sampleId, pri.getNormIntensity( priSumMap.get( sampleId ).get(clazz), 100f ) );
							break;
						case Quantity:
							priValueMap.put( priKey + sampleId, pri.getNormIntensity( priRefSumMap.get( sampleId ).get(clazz), 1f ) );
							break;
					}
				}

				priSampleMap.put( priKey + sampleId, map.get( pri ) );

				faSampleMap.put( priKey + sampleId, new HashMap<>(  ) );

				if( !speciesNameMap.containsKey( priKey ) )
					speciesNameMap.put( priKey, new LinkedHashSet<>(  ) );

				SampleEstimation.FASample[] samples = map.get( pri );

				faiSumMap.put( priKey + sampleId, new TreeMap<>(  ));

				SampleEstimation.FASample sumSample = null;
				if( outputOption == Quant.Output.All || outputOption == Quant.Output.Sum )
				{
					speciesNameMap.get( priKey ).add( "Sum" );
					sumSample = new SampleEstimation.FASample( "Sum" );
				}

//				if(priKey.equals( "PE 37:4:0 [269.25, 303.23]" ))
//				{
//					System.out.println( priKey );
//				}

				for( Float ce : pri.getNCE() )
				{
//					System.out.println(ce);
					Float min = 0f, max = 0f;

					for( SampleEstimation.FASample sample : samples)
					{
						Range<Float> intensity;

//						System.out.println("\t" + sample.getKey() + "<->" + sample.getMass() + ":" + compareSN( sample.getKey() ));

						if(noCorrection)
							intensity = Range.between( sample.get(ce).getNoCorrectedFAI(), sample.get(ce).getNoCorrectedFAI() );
						else
							intensity = sample.get(ce).getFai();

						if(sample.getKey().contains( "/" )) {
							if( compareSN( sample.getKey() ) == 0 ) {
								if( compareSYMIso( sample.getKey(), samples ) > 0 ) {
									min += intensity.getMinimum();
									max += intensity.getMaximum();
								}
								else
								{
									min += intensity.getMaximum();
									max += intensity.getMinimum();
								}
							}
							else if( compareSN( sample.getKey() ) == 1 )
							{
								min += intensity.getMaximum();
								max += intensity.getMinimum();
							}
							else
							{
								min += intensity.getMinimum();
								max += intensity.getMaximum();
							}
						}
						else
						{
							min += intensity.getMinimum();
							max += intensity.getMaximum();
						}
//						System.out.println(intensity);
					}

					Range<Float> sum = Range.between( min, max );

					faiSumMap.get( priKey + sampleId ).put( ce, sum );
					if( outputOption == Quant.Output.All || outputOption == Quant.Output.Sum )
					{
						sumSample.add( ce, sum, sum, sum, 0f, 0f, null, sum );
					}
				}

				// Find the clean compound and optimize NCE with TX function


				if( outputOption == Quant.Output.All || outputOption == Quant.Output.Sum )
					faSampleMap.get( priKey + sampleId ).put( "Sum", sumSample );

				if( outputOption == Quant.Output.All || outputOption == Quant.Output.Mspecies )
					for( SampleEstimation.FASample sample : samples )
					{
						if( null != sample )
						{
							speciesNameMap.get( priKey ).add( sample.getKey() );

							// e.g. sample.getKey() -> PC 21:0/22:6
							faSampleMap.get( priKey + sampleId ).put( sample.getKey(), sample );
						}
					}
			}
		}

		// Calculate Group PRI values again based on the sample data
		HashMap<String, String> priValueStringMap = new HashMap<>();

		for( String groupKey : groupMap.keySet() )
		{
			for ( String species : priMap.keySet() )
			{
				SampleEstimation.FASample pri = priMap.get( species );

				String priKey = pri.getFullKey();
				String groupId = priKey + groupKey;
				float min = Float.MAX_VALUE;
				float max = -Float.MAX_VALUE;
				for( String sampleId : groupMap.get(groupKey) )
				{
					Float value = priValueMap.get(priKey + sampleId);
					priValueStringMap.put(priKey + sampleId, value.toString());
					min = Math.min(value , min );
					max = Math.max(value, max );
				}

				priValueStringMap.put(groupId, Range.between( min, max ).toString());
			}
		}

		// Sum the all FAI sum by iterating all the Samples
		HashMap<String, HashMap<String, TreeMap<Float, Range<Float> > > > faiSum = new HashMap<>(  );

		for ( String sampleId : sampleData.keySet() )
		{
			faiSum.put( sampleId, new HashMap<>() );

         HashMap<String, Float> qtyValueMap = new HashMap<>();
         HashMap<String, TreeMap< Float, Float > > minMap = new HashMap<>();
         HashMap<String, TreeMap< Float, Float > > maxMap = new HashMap<>();
         HashMap<String, TreeMap<Float, Range<Float> > > faiSumHashMap = faiSum.get( sampleId );

			for ( String species : priMap.keySet() )
			{
				SampleEstimation.FASample pri = priMap.get( species );

//				for ( Float ce : pri.getNCE() )
//				{
//					String priKey = pri.getFullKey();
//					System.out.println(species + sampleId + ":" + ce);
//					for(String fragmentName : speciesNameMap.get( priKey ))
//					{
//						System.out.println("\t" + fragmentName);
//					}
//				}

				Float qty = 0f;
				String priKey = pri.getKey();

            final String clazz = priKey.split( " " )[ 0 ];

//            System.out.println(clazz);

            if(!faiSumHashMap.containsKey( clazz ))
            {
               faiSumHashMap.put( clazz, new TreeMap<>() );
               minMap.put( clazz, new TreeMap<>() );
               maxMap.put( clazz, new TreeMap<>() );
            }

				if(priKey.startsWith( "PC O-" )) {
					priKey = priKey.replace( "PC O-", "PCO" );
				} else if(priKey.startsWith( "PE O-" )) {
					priKey = priKey.replace( "PE O-", "PEO" );
				}

				if( refPRIMap.containsKey( priKey ) )
				{
               if( !qtyValueMap.containsKey( clazz ) )
                  qtyValueMap.put(clazz, 0f);

               qty = refPRIMap.get( priKey );
               qtyValueMap.put(clazz, qtyValueMap.get(clazz) + qty);
				}

				if( !(processOption.equals( Quant.Option.Quantity ) && qty == 0f) )
				{
					for ( Float ce : pri.getNCE() )
					{
						if ( !minMap.get(clazz).containsKey( ce ) )
						{
							minMap.get(clazz).put( ce, 0f );
							maxMap.get(clazz).put( ce, 0f );
						}

						Range< Float > sum = faiSumMap.get( species + sampleId ).get( ce );
						minMap.get(clazz).put( ce, minMap.get(clazz).get( ce ) + sum.getMinimum() );
						maxMap.get(clazz).put( ce, maxMap.get(clazz).get( ce ) + sum.getMaximum() );
					}
				}

//				if( processOption.equals( Quant.Option.Quantity ) && qty != 0f)
//				{
//					for ( Float ce : pri.getNCE() )
//					{
//						if ( !minMap.containsKey( ce ) )
//						{
//							minMap.put( ce, 0f );
//							maxMap.put( ce, 0f );
//						}
//
//						Range< Float > sum = faiSumMap.get( species + sampleId ).get( ce );
//						minMap.put( ce, minMap.get( ce ) + sum.getMinimum() );
//						maxMap.put( ce, maxMap.get( ce ) + sum.getMaximum() );
//					}
//				} else {
//					for ( Float ce : pri.getNCE() )
//					{
//						if ( !minMap.containsKey( ce ) )
//						{
//							minMap.put( ce, 0f );
//							maxMap.put( ce, 0f );
//						}
//
//						Range< Float > sum = faiSumMap.get( species + sampleId ).get( ce );
//						minMap.put( ce, minMap.get( ce ) + sum.getMinimum() );
//						maxMap.put( ce, maxMap.get( ce ) + sum.getMaximum() );
//					}
//				}
			}

         for ( String clazz : faiSumHashMap.keySet() ) {
            for ( Float ce : minMap.get(clazz).keySet() )
            {
               Range< Float > sum = Range.between( minMap.get(clazz).get( ce ), maxMap.get(clazz).get( ce ) );

               if( processOption.equals( Quant.Option.Quantity ) )
               {
                  float qtyValue = qtyValueMap.get(clazz);
//                  System.out.println(qtyValue);

                  sum = qtyValue == 0f ? Range.between( Float.NaN, Float.NaN ) :
                          Range.between( minMap.get(clazz).get( ce ) / qtyValue, maxMap.get(clazz).get( ce ) / qtyValue );
               }

               faiSum.get(sampleId).get( clazz ).put( ce, sum );
               //				System.out.println( ce + " -> " + sum );
            }
         }
		}

		// Group summary
		// Update the summary fields depending on the current calculated values
		for(String group : groupMap.keySet())
		{
			for( String species : priMap.keySet() )
			{
				SampleEstimation.FASample pri = priMap.get( species );
				String priKey = pri.getFullKey();

            final String clazz = priKey.split( " " )[ 0 ];

				for( String fragmentName : speciesNameMap.get( priKey ) )
				{
//						System.out.println(fragmentName);
					for ( Float ce : pri.getNCE() )
					{
						float min = Float.MAX_VALUE;
						float max = -Float.MAX_VALUE;

						//for(String sample : sampleData.keySet())
						for( String sample : groupMap.get(group) )
						{
//							if(sample.startsWith( "Group-" )) continue;
							switch ( processOption )
							{
								case Intensity:
									Range< Float > intensity = null;
									if ( fragmentName.equals( "Sum" ) )
									{
										intensity = faSampleMap.get( priKey + sample ).get( fragmentName ).get( ce ).getFai();
									}
									else
									{
										if(faSampleMap.get( priKey + sample ).containsKey( fragmentName ))
										{
											intensity = faSampleMap.get( priKey + sample ).get( fragmentName ).get( ce ).getFai(noCorrection);

										}
									}

									if(null != intensity)
									{
										faSampleMap.get( priKey + sample ).get( fragmentName ).get( ce ).setViewFAI( intensity );
										min = Math.min( intensity.getMinimum(), min );
										max = Math.max( intensity.getMaximum(), max );
									}
									break;
								case Profile:
									Range< Float > profile = null;
									if ( fragmentName.equals( "Sum" ) )
									{
										Range< Float > fai = faSampleMap.get( priKey + sample ).get( fragmentName ).get( ce ).getFai();
										Range< Float > sum = faiSum.get( sample ).get(clazz).get( ce );
										profile = Range.between( fai.getMinimum() / sum.getMaximum() * 100, fai.getMaximum() / sum.getMinimum() * 100 );
									}
									else
									{
										if(faSampleMap.get( priKey + sample ).containsKey( fragmentName ))
										{
											profile = faSampleMap.get( priKey + sample ).get( fragmentName ).get( ce ).getNormedFAI(noCorrection);
										}
									}

									if( null != profile )
									{
										faSampleMap.get( priKey + sample ).get( fragmentName ).get( ce ).setViewFAI( profile );
										min = Math.min( profile.getMinimum(), min );
										max = Math.max( profile.getMaximum(), max );
									}
									break;
								case Quantity:
									Range< Float > quantity = null;
									if ( fragmentName.equals( "Sum" ) )
									{
										Range< Float > fai = faSampleMap.get( priKey + sample ).get( fragmentName ).get( ce ).getFai();
										Range< Float > sum = faiSum.get( sample ).get(clazz).get( ce );

										quantity = sum == null ? Range.between( Float.NaN, Float.NaN ) : Range.between( fai.getMinimum() / sum.getMaximum(), fai.getMaximum() / sum.getMinimum() );
									}
									else
									{
										if(faSampleMap.get( priKey + sample ).containsKey( fragmentName ))
										{
											Range< Float > sum = faiSum.get( sample ).get( clazz ).get( ce );
											quantity = sum == null ? Range.between( Float.NaN, Float.NaN ) : faSampleMap.get( priKey + sample ).get( fragmentName ).get( ce ).getRatio(noCorrection, sum);
										}
									}

									if( null != quantity )
									{
										faSampleMap.get( priKey + sample ).get( fragmentName ).get( ce ).setViewFAI( quantity );
										min = Math.min( quantity.getMinimum(), min );
										max = Math.max( quantity.getMaximum(), max );
									}
									break;
							}
						}

//						System.out.println(priKey + group);
//						System.out.println( Arrays.toString( faSampleMap.keySet().toArray() ) );

						if( faSampleMap.get( priKey + group ).containsKey( fragmentName ) && faSampleMap.get( priKey + group ).get( fragmentName ).contains( ce ) )
							faSampleMap.get( priKey + group ).get( fragmentName ).get( ce ).setViewFAI( Range.between( min, max ) );
					}
				}
			}

			if( groupOnly )
			{
				Set<String> groupKeySet = groupMap.keySet();
				sampleData.keySet().retainAll( groupKeySet );
			}
		}

		ArrayList<String[]> data = new ArrayList<>(  );
		LinkedHashMap<Integer, String> tableIndexToKeyMap = new LinkedHashMap<>(  );
		int count = -1;

		for( String species : priMap.keySet() )
		{
			String specieKey = species.substring( 0, species.indexOf( '[' ) - 1 );
			if(specieKey.startsWith( "PC O-" )) {
				specieKey = specieKey.replace( "PC O-", "PCO" );
			} else if(specieKey.startsWith( "PE O-" )) {
				specieKey = specieKey.replace( "PE O-", "PEO" );
			}

			if( additionalOptions.contains( Quant.AdditionalOption.RemoveReference ) &&
					refPRIMap.containsKey( specieKey ) )
				continue;

			ArrayList<String> row = new ArrayList<>(  );
			SampleEstimation.FASample pri = priMap.get( species );
			String priKey = pri.getFullKey();
			row.add( pri.getKey() );

			// PRI
			sampleData.keySet().forEach( c -> row.add( priValueStringMap.get( species + c ).toString() ) );
			ArrayList<String[]> faData = new ArrayList<>(  );

			// FAI
			for( String fragmentName : speciesNameMap.get( priKey ) )
			{
				boolean bFirst = true;

				if( bNCE )
				{
					for ( Float ce : pri.getNCE() )
					{
						ArrayList< String > farow = new ArrayList<>();
						if ( bFirst )
						{
							farow.add( fragmentName );
							bFirst = false;
						}
						else
							farow.add( "" );

						farow.add( ce.toString() );

						count++;
						for ( String sampleId : sampleData.keySet() )
						{
							if(!fragmentName.equals( "Sum" ) &&
									faSampleMap.get( priKey + sampleId ).containsKey( fragmentName ))
							{
//								System.out.println(count + ":" + pri.getKey() + '-' + faSampleMap.get( priKey + sampleId ).get( fragmentName ).getMass() + ce );
								tableIndexToKeyMap.put( count, pri.getKey() + '-' + faSampleMap.get( priKey + sampleId ).get( fragmentName ).getMass() + ce );
							}

							if(faSampleMap.get( priKey + sampleId ).containsKey( fragmentName ))
							{
								farow.add( faSampleMap.get( priKey + sampleId ).get( fragmentName ).get( ce ).getViewFAI().toString() );
							}
							else farow.add( "" );
						}
						faData.add( farow.toArray( new String[] { } ) );
					}
				}
				else
				{
					ArrayList< String > farow = new ArrayList<>();
					farow.add( fragmentName );
					count++;

					for (String sampleId : sampleData.keySet() )
					{
						Float min = Float.MAX_VALUE, max = -Float.MAX_VALUE;

						if ( noCorrection ) {
							Float ce = (Float) ((TreeSet) pri.getNCE()).last();
							Range<Float> dat = null;
							if(faSampleMap.get( priKey + sampleId ).containsKey( fragmentName ))
								dat = faSampleMap.get( priKey + sampleId ).get( fragmentName ).get( ce ).getViewFAI();

							if( dat != null )
							{
								min = Float.min( min, dat.getMinimum() );
								max = Float.max( max, dat.getMaximum() );
							}
						}
						else
						{
							for ( Float ce : pri.getNCE() )
							{
								//							Float txcf = getTxCF( pri.getMass(), ce );
								Range<Float> dat = null;
								if(faSampleMap.get( priKey + sampleId ).containsKey( fragmentName ))
									dat = faSampleMap.get( priKey + sampleId ).get( fragmentName ).get( ce ).getViewFAI();

								if( dat != null )
								{
									min = Float.min( min, dat.getMinimum() );
									max = Float.max( max, dat.getMaximum() );
								}
							}
						}

						if( min.equals( Float.MAX_VALUE ) || max.equals( -Float.MAX_VALUE ) )
							farow.add( "" );
						else
							farow.add( Range.between( min, max ).toString() );
					}

					faData.add( farow.toArray( new String[] { } ) );
				}
			}

			String[] first = faData.get(0);
			faData.remove( 0 );

			int size = row.size();

			for( String str : first )
			{
				row.add( str );
			}

			data.add( row.toArray( new String[] { } ) );

			for( String[] farow : faData )
			{
				String[] str = new String[row.size()];

				for( int i = 0; i < row.size(); i++ )
				{
					if( i >= size )
						str[i] = farow[i - size];
					else
						str[i] = "";
				}

				data.add( str );
			}
		}

		sampleQuantTableView = TableViewUtil.createDataView( getHeaders( bNCE ) );
		sampleQuantTableView.getItems().addAll( data );
		Platform.runLater( () -> setCenter( sampleQuantTableView ) );

		TableViewUtil.addContextMenu( sampleQuantTableView, new EventHandler< MouseEvent >()
		{
			@Override public void handle( MouseEvent event )
			{
				if ( event.getClickCount() > 1 )
				{
					int idx = sampleQuantTableView.getSelectionModel().getFocusedIndex();

					for ( String sampleId : sampleIdSet )
					{
						String key = sampleId + "-" + tableIndexToKeyMap.get( idx );

						if ( estSampleTreeMap.containsKey( key ) )
						{
							EstSample sample = estSampleTreeMap.get( key );
							Event.fireEvent( sampleValidationPane, new ProcessEvent( ProcessEvent.VALIDATION_SAMPLE, sample ) );
							//							System.out.println( estSampleTreeMap.get( key ) );
						}
					}
				}
			}
		} );

		sampleQuantTableView.skinProperty().addListener( new ChangeListener< Skin< ? > >()
		{
			@Override public void changed( ObservableValue< ? extends Skin< ? > > observable, Skin< ? > oldValue, Skin< ? > newValue )
			{
				Platform.runLater( () -> autoFitTable( sampleQuantTableView ) );
			}
		} );
	}

	private static int compareSN( String sn1sn2 )
	{
//				System.out.println(sn1sn2);
		String snString = sn1sn2.replaceAll( "\\(?[0-9]+z\\)|[^0-9]+", "" );

//		System.out.println(snString);
		String sn1 = snString.substring( 0, 3 );
		String sn2 = snString.substring( 3, 6 );

		return Integer.compare( Integer.parseInt( sn1 ), Integer.parseInt( sn2 ) );
	}

	private static int compareSYMIso( String sn1sn2, SampleEstimation.FASample[] samples )
	{
		int subject = extractIso( sn1sn2 );

		if(subject != 0)
		{
			int targetMax = 0;
			for( SampleEstimation.FASample sample : samples )
			{
				targetMax = Math.max( targetMax, extractIso( sample.getKey() ) );
			}

			if(subject >= targetMax)
				return -1;
			else
				return 1;
		}
		else
		{
			return 1;
		}
	}

	private static Pattern extractIsoPattern = Pattern.compile("[^0-9\\s]+\\(?([0-9]+)z\\)");

	private static int extractIso( String sn1sn2 )
	{
		Matcher matcher = extractIsoPattern.matcher(sn1sn2);

		if(matcher.find())
		{
			return Integer.parseInt( matcher.group( 1 ) );
		}
		else
		{
			return 0;
		}
	}

	public String[] getHeaders( boolean bNCE )
	{
		ArrayList<String> list = new ArrayList<>(  );
		list.add( "Species" );

		sampleData.keySet().forEach( c -> list.add( "PRI_" + c ) );

		list.add( "Mspecies" );

		if(bNCE)
			list.add( "NCE" );

		sampleData.keySet().forEach( c -> list.add( "FAI.FC_" + c ) );

//		System.out.println(list);

		return list.toArray( new String[]{} );
	}
}
