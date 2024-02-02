package de.mpicbg.ms.view.pipeline.dataprep;

import de.mpicbg.ms.model.data.FAAnion;
import de.mpicbg.ms.model.event.ChartEvent;
import de.mpicbg.ms.model.CycleCollection;
import de.mpicbg.ms.model.Fragment;
import de.mpicbg.ms.model.FragmentCollection;
import de.mpicbg.ms.model.SplineComposite;
import de.mpicbg.ms.model.data.Mass;
import de.mpicbg.ms.model.MzIntCollection;
import de.mpicbg.ms.model.MzXMLConverter;
import de.mpicbg.ms.model.event.ProcessEvent;

import de.mpicbg.ms.model.fitter.ExponentialFitter;
import de.mpicbg.ms.model.fitter.WeightedSplineInterpolator;
import de.mpicbg.ms.view.chart.ErrorBarNode;
import de.mpicbg.ms.view.chart.HoveredNode;
import de.mpicbg.ms.view.chart.StickChart;
import de.mpicbg.ms.view.treecell.SelectiveCheckBoxTreeCell;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ContextMenuBuilder;
import javafx.scene.control.MenuItemBuilder;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONWriter;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * DataTreePane contains all the opened mzXML as checkTreeView
 */
public class DataPrepPane
{
	private LineChart< Number, Number > chart;
	private StickChart< Number, Number > stickChart;

	public TreeView< String > getCheckTreeView()
	{
		return checkTreeView;
	}

	private int numOfItems;
	private final TreeView<String> checkTreeView;
	private final TreeItem<String> dataNode, refined_averages;

	private final TreeMap<String, TreeMap<String, CycleCollection>> classToCycleCollection;

	private TreeMap<String, TreeMap<String, FragmentCollection>> classToFragmentCollection;

	private Float collisionEnergy;
	private HashMap<Integer, CycleCollection.Cycle> cycleHashMap = new HashMap<>();

	private DecimalFormat df;

	private final HashSet<TreeItem> selectedTreeItems = new HashSet<>();
	private final HashSet<TreeItem> removeTreeItems = new HashSet<>();

   protected ObservableList< FAAnion > mFaAnionsList;

	public DataPrepPane( LineChart< Number, Number > chart, StickChart< Number, Number > stickChart,
			TreeMap< String, TreeMap< String, CycleCollection > > classToCycleCollection )
	{
		this.chart = chart;
		this.stickChart = stickChart;

		this.classToCycleCollection = classToCycleCollection;
		this.classToFragmentCollection = new TreeMap<>();

		TreeItem< String > root = new TreeItem<>( "Root" );
		root.setExpanded( true );


		dataNode = new TreeItem<>( "Data" );
		refined_averages = new TreeItem<>( "Refined_averages" );

		root.getChildren().addAll( dataNode, refined_averages );

		checkTreeView = new TreeView<>( root );
		checkTreeView.setShowRoot( false );
		checkTreeView.setCellFactory( SelectiveCheckBoxTreeCell.<String>forTreeView());


		// Adding Process Event Types
		checkTreeView.addEventHandler( ProcessEvent.DATA_PREP, new EventHandler< ProcessEvent >()
		{
			@Override public void handle( ProcessEvent event )
			{
				handleProcessEvent( event );
			}
		} );

		checkTreeView.addEventHandler( ChartEvent.ANY, new EventHandler< ChartEvent >()
		{
			@Override public void handle( ChartEvent event )
			{
				handleChartEvent( event );
			}
		} );

		ContextMenu rootContextMenu;

		rootContextMenu = ContextMenuBuilder.create()
				.items(
						MenuItemBuilder.create()
								.text( "Clear charts" )
								.onAction(
										new EventHandler< ActionEvent >()
										{
											@Override
											public void handle( ActionEvent arg0 )
											{
												for ( TreeItem item : selectedTreeItems )
												{
													//Event.fireEvent( item, new ChartEvent( ChartEvent.CLEAR, null ) );
													if( item instanceof CheckBoxTreeItem )
													{
														((CheckBoxTreeItem)item).setSelected( false );
													}
												}

												selectedTreeItems.clear();
												chart.getData().clear();
												stickChart.getData().clear();
											}
										}
								)
								.build(),
						MenuItemBuilder.create()
								.text( "Remove" )
								.onAction(
										new EventHandler< ActionEvent >()
										{
											@Override
											public void handle( ActionEvent arg0 )
											{
												ArrayList<TreeItem> toRemove = new ArrayList< TreeItem >( removeTreeItems );
												for ( TreeItem item : toRemove )
												{
													Event.fireEvent( item, new ProcessEvent( ProcessEvent.REMOVE_TREE_ITEM, null ) );
												}
											}
										}
								)
								.build()
				)
				.build();

		checkTreeView.setContextMenu( rootContextMenu );
	}

	// The first step
	private void addCycles()
	{
		numOfItems = 0;
		// Clear Chart series
		chart.getData().clear();
		stickChart.getData().clear();

		// Clear collections
		cycleHashMap.values().forEach( c -> c.clear() );
		cycleHashMap.clear();

		// Clear dataNode children
		dataNode.getChildren().clear();

		classToFragmentCollection.values().forEach( c -> c.clear() );
		classToFragmentCollection.clear();

		for ( String clazz : classToCycleCollection.keySet() )
		{
			TreeItem<String> classTreeItem = new TreeItem<>( clazz );
			dataNode.getChildren().add( classTreeItem );

			TreeMap<String, CycleCollection> cycleCollectionTreeMap = classToCycleCollection.get(clazz);

			for ( String group : cycleCollectionTreeMap.keySet() )
			{
				TreeItem groupTreeItem = new TreeItem<>( group );
				classTreeItem.getChildren().add( groupTreeItem );

				TreeItem cycles = new TreeItem<>("Cycles");
				groupTreeItem.getChildren().add(cycles);

				numOfItems++;

				for ( CycleCollection.Cycle cycle : cycleCollectionTreeMap.get(group).getCycles() )
				{
					CheckBoxTreeItem<String> item = new CheckBoxTreeItem<>( String.format( "%d", cycle.getId() ) );

					item.selectedProperty().addListener( new ChangeListener< Boolean >()
					{
						@Override public void changed( ObservableValue< ? extends Boolean > observable, Boolean oldValue, Boolean newValue )
						{
							if(newValue)
							{
								removeTreeItems.add( item );
								if(collisionEnergy != null) drawStickChart( cycle, item );
							}
							else
							{
								removeTreeItems.remove( item );
								stickChart.getData().removeIf( series -> series.getName().equals( "Cycle-" + item.getValue() ) );
							}
						}
					} );

					item.addEventHandler( ProcessEvent.REMOVE_TREE_ITEM, new EventHandler< ProcessEvent >()
					{
						@Override public void handle( ProcessEvent event )
						{
							item.setSelected( false );
							item.getChildren().clear();
							cycles.getChildren().remove( item );

							cycleCollectionTreeMap.get(group).getCycles().remove( cycle );
						}
					} );

					cycleHashMap.put(cycle.getId(), cycle);

					cycles.getChildren().add( item );

				}
			}
		}
	}

	private void drawStickChart(CycleCollection.Cycle cycle, CheckBoxTreeItem<String> item)
	{
		stickChart.getData().removeIf( c -> c.getName().equals( "Cycle-" + item.getValue() ) );

		XYChart.Series series = new XYChart.Series();
		series.setName( "Cycle-" + item.getValue() );

		double mzValues[] ;
		float intensityValues[];

		mzValues = MzXMLConverter.extractMzValues( cycle.getScan( collisionEnergy ), null );
		intensityValues = MzXMLConverter.extractIntensityValues( cycle.getScan( collisionEnergy ), null );


		MzIntCollection collection = new MzIntCollection( mzValues, intensityValues );
		collection.sort( MzIntCollection.SortingProperty.MZ, MzIntCollection.SortingDirection.ASCENDING );

		for ( int i = 0; i < mzValues.length; i++ )
		{
			XYChart.Data node = new XYChart.Data( mzValues[i], intensityValues[i] );
			series.getData().add( node );
		}
		stickChart.getData().add( series );
	}



	// The second step
	private void createFragments( Object[] object )
	{
		// Clear chart series
		chart.getData().clear();

		// Set up the progress bar
		final ProgressIndicator pi = (ProgressIndicator) object[0];
		pi.progressProperty().unbind();
		pi.progressProperty().bind( FragmentCollection.progressPropertyProperty() );
		FragmentCollection.setSizeProperty( numOfItems );

		final int consecutiveNumber = (Integer) object[1];
		final int mmu = (Integer) object[2];

		initMMU( mmu );

		final ObservableList<Mass> validFragments = (ObservableList<Mass>) object[3];
		final SimpleBooleanProperty done = (SimpleBooleanProperty) object[4];

		classToFragmentCollection.values().forEach( c -> c.values().forEach( d -> d.clear() ) );
		classToFragmentCollection.values().forEach( c -> c.clear() );
		classToFragmentCollection.values().clear();
		classToFragmentCollection.clear();

		createFragments( consecutiveNumber, mmu, validFragments );

		done.set( true );
	}

	private void createFragments( final int consecutiveNumber, final int mmu, ObservableList<Mass> validFragments )
	{
		final TreeSet<Double> validFragmentsSet = new TreeSet<>(  );

		int count = 1;
		for ( String clazz : classToCycleCollection.keySet() )
		{
			TreeItem classTreeItem = searchTreeItem( dataNode, clazz );
			if(classTreeItem == null)
			{
				classTreeItem = new TreeItem<>( clazz );
				dataNode.getChildren().add( classTreeItem );
			}

			TreeMap<String, FragmentCollection> fragmentCollectionTreeMap = new TreeMap<String, FragmentCollection>();

			classToFragmentCollection.put(clazz, fragmentCollectionTreeMap);

			for ( String group : classToCycleCollection.get(clazz).keySet() )
			{
				TreeItem groupTreeItem = searchTreeItem( classTreeItem, group );
				if( groupTreeItem == null )
				{
					groupTreeItem = new TreeItem<>( group );
					classTreeItem.getChildren().add( groupTreeItem );
				}

				groupTreeItem.getChildren().removeIf( c -> ((TreeItem<String>)c).getValue().equals( "Fragments" ) );

				TreeItem fragments = new TreeItem<>("Fragments");
				groupTreeItem.getChildren().add(fragments);


				if( !fragmentCollectionTreeMap.containsKey( group ) )
					fragmentCollectionTreeMap.put( group, new FragmentCollection() );

				FragmentCollection fragmentCollection = fragmentCollectionTreeMap.get(group);

				CycleCollection cycleCollection = classToCycleCollection.get(clazz).get(group);

				fragmentCollection.addCycles( count++, cycleCollection, consecutiveNumber, mmu );

				addFragmentTree( fragments, validFragmentsSet, validFragments, fragmentCollection );
			}
		}

		validFragments.addAll( validFragmentsSet.stream().map( mz -> new Mass( mz, "", "" ) ).collect( Collectors.toList() ) );

	}

	private void initMMU( int mmu )
	{
		final int digit = 3 - (int) Math.log10(mmu);
		switch ( digit )
		{
			case 1 : df = new DecimalFormat( "###.0" );	break;
			case 2 : df = new DecimalFormat( "###.00" ); break;
			case 3 : df = new DecimalFormat( "###.000" ); break;
		}
	}

	private void loadFragments( TreeMap<String, TreeMap<String, FragmentCollection>> loadedFragmentCollection, int mmu, ObservableList<Mass> validFragments )
	{
		final TreeSet<Double> validFragmentsSet = new TreeSet<>(  );

		initMMU( mmu );

		for ( String clazz : loadedFragmentCollection.keySet() )
		{
			TreeItem classTreeItem = searchTreeItem( dataNode, clazz );
			if(classTreeItem == null)
			{
				classTreeItem = new TreeItem<>( clazz );
				dataNode.getChildren().add( classTreeItem );
			}

			if( !classToFragmentCollection.containsKey( clazz ) )
				classToFragmentCollection.put( clazz, loadedFragmentCollection.get( clazz ) );

			TreeMap<String, FragmentCollection> fragmentCollectionTreeMap = classToFragmentCollection.get(clazz);

			for ( String group : loadedFragmentCollection.get(clazz).keySet() )
			{
				TreeItem groupTreeItem = searchTreeItem( classTreeItem, group );
				if( groupTreeItem == null )
				{
					groupTreeItem = new TreeItem<>( group );
					classTreeItem.getChildren().add( groupTreeItem );
				}

				TreeItem fragments = new TreeItem<>("Fragments");
				groupTreeItem.getChildren().add(fragments);

				FragmentCollection fragmentCollection = fragmentCollectionTreeMap.get(group);

				if( !classToFragmentCollection.get( clazz ).containsKey( group ) )
					classToFragmentCollection.get( clazz ).put( group, fragmentCollection );

				addFragmentTree( fragments, validFragmentsSet, validFragments, fragmentCollection );
			}
		}

		validFragments.addAll( validFragmentsSet.stream().map( mz -> new Mass( mz, "", "" ) ).collect( Collectors.toList() ) );
	}

	private void addFragmentTree( TreeItem fragments, TreeSet<Double> validFragmentsSet,
			ObservableList<Mass> validFragments, FragmentCollection fragmentCollection )
	{
		TreeMap< Double, TreeMap< Integer, Fragment > > fragmentMap = fragmentCollection.getFragments();

		// Setup TIC
		CheckBoxTreeItem<String> ticItem = new CheckBoxTreeItem<>( "TIC" );
		TreeMap< Integer, TreeMap< Float, Float > > ticMap = fragmentCollection.getTicMap();

		for( Integer cycle : ticMap.keySet() )
		{
			CheckBoxTreeItem<String> cycleItem = new CheckBoxTreeItem<>( String.format( "Cycle-%d", cycle ) );

			cycleItem.selectedProperty().addListener( new ChangeListener< Boolean >()
			{
				@Override public void changed( ObservableValue< ? extends Boolean > observable, Boolean oldValue, Boolean newValue )
				{
					if(newValue)
					{
						selectedTreeItems.add( cycleItem );
						XYChart.Series series = new XYChart.Series();
						series.setName( "TIC-" + cycleItem.getValue() );

						TreeMap< Float, Float > ticValues = ticMap.get( cycle );

						for ( Float key: ticValues.keySet() )
						{
							XYChart.Data node = new XYChart.Data( key, ticValues.get( key ) );
							series.getData().add( node );
						}
						chart.getData().add( series );
					}

					else
					{
						chart.getData().removeIf( series -> series.getName().equals( "TIC-" + cycleItem.getValue() ) );
					}
				}
			} );

			ticItem.getChildren().add( cycleItem );
		}
		fragments.getChildren().add( ticItem );

		ArrayList<Double> list = new ArrayList<>( fragmentMap.keySet() );

		// Setup Mass
		for ( Double mz : list )
		{
			validFragmentsSet.add( mz );

			CheckBoxTreeItem<String> item = new CheckBoxTreeItem<>( df.format( mz ) );

			item.selectedProperty().addListener( new ChangeListener< Boolean >()
			{
				@Override public void changed( ObservableValue< ? extends Boolean > observable, Boolean oldValue, Boolean newValue )
				{
					if(newValue)
						removeTreeItems.add( item );
					else
						removeTreeItems.remove( item );
				}
			} );

			item.addEventHandler( ProcessEvent.REMOVE_TREE_ITEM, new EventHandler< ProcessEvent >()
			{
				@Override public void handle( ProcessEvent event )
				{
					item.setSelected( false );
					item.getChildren().clear();
					fragmentMap.get( mz ).clear();

					fragments.getChildren().remove( item );
					fragmentMap.remove( mz );
					validFragments.remove( mz );
				}
			} );

			for ( Integer cycle : fragmentMap.get(mz).keySet() )
			{
				CheckBoxTreeItem<String> cycleItem = new CheckBoxTreeItem<>( String.format( "Cycle-%d", cycle ) );

				cycleItem.selectedProperty().addListener( new ChangeListener< Boolean >()
				{
					@Override public void changed( ObservableValue< ? extends Boolean > observable, Boolean oldValue, Boolean newValue )
					{
						if(newValue)
						{
							selectedTreeItems.add( cycleItem );
							XYChart.Series series = new XYChart.Series();
							series.setName( "M/Z-" + item.getValue() + "-" + cycleItem.getValue() );

							Fragment fragment = fragmentMap.get( mz ).get( cycle );

							for ( Float key: fragment.keys() )
							{
								//								System.out.println( key + "  " + fragment.get( key ) );
								XYChart.Data node = new XYChart.Data( key, fragment.get( key ) );
								series.getData().add( node );
							}
							chart.getData().add( series );
						}
						else
						{
							chart.getData().removeIf( series -> series.getName().equals( "M/Z-" + item.getValue() + "-" + cycleItem.getValue() ) );
						}

					}
				} );

				item.getChildren().add( cycleItem );
			}

			fragments.getChildren().add( item );
		}
	}

	private void refineData( Object[] params )
	{
		// Initialize Average Tree Map
		avgTreeMap.clear();

		// Clear chart series
		chart.getData().clear();
		refined_averages.getChildren().clear();

		final ObservableList<Mass> validFragments = (ObservableList<Mass>) params[5];

		for ( String clazz : classToFragmentCollection.keySet() )
		{
			TreeItem<String> classTreeItem = new TreeItem<>( clazz );
			refined_averages.getChildren().add( classTreeItem );
			refined_averages.setExpanded( true );

			TreeMap<String, FragmentCollection> fragmentCollectionTreeMap =
				classToFragmentCollection.get(clazz);

			for ( String group : classToFragmentCollection.get(clazz).keySet() )
			{
				FragmentCollection fragmentCollection = fragmentCollectionTreeMap.get(group);
				TreeMap< Double, TreeMap<Integer, Fragment > > data = fragmentCollection.getFragments();

				// Remove the unchecked items from the valid fragments
				final TreeMap< Double, TreeMap< Integer, Fragment > > finalData = data;
				validFragments.stream().forEach( c ->
				{
					if( !c.getValidProperty() )
						finalData.remove( c.getMass() );
				});

				// 3. Refine data
				// 3.1 Normalize
				if( (boolean) params[0] )
				{
					data = normalize( clazz, group, data, (int) params[1], (int) params[6] );
					fragmentCollection.addSubDataset( data );
				}

				// 3.2 boxcar average
//				if ( (boolean) params[2] )
//				{
//					data = averageWithBoxcar( clazz, group, data, 2 );
//					fragmentCollection.addSubDataset( data );
//				}
				// 3.2 3-point average
				if ( (boolean) params[2] )
				{
					data = averageWithThreePoints( clazz, group, data );
					fragmentCollection.addSubDataset( data );
				}

				// 3.3 Remove outliers
				if ( (boolean) params[3] )
				{
					data = removeOutliers( clazz, group, data, (int) params[4] );
					fragmentCollection.addSubDataset( data );
				}
			}
		}
	}

   private void exportJson(Window parent) {
      if(avgTreeMap.size() == 0) {
         Alert alert = new Alert( Alert.AlertType.WARNING, "Please refine first.");
         alert.show();
         return;
      }

      final FileChooser fileChooser = new FileChooser();
      fileChooser.setTitle("Save JSON file");
      fileChooser.getExtensionFilters().addAll(
              new FileChooser.ExtensionFilter( "JSON File", "*.json")
      );

      File fileName = fileChooser.showSaveDialog( parent );

      new Thread( () ->
      {
         exportAvgCollection(fileName, avgTreeMap);
      } ).start();
   }

   private void exportAvgCollection(File file, TreeMap<String, TreeMap<String, TreeMap< Double, Fragment > > > avgTree)
   {
      JSONObject jo = new JSONObject();
      for ( String clazz : avgTree.keySet() )
      {
         TreeMap< String, TreeMap< Double, Fragment > > fragmentCollectionTreeMap =
                 avgTree.get( clazz );

         for ( String group : fragmentCollectionTreeMap.keySet() )
         {
            JSONObject jg = new JSONObject();

            System.out.println( reformatFragStr(group) );

            TreeMap< Double, Fragment > map = fragmentCollectionTreeMap.get(group);
            for ( Double mz : map.keySet()) {
               if (!mz.equals( 0d ))
               {
                  Fragment frag = map.get(mz);

                  JSONObject obj = new JSONObject();
                  obj.put( "mz", mz );

                  Float[] keys = frag.keys().toArray(new Float[0]);
                  obj.put( "nce", keys);

                  Float[] nce = keys.clone();

                  for ( int i = 0; i < keys.length; i++ )
                  {
                     nce[i] = frag.get(keys[i]);
                  }

                  obj.put( "int", nce);

                  jg.put( mz + "", obj );
               }
            }

            jo.put( reformatFragStr(group), jg );
         }

//         System.out.println(jo.toString(4));
      }

      FileWriter writer;
      try
      {
         writer = new FileWriter( file );
         jo.write( writer, 4, 0 );
         writer.close();
      }
      catch ( IOException e )
      {
         throw new RuntimeException( e );
      }
   }

   public static String reformatFragStr( String group )
   {
      if (group.startsWith( "DM" )) {
         if(group.endsWith( "z" )) {
            return String.format( "%s %s:%s/%s:%s (%s)", group.substring( 0, 4 ), group.substring( 4, 6 ), group.substring( 6, 7 ), group.substring( 7, 9 ), group.substring( 9, 10 ), group.substring( 10 ) );
         } else {
            return String.format( "%s %s:%s/%s:%s", group.substring( 0, 4 ), group.substring( 4, 6 ), group.substring( 6, 7 ), group.substring( 7, 9 ), group.substring( 9 ) );
         }
      }
      else if (group.startsWith( "PEO" ) || group.startsWith( "PCO" ) )
      {
         if(group.endsWith( "s" ) || group.endsWith( "c" ))
            group = group.replaceAll( "s|c", "" );

         if(group.endsWith( "z" )) {
            return String.format( "%s %s:%s/%s:%s (%s)", group.substring( 0, 3 ), group.substring( 3, 5 ), group.substring( 5, 6 ), group.substring( 6, 8 ), group.substring( 8, 9 ), group.substring( 9 ) );
         } else {
            return String.format( "%s %s:%s/%s:%s", group.substring( 0, 3 ), group.substring( 3, 5 ), group.substring( 5, 6 ), group.substring( 6, 8 ), group.substring( 8 ) );
         }
      } else {
         return String.format( "%s %s:%s/%s:%s", group.substring( 0, 2 ), group.substring( 2, 4 ), group.substring( 4, 5 ), group.substring( 5, 7 ), group.substring( 7 ) );
      }
   }

	private TreeMap<String, TreeMap<String, TreeMap< Double, Fragment > > > avgTreeMap = new TreeMap<>();

	private TreeMap< Double, TreeMap<Integer, Fragment > > normalize(
			String clazz,
			String group,
			TreeMap< Double, TreeMap<Integer, Fragment > > data,
			int basisPoints, int maximaThreshold )
	{
		final String lNormalized = "Normalized";
		final FragmentCollection fragmentCollection = classToFragmentCollection.get(clazz).get( group );

		final TreeMap< Double, TreeMap<Integer, Fragment > > normalizedTreeMap = fragmentCollection.normalize( data, basisPoints, maximaThreshold );

		//createTICSeries( group, fragmentCollection.getCurrentTicInformation(), lNormalized );
		createSeries( clazz, group, normalizedTreeMap, lNormalized );

		return normalizedTreeMap;
	}

	private TreeMap< Double, TreeMap<Integer, Fragment > > averageWithThreePoints (
			String clazz,
			String group,
			TreeMap< Double, TreeMap<Integer, Fragment > > data )
	{

		final String lThreePointAvg = "3-point averaged";
		final FragmentCollection fragmentCollection = classToFragmentCollection.get(clazz).get( group );

		final TreeMap< Double, TreeMap<Integer, Fragment > > avgThreePoints = fragmentCollection.averageWithThreePoints( data );

		createSeries( clazz, group, avgThreePoints, lThreePointAvg );

		return avgThreePoints;
	}

	private TreeMap< Double, TreeMap<Integer, Fragment > > averageWithBoxcar (
			String clazz,
			String group,
			TreeMap< Double, TreeMap<Integer, Fragment > > data, int size )
	{

		final String lAvgBoxcar = size + "-point averaged boxcar";
		final FragmentCollection fragmentCollection = classToFragmentCollection.get(clazz).get( group );

		final TreeMap< Double, TreeMap<Integer, Fragment > > avgBoxcar = fragmentCollection.averageWithBoxCar( data, size );

		createSeries( clazz, group, avgBoxcar, lAvgBoxcar );

		return avgBoxcar;
	}

	private TreeMap< Double, TreeMap<Integer, Fragment > > removeOutliers (
			String clazz,
			String group,
			TreeMap< Double, TreeMap<Integer, Fragment > > data,
			int range)
	{
		final FragmentCollection fragmentCollection = classToFragmentCollection.get(clazz).get( group );

		final List<TreeMap> refinedData = fragmentCollection.removeOutliers( data, range );

		createSeries( clazz, group, refinedData.get(0), "Refined" );

		createSeries( clazz, group, refinedData.get(1), "Outliers" );

		TreeMap<Double, Fragment> interpolated = createRepSeries( clazz, group, refinedData.get( 2 ), refinedData.get( 3 ), refinedData.get( 4 ), "Refined Average" );

		if(!avgTreeMap.containsKey( clazz ))
			avgTreeMap.put(clazz, new TreeMap<>(  ));

//		avgTreeMap.get(clazz).put(group, interpolated );
      avgTreeMap.get(clazz).put(group, refinedData.get( 2 ) );

		return refinedData.get(0);
	}

	TreeItem<String> searchTreeItem(TreeItem<String> item, String name) {

		if(item.getValue().equals(name)) return item; // hit!

		// continue on the children:
		TreeItem<String> result = null;
		for(TreeItem<String> child : item.getChildren()){
			result = searchTreeItem(child, name);
			if(result != null) return result; // hit!
		}

		//no hit:
		return null;
	}

	final int[] height = new int[] { 4, 9, 14, 19, 24, 29, 34, 39, 44, 49, 54, 59, 64, 69, 74, 79, 84, // 0-16
			89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, // 17-37
			114, 119, 124, 129, 134, 139, 144, 149, 154, 159, 164, 169, 174, 179, 184, 189, 194 }; // 38-54

	private TreeMap< Double, Fragment> createRepSeries( String clazz, String group, TreeMap treeMap, TreeMap stdDevMap, TreeMap stdErrMap, String treeName )
	{
		TreeMap< Double, Fragment> data = (TreeMap<Double, Fragment>)treeMap;
		TreeMap< Double, TreeMap<Float, Float> > stddev = stdDevMap;
		TreeMap< Double, TreeMap<Float, Float> > stderr = stdErrMap;

		TreeMap< Double, Fragment> interpolatedData = new TreeMap<>();

		// Get class from user input panel, otherwise add group directly
		TreeItem classItem;
		TreeItem treeItem;

		ArrayList<Double> list = new ArrayList<>( data.keySet() );

		for ( Double mz : list )
		{
			Fragment frag = data.get( mz );

         mFaAnionsList.filtered( p -> p.getMass().equals( mz ) ).stream()
                 .findFirst().ifPresent( c ->
         {
            frag.setFaIndex( c.getIndex() );
            frag.setIsomer( c.getFAIsomer() );
            frag.setCarbon( c.getFACarbon() );
            frag.setDoubleBond( c.getFADoubleBonds() );
         } );


         //                        25%, 55%, 90%, 95%, 100%, 105%, 110%, 145%, 175%, 180%
			//int[] knots = new int[] { 4,  10,  17,  22,   27,   32,   37,   44,   49,   51 };
			//                        25%, 55%, 90%, 100%, 110%, 145%, 175%, 180%
			int[] knots = new int[] { 4,  10,  17,   27,   37,   44,   49,   51 };

			double[] xVals = new double[ knots.length ];
			double[] yVals = new double[ knots.length ];
			int idx = 0;

			ArrayList< Float[] > interpolated = frag.interpolate();
			for ( int point : knots )
			{
				xVals[ idx ] = interpolated.get( height[ point ] )[ 0 ].doubleValue() + idx * 1E-10;
				yVals[ idx ] = interpolated.get( height[ point ] )[ 1 ].doubleValue();
				idx++;
			}

			// Check order of x
			Object[] ordered = removeUnordered(xVals, yVals);
			xVals = (double[]) ordered[0];
			yVals = (double[]) ordered[1];

			idx = 0;
			double[] x = new double[height.length];
			double[] y = new double[height.length];

			for ( int point : height )
			{
				x[idx] = interpolated.get(point)[0].doubleValue();
				y[idx] = interpolated.get(point)[1].doubleValue();
				idx++;
			}

			final WeightedObservedPoints obs = new WeightedObservedPoints();

			obs.clear();
			for(idx = 0; idx < 5; idx++)
			{
				obs.add( x[idx], y[idx] );
			}

			ExponentialFitter left = new ExponentialFitter( obs.toList() );

			HashSet<Double> set = new HashSet<>(  );

			// Update the splineFunction
			for(idx = 0; idx < 4; idx++)
			{
				y[idx] = (float) left.value( x[idx] );

				if( set.contains( x[idx] ) )
				{
					x[idx] += 0.1;
					y[idx] = (float) left.value( x[idx] );
				}

				set.add( x[idx] );
			}

			obs.clear();
			for(idx = 50; idx < height.length; idx++)
			{
				obs.add( x[idx], y[idx] );
			}

			ExponentialFitter right = new ExponentialFitter( obs.toList() );

			// Update the splineFunction
			for(idx = 0; idx < 4; idx++)
			{
				y[idx] = (float) left.value( x[idx] );
			}


			for(idx = 51; idx < height.length; idx++)
			{
				y[idx] = (float) right.value( x[idx] );
			}

			for(idx = 0; idx < y.length; idx++)
			{
				if( Double.isNaN( y[idx] ) ) y[idx] = 0;
			}

			Fragment newFrag = new Fragment( mz );
			SplineComposite function = new SplineComposite( left, right, new WeightedSplineInterpolator().interpolate( xVals, yVals ), x[4], x[51] );

			for ( Float key : frag.keys() )
			{
				newFrag.put( key, (float) function.value( key ) );
			}

			interpolatedData.put( mz, newFrag );


			classItem = searchTreeItem( refined_averages, clazz );
//			if(classItem == null)
//			{
//				classItem = new TreeItem<>( massClassTreeMap.get( mz ) );
//				refined_averages.getChildren().add( classItem );
//			}

			treeItem = searchTreeItem( classItem, group );
			if(treeItem == null)
			{
				treeItem = new TreeItem<>( group );
				classItem.getChildren().add(treeItem);
				classItem.setExpanded( true );
			}

			String name;
			String seriesName;

			if(mz == 0d) {
				name = "TIC";
				seriesName =  name + " (" + group + ")";
			}
			else{
				name = df.format( mz );
				seriesName =  "M/Z-" + name + " (" + group + ")";
			}

			CheckBoxTreeItem<String> item = new CheckBoxTreeItem<>( name );

			item.selectedProperty().addListener( new ChangeListener< Boolean >()
			{
				@Override public void changed( ObservableValue< ? extends Boolean > observable, Boolean oldValue, Boolean newValue )
				{
					if(newValue)
					{
						selectedTreeItems.add( item );

						XYChart.Series series = new XYChart.Series();
						series.setName(seriesName + ".RefAvg" );

						Fragment fragment = data.get( mz );

						TreeMap< Float, Float > stdErrValue = stderr.get( mz );
						TreeMap< Float, Float > stdDevValue = stddev.get( mz );

						for ( Float key: fragment.keys() )
						{
							XYChart.Data node = new XYChart.Data( key, fragment.get( key ) );
							if( 0d == mz )
								node.setNode( new HoveredNode( fragment.get( key ) ) );
							else
								node.setNode( new ErrorBarNode( fragment.get( key ), stdErrValue.get( key ), stdDevValue.get(key) ) );

							series.getData().add( node );
						}

						chart.getData().add( series );

						if( 0d != mz )
						{
							series = new XYChart.Series();
							series.setName( seriesName + ".Interpolated" );

							for ( Float key : fragment.keys() )
							{
								XYChart.Data node = new XYChart.Data( key, interpolatedData.get(mz).get(key) );

								node.setNode( new HoveredNode( key ) );
								series.getData().add( node );
							}

							chart.getData().add( series );
						}

//						if(stddev.containsKey( mz ))
//						{
//							series = new XYChart.Series();
//							series.setName( seriesName + ".StdDev" );
//
//							TreeMap< Float, Float > stdDevValue = stddev.get( mz );
//							for ( Float ce : stdDevValue.keySet() )
//							{
//								XYChart.Data node = new XYChart.Data( ce, stdDevValue.get( ce ) );
//								node.setNode( new HoveredNode( chart.getData().size(), stdDevValue.get( ce ) ) );
//								series.getData().add( node );
//							}
//							chart.getData().add( series );
//						}
					}
					else
					{
						chart.getData().removeIf( series -> series.getName().startsWith( seriesName ) );
					}
				}
			} );

			treeItem.getChildren().add( item );
		}

		return interpolatedData;
	}

	static int checkOrder(double[] val) {
		double previous = val[0];
		final int max = val.length;

		int index;

		ITEM:
		for (index = 1; index < max; index++) {
			if (val[index] <= previous) {
				break ITEM;
			}
			previous = val[index];
		}

		if (index == max) {
			// Loop completed.
			return -1;
		} else {
			return index - 1;
		}
	}

	public static Object[] removeUnordered(double[] xVal, double[] yVal) {
		int ret;

		while((ret = checkOrder( xVal )) > -1) {
			xVal = ArrayUtils.remove(xVal, ret);
			yVal = ArrayUtils.remove(yVal, ret);
		}

		return new Object[]{xVal, yVal};
	}

	private void clearCategoryItems( String clazz, String group, String treeName )
	{
		TreeItem classItem;
		TreeItem groupTreeItem;
		TreeItem categoryItem;

		classItem = searchTreeItem( dataNode, clazz );
		if(classItem == null) return;

		groupTreeItem = searchTreeItem( classItem, group );
		if(groupTreeItem == null) return;

		categoryItem = searchTreeItem( groupTreeItem, treeName );
		if(categoryItem == null) return;

		categoryItem.getChildren().clear();
	}

	private void createSeries(String clazz, String group, TreeMap< Double, TreeMap<Integer, Fragment > > data, String treeName)
	{
		clearCategoryItems( clazz, group, treeName );

		// Get class from user input panel, otherwise add group directly
		TreeItem classItem;
		TreeItem groupTreeItem;
		TreeItem categoryItem;

		ArrayList<Double> list = new ArrayList<>( data.keySet() );

		for ( Double mz : list )
		{
			classItem = searchTreeItem( dataNode, clazz );
//			if(classItem == null)
//			{
//				classItem = new TreeItem<>( massClassTreeMap.get( mz ) );
//				dataNode.getChildren().add( classItem );
//			}

			groupTreeItem = searchTreeItem( classItem, group );
//			if(groupTreeItem == null)
//			{
//				groupTreeItem = new TreeItem<>( group );
//				classItem.getChildren().add(groupTreeItem);
//			}

			categoryItem = searchTreeItem( groupTreeItem, treeName );
			if(categoryItem == null)
			{
				categoryItem = new TreeItem<>( treeName );
				groupTreeItem.getChildren().add( categoryItem );
			}

			String name;
			if(mz == 0d) name = "TIC";
			else name = df.format( mz );

			CheckBoxTreeItem<String> item = (CheckBoxTreeItem) searchTreeItem( categoryItem, name );
			if( item == null )
			{
				item = new CheckBoxTreeItem<>( name );
				categoryItem.getChildren().add( item );
			}

			for ( Integer cycle : data.get(mz).keySet())
			{
				CheckBoxTreeItem<String> cycleItem = new CheckBoxTreeItem<>( String.format( "Cycle-%d", cycle ) );
				String seriesName = "M/Z-" + item.getValue() + "-" + cycleItem.getValue();

				cycleItem.selectedProperty().addListener( new ChangeListener< Boolean >()
				{
					@Override public void changed( ObservableValue< ? extends Boolean > observable, Boolean oldValue, Boolean newValue )
					{
						if(newValue)
						{
							selectedTreeItems.add( cycleItem );
							XYChart.Series series = new XYChart.Series();
							series.setName( seriesName );

							Fragment fragment = data.get( mz ).get( cycle );

							for ( Float key: fragment.keys() )
							{
								XYChart.Data node = new XYChart.Data( key, fragment.get( key ) );
								series.getData().add( node );
							}
							chart.getData().add( series );
						}
						else
						{
							chart.getData().removeIf( series -> series.getName().equals( seriesName ) );
						}
					}
				} );

				item.getChildren().add( cycleItem );
			}
		}
	}

	private void handleProcessEvent( ProcessEvent event )
	{
		ProcessEvent.ProcessEventType eventType = ProcessEvent.getProcessEventType( event.getEventType() );

		switch ( eventType )
		{
			case CYCLES_DETECTED: addCycles(); break;

			case FRAGMENTS_CREATED:
            mFaAnionsList = (ObservableList< FAAnion >) event.getParam()[5];

				new Thread( () -> createFragments( ((ProcessEvent)event).getParam() ) ).start();
				break;

			case DATA_REFINED:
				refineData( ((ProcessEvent)event).getParam() );
				Event.fireEvent( checkTreeView.getParent(), new ProcessEvent( ProcessEvent.MZ_CALIBRATION,
						avgTreeMap ) );
				break;

         case DATA_EXPORTED:
            System.out.println(eventType);
            exportJson(( Window ) event.getParam()[0]);
            break;

			case SAVE_FRAGMENTS:
			{
				final ProgressIndicator pi = ( ProgressIndicator ) event.getParam()[ 1 ];
				pi.progressProperty().unbind();
				pi.progressProperty().bind( FragmentCollection.progressPropertyProperty() );

				SimpleBooleanProperty done = ( SimpleBooleanProperty ) event.getParam()[ 2 ];
				done.set( false );

				new Thread( () ->
				{
					storeCollection( ( File ) event.getParam()[ 0 ], classToFragmentCollection );

					done.set( true );

					pi.progressProperty().unbind();
				} ).start();
			}
				break;

			case LOAD_FRAGMENTS:
				final int mmu = (Integer) event.getParam()[0];
				final ObservableList<Mass> validFragments = (ObservableList<Mass>) event.getParam()[1];

				final ProgressIndicator indicator = (ProgressIndicator) event.getParam()[3];
				indicator.progressProperty().unbind();
				indicator.progressProperty().bind( FragmentCollection.progressPropertyProperty() );

				SimpleBooleanProperty fragmentDone = (SimpleBooleanProperty) event.getParam()[4];
				fragmentDone.set( false );

            mFaAnionsList = (ObservableList< FAAnion >) event.getParam()[5];

				new Thread( () ->
				{
					FragmentCollection.setSizeProperty( 10 );

					TreeMap<String, TreeMap<String, FragmentCollection>> loadedFragmentCollection =
							(TreeMap<String, TreeMap<String, FragmentCollection>>) loadCollection( (File) event.getParam()[2] );

					FragmentCollection.setProgressPropertyProperty( 5 );

					loadFragments( loadedFragmentCollection, mmu, validFragments );

					FragmentCollection.setProgressPropertyProperty( 10 );

					fragmentDone.set( true );

					indicator.progressProperty().unbind();
				}).start();

				break;

			case CONVERT_FRAGMENTS_CSV:
			{
				final ProgressIndicator pi = (ProgressIndicator) event.getParam()[2];
				pi.progressProperty().unbind();
				pi.progressProperty().bind( FragmentCollection.progressPropertyProperty() );

				SimpleBooleanProperty done = (SimpleBooleanProperty) event.getParam()[3];
				done.set( false );

				if(classToFragmentCollection.size() == 0) {
					Alert alert = new Alert( Alert.AlertType.WARNING, "Please load a Fragment file first.");
					alert.show();
					return;
				}

				final FileChooser fileChooser = new FileChooser();
				fileChooser.setTitle("Save Csv file");
				fileChooser.getExtensionFilters().addAll(
						new FileChooser.ExtensionFilter( "Csv File", "*.csv")
				);

				Window parent = ( Window ) event.getParam()[0];

				File fileName = fileChooser.showSaveDialog( parent );

				new Thread( () ->
				{
					exportCollection( (ObservableList< FAAnion >) event.getParam()[1], fileName, classToFragmentCollection );

					done.set( true );

					pi.progressProperty().unbind();
				} ).start();
			}

				break;
		}
	}

	private Object loadCollection( File fileName )
	{
		XMLDecoder e = null;
		try
		{
			e = new XMLDecoder(
					new BufferedInputStream(
							new FileInputStream( fileName ) ) );
		}
		catch ( FileNotFoundException e1 )
		{
			//e1.printStackTrace();
			System.err.println( e1.getMessage() );
			return null;
		}

		Object collection = e.readObject();

		e.close();

		return collection;
	}

	private void storeCollection( File fileName, Object collection )
	{
		FragmentCollection.setSizeProperty( 10 );

		XMLEncoder e = null;
		try
		{
			e = new XMLEncoder(
					new BufferedOutputStream(
							new FileOutputStream( fileName ) ) );
		}
		catch ( FileNotFoundException e1 )
		{
			e1.printStackTrace();
		}

		FragmentCollection.setProgressPropertyProperty( 3 );

		e.writeObject( collection );

		e.close();

		FragmentCollection.setProgressPropertyProperty( 10 );
	}

	private void exportCollection( final ObservableList< FAAnion > faAnionsList, final File fileName, TreeMap<String, TreeMap<String, FragmentCollection>> collection)
	{
		FragmentCollection.setSizeProperty( 10 );

		try (
				BufferedWriter writer = Files.newBufferedWriter( Paths.get( fileName.getPath() ) );

				CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
						.withHeader("Class", "Specie", "Category", "Mass", "Cycle", "CE", "Intensity", "FA_C", "FA_DB", "FA_iso", "sn_1", "sn_2", "CO2_mass", "CO2_sn_1", "CO2_sn_2"))
		) {

			for(String clazz : collection.keySet()) {

				// Preferred iso list
				HashMap<Double, Integer> faList = new HashMap<>();
				faList.put(277.22d, 1);  // #16: The second of FA 18:3 -> FA_ind = 0.73, FA_iso = 9
				faList.put(305.25d, 1);  // #23: The second of FA 20:3
				faList.put(303.23d, 0);  // #24: The first  of FA 20:4
				faList.put(331.26d, 1);  // #32: The second of FA 22:4
				faList.put(329.25d, 1);  // #34: The second of FA 22:5
				faList.put(357.28d, 0);  // #43: The first  of FA 24:5

				TreeMap<String, FragmentCollection> classCollection = collection.get(clazz);
				for(String specie: classCollection.keySet()) {
					FragmentCollection fragmentCollection = classCollection.get(specie);

					ArrayList collisionEnergyList = fragmentCollection.getCollisionEnergyList();

					TreeMap< Integer, TreeMap< Float, Float > > ticMap = fragmentCollection.getTicMap();

					// TIC iteration
					for(Integer cycle : ticMap.keySet()) {
						TreeMap< Float, Float> tic = ticMap.get(cycle);
						for(Float ce : tic.keySet()) {
							Float intensity = tic.get(ce);

							csvPrinter.printRecord(clazz, specie, "Fragments", "TIC", cycle, ce, intensity, "", "", "", "", "", "", "", "");
						}
					}

					TreeMap< Double, TreeMap<Integer, Fragment> > fragments = fragmentCollection.getFragments();

					// FA Fragments iteration

					for(Double mz : fragments.keySet()) {
						TreeMap<Integer, Fragment> fragmentMap = fragments.get(mz);

						List< FAAnion > faAnions = getMassIndexes( faAnionsList, mz );

						FAAnion faAnion = null;
						if ( faAnions.size() > 0 )
						{
							if(faAnions.size() > 1)
							{
//								System.out.println( Arrays.toString( faAnions.toArray() ) );
								faAnion = faAnions.get( faList.get(mz) );
							}
							else
							{
								faAnion = faAnions.get( 0 );
							}
						}


//						faAnion.getCo2mass()

						for(Integer cycle : fragmentMap.keySet()) {
							Fragment fragment = fragmentMap.get( cycle );

							for(Float ce : fragment.keys()) {
								Float intensity = fragment.get(ce);

								if(faAnion != null) {
									String co2Mass = null;
									if(faAnion.getCo2mass() != null) {
										co2Mass = fragments.keySet().contains( faAnion.getCo2mass().getMass() ) ? faAnion.getCo2mass().getMass().toString() : null;
									}

									// Extract SN strings from specie string
									String[] snStrings = getSN( specie );
									String sn = faAnion.getFACarbon() + "" + faAnion.getFADoubleBonds();
									String iso = snStrings[2];

									// If iso is written in the specie string, we use the iso information to match FA Anion
									if(!iso.isEmpty()) {
										Optional<FAAnion> fa = faAnions.stream().filter( c -> c.getFAIsomer().equals( Float.parseFloat( iso ) ) ).findFirst();

										if(fa.isPresent())
											faAnion = fa.get();
									}

									// If there is an explicit iso or sn is one of these [ FA 22:6, FA 20:4, FA 18:3 ],
									// we add CO2 NL
//									if(!iso.isEmpty() || sn.equals( "226" ) || sn.equals( "204" ) || sn.equals( "183" )) {
									if(specie.startsWith( "PCO" ) || specie.startsWith( "PEO" ))
									{
										if ( snStrings[ 1 ].equals( sn ) )
										{
											printFARecord( csvPrinter, clazz, specie, mz, faAnion, cycle, ce, intensity, co2Mass, snStrings, sn );
										}
										else
										{
											csvPrinter.printRecord( clazz, specie, "Fragments", mz, cycle, ce, intensity, "", "", "", "", "", "", "", "" );
										}
									} else {
										if ( snStrings[ 0 ].equals( sn ) || snStrings[ 1 ].equals( sn ) )
										{
											printFARecord( csvPrinter, clazz, specie, mz, faAnion, cycle, ce, intensity, co2Mass, snStrings, sn );
										}
										else
										{
											csvPrinter.printRecord( clazz, specie, "Fragments", mz, cycle, ce, intensity, "", "", "", "", "", "", "", "" );
										}
									}

								} else {
									// Add FA without CO2 NL information
									csvPrinter.printRecord(clazz, specie, "Fragments", mz, cycle, ce, intensity, "", "", "", "", "", "", "", "" );
								}
							}
						}
					}
				}
			}

			csvPrinter.flush();
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}

	private void printFARecord( CSVPrinter csvPrinter, String clazz, String specie, Double mz, FAAnion faAnion, Integer cycle, Float ce, Float intensity, String co2Mass, String[] snStrings, String sn ) throws IOException
	{
		if ( co2Mass != null )
		{
			csvPrinter.printRecord( clazz, specie, "FA", mz, cycle, ce, intensity, faAnion.getFACarbon(),
					faAnion.getFADoubleBonds(), faAnion.getFAIsomer(),
					snStrings[ 0 ].equals( sn ) ? 1 : 0, snStrings[ 1 ].equals( sn ) ? 1 : 0,
					co2Mass, snStrings[ 0 ].equals( sn ) ? 1 : 0, snStrings[ 1 ].equals( sn ) ? 1 : 0 );
		}
		else
		{
			csvPrinter.printRecord( clazz, specie, "FA", mz, cycle, ce, intensity, faAnion.getFACarbon(),
					faAnion.getFADoubleBonds(), faAnion.getFAIsomer(),
					snStrings[ 0 ].equals( sn ) ? 1 : 0, snStrings[ 1 ].equals( sn ) ? 1 : 0,
					"", "", "" );
		}
	}

	private static String[] getSN( String sn1sn2 )
	{
		//				System.out.println(sn1sn2);
		String snString = sn1sn2.replaceAll( "\\(?[0-9]+z\\)|[^0-9]+", "" );

		//		System.out.println(snString);
		String sn1 = snString.substring( 0, 3 );
		String sn2 = snString.substring( 3, 6 );
		String iso = "";
		if(snString.length() > 6) iso = snString.substring( 6 );

		return new String[]{sn1, sn2, iso};
	}

	public static List< FAAnion > getMassIndexes( Collection<FAAnion> collection, Double mass )
	{
		return collection.stream().filter( c -> c.getMass().equals( mass ) ).collect( Collectors.toList() );
	}


	private void handleChartEvent( ChartEvent event )
	{
		ChartEvent.ChartEventType eventType = ChartEvent.getChartEventType( event.getEventType() );

		switch ( eventType )
		{
			case UPDATE_CE:
				collisionEnergy = (Float) event.getTargetValue();

				chart.getData().clear();

				for ( TreeItem<String> group : dataNode.getChildren() )
				{
					for( TreeItem<String> groupTreeItem : group.getChildren() )
					{
						groupTreeItem.getChildren().stream().filter( c -> c.getValue().equals( "Cycles" ) ).forEach( c ->

								c.getChildren().filtered( cycle ->
										( ( CheckBoxTreeItem ) cycle ).isSelected() ).forEach( ( i ) ->
								{
									int cycleNo = Integer.parseInt( i.getValue() );
									drawStickChart( cycleHashMap.get( cycleNo ), ( CheckBoxTreeItem< String > ) i );
								} ) );
					}

				}
				break;

			case CLEAR:
				stickChart.getData().clear();
				chart.getData().clear();
				dataNode.getChildren().clear();
				refined_averages.getChildren().clear();
				break;

			case CLEAR_CHART:
				uncheckNode( dataNode );
				uncheckNode( refined_averages );
				break;

			case DRAW_SPECIFIC_MZ_CHART:
				TreeSet<Double> set = (TreeSet<Double>) event.getTargetValue();

				//set.forEach( System.out::println );
				set.forEach( c -> checkNodeIfEqual( refined_averages, df.format( c ) ) );
				break;

			case CLEAR_SPECIFIC_MZ_CHART:
				uncheckNode( refined_averages );
				break;
		}
	}

	private void uncheckNode( TreeItem item )
	{
		if(item instanceof CheckBoxTreeItem)
			((CheckBoxTreeItem) item).setSelected( false );
		else
			item.getChildren().forEach( c -> uncheckNode( ( TreeItem ) c ) );
	}

	private void checkNodeIfEqual( TreeItem item, String mass )
	{
		if(item instanceof CheckBoxTreeItem && item.getValue().equals( mass ))
			((CheckBoxTreeItem) item).setSelected( true );
		else
			item.getChildren().forEach( c -> checkNodeIfEqual( ( TreeItem ) c, mass ) );
	}
}
