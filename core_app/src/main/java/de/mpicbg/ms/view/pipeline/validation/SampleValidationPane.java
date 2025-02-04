package de.mpicbg.ms.view.pipeline.validation;

import de.mpicbg.ms.db.MasterDatabase;
import de.mpicbg.ms.model.SampleEstimation;
import de.mpicbg.ms.model.data.BA;
import de.mpicbg.ms.model.data.BARow;
import de.mpicbg.ms.model.data.Sample;
import de.mpicbg.ms.model.event.ChartEvent;
import de.mpicbg.ms.model.event.ProcessEvent;
import de.mpicbg.ms.model.data.EstSample;
import de.mpicbg.ms.model.data.FAAnion;
import de.mpicbg.ms.model.data.Fraction;
import de.mpicbg.ms.model.regression.Bicubic;
import de.mpicbg.ms.model.regression.Percent;
import de.mpicbg.ms.view.chart.HoveredNode;
import de.mpicbg.ms.view.pane.component.LabeledPane;
import de.mpicbg.ms.view.treecell.SelectiveCheckBoxTreeCell;
import de.mpicbg.ms.util.TableViewUtil;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.geometry.Side;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

import org.apache.commons.lang3.Range;
import org.apache.commons.math3.analysis.BivariateFunction;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.exception.NumberIsTooSmallException;
import org.apache.commons.math3.util.Precision;
import org.controlsfx.control.MasterDetailPane;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static de.mpicbg.ms.model.SampleEstimation.createEstSamples;
import static de.mpicbg.ms.model.SampleEstimation.getTxCF;
import static de.mpicbg.ms.model.SampleEstimation.updateCorrectionFactor;
import static de.mpicbg.ms.model.regression.SimpleRegression.computeRegressionParameters;
import static de.mpicbg.ms.model.regression.SimpleRegression.computeRegressionParametersForChart;
import static de.mpicbg.ms.view.pipeline.quantification.QuantificationPane.updateEstSamples;

/**
 * Author: HongKee Moon (moon@mpi-cbg.de), Scientific Computing Facility
 * Organization: MPI-CBG Dresden
 * Date: January 2017
 */
public class SampleValidationPane extends MasterDetailPane
{
	final private TableView<EstSample> estSampleTableView;
	final private TableView<String[]> masterDataTableView;

	TreeMap<String, TreeMap< Integer, FAAnion > > masterDBSet;

	TreeMap<String, Fraction > referenceFAIMap;


	TreeItem< String > estTreeRoot;

	TreeMap< String, TreeItem<String> > sampleMap;
	TreeMap< String, TreeItem<String> > isomerMap;
	TreeMap< String, TreeItem<String> > positionMap;

	TreeMap< Integer, Fraction > fractionTreeMap;

	TreeMap< String, Fraction > sampleTreeMap;

	TreeMap< String, EstSample > estSampleTreeMap;

	HashMap< String, HashMap<String, HashSet<EstSample> > > species;

	public SampleValidationPane(LineChart< Number, Number > chart)
	{
		sampleMap = new TreeMap< String, TreeItem<String> >();
		isomerMap = new TreeMap< String, TreeItem<String> >();
		positionMap = new TreeMap< String, TreeItem<String> >();

		fractionTreeMap = new TreeMap< Integer, Fraction >();
		sampleTreeMap = new TreeMap< String, Fraction >();
		estSampleTreeMap = new TreeMap<String, EstSample>();

		species = new HashMap<>();

		referenceFAIMap = new TreeMap<>();

		addEventHandler( ProcessEvent.VALIDATION_MZ_CORRECTION, new EventHandler< ProcessEvent >()
		{
			@Override public void handle( ProcessEvent event )
			{
				Object[] params = event.getParam();

			}
		} );

		addEventHandler( ProcessEvent.VALIDATION_GROUPING, new EventHandler< ProcessEvent >()
		{
			@Override public void handle( ProcessEvent event )
			{
				Object[] params = event.getParam();
				final String group = (String) params[0];

				if( group.equals( "Reset" ) )
				{
					// Clear all the items
					estSampleTableView.getItems().clear();
					masterDataTableView.getItems().clear();
					chart.getData().clear();
					estTreeRoot.getChildren().clear();
					fractionTreeMap.clear();
					sampleTreeMap.clear();
					sampleMap.clear();
					estSampleTreeMap.clear();
					species.clear();
				}
				else
				{
					int index = (int) params[1];

					boolean isSym = (boolean) params[2];

					// fa can be "FA-1" or "FA-2"
					String specie = (String) params[3];
					String fa = (String) params[4];

					ArrayList<String[]> sampleData = (ArrayList<String[]>) params[5];

					if(!species.containsKey( specie ))
						species.put( specie, new HashMap<>() );

					species.get( specie ).put( fa, new HashSet<>() );

					final MasterDatabase masterDatabase = new MasterDatabase();

					// Refresh masterDataTableView
					masterDatabase.connect();

					setupMasterDBSet( (ObservableList< FAAnion >) params[6] );

					final String clazz = specie.split( " " )[0];
					final FAAnion faAnion = masterDBSet.get(clazz).get(index);

					final Double priMass = (Double) params[7];

					String key = null;
					for( String[] row : sampleData )
					{
						float ce = Float.parseFloat( row[4] );

						String mass = row[2];
						String sampleName = row[3];

						key = sampleName + " - " + mass;

						if (!sampleTreeMap.containsKey( key ))
							sampleTreeMap.put( key, new Fraction(clazz, index) );

						EstSample estSample = new EstSample( sampleName, row[0], specie, row[2], ce, getTxCF( priMass, ce ) );

						species.get( specie ).get( fa ).add( estSample );

						List<String[]> rows = clazz.equals( "PCO" ) || clazz.equals( "PEO" ) ? masterDatabase.getDetails( index, clazz, "0", "1.0", "0" ) : masterDatabase.getDetails( index, clazz, "0", "0", "1.0" );
						for ( String[] detailRow : rows )
						{
							if( ce == Float.parseFloat( detailRow[0] ) )
							{
								Float cInt = Float.parseFloat( row[5] );
//                        Float cInt = Float.parseFloat( row[5] ) * Float.parseFloat( detailRow[2] );

								sampleTreeMap.get(key).put( ce, "Sample", cInt );

								row[5] = cInt + "";
								Float cCoi = row[6].isEmpty() ?
										0f : Float.parseFloat( row[6] );

								if( cCoi != 0f )
								{
									sampleTreeMap.get(key).put( ce, "Sample:CO2", cCoi );
								}

								row[6] = cCoi + "";
								row[7] = cInt == 0 ? 0 + "": cCoi / cInt + "";

								estSample.setcFAI( cInt );
                        estSample.addCFRange( Float.parseFloat( detailRow[ 2 ] ) );
                        estSample.setSecondaryCF( estSample.getCF() );
								estSample.setcCOI( cCoi );
								estSample.setFaCoRatio( cInt == 0 ? 0 : cCoi / cInt );

//								System.out.println(estSample);

								break;
							}
						}

						// Create EstSample instances
						estSampleTableView.getItems().add( estSample );

						if( !estSampleTreeMap.containsKey( key + ce ) )
						{
							estSampleTreeMap.put( key + ce, estSample );
						}

						// Get all the FAAnions with the same masses
						List<FAAnion> faAnions = getMassIndexes( masterDBSet.get(clazz).values(), faAnion );

						TreeItem<String> isomerItem;
						TreeItem<String> positionItem;

						if( !sampleMap.containsKey( key ) )
						{
							TreeItem<String> sampleItem = new TreeItem<>( key );
							sampleItem.setExpanded( true );
							estTreeRoot.getChildren().add( sampleItem );
							sampleMap.put( key, sampleItem );

							isomerItem = new TreeItem<>( "Isomer" );
							isomerItem.setExpanded( true );
							positionItem = new TreeItem<>( "Position" );
							positionItem.setExpanded( true );

							if( isSym )
								sampleItem.getChildren().add( isomerItem);
							else
								sampleItem.getChildren().addAll( isomerItem, positionItem );

							isomerMap.put( key, isomerItem );
							positionMap.put( key, positionItem );
						}

						isomerItem = isomerMap.get( key );
						positionItem = positionMap.get( key );

						// Update masterDataTable with the given index
						updateMasterDataTable( masterDataTableView, masterDatabase, clazz, faAnions, ce, isSym, chart, key, isomerItem, positionItem );
					}

					masterDatabase.close();

					// Complement check from here
//					if( species.get( specie ).size() > 1 )
//					{
//						System.out.println( "Complement check for " + specie );
//
//						if( species.get( specie ).containsKey( "FA-2" ))
//						{
//							// Replace FA-1's Position data with FA-2's complement
//							for(EstSample item2 : species.get( specie ).get( "FA-2") )
//							{
//								for(EstSample item1 : species.get( specie ).get( "FA-1") )
//								{
//									if( item2.getCe().equals( item1.getCe() ))
//									{
//										Range<Float> range = item2.getPosition();
//										item1.setPosition( Range.between( 1f - range.getMaximum(), 1f - range.getMinimum() ) );
//									}
//								}
//							}
//						}
//					}
				}
			}
		} );

		addEventHandler( ProcessEvent.VALIDATION_SAMPLE, new EventHandler< ProcessEvent >()
		{
			@Override public void handle( ProcessEvent event )
			{
				Object[] params = event.getParam();

				EstSample estSample = (EstSample) params[0];

				System.out.println(estSample);

				// Create EstSample instances
				estSampleTableView.getItems().add( estSample );
			}
		} );

		addEventHandler( ProcessEvent.VALIDATION_INTENSITY_RATIO_CHECK, new EventHandler< ProcessEvent >()
		{
			@Override public void handle( ProcessEvent event )
			{
				Object[] params = event.getParam();

				final LinkedHashSet< TreeItem< BARow > > baSet = (LinkedHashSet< TreeItem< BARow > >) params[0];
				final HashMap<TreeItem< BARow >, BA > baMap = (HashMap<TreeItem< BARow >, BA>) params[1];
				final ObservableList< FAAnion > mFaAnionList = (ObservableList< FAAnion >) params[2];
				final ObservableList<String> excludedSpecies = (ObservableList<String>) params[3];
				final ObservableList<String> samples = (ObservableList<String>) params[4];

				// XML database connection
				final MasterDatabase masterDatabase = new MasterDatabase();

				// Get MasterDBSet
				masterDatabase.connect();

				// 1.25
				float tolerance = 1.25f;

				setupMasterDBSet( mFaAnionList );

				TreeMap< String, ArrayList<String> > groupMap = new TreeMap<>(  );
				groupMap.put( "", new ArrayList<>(  ) );

				final TreeMap< String, TreeMap< String, EstSample > > groupSampleTreeMap = new TreeMap<>(  );

				for( String sampleId : samples )
				{
					groupMap.get("").add( sampleId );

					TreeMap< String, EstSample > estSampleTreeMap = createEstSamples( masterDatabase, "",
							sampleId, baSet, baMap, mFaAnionList, true );

					groupSampleTreeMap.put( "-" + sampleId, estSampleTreeMap );
				}

				updateEstSamples( masterDatabase, mFaAnionList, baMap, baSet, groupMap, groupSampleTreeMap );

				HashMap<TreeItem< BARow >, Float> ratioCheckMap = new HashMap<>(  );

				HashMap<Float, Float[]> minMaxXml = new HashMap<>(  );

				collectIntensityRatio( baSet, baMap, samples, masterDatabase, tolerance, groupSampleTreeMap, ratioCheckMap, minMaxXml );

				masterDatabase.close();

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
						excludedSpecies.add( priMass + "(" + fais + ") = " + count / total );
					} else {
						System.out.println( priMass + "(" + fais + ") = " + count / total );
					}
				}
			}
		} );

		addEventHandler( ChartEvent.ANY, event -> handleChartEvent( event ) );

		estSampleTableView = TableViewUtil.createEstSampleDataView();

		TableViewUtil.addContextMenu( estSampleTableView );

		masterDataTableView = TableViewUtil.createDataView(new String[]{ "Index", "MZ", "C", "DB", "Iso", "SN1", "SN2", "CE", "INT", "COI", "Ratio" });

		TableViewUtil.addContextMenu( masterDataTableView );

		estTreeRoot = new TreeItem<>( "Root" );
		estTreeRoot.setExpanded( true );

		TreeView<String> estTreeView = new TreeView<>( estTreeRoot );
		estTreeView.setShowRoot( false );
		estTreeView.setCellFactory( SelectiveCheckBoxTreeCell.<String>forTreeView() );

      MenuItem sn1stPosition = new MenuItem( "Create Position #1");
      sn1stPosition.setOnAction( new EventHandler< ActionEvent >()
      {
         @Override public void handle( ActionEvent actionEvent )
         {
            TreeItem<String> item = estTreeView.getSelectionModel().getSelectedItem();

            // 1. Compute the first position
            if( estTreeView.getTreeItemLevel( item ) == 1 )
            {
               final MasterDatabase masterDatabase = new MasterDatabase();
               masterDatabase.connect();

               for ( String key : sampleTreeMap.keySet() )
               {
                  Fraction fraction = sampleTreeMap.get( key );

                  FAAnion faAnion = masterDBSet.get( fraction.getClazz() ).get( fraction.getIndex() );
                  List< FAAnion > faAnions = getMassIndexes( masterDBSet.get( fraction.getClazz() ).values(), faAnion );

                  updateCorrectionFactor( key, masterDatabase, sampleTreeMap, estSampleTreeMap, fractionTreeMap, faAnions, sampleTreeMap.keySet().size() == 1 );
               }

               // Complement check for FA-1 and FA-2 positions
               if(sampleTreeMap.keySet().size() > 1)
               {
                  String sn1Key = sampleTreeMap.firstKey();
                  String sn2Key = sampleTreeMap.lastKey();

                  // SN1 Fraction
                  Fraction fraction = sampleTreeMap.get( sn1Key );

                  for ( Float ce : fraction.keySet() )
                  {
                     Range<Float> range = estSampleTreeMap.get( sn2Key + ce ).getPosition();
                     estSampleTreeMap.get( sn1Key + ce ).setPosition( Range.between( 1f - range.getMaximum(), 1f - range.getMinimum() ) );
                  }

                  // Update SN1 and SN2 correction factors according to the updated position
                  for ( String snKey : sampleTreeMap.keySet() ) {
                     Fraction snFraction = sampleTreeMap.get( snKey );
                     int refIndex = snFraction.getIndex();

                     for ( Float ce : fraction.keySet() )
                     {
                        String[] sn1Row = masterDatabase.getDetail( ce, refIndex, fraction.getClazz(), "1.0", "0", "0" );
                        String[] sn2Row = masterDatabase.getDetail( ce, refIndex, fraction.getClazz(), "0", "1.0", "0" );

                        float cf1 = Float.parseFloat( sn1Row[ 2 ] );
                        float cf2 = Float.parseFloat( sn2Row[ 2 ] );

                        float a = cf2 - cf1;
                        float b = cf1;

                        Range< Float > posRange = estSampleTreeMap.get( snKey + ce ).getPosition();

                        float min = a * posRange.getMinimum() + b;
                        float max = a * posRange.getMaximum() + b;

                        Range< Float > cfRange = Range.between( min, max );

                        System.out.println( "1st CF:" + cfRange + " at " + ce );

                        estSampleTreeMap.get( snKey + ce ).setCF( cfRange );
                     }
                  }
               }

               masterDatabase.close();
            }
         }
      } );

		MenuItem sn1stCorrect = new MenuItem( "Create Position #2");
		sn1stCorrect.setOnAction( new EventHandler< ActionEvent >()
		{
			@Override public void handle( ActionEvent event )
			{
				if( null != estTreeView.getSelectionModel().getSelectedItem() )
				{
					TreeItem<String> item = estTreeView.getSelectionModel().getSelectedItem();

					if( estTreeView.getTreeItemLevel( item ) == 1 )
					{
						if( sampleTreeMap.keySet().size() == 2 )
						{
							Fraction fraction = sampleTreeMap.get( item.getValue() );

							//System.out.println(fraction.keySet());

							for( Float ce : fraction.keySet() )
							{
                        Float sumMin = null, sumMax = null;
                        for( String key : sampleTreeMap.keySet() )
                        {
                           Range<Float> faiRange = estSampleTreeMap.get( key + ce ).getCFCorrectedFAI();

                           if( null == sumMin )
                           {
                              sumMin = faiRange.getMinimum();
                              sumMax = faiRange.getMaximum();
                           }
                           else
                           {
                              sumMin += faiRange.getMinimum();
                              sumMax += faiRange.getMaximum();
                           }
                        }

                        for( String key : sampleTreeMap.keySet() )
                        {
                           Range<Float> val = estSampleTreeMap.get( key + ce ).getCFCorrectedFAI();

                           float posAvg = val.getMinimum() / sumMin;

                           Range<Float> pos = posAvg > 0.5f ? Range.between( val.getMinimum() / sumMin, val.getMaximum() / sumMax ) :
                                   Range.between( val.getMaximum() / sumMax, val.getMinimum() / sumMin );

                           if( sumMin != 0 && sumMax != 0) {
                              estSampleTreeMap.get( key + ce ).setRel_FAI( pos );
                           }
                        }
							}

							for( String key : sampleTreeMap.keySet() )
							{
                        Float posMin = null, posMax = null;

                        for( Float ce : sampleTreeMap.get(key).keySet() )
                        {
                           Range<Float> pos = estSampleTreeMap.get( key + ce ).getRel_FAI();

                           if( null == posMin )
                           {
                              posMin = pos.getMinimum();
                              posMax = pos.getMaximum();
                           }

                           posMin = Float.min( posMin, pos.getMinimum() );
                           posMax = Float.max( posMax, pos.getMaximum() );
                        }

                        Float estPosMin = null, estPosMax = null;

                        for( Float ce : sampleTreeMap.get(key).keySet() )
                        {
                           // Get the reference line from 255.23
                           Float position0 = referenceFAIMap.get( fraction.getClazz() ).get( ce, "rel0" );
                           Float position1 = referenceFAIMap.get( fraction.getClazz() ).get( ce, "rel1" );

                           if(position0 == null) break;

                           float a = position1 - position0;
                           float b = position0;

                           float min = a * posMin + b;
                           float max = a * posMax + b;

                           if( null == estPosMin )
                           {
                              estPosMin = min;
                              estPosMax = max;
                           }

                           estPosMin = Float.min( estPosMin, min );
                           estPosMax = Float.max( estPosMax, max );
                        }

                        estPosMin = estPosMin == null ? posMin : estPosMin;
                        estPosMax = estPosMax == null ? posMax : estPosMax;

                        if(estPosMin < 0)
                           estPosMin = 0f;
                        else if(estPosMin > 1)
                           estPosMin = 1f;

                        if(estPosMax < 0)
                           estPosMax = 0f;
                        else if(estPosMax > 1)
                           estPosMax = 1f;

								System.out.println( "[" + estPosMin + "..." + estPosMax + "]");

								for( Float ce : sampleTreeMap.get(key).keySet() )
								{
                           estSampleTreeMap.get( key + ce ).setSecondaryPosition( Range.between( estPosMin, estPosMax ));
								}
							}

							estSampleTableView.refresh();
						}
					}
				}
			}
		} );

		MenuItem sn2ndCorrect = new MenuItem( "Create Position #3");
		sn2ndCorrect.setOnAction( new EventHandler< ActionEvent >()
		{
			@Override public void handle( ActionEvent event )
			{
				if( null != estTreeView.getSelectionModel().getSelectedItem() )
				{
					TreeItem<String> item = estTreeView.getSelectionModel().getSelectedItem();

					if( estTreeView.getTreeItemLevel( item ) == 1 )
					{
						if( sampleTreeMap.keySet().size() == 2 )
						{
							final MasterDatabase masterDatabase = new MasterDatabase();
							masterDatabase.connect();

							// 1. Compute the secondary Double Bond Correction Factor (2nd iteration)
							for ( String key : sampleTreeMap.keySet() )
							{
								Fraction fraction = sampleTreeMap.get( key );

								FAAnion faAnion = masterDBSet.get( fraction.getClazz() ).get( fraction.getIndex());
								List<FAAnion> faAnions = getMassIndexes( masterDBSet.get( fraction.getClazz() ).values(), faAnion );
								final int FA_db = faAnions.stream().findFirst().get().getFADoubleBonds();

                        if (faAnion.getMass().equals( 255.23d )) {
                           for ( Float ce : fraction.keySet() )
                           {
                              estSampleTreeMap.get( key + ce ).setSecondaryCF( Range.between( 1f, 1f ) );
                           }
                        } else {
                           if( FA_db > 0 )
                           {
                              Float isoMin = null, isoMax = null;
                              Float posMin = null, posMax = null;

                              for ( Float ce : fraction.keySet() )
                              {
                                 Range< Float > isoRange = estSampleTreeMap.get( key + ce ).getIsomer();
                                 Range< Float > posRange = estSampleTreeMap.get( key + ce ).getSecondaryPosition();

                                 if ( null == isoMin )
                                 {
                                    isoMin = isoRange.getMinimum();
                                    isoMax = isoRange.getMaximum();

                                    posMin = posRange.getMinimum();
                                    posMax = posRange.getMaximum();
                                 }

                                 isoMin = Float.min( isoMin, isoRange.getMinimum() );
                                 isoMax = Float.max( isoMax, isoRange.getMaximum() );

                                 posMin = Float.min( posMin, posRange.getMinimum() );
                                 posMax = Float.max( posMax, posRange.getMaximum() );
                              }

                              for ( Float ce : fraction.keySet() )
                              {
                                 // 3rd method
                                 TreeMap< Float, TreeMap< Float, Float > > cfMap = new TreeMap<>();
                                 cfMap.put(0f, new TreeMap<>());
                                 cfMap.put(0.5f, new TreeMap<>());
                                 cfMap.put(1f, new TreeMap<>());

                                 String[][] scopes = new String[][] { { "1.0", "0", "0" }, { "0.5", "0.5", "0" },
                                         { "0", "1.0", "0" } };

                                 for ( FAAnion faItem : faAnions )
                                 {
                                    for ( String[] scope : scopes )
                                    {
                                       String[] detailRow = masterDatabase.getDetail( ce, faItem.getIndex(), fraction.getClazz(), scope[ 0 ], scope[ 1 ], scope[ 2 ] );

                                       float sn2 = Float.parseFloat( scope[ 1 ] );
                                       float isomer = faItem.getFAIsomer();
                                       float ret = Float.parseFloat( detailRow[ 2 ] );

                                       cfMap.get( sn2 ).put( isomer, ret );
                                    }
                                 }

                                 if( isoMin.equals( isoMax ) ) {
                                    // One iso value
                                    //                  System.out.println( "1st CF used for 2nd CF due to only one isomer" );
                                    Range< Float > cfRange = estSampleTreeMap.get( key + ce ).getCF();

                                    float a = cfRange.getMaximum() - cfRange.getMinimum();
                                    float b = cfRange.getMinimum();

                                    Range< Float > posRange = estSampleTreeMap.get( key + ce ).getSecondaryPosition();

                                    float min = a * posRange.getMinimum() + b;
                                    float max = a * posRange.getMaximum() + b;

                                    Range< Float > nextCfRange = Range.between( min, max );
                                    //                  System.out.println( "2nd CF:" + nextCfRange + " at " + ce );
                                    estSampleTreeMap.get( key + ce ).setSecondaryCF( nextCfRange );
                                 }
                                 else
                                 {

                                    TreeMap< Float, Float > minCF = new TreeMap<>();
                                    TreeMap< Float, Float > maxCF = new TreeMap<>();

                                    Range< Float > isoRange = Range.between( isoMin, isoMax );

                                    for ( float i = 0f; i <= 1; i += 0.5f )
                                    {
                                       Range< Float > range = getCFValue( cfMap.get( i ), isoRange );
                                       minCF.put( i, range.getMinimum() );
                                       maxCF.put( i, range.getMaximum() );
                                    }

                                    Range< Float > posRange = estSampleTreeMap.get( key + ce ).getSecondaryPosition();

                                    Range< Float > range = Range.between( getCFValue( minCF, posRange.getMinimum() ), getCFValue( maxCF, posRange.getMaximum() ) );

                                    //                  System.out.println( "Second CF:" + range + " at " + ce );
                                    estSampleTreeMap.get( key + ce ).setSecondaryCF( range );
                                 }
                              }
                           }
                           else
                           {
                              // There is no isomer specific values, estimate CF based on the position
                              for ( Float ce : fraction.keySet() )
                              {
                                 Range< Float > cfRange = estSampleTreeMap.get( key + ce ).getCF();

                                 float a = cfRange.getMaximum() - cfRange.getMinimum();
                                 float b = cfRange.getMinimum();

                                 Range< Float > posRange = estSampleTreeMap.get( key + ce ).getSecondaryPosition();

                                 float min = a * posRange.getMinimum() + b;
                                 float max = a * posRange.getMaximum() + b;

                                 Range< Float > nextCfRange = Range.between( min, max );
                                 estSampleTreeMap.get( key + ce ).setSecondaryCF( nextCfRange );
                              }
                           }
								}
							}

							masterDatabase.close();

							Fraction fraction = sampleTreeMap.get( item.getValue() );

							// 2. Position estimation by using the secondary corrected FAI
							for( Float ce : fraction.keySet() )
							{
                        Float sumMin = null, sumMax = null;
                        for ( String key : sampleTreeMap.keySet() )
                        {
                           Range< Float > faiRange = estSampleTreeMap.get( key + ce ).getSecondCFCorrectedFAI();

                           if ( null == sumMin )
                           {
                              sumMin = faiRange.getMinimum();
                              sumMax = faiRange.getMaximum();
                           }
                           else
                           {
                              sumMin += faiRange.getMinimum();
                              sumMax += faiRange.getMaximum();
                           }
                        }

                        for ( String key : sampleTreeMap.keySet() )
                        {
                           Range< Float > val = estSampleTreeMap.get( key + ce ).getSecondCFCorrectedFAI();

                           //            Range< Float > pos = Range.between( val.getMinimum() / sumMin, val.getMaximum() / sumMax );
                           float posAvg = val.getMinimum() / sumMin;

                           Range<Float> pos = posAvg > 0.5f ? Range.between( val.getMinimum() / sumMin, val.getMaximum() / sumMax ) :
                                   Range.between( val.getMaximum() / sumMax, val.getMinimum() / sumMin );

                           if( sumMin != 0 && sumMax != 0)
                              estSampleTreeMap.get( key + ce ).setSecondaryRel_FAI( pos );
                        }
							}

							for( String key : sampleTreeMap.keySet() )
							{
                        Float posMin = null, posMax = null;

                        for( Float ce : sampleTreeMap.get(key).keySet() )
                        {
                           Range<Float> pos = estSampleTreeMap.get( key + ce ).getSecondaryRel_FAI();

                           if( null == posMin )
                           {
                              posMin = pos.getMinimum();
                              posMax = pos.getMaximum();
                           }

                           posMin = Float.min( posMin, pos.getMinimum() );
                           posMax = Float.max( posMax, pos.getMaximum() );
                        }

                        Float estPosMin = null, estPosMax = null;

                        for( Float ce : sampleTreeMap.get(key).keySet() )
                        {
                           // Get the reference line from 255.23
                           Float position0 = referenceFAIMap.get( fraction.getClazz() ).get( ce, "rel0" );
                           Float position1 = referenceFAIMap.get( fraction.getClazz() ).get( ce, "rel1" );

                           if(position0 == null) break;

                           float a = position1 - position0;
                           float b = position0;

                           float min = a * posMin + b;
                           float max = a * posMax + b;

                           if( null == estPosMin )
                           {
                              estPosMin = min;
                              estPosMax = max;
                           }

                           estPosMin = Float.min( estPosMin, min );
                           estPosMax = Float.max( estPosMax, max );
                        }

                        estPosMin = estPosMin == null ? posMin : estPosMin;
                        estPosMax = estPosMax == null ? posMax : estPosMax;

                        if ( estPosMin < 0 )
                           estPosMin = 0f;
                        else if ( estPosMin > 1 )
                           estPosMin = 1f;

                        if ( estPosMax < 0 )
                           estPosMax = 0f;
                        else if ( estPosMax > 1 )
                           estPosMax = 1f;

                        for( Float ce : sampleTreeMap.get(key).keySet() )
                           estSampleTreeMap.get( key + ce ).setThirdPosition( Range.between( estPosMin, estPosMax ) );
							}

							estSampleTableView.refresh();
						}
					}
				}
			}
		} );

		estTreeView.setContextMenu( new ContextMenu( sn1stPosition, sn1stCorrect, sn2ndCorrect ) );


		SplitPane detailSplitPane = new SplitPane(
				new LabeledPane( "Master Database Detail", masterDataTableView ),
				new LabeledPane( "Estimation", estTreeView ) );
		detailSplitPane.setOrientation( Orientation.HORIZONTAL );

		setMasterNode( estSampleTableView );
		setDetailNode( detailSplitPane );

		setDetailSide( Side.BOTTOM );
		setShowDetailNode( true );
		setDividerPosition( 0.5 );
	}

	public static void collectIntensityRatio( LinkedHashSet< TreeItem< BARow > > baSet, HashMap< TreeItem< BARow >, BA > baMap, ObservableList< String > samples, MasterDatabase masterDatabase, float tolerance, TreeMap< String, TreeMap< String, EstSample > > groupSampleTreeMap, HashMap< TreeItem< BARow >, Float > ratioCheckMap, HashMap< Float, Float[] > minMaxXml )
	{
		for( String sampleId : samples )
		{
			TreeMap< String, EstSample > estSampleMap = groupSampleTreeMap.get( "-" + sampleId );

			for( TreeItem< BARow > baRow : baSet )
			{
				if(!ratioCheckMap.containsKey( baRow )) ratioCheckMap.put( baRow, 0.0f );

				final String specie = baRow.getValue().getTitle();
				final String clazz = specie.split( " " )[0];
				final Double priMass = baRow.getValue().getMass();

				BA priBA = baMap.get( baRow );
				Sample priSample = priBA.getSample( sampleId );

				for( Float ce : priSample.getKeys() )
				{
					System.out.println( ce + ": " );
					Float minInt = Float.MAX_VALUE, maxInt = -1 * Float.MAX_VALUE;
					String fais = null;
					Float minXml = null, maxXml = null;

					for( TreeItem< BARow > child : baRow.getChildren() )
					{
						BARow childBaRow = child.getValue();

						EstSample estSample = estSampleMap.get( specie + "-" + childBaRow.getMassString() + ce );

						if(null == fais)
						{
							fais = childBaRow.getMassString() + ",";
						}
						else
						{
							fais += childBaRow.getMassString();
						}

						Range<Float> exp = estSample.getCFCorrectedFAI();

						minInt = Float.min( exp.getMinimum(), minInt );
						maxInt = Float.max( exp.getMaximum(), maxInt );

						if ( !minMaxXml.containsKey( ce ) ) {
							for ( String[] detailRow : masterDatabase.getDetails( 7, clazz, "1.0", "0", "0" ) )
							{
								if( ce == Float.parseFloat( detailRow[0] ) )
								{
									minXml = Float.parseFloat( detailRow[1] );
									maxXml = Float.parseFloat( detailRow[1] );
								}
							}

							for ( String[] detailRow : masterDatabase.getDetails( 7, clazz, "0", "1.0", "0" ) )
							{
								if( ce == Float.parseFloat( detailRow[0] ) )
								{
									minXml = minXml == null ? Float.parseFloat( detailRow[1] ) : Float.min( Float.parseFloat( detailRow[1] ), minXml);
									maxXml = maxXml == null ? Float.parseFloat( detailRow[1] ) : Float.max( Float.parseFloat( detailRow[1] ), maxXml);
								}
							}

							minMaxXml.put(ce, new Float[]{minXml, maxXml});
						}
					}

					Float[] minMax = minMaxXml.get( ce );
					minXml = minMax[0];
					maxXml = minMax[1];

					float condition = maxXml / minXml * tolerance;
					float expValue = maxInt / minInt;

					System.out.println("Exp = " + maxInt + "/" + minInt + " = " + expValue);
					System.out.println("Xml = " + maxXml + "/" + minXml + " * " + tolerance + " = " + condition);

					if( expValue > condition || expValue < 1 ) {
						String error = String.format( "Sample: %s, PRI: %s, FAIs: %s, CE: %s", sampleId, priMass, fais, ce );
						System.err.println( "Intensity Check Failure at " + error );
					}
					else {
						ratioCheckMap.put( baRow, ratioCheckMap.get(baRow) + 1 );
					}
				}
			}
		}
	}

	private void setupMasterDBSet( final ObservableList< FAAnion > mFaAnionsList )
	{
		// Get MasterDBSet
		if(masterDBSet == null)
		{
			if(mFaAnionsList != null && mFaAnionsList.size() > 0)
			{
				MasterDatabase db = new MasterDatabase();

				db.connect();

				masterDBSet = SampleEstimation.createReferenceFAIMap( db, mFaAnionsList, referenceFAIMap );

				db.close();
			}
		}
	}

	protected void uncheckNode( TreeItem<String> item )
	{
		if(item instanceof CheckBoxTreeItem )
			( ( CheckBoxTreeItem ) item ).setSelected( false );
		else
			item.getChildren().forEach( this::uncheckNode );
	}

	private void handleChartEvent( ChartEvent event )
	{
		ChartEvent.ChartEventType eventType = ChartEvent.getChartEventType( event.getEventType() );

		switch ( eventType )
		{
			case CLEAR_CHART:
				uncheckNode( estTreeRoot );
				break;
		}
	}

	private void updateMasterDataTable( final TableView masterDataTableView, final MasterDatabase masterDatabase, final String clazz, final List<FAAnion> faAnions,
			final float ce, final boolean isSym, final LineChart< Number, Number > chart, final String key,
			final TreeItem<String> isomerTreeItem, final TreeItem<String> positionTreeItem )
	{
		// Decide if the given fragment is symmetric or asymmetric
		String[][] scopes = isSym ? (
				clazz.equals( "PCO" ) || clazz.equals( "PEO" ) ? new String[][] { {"0", "1.0", "0"} } : new String[][] { {"0", "0", "1.0"} }) :
				new String[][] { {"1.0", "0", "0"}, {"0.5", "0.5", "0"}, {"0", "1.0", "0"} };

		CheckBoxTreeItem<String> ceItem = null;

		if(faAnions.size() > 0)
		{
			ceItem = new CheckBoxTreeItem<>( ce + " NCE" );

			CheckBoxTreeItem< String > finalCeItem = ceItem;
			ceItem.selectedProperty().addListener( new ChangeListener< Boolean >()
			{
				@Override public void changed( ObservableValue< ? extends Boolean > observable, Boolean oldValue, Boolean newValue )
				{
					finalCeItem.getChildren().forEach( c -> ((CheckBoxTreeItem) c).setSelected( newValue ) );
				}
			} );
		}

		List<Float> estIsomer = new ArrayList<>(  );

		for(String[] scope : scopes)
		{
			TreeMap<Float, Float> isomer = new TreeMap<>(  );

			for(FAAnion faAnion : faAnions)
			{
				if( !fractionTreeMap.containsKey( faAnion.getIndex() ) )
					fractionTreeMap.put( faAnion.getIndex(), new Fraction(clazz, faAnion.getIndex()) );

				for ( String[] detailRow : masterDatabase.getDetails( faAnion.getIndex(), clazz, scope[ 0 ], scope[ 1 ], scope[2] ) )
				{
					if ( ce == Float.parseFloat( detailRow[ 0 ] ) )
					{
						final float ratio = detailRow[ 3 ].isEmpty() || detailRow[ 1 ].isEmpty() ? 0f : Float.parseFloat( detailRow[ 3 ] ) / Float.parseFloat( detailRow[ 1 ] );

						// Add FAI
						fractionTreeMap.get( faAnion.getIndex() ).put( ce, "SN2-" + Float.parseFloat( scope[1] ), Float.parseFloat( detailRow[1] ) );

						if( ratio > 0 )
						{
							// We assume there is CO2Loss
							// Add COI
							fractionTreeMap.get( faAnion.getIndex() ).setContainsCo2Loss( true );

							fractionTreeMap.get( faAnion.getIndex() ).put( ce, "CO2:SN2-" + Float.parseFloat( scope[1] ), Float.parseFloat( detailRow[3] ) );
						}

						final String[] masterRow = new String[]
								{
										faAnion.getIndex() + "",
										faAnion.getMass() + "",
										faAnion.getFACarbon() + "",
										faAnion.getFADoubleBonds() + "",
										faAnion.getFAIsomer() + "",
										scope[ 0 ], scope[ 1 ],
										detailRow[ 0 ], detailRow[ 1 ], detailRow[ 3 ],
										ratio + ""
								};
						masterDataTableView.getItems().add( masterRow );

						isomer.put( ratio, faAnion.getFAIsomer() );

						break;
					}
				}
			}

			if(ceItem != null)
			{
				// For Isomer validation TreeView
				CheckBoxTreeItem<String> snItem = isSym ? new CheckBoxTreeItem<>( String.format( "%s SYM", scope[2] ) ) :
						new CheckBoxTreeItem<>( String.format( "%s SN2", scope[1].equals( "0" )? "0.0" : scope[1] ) );

				snItem.selectedProperty().addListener( new ChangeListener< Boolean >()
				{
					@Override public void changed( ObservableValue< ? extends Boolean > observable, Boolean oldValue, Boolean newValue )
					{
						final String seriesName = key + ":" + snItem.getValue() + "@" + ce + "hcd";
						if(newValue)
						{
							XYChart.Series series = new XYChart.Series();
							series.setName( seriesName );

							for ( Float key: isomer.keySet() )
							{
								//								System.out.println( key + "  " + fragment.get( key ) );
								XYChart.Data node = new XYChart.Data( key, isomer.get( key ) );
								node.setNode( new HoveredNode( isomer.get( key ) ) );
								series.getData().add( node );
							}
							chart.getData().add( series );

							// Print out the equations
							System.err.println( seriesName + " Equation:");

							// Suggest isomer value for the sample with ce
							estSampleTableView.getItems().forEach( c -> {
								if( c.getKey().equals( key ) && c.getCe().equals( ce ) )
								{
									//System.err.println( "For Sample Group - " + c[0] );
									checkIsomerValue( isomer, c.getFaCoRatio() );

                           System.out.println(isomer);
								}
							} );
						}
						else
						{
							chart.getData().removeIf( series -> series.getName().equals( seriesName ) );
						}
					}
				} );

				// Suggest isomer value for the sample with ce
				if(isomer.size() == 1)
				{
					estIsomer.add( isomer.get( isomer.firstKey() ) );
				}
				else
				{
					estSampleTableView.getItems().forEach( c -> {
						if( c.getKey().equals( key ) && c.getCe().equals( ce ) )
						{
							//System.err.println( "For Sample Group - " + c[0] );
							//System.out.println( key + ce + " -- " + isomer + ":" + c.getFaCoRatio() );
							if( c.getFaCoRatio().equals( 0f ) )
								estIsomer.addAll( isomer.values() );
							else
								estIsomer.add( getIsomerValue( isomer, c.getFaCoRatio() ) );
						}
					} );
				}

				ceItem.getChildren().add( snItem );
			}
		}

//		System.out.println( Range.between( Collections.min(estIsomer), Collections.max( estIsomer ) ) );
		Range isomerRange = Range.between( Collections.min( estIsomer ), Collections.max( estIsomer ) );
		//System.out.println( key + ce + " : " + isomerRange );
		estSampleTreeMap.get( key + ce ).setIsomer( isomerRange );

		if(ceItem != null)
		{
			if(faAnions.size() > 1)
				isomerTreeItem.getChildren().add( ceItem );

			if(isSym)
				return;

			// For Sn validation TreeView
			CheckBoxTreeItem<String> nceItem = new CheckBoxTreeItem<>( String.format( "%s NCE", ce ) );
			nceItem.selectedProperty().addListener( new ChangeListener< Boolean >()
			{
				@Override public void changed( ObservableValue< ? extends Boolean > observable, Boolean oldValue, Boolean newValue )
				{
					nceItem.getChildren().forEach( c -> ((CheckBoxTreeItem) c).setSelected( newValue ) );
				}
			} );

			faAnions.forEach( c ->
			{
				Fraction fraction = fractionTreeMap.get( c.getIndex() );

				//System.out.println( c.getIndex() );
				CheckBoxTreeItem<String> idxItem = new CheckBoxTreeItem< String >( c.getIndex() + "" );
				idxItem.selectedProperty().addListener( new ChangeListener< Boolean >()
				{
					final String seriesName = key + ":" + c.getIndex() + "@" + ce + "hcd";

					@Override public void changed( ObservableValue< ? extends Boolean > observable, Boolean oldValue, Boolean newValue )
					{
						if(newValue)
						{
							TreeMap<Float, Float> data = fraction.getFAData( ce );

							XYChart.Series series = new XYChart.Series();
							series.setName( seriesName + "(regression)");

							double[] params = computeRegressionParametersForChart( data );

							if( params != null )
							{
								for ( Float f = 0f; f <= 1f; f += 0.25f)
								{
									Float res = (float) Precision.round( params[2] * f * f + params[1] * f + params[0], 1 );
									XYChart.Data node = new XYChart.Data( f, res );
									node.setNode( new HoveredNode( res ) );
									series.getData().add( node );
								}

								chart.getData().add( series );

								// Print out the equations
								System.err.println( seriesName + " Equation:");

								// Suggest sn value for the sample with ce
								float sampleRatio = sampleTreeMap.get( key ).getNormalizedValue( ce, "Sample" );
								checkSN2Value( data, sampleRatio );
							}

							// Only if CO2Loss is present, add CO2Loss chart
							if( fraction.isContainsCo2Loss() )
							{

								data = fraction.getCOData( ce );

								series = new XYChart.Series();
								series.setName( seriesName + ":CO2Loss (regression)" );

								params = computeRegressionParametersForChart( data );

								if ( params != null )
								{
									for ( Float f = 0f; f <= 1f; f += 0.25f )
									{
										Float res = ( float ) Precision.round( params[ 2 ] * f * f + params[ 1 ] * f + params[ 0 ], 1 );
										XYChart.Data node = new XYChart.Data( f, res );
										node.setNode( new HoveredNode( res ) );
										series.getData().add( node );
									}

									chart.getData().add( series );

									// Print out the equations
									System.err.println( seriesName + ":CO2Loss Equation:" );

									// Suggest sn value for the sample with ce
									float sampleRatio = sampleTreeMap.get( key ).getNormalizedValue( ce, "Sample:CO2" );
									checkSN2Value( data, sampleRatio );
								}
							}

						}
						else
						{
							chart.getData().removeIf( series -> series.getName().startsWith( seriesName ) );
						}
					}
				} );

				nceItem.getChildren().add( idxItem );
			} );

			positionTreeItem.getChildren().add( nceItem );
		}
	}

	/**
	 *
	 * @param masterDatabase
	 * @param faAnions
	 * @param FA_db
	 * @param ce
	 * @param avgIsomer
	 * @param avgPosition
	 * @return
	 */
	private double computeDBCFcurve(
			final MasterDatabase masterDatabase, String clazz, final List<FAAnion> faAnions, boolean isSym, int FA_db, float ce,
			double avgIsomer, double avgPosition )
	{
		String[][] scopes = FA_db < 3 ? new String[][] { {"0.5", "0.5", "0"} } :
				new String[][] { {"1.0", "0", "0"}, {"0.75", "0.25", "0"}, {"0.5", "0.5", "0"}, {"0.25", "0.75", "0"}, {"0", "1.0", "0"} };

		scopes = isSym ? (
				clazz.equals( "PCO" ) || clazz.equals( "PEO" ) ? new String[][] { {"0", "1.0", "0"} } : new String[][] { {"0", "0", "1.0"} }) : scopes;

		double[] xval = new double[scopes.length];
		for(int i = 0; i < xval.length; i++)
			xval[i] = Float.parseFloat( scopes[i][1] );

		double[] yval = faAnions.stream().mapToDouble( FAAnion::getFAIsomer ).toArray();

		double[][] fval = new double[xval.length][yval.length];

		double ret = 0;

		if( isSym )
		{
			for(String[] scope : scopes)
			{
				int j = 0;
				for ( FAAnion faAnion : faAnions )
				{
					for ( String[] detailRow : masterDatabase.getDetails( faAnion.getIndex(), clazz, scope[ 0 ], scope[ 1 ], scope[ 2 ] ) )
					{
						if ( ce == Float.parseFloat( detailRow[ 0 ] ) )
						{
							fval[0][j] = Float.parseFloat( detailRow[ 2 ] );

							break;
						}
					}
					j++;
				}
			}

			LinearInterpolator interpolator = new LinearInterpolator();

			xval = new double[yval.length];

			for( int i = 0; i < yval.length; i++)
				xval[i] = fval[0][i];

			try
			{
				PolynomialSplineFunction func = interpolator.interpolate( yval, xval );

				//			System.out.println( "SN2: 0.5 Isomer: " + avgIsomer + " -> DBCF. = " + func.value( avgIsomer ) );

				ret = func.value( avgIsomer );
			}
			// If there are not enought data points, we assume the only one value for return
			catch (NumberIsTooSmallException exp)
			{
				ret = xval[0];
			}

		}
		else
		{
			if( xval.length == 1 )
			{
				for(String[] scope : scopes)
				{
					for ( FAAnion faAnion : faAnions )
					{
						for ( String[] detailRow : masterDatabase.getDetails( faAnion.getIndex(), clazz, scope[ 0 ], scope[ 1 ], scope[ 2 ] ) )
						{
							if ( ce == Float.parseFloat( detailRow[ 0 ] ) )
							{
								ret = Float.parseFloat( detailRow[ 2 ] );

								break;
							}
						}
					}
				}
			}
			else
			{
				int i = 0;
				for(String[] scope : scopes)
				{
					int j = 0;
					for ( FAAnion faAnion : faAnions )
					{
						for ( String[] detailRow : masterDatabase.getDetails( faAnion.getIndex(), clazz, scope[ 0 ], scope[ 1 ], scope[ 2 ] ) )
						{
							if ( ce == Float.parseFloat( detailRow[ 0 ] ) )
							{
								float dbcf = Float.parseFloat( detailRow[ 2 ] );

								fval[i][j] = dbcf;

								break;
							}
						}
						j++;
					}
					i++;
				}

				if( yval.length == 1 )
				{
					LinearInterpolator interpolator = new LinearInterpolator();

					yval = new double[xval.length];

					for( i = 0; i < yval.length; i++)
						yval[i] = fval[i][0];

					PolynomialSplineFunction func = interpolator.interpolate( xval, yval );

//					System.out.println( "SN2: " + avgPosition + " Isomer: " + avgIsomer + " -> DBCF. = " + func.value( avgPosition ) );

					ret = func.value( avgPosition );
				}
				else
				{
					BivariateFunction func = Bicubic.createBivariateFunction( xval, yval, fval );

//					System.out.println( "SN2: " + avgPosition + " Isomer: " + avgIsomer + " -> DBCF. = " + func.value( avgPosition, avgIsomer ) );

					ret = func.value( avgPosition, avgIsomer );
				}
			}
		}

		return ret;
	}

   private double computeCFcurve(
           final MasterDatabase masterDatabase, String clazz, final List<FAAnion> faAnions, float ce, double isomer, double position )
   {
      String[][] scopes = new String[][] { {"1.0", "0", "0"}, {"0.5", "0.5", "0"}, {"0", "1.0", "0"} };

      double[] xval = new double[scopes.length];
      for(int i = 0; i < xval.length; i++)
         xval[i] = Float.parseFloat( scopes[i][1] );

      double[] yval = faAnions.stream().mapToDouble( FAAnion::getFAIsomer ).toArray();

      double[][] fval = new double[xval.length][yval.length];

      double ret = 0;


      if( xval.length == 1 )
      {
         for(String[] scope : scopes)
         {
            for ( FAAnion faAnion : faAnions )
            {
               for ( String[] detailRow : masterDatabase.getDetails( faAnion.getIndex(), clazz, scope[ 0 ], scope[ 1 ], scope[ 2 ] ) )
               {
                  if ( ce == Float.parseFloat( detailRow[ 0 ] ) )
                  {
                     ret = Float.parseFloat( detailRow[ 2 ] );

                     break;
                  }
               }
            }
         }
      }
      else
      {
         int i = 0;
         for(String[] scope : scopes)
         {
            int j = 0;
            for ( FAAnion faAnion : faAnions )
            {
               for ( String[] detailRow : masterDatabase.getDetails( faAnion.getIndex(), clazz, scope[ 0 ], scope[ 1 ], scope[ 2 ] ) )
               {
                  if ( ce == Float.parseFloat( detailRow[ 0 ] ) )
                  {
                     float dbcf = Float.parseFloat( detailRow[ 2 ] );

                     fval[i][j] = dbcf;

                     break;
                  }
               }
               j++;
            }
            i++;
         }

         if( yval.length == 1 )
         {
            LinearInterpolator interpolator = new LinearInterpolator();

            yval = new double[xval.length];

            for( i = 0; i < yval.length; i++)
               yval[i] = fval[i][0];

            PolynomialSplineFunction func = interpolator.interpolate( xval, yval );

            ret = func.value( position );
         }
         else
         {
            BivariateFunction func = Bicubic.createBivariateFunction( xval, yval, fval );

            ret = func.value( position, isomer );
         }
      }

      return ret;
   }

	private Range<Float> getCFValue( TreeMap< Float, Float > dbcf, Range<Float> ratio )
	{
		if( dbcf.size() < 2 )
		{
			System.err.println( "CF: Data set does not have enough items to extract a function." );
			return Range.between( 1f, 1f );
		}
		else
		{
			double[] params = computeRegressionParameters( false, dbcf );

         if( params.length  == 2)
         {
            double resultMin = Precision.round( params[1] * ratio.getMinimum() + params[0], 3 );

            double resultMax = Precision.round( params[1] * ratio.getMaximum() + params[0], 3 );

            return Range.between( (float) resultMin, (float) resultMax);
         }
         else if(params.length == 3)
			{
				double resultMin = Precision.round( params[2] * ratio.getMinimum() * ratio.getMinimum() + params[1] * ratio.getMinimum() + params[0], 3 );

				double resultMax = Precision.round( params[2] * ratio.getMaximum() * ratio.getMaximum() + params[1] * ratio.getMaximum() + params[0], 3 );

				return Range.between( (float) resultMin, (float) resultMax);
			}
			else
			{
				System.err.println( "Error" );
				return Range.between( 1f, 1f );
			}
		}
	}

   private Float getCFValue( TreeMap< Float, Float > cfMap, Float ratio )
   {
      if( cfMap.size() < 2 )
      {
         System.err.println( "CF: Data set does not have enough items to extract a function." );
         return 1f;
      }
      else
      {
         double[] params = computeRegressionParameters( false, cfMap );

         if( params.length  == 2)
         {
            return (float) Precision.round( params[1] * ratio + params[0], 3 );
         }
         else if(params.length == 3)
         {
            return (float) Precision.round( params[2] * ratio * ratio + params[1] * ratio + params[0], 3 );
         }
         else
         {
            System.err.println( "Error" );
            return 1f;
         }
      }
   }

	private void checkIsomerValue( TreeMap< Float, Float > isomer, float ratio )
	{
		if( isomer.size() < 2 )
		{
			System.err.println( "ISO: Data set does not have enough items to extract a function." );
		}
		else
		{
			double[] params = computeRegressionParameters( true, isomer );

			DoubleSummaryStatistics stat = isomer.values().stream().collect( Collectors.summarizingDouble( Float::doubleValue ) );

			Range<Double> range = Range.between( stat.getMin(), stat.getMax() );


			if( params.length  == 2){
				double result = Precision.round( params[1] * ratio + params[0], 1 );

				if(range.isBefore( result ))
				{
					result = range.getMaximum();
				}
				else if(range.isAfter( result ))
				{
					result = range.getMinimum();
				}

				double pct = (((int) result) - stat.getMin()) / (stat.getMax() - stat.getMin());

				pct = Percent.toFivePercentUnit( pct ) * 100;

				String out = String.format( ">> FAI_iso = %.1f %d %% (%dz) and %d %% (%dz)",
						result, (int) pct, (int) stat.getMax(), (int) (100 - pct), (int) stat.getMin());
				System.out.println( out );

			}
			else if(params.length == 3)
			{
				double result = Precision.round( params[2] * ratio * ratio + params[1] * ratio + params[0], 1 );

				if(range.isBefore( result ))
				{
					result = range.getMaximum();
				}
				else if(range.isAfter( result ))
				{
					result = range.getMinimum();
				}

				double pct = (((int) result) - stat.getMin()) / (stat.getMax() - stat.getMin());

				pct = Percent.toFivePercentUnit( pct ) * 100;

				String out = String.format( ">> FAI_iso = %.1f, %d %% (%dz) and %d %% (%dz)",
						result, (int) pct, (int) stat.getMax(), (int) (100 - pct), (int) stat.getMin());
				System.out.println( out );
			}
			else
				System.err.println( "Error" );
		}
	}

	private float getIsomerValue( TreeMap< Float, Float > isomer, float ratio )
	{
		if( isomer.size() < 2 )
		{
			System.err.println( "ISO: Data set does not have enough items to extract a function." );
			return 0f;
		}
		else
		{
			double[] params = computeRegressionParameters( false, isomer );

			DoubleSummaryStatistics stat = isomer.values().stream().collect( Collectors.summarizingDouble( Float::doubleValue ) );

			Range<Double> range = Range.between( stat.getMin(), stat.getMax() );

			if( params.length  == 2){
				double result = Precision.round( params[1] * ratio + params[0], 1 );

				if(range.isBefore( result ))
				{
					result = range.getMaximum();
				}
				else if(range.isAfter( result ))
				{
					result = range.getMinimum();
				}

				return (float) result;
			}
			else if(params.length == 3)
			{
				double result = Precision.round( params[2] * ratio * ratio + params[1] * ratio + params[0], 1 );

				if(range.isBefore( result ))
				{
					result = range.getMaximum();
				}
				else if(range.isAfter( result ))
				{
					result = range.getMinimum();
				}

				return (float) result;
			}
			else
			{
				System.err.println( "Error" );
				return 0f;
			}
		}
	}

	private void checkSN2Value( TreeMap< Float, Float > data, float ratio )
	{
		if( data.size() < 3 )
		{
//			System.err.println( "(SampleValidation) SN2: Data set does not have enough items to extract a function." );
		}
		else
		{
			double[] params = computeRegressionParameters( true, data );

			DoubleSummaryStatistics stat = data.values().stream().collect( Collectors.summarizingDouble( Float::doubleValue ) );

			Range<Double> range = Range.between( stat.getMin(), stat.getMax() );

			if(params.length == 3)
			{
				double result = Precision.round( params[2] * ratio * ratio + params[1] * ratio + params[0], 1 );

				if(range.isBefore( result ))
				{
					result = range.getMaximum();
				}
				else if(range.isAfter( result ))
				{
					result = range.getMinimum();
				}

				double pct = (result - stat.getMin()) / (stat.getMax() - stat.getMin());

				pct = Percent.toFivePercentUnit( pct ) * 100;

				String out = String.format( ">> FAI_SN2 = %.1f, %d %% (SN2) and %d %% (SN1)",
						result, (int) pct, (int) (100 - pct) );
				System.out.println( out );
			}
			else
				System.err.println( "Error" );
		}
	}

	private float getSN2Value( TreeMap< Float, Float > data, float ratio )
	{
		if( data.size() < 3 )
		{
			System.err.println( "(SampleValidation-updateDBCF) SN2: Data set does not have enough items to extract a function." );
			return 0f;
		}
		else
		{
			double[] params = computeRegressionParameters( true, data );

			DoubleSummaryStatistics stat = data.values().stream().collect( Collectors.summarizingDouble( Float::doubleValue ) );

			Range<Double> range = Range.between( stat.getMin(), stat.getMax() );

			if(params.length == 3)
			{
				double result = Precision.round( params[2] * ratio * ratio + params[1] * ratio + params[0], 1 );

				if(range.isBefore( result ))
				{
					result = range.getMaximum();
				}
				else if(range.isAfter( result ))
				{
					result = range.getMinimum();
				}

				double pct = (result - stat.getMin()) / (stat.getMax() - stat.getMin());

				pct = Percent.toFivePercentUnit( pct ) * 100;

				String out = String.format( ">> FAI_SN2 = %.1f, %d %% (SN2) and %d %% (SN1)",
						result, (int) pct, (int) (100 - pct) );
				System.out.println( out );

				return (float) result;
			}
			else
			{
				System.err.println( "Error" );
				return 0f;
			}
		}
	}

	private List< FAAnion > getMassIndexes( Collection<FAAnion> collection, FAAnion faAnion )
	{
		return collection.stream().filter( c -> c.getMass().equals( faAnion.getMass() ) ).collect( Collectors.toList() );
	}

	private int getFA_ID( Collection<FAAnion> collection, Double mass )
	{
		return collection.stream().filter( c -> c.getMass().equals( mass ) ).findFirst().get().getIndex();
	}
}
