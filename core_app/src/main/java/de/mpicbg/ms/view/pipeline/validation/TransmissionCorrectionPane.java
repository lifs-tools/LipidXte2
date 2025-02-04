package de.mpicbg.ms.view.pipeline.validation;

import de.mpicbg.ms.db.MasterDatabase;
import de.mpicbg.ms.model.SampleEstimation;
import de.mpicbg.ms.model.data.EstSample;
import de.mpicbg.ms.model.event.ProcessEvent;
import de.mpicbg.ms.model.data.BA;
import de.mpicbg.ms.model.data.BARow;
import de.mpicbg.ms.model.data.FAAnion;
import de.mpicbg.ms.model.fitter.ExponentialDecayFitter;
import de.mpicbg.ms.model.fitter.ExponentialDecayFunction;
import de.mpicbg.ms.model.fitter.SimpleExponentialFitter;
import de.mpicbg.ms.model.fitter.SimpleExponentialFunction;
import de.mpicbg.ms.util.Data;
import de.mpicbg.ms.util.Validation;
import de.mpicbg.ms.view.chart.HoveredNode;
import de.mpicbg.ms.view.pane.component.LabeledPane;
import de.mpicbg.ms.view.treecell.SelectiveCheckBoxTreeCell;
import de.mpicbg.ms.util.TableViewUtil;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.analysis.differentiation.UnivariateDifferentiableFunction;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.controlsfx.control.MasterDetailPane;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static de.mpicbg.ms.model.SampleEstimation.createEstSamples;
import static de.mpicbg.ms.model.SampleEstimation.processEstSample;

/**
 * Author: HongKee Moon (moon@mpi-cbg.de), Scientific Computing Facility
 * Organization: MPI-CBG Dresden
 * Date: May 2017
 */
public class TransmissionCorrectionPane extends MasterDetailPane
{
	final private TableView<String[]> txCorrectionTableView;
	final HashMap<String, List<WeightedObservedPoint>> observedPointsMap = new HashMap<>(  );

	// Input
	public TransmissionCorrectionPane( LineChart< Number, Number > chart )
	{
		txCorrectionTableView = TableViewUtil.createDataView(new String[]{ "CE", "Sample", "PRI", "TX.CF" });
		TableViewUtil.addContextMenu( txCorrectionTableView );

		setMasterNode( txCorrectionTableView );
		setDividerPosition( 0.7d );

		TreeItem sampleTreeRoot = new TreeItem<>( "Root" );
		sampleTreeRoot.setExpanded( true );

		TreeView<String> sampleTreeView = new TreeView( sampleTreeRoot );
		sampleTreeView.setShowRoot( false );
		sampleTreeView.setCellFactory( SelectiveCheckBoxTreeCell.<String>forTreeView() );

		setDetailNode( new LabeledPane( "Sample", sampleTreeView ) );

		addEventHandler( ProcessEvent.VALIDATION_RESET_TX_CORRECTION, new EventHandler< ProcessEvent >()
		{
			@Override public void handle( ProcessEvent event )
			{
				uncheckNode( sampleTreeRoot );
//				int n = chart.getData().size();
//				chart.getData().remove( 0, n - 1 );
				sampleTreeRoot.getChildren().clear();
				txCorrectionTableView.getItems().clear();
			}
		} );

		addEventHandler( ProcessEvent.VALIDATION_TX_CORRECTION, new EventHandler< ProcessEvent >()
		{
			@Override public void handle( ProcessEvent event )
			{
				Object[] params = event.getParam();

				TreeMap< String, ArrayList<String> > groupMap = (TreeMap< String, ArrayList<String> >) params[0];
//				ArrayList<String> samples = (ArrayList) params[0];

				ObservableList<TreeItem<BARow>> species = (ObservableList<TreeItem<BARow>>) params[1];

				HashMap<TreeItem< BARow >, BA> baMap = (HashMap<TreeItem< BARow >, BA>) params[2];

				Map<String, Float> refPRIMap = (Map<String, Float>) params[3];

				ObservableList< FAAnion > mFaAnionsList = (ObservableList< FAAnion >) params[4];

				HashSet<Double> validMasses = new HashSet<>(  );

				species.forEach( c ->
						{
							//System.out.println(	baMap.get( c ).getMass() )
							if ( refPRIMap.containsKey( c.getValue().getTitle() ) )
								validMasses.add( c.getValue().getMass() );
//								System.out.println( "Ref--> " + c.getValue().getTitle() );
//							else
//								System.out.println( c.getValue().getTitle() );
						}
				);

//				HashSet<Double> removeMasses = new HashSet<>(  );
//
//				species.forEach( c ->
//						{
//
//							long count = species.stream().filter( d -> c.getValue().getTitle().equals( d.getValue().getTitle() )).count();
//							if( count > 1 )
//							{
//								System.out.println( c.getValue().getMass() + " will be ignored.");
//								removeMasses.add( c.getValue().getMass() );
//							}
//						}
//				);

				BA ba = baMap.values().stream().findFirst().get();

				TreeSet<Float> ceSet = new TreeSet<>( ba.getKeys() );

				final MasterDatabase masterDatabase = new MasterDatabase();
				masterDatabase.connect();

				for ( String groupKey : groupMap.keySet() )
				{
					TreeMap<Float, List< WeightedObservedPoint >> refMap = new TreeMap<>(  );
					TreeMap<Float, List< WeightedObservedPoint >> obsMap = new TreeMap<>(  );

					for ( String sampleId : groupMap.get( groupKey ) )
					{
						final TreeMap< String, EstSample > estSampleTreeMap = createEstSamples(masterDatabase, groupKey, sampleId, species, baMap, mFaAnionsList, true);

						processEstSample( sampleId, species, baMap, refPRIMap, mFaAnionsList, false, false, estSampleTreeMap,
								( priReferenceMap, computedPriFragmentMap, faSampleMap, sampleTreeMap ) ->
								{
								   for ( String className : priReferenceMap.keySet() )
								   {
								   	for ( Float ce : priReferenceMap.get(className).keySet() )
									   {
										   List< WeightedObservedPoint > ref = new ArrayList<>();
										   List< WeightedObservedPoint > obs = new ArrayList<>();

										   for ( Double mass : computedPriFragmentMap.get(className).keySet() )
										   {
											   Float txcf = computedPriFragmentMap.get(className).get( mass ).getCF( ce );

											   if ( validMasses.contains( mass ) )
												   obs.add( new WeightedObservedPoint( 1, mass, txcf ) );

											   if ( !refMap.containsKey( ce ) && validMasses.contains( mass ) )
												   ref.add( new WeightedObservedPoint( 1, mass, txcf ) );
										   }

										   if ( !refMap.containsKey( ce ) )
											   refMap.put( ce, ref );

										   if ( !obsMap.containsKey( ce ) )
											   obsMap.put( ce, obs );
										   else
											   obsMap.get( ce ).addAll( obs );
									   }
									}
								} );
					}


					CheckBoxTreeItem<String> groupTreeItem = new CheckBoxTreeItem<>( groupKey );

					groupTreeItem.selectedProperty().addListener( new ChangeListener< Boolean >()
					{
						@Override public void changed( ObservableValue< ? extends Boolean > observable, Boolean oldValue, Boolean newValue )
						{
							groupTreeItem.getChildren().forEach( c -> ((CheckBoxTreeItem) c).setSelected( newValue ) );
						}
					} );

					for(Float ce : ceSet)
					{
						CheckBoxTreeItem< String > ceTreeItem = new CheckBoxTreeItem<>( ce + "" );

						ceTreeItem.selectedProperty().addListener( new ChangeListener< Boolean >()
						{
							@Override public void changed( ObservableValue< ? extends Boolean > observable, Boolean oldValue, Boolean newValue )
							{
								final String seriesName = groupKey + ":" + ce;

								TreeSet<Double> massSet = new TreeSet<>(  );

								if ( newValue )
								{
									XYChart.Series series = new XYChart.Series();
									series.setName( seriesName );

									List< WeightedObservedPoint > ref = refMap.get(ce);
									List< WeightedObservedPoint > obs = obsMap.get(ce);

									obs.sort( Comparator.comparingInt( o -> ( int ) o.getX() ) );

									for ( WeightedObservedPoint point : obs )
									{
										Double mass = point.getX();
										Double txcf = point.getY();
										XYChart.Data node = new XYChart.Data( mass, txcf );
										node.setNode( new HoveredNode( txcf ) );
										series.getData().add( node );

										txCorrectionTableView.getItems().add( new String[] {
												ce + "", groupKey, mass + "", txcf + ""
										} );

										massSet.add( mass );

									}

									chart.getData().add( series );

									refreshFitFunction( massSet, ref, obs, chart );
								}
								else
								{
									chart.getData().removeIf( series -> series.getName().startsWith( seriesName ) );

									txCorrectionTableView.getItems().removeIf( c -> c[ 0 ].equals( ce + "" ) && c[ 1 ].equals( groupKey ) );

									observedPointsMap.remove( seriesName );

									refreshFitFunction( massSet, null, null, chart );
								}
							}
						} );

						groupTreeItem.getChildren().add(ceTreeItem);
					}

					sampleTreeRoot.getChildren().add( groupTreeItem );
				}
				masterDatabase.close();
			}
		} );

		addEventHandler( ProcessEvent.VALIDATION_MACHINE_PERFORMANCE, new EventHandler< ProcessEvent >()
		{
			@Override public void handle( ProcessEvent event )
			{
				Object[] params = event.getParam();

				TreeMap< String, ArrayList<String> > groupMap = (TreeMap< String, ArrayList<String> >) params[0];
				//				ArrayList<String> samples = (ArrayList) params[0];

				ObservableList<TreeItem<BARow>> species = (ObservableList<TreeItem<BARow>>) params[1];

				HashMap<TreeItem< BARow >, BA> baMap = (HashMap<TreeItem< BARow >, BA>) params[2];

				Map<String, Float> refPRIMap = (Map<String, Float>) params[3];

				ObservableList< FAAnion > mFaAnionsList = (ObservableList< FAAnion >) params[4];

				HashMap<String, HashSet<TreeItem< BARow >>> map = (HashMap<String, HashSet<TreeItem< BARow >>>) params[5];

				final File outputFile = (File) params[6];

            System.err.println("## Machine Performance");

				HashSet<Double> validMasses = new HashSet<>(  );

				species.forEach( c ->
						{
							//System.out.println(	baMap.get( c ).getMass() )
							if ( refPRIMap.containsKey( c.getValue().getTitle() ) )
								validMasses.add( c.getValue().getMass() );
							//								System.out.println( "Ref--> " + c.getValue().getTitle() );
							//							else
							//								System.out.println( c.getValue().getTitle() );
						}
				);

//				HashSet<Double> removeMasses = new HashSet<>();
//
//				species.forEach( c ->
//						{
//
//							long count = species.stream().filter( d -> c.getValue().getTitle().equals( d.getValue().getTitle() )).count();
//							if( count > 1 )
//							{
//								System.out.println( c.getValue().getMass() + " will be ignored.");
//								removeMasses.add( c.getValue().getMass() );
//							}
//						}
//				);

				if(outputFile != null) {
					// This case is for command line calling
					computeMachinePerformance( groupMap, species, baMap, refPRIMap, mFaAnionsList, map, outputFile, validMasses );
				}
				else {
					// This involves UI
					new Thread( () ->
							computeMachinePerformance( groupMap, species, baMap, refPRIMap, mFaAnionsList, map, outputFile, validMasses ) ).start();
				}
			}
		} );

		addEventHandler( ProcessEvent.COMMAND_TX_CORRECTION, new EventHandler< ProcessEvent >()
		{
			@Override public void handle( ProcessEvent event )
			{
				Object[] params = event.getParam();

				TreeMap< String, ArrayList<String> > groupMap = (TreeMap< String, ArrayList<String> >) params[0];
				//				ArrayList<String> samples = (ArrayList) params[0];

				ObservableList<TreeItem<BARow>> species = (ObservableList<TreeItem<BARow>>) params[1];

				HashMap<TreeItem< BARow >, BA> baMap = (HashMap<TreeItem< BARow >, BA>) params[2];

				Map<String, Float> refPRIMap = (Map<String, Float>) params[3];

				ObservableList< FAAnion > mFaAnionsList = (ObservableList< FAAnion >) params[4];

				HashMap<String, HashSet<TreeItem< BARow >>> map = (HashMap<String, HashSet<TreeItem< BARow >>>) params[5];

				File transmissionCorrectionFile = (File) params[6];

				File machinePerformanceFile = (File) params[7];

				HashSet<Double> validMasses = new HashSet<>(  );

				species.forEach( c ->
						{
							if ( refPRIMap.containsKey( c.getValue().getTitle() ) )
								validMasses.add( c.getValue().getMass() );
						}
				);

				String[] nceStringArray = null;

				if(null != machinePerformanceFile && machinePerformanceFile.exists())
				{
					String firstLine = Data.getFirstLine( machinePerformanceFile );
					//				System.out.println(firstLine);

					String[] split = firstLine.split( "\t" );

					if( !split[2].equals( "-Infinity" ) && split[0].equals( "BEST" ) )
					{
						System.out.println( split[1] );
						//					System.out.println( split[1].split( "\\[|\\]|,|\\s" ).length );
						String withBracket = split[1].replaceAll( "[\\p{Ps}\\p{Pe}]", "" );
						nceStringArray = withBracket.split( ",\\s" );
						//					System.out.println( Arrays.toString( nceStringArray ));
					}

					if (nceStringArray != null) {
						Float offset = Float.parseFloat( nceStringArray[0] ) - 25f;
						if(!offset.equals( 0f ))
							setOffset( Float.parseFloat( nceStringArray[0] ) - 25f, baMap, map );
					}
				}

            System.err.println("## Transmission Correction");

				BA ba = baMap.values().stream().findFirst().get();

				TreeSet<Float> ceSet = new TreeSet<>( ba.getKeys() );

				// [Group, [NCE, [A, B]]]
				HashMap<String, TreeMap<Float, double[]>> txFunctionParameters = new HashMap<>();

				final MasterDatabase masterDatabase = new MasterDatabase();
				masterDatabase.connect();

				for ( String groupKey : groupMap.keySet() )
				{
					if ( !txFunctionParameters.containsKey( groupKey ) )
						txFunctionParameters.put( groupKey, new TreeMap<>() );

					TreeMap<Float, List< WeightedObservedPoint >> refMap = new TreeMap<>(  );
					TreeMap<Float, List< WeightedObservedPoint >> obsMap = new TreeMap<>(  );

					for ( String sampleId : groupMap.get( groupKey ) )
					{
						final TreeMap< String, EstSample > estSampleTreeMap = createEstSamples(masterDatabase, groupKey, sampleId, species, baMap, mFaAnionsList, true);

						processEstSample( sampleId, species, baMap, refPRIMap, mFaAnionsList, false, false, estSampleTreeMap,
								( priReferenceMap, computedPriFragmentMap, faSampleMap, sampleTreeMap ) ->
								{
									for ( String className : priReferenceMap.keySet() ) {
										for ( Float ce : priReferenceMap.get(className).keySet() )
										{
											List< WeightedObservedPoint > ref = new ArrayList<>();
											List< WeightedObservedPoint > obs = new ArrayList<>();

											for ( Double mass : computedPriFragmentMap.get(className).keySet() )
											{
												Float txcf = computedPriFragmentMap.get(className).get( mass ).getCF( ce );

												if ( validMasses.contains( mass ) )
													obs.add( new WeightedObservedPoint( 1, mass, txcf ) );

												if ( !refMap.containsKey( ce ) && validMasses.contains( mass ) )
													ref.add( new WeightedObservedPoint( 1, mass, txcf ) );
											}

											if ( !refMap.containsKey( ce ) )
												refMap.put( ce, ref );

											if( !obsMap.containsKey( ce ) )
												obsMap.put( ce, obs );
											else
												obsMap.get(ce).addAll( obs );
										}
									}
								} );
					}

					for(Float ce : ceSet)
					{
						TreeSet<Double> massSet = new TreeSet<>(  );
						List< WeightedObservedPoint > ref = refMap.get(ce);
						List< WeightedObservedPoint > obs = obsMap.get(ce);

						double[] functionParameters = fitSimpleExpFunctionParameters( massSet, ref, obs );

						if(!txFunctionParameters.get(groupKey).containsKey( ce ))
							txFunctionParameters.get(groupKey).put(ce, functionParameters);
					}
				}

				masterDatabase.close();

				StringBuffer export = new StringBuffer();
				export.append( "Group" ).append( '\t' )
						.append( "CE" ).append( '\t' )
						.append( "A" ).append( '\t' )
						.append( "B" ).append( '\n' );

				for ( String groupId : txFunctionParameters.keySet() )
				{
					TreeMap< Float, double[] > list = txFunctionParameters.get( groupId );

					for ( Float ce : list.keySet() )
					{
						export.append( groupId ).append( '\t' )
								.append( ce ).append( '\t' )
								.append( list.get(ce)[0] ).append( '\t' )
								.append( list.get(ce)[1] ).append( '\n' );
					}

				}

				if( transmissionCorrectionFile != null )
				{
					try
					{
						FileUtils.writeStringToFile( transmissionCorrectionFile, export.toString() );
					}
					catch ( IOException e )
					{
						e.printStackTrace();
					}
					Platform.exit();
				}
				else
				{
					for(String str : export.toString().split( "\n" ))
						System.err.println(str);
				}
			}
		} );
	}

	private void computeMachinePerformance( TreeMap< String, ArrayList< String > > groupMap, ObservableList< TreeItem< BARow > > species, HashMap< TreeItem< BARow >, BA > baMap, Map< String, Float > refPRIMap, ObservableList< FAAnion > mFaAnionsList, HashMap< String, HashSet< TreeItem< BARow > > > map, File outputFile, HashSet< Double > validMasses )
	{
		HashMap<String, ArrayList<TreeMap<Float, Double>>> machinePerformance = new HashMap<>();
		HashMap<String, ArrayList<TreeMap<Float, Double>>> machinePerformanceCo2 = new HashMap<>();

		BA ba = baMap.values().stream().findFirst().get();

		TreeSet<Float> ceSet = new TreeSet<>( ba.getKeys() );

		//				System.out.println(ceSet);
		String clazz = species.stream().findFirst().get().getValue().getTitle().split( " " )[0];

		final MasterDatabase masterDatabase = new MasterDatabase();
		masterDatabase.connect();

		// Asymmetric
		TreeMap<Float, Float> co2AsymFaiRatioMap = new TreeMap<>(  );

		// 49 is 22:6 fragment
		for ( String[] detailRow : masterDatabase.getDetails( 49, clazz, "0.5", "0.5", "0" ) )
		{
         String co2Int = detailRow[ 3 ];
         if (co2Int.isEmpty()) {
            co2Int = "0";
         }

			co2AsymFaiRatioMap.put( Float.parseFloat( detailRow[ 0 ] ),
					Float.parseFloat( co2Int ) / Float.parseFloat( detailRow[ 1 ] ));
		}

		//				co2AsymFaiRatioMap.forEach( (c, v) -> System.out.println(c + "->" + v) );

		// Symmetric
		TreeMap<Float, Float> co2SymFaiRatioMap = new TreeMap<>(  );

		// 49 is 22:6 fragment
		List<String[]> details = clazz.equals( "PCO" ) || clazz.equals( "PEO" )  ? masterDatabase.getDetails( 49, clazz, "0", "1.0", "0" ) : masterDatabase.getDetails( 49, clazz, "0", "0", "1.0" );

		for ( String[] detailRow : details )
		{
         String co2Int = detailRow[ 3 ];
         if (co2Int.isEmpty()) {
            co2Int = "0";
         }
			co2SymFaiRatioMap.put( Float.parseFloat( detailRow[ 0 ] ),
					Float.parseFloat( co2Int ) / Float.parseFloat( detailRow[ 1 ] ));
		}

		//				co2SymFaiRatioMap.forEach( (c, v) -> System.out.println(c + "->" + v) );
		//
		//				TreeMap< String, EstSample > lastEstSampleTreeMap = null;

		for ( String groupKey : groupMap.keySet() )
		{
			if ( !machinePerformance.containsKey( groupKey ) )
				machinePerformance.put( groupKey, new ArrayList<>() );

			if ( !machinePerformanceCo2.containsKey( groupKey ) )
				machinePerformanceCo2.put( groupKey, new ArrayList<>() );

			float firstCe = ceSet.first();
			float lastCe = ceSet.last();

			float firstOffset = 20f - firstCe;
			System.out.println( firstOffset );
			setOffset( firstOffset, baMap, map );

			float count = 0;
			for ( float lastOffset = lastCe + firstOffset; lastOffset < 44; lastOffset++ )
			{
				count += 1f;

				TreeMap<Float, List< WeightedObservedPoint > > refMap = new TreeMap<>(  );
				TreeMap<Float, List< Float >> obsAsymFaCoMap = new TreeMap<>(  );
				TreeMap<Float, List< Float >> obsSymFaCoMap = new TreeMap<>(  );

				for ( String sampleId : groupMap.get( groupKey ) )
				{
					final TreeMap< String, EstSample > estSampleTreeMap = createEstSamples( masterDatabase, groupKey, sampleId, species, baMap, mFaAnionsList, true );

               processEstSample( sampleId, species, baMap, refPRIMap, mFaAnionsList,
                       true, false, estSampleTreeMap,
							( priReferenceMap, computedPriFragmentMap, faSampleMap, sampleTreeMap ) ->
							{
								for ( String className : priReferenceMap.keySet() )
								{
									for ( Float ce : priReferenceMap.get(className).keySet() )
									{
										List< WeightedObservedPoint > ref = new ArrayList<>();

										for ( Double mass : computedPriFragmentMap.get(className).keySet() )
										{
											Float txcf = computedPriFragmentMap.get(className).get(mass).getCF( ce );

											if ( validMasses.contains( mass ) )
												ref.add( new WeightedObservedPoint( 1, mass, txcf ) );
										}

										if ( !refMap.containsKey( ce ) )
											refMap.put( ce, ref );
										else
											refMap.get( ce ).addAll( ref );

										// Find 22:6 nodes
                              //
                              // use the highest abundance intensity!
										estSampleTreeMap.keySet().stream().forEach( c -> {
											if(c.contains( "44:12:0-327.23" + ce ))
											{
												if ( !obsSymFaCoMap.containsKey( ce ) )
													obsSymFaCoMap.put( ce, new ArrayList<>() );

//                                    System.out.println("Sym:" + estSampleTreeMap.get( c ).getCorrectedFAI());
                                    if (estSampleTreeMap.get( c ).getFaCoRatio() > 0f)
												   obsSymFaCoMap.get( ce ).add( estSampleTreeMap.get( c ).getFaCoRatio() );
											}
											else if(c.contains( "-327.23" + ce))
											{
												if ( !obsAsymFaCoMap.containsKey( ce ) )
													obsAsymFaCoMap.put( ce, new ArrayList<>() );

//                                    System.out.println("Asym(" + ce + "):" + estSampleTreeMap.get( c ).getFaCoRatio() );
                                    if (estSampleTreeMap.get( c ).getFaCoRatio() > 0f)
												   obsAsymFaCoMap.get( ce ).add( estSampleTreeMap.get( c ).getFaCoRatio() );
											}
										} );
									}
								}
							} );
				}

				TreeMap< Float, Double > rMap = new TreeMap<>();
				TreeMap< Float, Double > faCoMap = new TreeMap<>();
				for(Float ce : refMap.keySet() )
				{
					rMap.put( ce, checkRSquared( refMap.get(ce) ) );

					ArrayList<Double> list = new ArrayList<>(  );
					if(obsAsymFaCoMap.containsKey( ce )) {
//                  System.out.println("Sample_Co2_ratio: " + obsAsymFaCoMap.get( ce ) + "\tTheoretical_Co2_ratio: " + co2AsymFaiRatioMap.get(ce));
                  list.add( Validation.computeRsquared( obsAsymFaCoMap.get( ce ), co2AsymFaiRatioMap.get(ce) ) );
               }

					if(obsSymFaCoMap.containsKey( ce )) {
//                  System.out.println("Sample_Co2_ratio: " + obsSymFaCoMap.get( ce ) + "\tTheoretical_Co2_ratio: " + co2SymFaiRatioMap.get(ce));
                  list.add( Validation.computeRsquared( obsSymFaCoMap.get( ce ), co2SymFaiRatioMap.get(ce) ) );
               }

               System.out.println(list);

					if(!list.isEmpty())
						faCoMap.put( ce, list.stream().mapToDouble( c -> c ).average().getAsDouble() );
				}

				machinePerformance.get( groupKey ).add( rMap );

				if(!faCoMap.isEmpty())
					machinePerformanceCo2.get( groupKey ).add( faCoMap );

				setOffset( 1f, baMap, map );
			}

			setOffset( -count - firstOffset, baMap, map );
		}

		masterDatabase.close();

		StringBuffer export = new StringBuffer();
		TreeMap< String, Double > avgList = new TreeMap<>(  );
		String bestKey = null;
		Double bestVal = null;

		for ( String groupId : machinePerformance.keySet() )
		{
//			System.err.println( "Machine Performance check by TX.CF:" );
//			ArrayList< TreeMap< Float, Double > > list = machinePerformance.get( groupId );
//			for ( TreeMap< Float, Double > rMap : list )
//			{
//				Double avg = rMap.values().stream().mapToDouble( c -> c ).average().getAsDouble();
//
//				avgList.put( rMap.keySet().toString(), avg );
//
//				System.err.println( rMap.keySet() + " = " + rMap.values() + " -> " + avg );
//				export.append( "TX.CF" ).append( '\t' )
//						.append( rMap.keySet() ).append( '\t' )
//						.append( rMap.values() ).append( '\t' )
//						.append( avg ).append( '\n' );
//			}
//         list = machinePerformanceCo2.get( groupId );

			System.err.println( "Machine Performance check by CO2/FAI ratio with 22:6 XML data:" );
         ArrayList< TreeMap< Float, Double > > list = machinePerformanceCo2.get( groupId );

			if(list.isEmpty()) System.err.println( "Skipped: 22:6 XML data are not present in the dataset." );

			if(!list.isEmpty())
			{
				for ( TreeMap< Float, Double > faCoMap : list )
				{
					Double avg = faCoMap.values().stream().mapToDouble( c -> c ).average().getAsDouble();

					String key = faCoMap.keySet().toString();
//					avgList.put( key, ( avgList.get( key ) + avg ) / 2d );
               avgList.put( key, avg );

					System.err.println( faCoMap.keySet() + " = " + faCoMap.values() + " -> " + avg );
					export.append( "CO2/FAI" ).append( '\t' )
							.append( faCoMap.keySet() ).append( '\t' )
							.append( faCoMap.values() ).append( '\t' )
							.append( avg ).append( '\n' );
				}
			}

			for ( String key : avgList.keySet() )
			{
				if( bestVal == null || bestVal < avgList.get(key) ) {
					bestVal = avgList.get(key);
					bestKey = key;
				}
				export.append( "AVG" ).append( '\t' )
						.append( key ).append( '\t' )
						.append( avgList.get(key) ).append( '\n' );
			}
		}

		export.insert( 0, "BEST\t" + bestKey + "\t" + bestVal + '\n' );

		if(outputFile != null)
		{
			try
			{
				FileUtils.writeStringToFile( outputFile, export.toString() );
			}
			catch ( IOException e )
			{
				e.printStackTrace();
			}
//											Platform.exit();
		}
	}

	private void setOffset( float offset, final HashMap<TreeItem< BARow >, BA> baMap, HashMap<String, HashSet<TreeItem<BARow>>> map )
	{
		for( String pre : map.keySet() )
		{
			Optional<BA> ba = baMap.values().stream().filter( c -> pre.equals( c.toString() ) ).findFirst();
			if(ba.isPresent())
				ba.get().setOffset( offset );

			for( TreeItem<BARow> fa : map.get( pre ))
			{
				baMap.get(fa).setOffset( offset );
			}
		}
	}

	private double checkRSquared( List< WeightedObservedPoint > ref )
	{
		double[] params;

		UnivariateDifferentiableFunction func = null;

		if(SampleEstimation.getSelectedTxCFunction() == SampleEstimation.TxCorrectionFunc.SimpleExp )
		{
			// Fit the data with Simple Exponential Function
			SimpleExponentialFitter simpleExpFitter = SimpleExponentialFitter.create();

			params = simpleExpFitter.fit( ref );

			func = new SimpleExponentialFunction( params );
		}
		else if( SampleEstimation.getSelectedTxCFunction() == SampleEstimation.TxCorrectionFunc.ExpDecay )
		{
			// Fit the data with Exponential Decay Function
			ExponentialDecayFitter expDecayFuncFitter = ExponentialDecayFitter.create();

			params = expDecayFuncFitter.fit( ref );

			func = new ExponentialDecayFunction( params );
		}

		ArrayList<Float> targetList = new ArrayList<>(  );
		ArrayList<Float> sourceList = new ArrayList<>(  );

		for( WeightedObservedPoint p : ref )
		{
			targetList.add( (float) func.value( p.getX() ) );
			sourceList.add( (float) p.getY() );
		}

//		for( double mass = start; mass < end; mass += 50 )
//		{
//			expDecayTargetList.add( (float) expDecayFunc.value( mass ) );
//			simpleExpTargetList.add( (float) simpleExpFunc.value( mass ) );
//			sourceList.add( SampleEstimation.getTxCF( mass, ce ) );
//		}

		double rSquared = Validation.computeRsquared(sourceList, targetList);

//		System.err.println( SampleEstimation.getTxFunctionName( ce ) + "(" + ce + ") R² = " + rSquared);

//		rSquared = Validation.computeRsquared(sourceList, expDecayTargetList);
//
//		System.err.println( "Exp Decay R² = " + rSquared);

		return rSquared;
	}

	protected void uncheckNode( TreeItem<String> item )
	{
		if(item instanceof CheckBoxTreeItem )
			( ( CheckBoxTreeItem ) item ).setSelected( false );

		item.getChildren().forEach( this::uncheckNode );
	}

	private int getIndex( ObservableList< FAAnion > mFaAnionsList, double mass, int carbon, int db )
	{
		int found = 0;

		Optional<FAAnion> faAnion = mFaAnionsList.stream().filter( c ->
				c.getMass().equals( mass ) &&
						c.getFACarbon().equals( carbon ) &&
						c.getFADoubleBonds().equals( db ) ).findFirst();

		if(faAnion.isPresent())
			found = faAnion.get().getIndex();
		else
			System.err.println( mass + ":carbon - " + carbon + ":db - " + db + " => Not found in FAAnion List!" );

		return found;
	}

	private static void refreshFitFunction( Set<Double> keySet, List<WeightedObservedPoint> refs, List< WeightedObservedPoint > obs, LineChart< Number, Number > chart )
	{
		String expDecapFunctionString = "Exp.Decay function";

		//		String simpleLogFunctionString = "Simple.Log function";

		String simpleExpFunctionString = "Simple.Exp function";

		chart.getData().removeIf( series -> series.getName().startsWith( expDecapFunctionString ) );

		//		chart.getData().removeIf( series -> series.getName().startsWith( simpleLogFunctionString ) );

		chart.getData().removeIf( series -> series.getName().startsWith( simpleExpFunctionString ) );

		if(null == refs) return;

//		List<WeightedObservedPoint> obs = new ArrayList<>();
//
//		for( String nce : observedPointsMap.keySet() )
//		{
//			obs.addAll( observedPointsMap.get(nce) );
//		}

		// Fit the data with Exponential Decay Function
		ExponentialDecayFitter expDecayFuncFitter = ExponentialDecayFitter.create();

		double[] params = expDecayFuncFitter.fit( refs );

		System.err.println( "1. Exponential Decay Function: " + params[0] + " * exp(-x / " + params[1] + ")");

		ExponentialDecayFunction expDecayFunc = new ExponentialDecayFunction( params );

		ArrayList<Float> sourceList = new ArrayList<>(  );
		ArrayList<Float> expDecayTargetList = new ArrayList<>(  );

		for( WeightedObservedPoint p : obs )
		{
			sourceList.add( (float) p.getY() );
			expDecayTargetList.add( (float) expDecayFunc.value( p.getX() ) );
		}

		//		for( double mass = start; mass < end; mass += 50 )
		//		{
		//			expDecayTargetList.add( (float) expDecayFunc.value( mass ) );
		//			simpleExpTargetList.add( (float) simpleExpFunc.value( mass ) );
		//			sourceList.add( SampleEstimation.getTxCF( mass, ce ) );
		//		}

		double rSquared = Validation.computeRsquared(sourceList, expDecayTargetList);
		System.err.println( "R² = " + rSquared);

		//		// Fit the data with Simple Exponential Function
		//		SimpleLogFitter simpleLogFitter = SimpleLogFitter.create();
		//
		//		double[] params = simpleLogFitter.fit( obs );
		//
		//		System.err.println( "1. Exp Function: exp(" + params[0] + " * x + " + params[1] );
		//
		//		SimpleLogFunction simpleLogFunction = new SimpleLogFunction( params );
		//
		// Fit the data with Simple Exponential Function
		SimpleExponentialFitter simpleExpFitter = SimpleExponentialFitter.create();

		params = simpleExpFitter.fit( refs );

		System.err.println( "2. Exponential Function: " + params[0] + " * x ^ " + params[1] );

		SimpleExponentialFunction simpleExpFunction = new SimpleExponentialFunction( params );

		ArrayList<Float> simpleExponentialTargetList = new ArrayList<>(  );

		for( WeightedObservedPoint p : obs )
		{
			simpleExponentialTargetList.add( (float) simpleExpFunction.value( p.getX() ) );
		}

		rSquared = Validation.computeRsquared(sourceList, simpleExponentialTargetList);
		System.err.println( "R² = " + rSquared);

		// Create charts
		XYChart.Series expDecapSeries = new XYChart.Series();
		expDecapSeries.setName( expDecapFunctionString );

		//		XYChart.Series simpleLogSeries = new XYChart.Series();
		//		simpleLogSeries.setName( simpleLogFunctionString );

		XYChart.Series simpleExpSeries = new XYChart.Series();
		simpleExpSeries.setName( simpleExpFunctionString );

		TreeSet<Double> masses = new TreeSet(keySet);
		double start = masses.first() - 300;
		double end = masses.last() + 300;

		for( double mass = start; mass < end; mass += 50 )
		{
			XYChart.Data node = new XYChart.Data( mass, (float) expDecayFunc.value( mass ) );
			expDecapSeries.getData().add( node );

			//			XYChart.Data node = new XYChart.Data( mass, (float) simpleLogFunction.value( mass ) );
			//			simpleLogSeries.getData().add( node );

			node = new XYChart.Data( mass, (float) simpleExpFunction.value( mass ) );
			simpleExpSeries.getData().add( node );
		}

		chart.getData().add( expDecapSeries );
		//		chart.getData().add( simpleLogSeries );
		chart.getData().add( simpleExpSeries );
	}

	private double[] fitSimpleExpFunctionParameters( Set<Double> keySet, List<WeightedObservedPoint> refs, List< WeightedObservedPoint > obs)
	{
		if(null == refs) return null;

		// Fit the data with Simple Exponential Function
		SimpleExponentialFitter simpleExpFitter = SimpleExponentialFitter.create();

		double[] params = simpleExpFitter.fit( refs );

		System.err.println( "Exponential Function: " + params[0] + " * x ^ " + params[1] );

		SimpleExponentialFunction simpleExpFunction = new SimpleExponentialFunction( params );

		ArrayList<Float> sourceList = new ArrayList<>(  );
		ArrayList<Float> simpleExponentialTargetList = new ArrayList<>(  );

		for( WeightedObservedPoint p : obs )
		{
			sourceList.add( (float) p.getY() );
			simpleExponentialTargetList.add( (float) simpleExpFunction.value( p.getX() ) );
		}

		double rSquared = Validation.computeRsquared(sourceList, simpleExponentialTargetList);
		System.err.println( "R² = " + rSquared);

		return params;
	}
}
