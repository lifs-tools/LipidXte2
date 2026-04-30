package de.mpicbg.ms;

import de.mpicbg.ms.model.event.ChartEvent;
import de.mpicbg.ms.model.CycleCollection;
import de.mpicbg.ms.model.LipidClass;
import de.mpicbg.ms.model.LipidClassCollection;
import de.mpicbg.ms.model.data.FAAnion;
import de.mpicbg.ms.model.data.Mass;
import de.mpicbg.ms.model.MzXMLFileImportMethod;
import de.mpicbg.ms.model.event.ProcessEvent;
import de.mpicbg.ms.view.pane.MasterXmlPane;
import de.mpicbg.ms.view.pipeline.calibration.CalibrationPane;
import de.mpicbg.ms.view.pipeline.dataprep.DataPrepPane;
import de.mpicbg.ms.view.pipeline.quantification.QuantificationPane;
import de.mpicbg.ms.view.pipeline.validation.SampleValidationPane;
import de.mpicbg.ms.view.pipeline.quantification.QuantificationTab;
import de.mpicbg.ms.view.pipeline.validation.TransmissionCorrectionPane;
import de.mpicbg.ms.view.chart.ErrorBarNode;
import de.mpicbg.ms.view.pipeline.validation.ValidationTab;
import de.mpicbg.ms.view.treecell.LipidCheckBoxTreeCell;
import de.mpicbg.ms.util.TableViewUtil;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.chart.LineChart;

import de.mpicbg.ms.view.chart.StickChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;

import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Callback;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;

import java.util.List;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Pipeline Tab
 */
public class Pipeline extends TabPane
{
   ObservableList< EventHandler< ActionEvent > > clearChartEvents = FXCollections.observableArrayList();
   ObservableList< FAAnion > mFaAnionsList;
   LineChart< Number, Number > currentChart;

   LinkedHashSet< String > folderList = new LinkedHashSet<>();
   String lastAccessedFolder = null;

   public Pipeline( LineChart< Number, Number > chart, LineChart< Number, Number > correctionFactorChart,
           LineChart< Number, Number > extraChart, StickChart< Number, Number > stickChart, MasterXmlPane masterXmlPane, SampleValidationPane quantValidationPane, TransmissionCorrectionPane transmissionCorrectionPane, QuantificationPane quantificationPane )
   {
      // 1. Data Preparation Tab
      final Tab dataPrepTab = makeDataPreparationTab( chart, stickChart );

      // 2. Calibration Step.
      loadFolderList();

      final Tab calibrationTab = makeCalibrationTab( chart, correctionFactorChart, extraChart, masterXmlPane );

      //		final Tab analysis = new Tab( "Analysis" );
      //		analysis.setClosable( false );
      //		analysis.setContent( new VBox(  ) );

      final Tab validationTab = makeValidationTab( chart, correctionFactorChart, extraChart, masterXmlPane, quantValidationPane, transmissionCorrectionPane );

      final Tab quantificationTab = makeQuantificationTab( chart, correctionFactorChart, extraChart, masterXmlPane, quantificationPane );

      getTabs().addAll( dataPrepTab, calibrationTab, validationTab, quantificationTab );

      // Receive the event from DataPrepPane and fire to CalibrationTabPane
      addEventHandler( ProcessEvent.MZ_CALIBRATION, new EventHandler< ProcessEvent >()
      {
         @Override public void handle( ProcessEvent event )
         {
            // Save all the necessary data here
            folderList.remove( lastAccessedFolder );
            folderList.add( lastAccessedFolder );
            storeFolderList();

            // Serialize [event.getParam()[0], mFaAnionsList]
            //storeRefinedCollection( lastAccessedFolder, event.getParam()[ 0 ] );

            Event.fireEvent( mCalibrationPane, new ProcessEvent( ProcessEvent.MZ_CALIBRATION,
                    event.getParam()[ 0 ], mFaAnionsList ) );
         }
      } );

      // Export csv menu in the chart
      final MenuItem exportCsvItem = new MenuItem( "Export to Clipboard" );
      exportCsvItem.setOnAction( new EventHandler< ActionEvent >()
      {
         @Override public void handle( ActionEvent event )
         {
            copyToClipboard( currentChart );
         }
      } );

      final MenuItem clearChartItem = new MenuItem( "Clear" );
      clearChartItem.setOnAction( new EventHandler< ActionEvent >()
      {
         @Override public void handle( ActionEvent event )
         {
            for ( EventHandler handler : clearChartEvents )
            {
               handler.handle( event );
            }

            chart.fireEvent( new ChartEvent( ChartEvent.CLEAR_CHART, null ) );
         }
      } );

      final ContextMenu chartContextMenu = new ContextMenu( exportCsvItem, clearChartItem );

      chart.setOnMouseClicked( new EventHandler< MouseEvent >()
      {
         @Override public void handle( MouseEvent event )
         {
            currentChart = chart;
            if ( MouseButton.SECONDARY.equals( event.getButton() ) )
            {
               chartContextMenu.show( currentChart, event.getScreenX(), event.getScreenY() );
            }
         }
      } );

      correctionFactorChart.setOnMouseClicked( new EventHandler< MouseEvent >()
      {
         @Override public void handle( MouseEvent event )
         {
            currentChart = correctionFactorChart;
            if ( MouseButton.SECONDARY.equals( event.getButton() ) )
            {
               chartContextMenu.show( currentChart, event.getScreenX(), event.getScreenY() );
            }
         }
      } );

      extraChart.setOnMouseClicked( new EventHandler< MouseEvent >()
      {
         @Override public void handle( MouseEvent event )
         {
            currentChart = extraChart;
            if ( MouseButton.SECONDARY.equals( event.getButton() ) )
            {
               chartContextMenu.show( currentChart, event.getScreenX(), event.getScreenY() );
            }
         }
      } );
   }

   private void copyToClipboard( LineChart< Number, Number > chart )
   {
      final Clipboard clipboard = Clipboard.getSystemClipboard();
      final ClipboardContent content = new ClipboardContent();

      LinkedHashSet< String > columns = new LinkedHashSet<>();

      TreeMap< Number, HashMap< String, Number > > collection = new TreeMap<>();

      for ( XYChart.Series< Number, Number > series : chart.getData() )
      {
         columns.add( series.getName() );

         ObservableList< XYChart.Data< Number, Number > > data = series.getData();

         for ( XYChart.Data< Number, Number > chartData : data )
         {
            Number rowIndex = chartData.getXValue();
            if ( !collection.containsKey( rowIndex ) )
               collection.put( rowIndex, new HashMap<>() );

            collection.get( rowIndex ).put( series.getName(), chartData.getYValue() );

            if ( chartData.getNode() instanceof ErrorBarNode )
            {
               if ( !columns.contains( series.getName() + ".StdDev" ) )
                  columns.add( series.getName() + ".StdDev" );

               ErrorBarNode n = ( ErrorBarNode ) chartData.getNode();
               collection.get( rowIndex ).put( series.getName() + ".StdDev", n.getStdDev() );
            }
         }
      }

      // Put series names
      StringBuilder sb = new StringBuilder();
      sb.append( '\t' ).append( TableViewUtil.tabString( columns ) );

      for ( Number rowIndex : collection.keySet() )
      {
         sb.append( rowIndex.toString() + '\t' );

         for ( String column : columns )
         {
            if ( collection.get( rowIndex ).containsKey( column ) )
               sb.append( collection.get( rowIndex ).get( column ).toString() + '\t' );
            else
               sb.append( '\t' );
         }
         sb.append( '\n' );
      }

      content.putString( sb.toString() );

      clipboard.setContent( content );
   }

   private String getGroupName( File item )
   {
      String groupName;
      if ( item.getName().indexOf( '-' ) != -1 )
         groupName = item.getName().substring( 0, item.getName().lastIndexOf( '-' ) );
      else
         groupName = item.getName().substring( 0, item.getName().lastIndexOf( '_' ) );

      // Remove date string if it exists
      SimpleDateFormat simpleDateFormat = new SimpleDateFormat( "yyyyMMdd" );
      simpleDateFormat.setLenient( false );

      String dateString = groupName.substring( 0, 8 );
      Date retVal = null;
      try
      {
         retVal = simpleDateFormat.parse( dateString );
      }
      catch ( ParseException ex )
      {
      }

      if ( retVal != null )
         groupName = groupName.substring( 9 );
      return groupName;
   }

   private Tab makeDataPreparationTab( LineChart< Number, Number > chart, StickChart< Number, Number > stickChart )
   {
      // 1. Data preparation step
      //ObservableList<File> items = FXCollections.observableArrayList();

      ObservableMap< String, ObservableList< File > > items = FXCollections.observableMap( new TreeMap<>() );

      final TitledPane lipidTp = new TitledPane();
      lipidTp.setText( "Drag and Drop files on proper Lipid classes" );

      TreeItem lipidCollectionRoot = new TreeItem<>();

      TreeView lipidClassListView = new TreeView( lipidCollectionRoot );
      lipidClassListView.setShowRoot( false );

      lipidClassListView.setCellFactory( LipidCheckBoxTreeCell.< LipidClass >forTreeView( items ) );
      lipidTp.setContent( lipidClassListView );

      // Class to CycleCollection
      TreeMap< String, TreeMap< String, CycleCollection > > classToCycleCollection = new TreeMap<>();

      ObservableList< Mass > validFragments = FXCollections.observableArrayList();
      SimpleBooleanProperty fragmentDone = new SimpleBooleanProperty( false );
      //		cycleCollection.setRemoveIncompleteCycle( false );

      DataPrepPane dataTreePane = new DataPrepPane( chart, stickChart, classToCycleCollection );

      clearChartEvents.add( event -> Event.fireEvent(
              dataTreePane.getCheckTreeView(), new ChartEvent( ChartEvent.CLEAR_CHART, null ) ) );

      Button clearButton = new Button( "Clear Data" );
      clearButton.setOnAction( new EventHandler< ActionEvent >()
      {
         @Override public void handle( ActionEvent event )
         {
            lipidCollectionRoot.getChildren().forEach( c -> {
               if ( c instanceof CheckBoxTreeItem )
                  ( ( CheckBoxTreeItem ) c ).setSelected( false );
            } );
            items.values().forEach( c -> c.clear() );
            items.clear();

            lipidCollectionRoot.getChildren().forEach( c -> ( ( TreeItem ) c ).getChildren().clear() );

            classToCycleCollection.values().forEach( c -> c.values().forEach( d -> d.clear() ) );
            classToCycleCollection.values().forEach( c -> c.clear() );
            classToCycleCollection.values().clear();
            classToCycleCollection.clear();

            Event.fireEvent( dataTreePane.getCheckTreeView(), new ChartEvent( ChartEvent.CLEAR, null ) );
         }
      } );

      VBox vBox1 = new VBox( clearButton, lipidTp, dataTreePane.getCheckTreeView() );

      // Cycle pane
      Spinner< Number > collisionEnergySpinner = new Spinner<>();
      Spinner< Integer > consecutiveCheckerSpinner = new Spinner<>();

      TitledPane tp1 = new TitledPane();
      tp1.setText( "Create cycles/fragments" );
      Button createCycles = new Button( "Create cycles" );
      createCycles.setOnAction( new EventHandler< ActionEvent >()
      {
         @Override public void handle( ActionEvent event )
         {
            if ( items.size() < 1 )
            {
               System.err.println( "Please, add your data." );
               return;
            }

            classToCycleCollection.values().forEach( c -> c.values().forEach( d -> d.clear() ) );
            classToCycleCollection.values().forEach( c -> c.clear() );
            classToCycleCollection.values().clear();
            classToCycleCollection.clear();

            CycleCollection lastCycleCollection = null;

            for ( String clazz : items.keySet() )
            {
               if ( !classToCycleCollection.containsKey( clazz ) )
                  classToCycleCollection.put( clazz, new TreeMap< String, CycleCollection >() );

               TreeMap< String, CycleCollection > cycleCollectionTreeMap = classToCycleCollection.get( clazz );

               // For the first time, check the ranges
               for ( File item : items.get( clazz ) )
               {
                  String groupName = getGroupName( item );

                  if ( !cycleCollectionTreeMap.containsKey( groupName ) )
                  {
                     cycleCollectionTreeMap.put( groupName, new CycleCollection() );
                     System.out.println( groupName + " is created." );
                  }

                  System.out.println( item.getName() + " is being checked for range." );
                  MzXMLFileImportMethod importer = new MzXMLFileImportMethod( item );
                  lastCycleCollection = cycleCollectionTreeMap.get( groupName );
                  lastCycleCollection.rangeCheck( importer.executeForMs2() );
               }

               // Actually, it process the scans
               for ( File item : items.get( clazz ) )
               {
                  String groupName = getGroupName( item );

                  System.out.println( item.getName() + " is processed." );
                  MzXMLFileImportMethod importer = new MzXMLFileImportMethod( item );
                  lastCycleCollection = cycleCollectionTreeMap.get( groupName );
                  lastCycleCollection.addScanCollection( importer.executeForMs2() );

                  lastAccessedFolder = item.getParent();
               }
            }

            ObservableList list = FXCollections.observableArrayList( lastCycleCollection.getRangeSet() );

            collisionEnergySpinner.valueProperty().addListener( new ChangeListener< Number >()
            {
               @Override public void changed( ObservableValue< ? extends Number > observable, Number oldValue, Number newValue )
               {
                  Event.fireEvent( dataTreePane.getCheckTreeView(), new ChartEvent( ChartEvent.UPDATE_CE, newValue ) );
               }
            } );
            collisionEnergySpinner.setValueFactory( new SpinnerValueFactory.ListSpinnerValueFactory< Number >( list ) );

            Event.fireEvent( dataTreePane.getCheckTreeView(), new ProcessEvent( ProcessEvent.CYCLES_DETECTED ) );
         }
      } );

      final ProgressIndicator piCreateFragments = new ProgressIndicator( 0 );
      piCreateFragments.setMinSize( 40, 40 );
      final Spinner< Integer > massToleranceSpinner = new Spinner<>();
      final Button createFragments = new Button( "Create Fragments" );
      createFragments.setOnAction( new EventHandler< ActionEvent >()
      {
         @Override public void handle( ActionEvent event )
         {
            if ( mFaAnionsList != null )
            {
               mFaAnionsList.clear();
               List< FAAnion > faAnionList = FAAnion.tryParseAnalyteInput( loadFile( "lipid.txt" ), massToleranceSpinner.getValue() );

               if ( faAnionList != null )
               {
                  mFaAnionsList.addAll( faAnionList );
               }
            }

            fragmentDone.set( false );

            Event.fireEvent( dataTreePane.getCheckTreeView(), new ProcessEvent( ProcessEvent.FRAGMENTS_CREATED,
                    piCreateFragments, consecutiveCheckerSpinner.getValue(), massToleranceSpinner.getValue(), validFragments, fragmentDone, mFaAnionsList ) );
         }
      } );

      final HBox createFragmentBox = new HBox( 5, createFragments, piCreateFragments );
      createFragmentBox.setAlignment( Pos.CENTER_LEFT );

      collisionEnergySpinner.setPrefWidth( 77 );
      final HBox cycleBox = new HBox( new Label( "Collision Energy: " ), collisionEnergySpinner );
      cycleBox.setAlignment( Pos.CENTER_LEFT );

      consecutiveCheckerSpinner.setPrefWidth( 77 );
      final HBox fragFilterBox = new HBox( new Label( "Consecutive check: " ), consecutiveCheckerSpinner );
      consecutiveCheckerSpinner.setValueFactory( new SpinnerValueFactory.IntegerSpinnerValueFactory( 2, 20, 5 ) );
      fragFilterBox.setAlignment( Pos.CENTER_LEFT );

      massToleranceSpinner.setPrefWidth( 77 );
      massToleranceSpinner.setValueFactory( new SpinnerValueFactory.IntegerSpinnerValueFactory( 10, 500, 10, 10 ) );
      massToleranceSpinner.setEditable( true );
      final HBox massBox = new HBox( new Label( "Mass Tolerance: ± " ), massToleranceSpinner, new Label( " 10⁻³u" ) );
      massBox.setAlignment( Pos.CENTER_LEFT );

      final Button loadFragments = new Button( "Load " );
      loadFragments.setOnAction( new EventHandler< ActionEvent >()
      {
         @Override public void handle( ActionEvent event )
         {
            final FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle( "Load Fragments file" );
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter( "Fragments", "*.frags" )
            );

            File file = fileChooser.showOpenDialog( getScene().getWindow() );
            if ( file != null )
            {
               Event.fireEvent( dataTreePane.getCheckTreeView(), new ProcessEvent( ProcessEvent.LOAD_FRAGMENTS,
                       massToleranceSpinner.getValue(), validFragments, file, piCreateFragments, fragmentDone, mFaAnionsList ) );
            }
         }
      } );

      loadFragments.setOnDragOver( new EventHandler< DragEvent >()
      {
         @Override public void handle( DragEvent event )
         {
            Dragboard db = event.getDragboard();
            if ( db.hasFiles() )
            {
               event.acceptTransferModes( TransferMode.COPY );
            }
            else
            {
               event.consume();
            }
         }
      } );

      loadFragments.setOnDragDropped( new EventHandler< DragEvent >()
      {
         @Override public void handle( DragEvent event )
         {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if ( db.hasFiles() )
            {
               success = true;

               for ( File file : db.getFiles() )
               {
                  lastAccessedFolder = file.getParent();

                  Event.fireEvent( dataTreePane.getCheckTreeView(), new ProcessEvent( ProcessEvent.LOAD_FRAGMENTS,
                          massToleranceSpinner.getValue(), validFragments, file, piCreateFragments, fragmentDone, mFaAnionsList ) );
               }
            }
            event.setDropCompleted( success );
            event.consume();
         }
      } );

      final Button saveFragments = new Button( "Save" );
      saveFragments.setOnAction( new EventHandler< ActionEvent >()
      {
         @Override public void handle( ActionEvent event )
         {
            final FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle( "Save Fragments file" );
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter( "Fragments", "*.frags" )
            );

            File file = fileChooser.showSaveDialog( getScene().getWindow() );
            if ( file != null )
            {
               System.out.println( file );
               Event.fireEvent( dataTreePane.getCheckTreeView(), new ProcessEvent( ProcessEvent.SAVE_FRAGMENTS, file, piCreateFragments, fragmentDone ) );
            }
         }
      } );

      final Button convertCsv = new Button( "Export to Csv" );
      convertCsv.setOnAction( new EventHandler< ActionEvent >()
      {
         @Override public void handle( ActionEvent event )
         {
            Event.fireEvent( dataTreePane.getCheckTreeView(), new ProcessEvent( ProcessEvent.CONVERT_FRAGMENTS_CSV, getScene().getWindow(), mFaAnionsList,
                    piCreateFragments, fragmentDone ) );
         }
      } );

      final HBox loadSaveBox = new HBox( 5, loadFragments, saveFragments, convertCsv );

      tp1.setContent( new VBox( 8, createCycles, cycleBox, createFragmentBox, fragFilterBox, massBox, loadSaveBox ) );

      // Refine pane
      final TitledPane tp2 = new TitledPane();
      tp2.setText( "Refine data" );
      final CheckBox normalize = new CheckBox( "Normalize" );

      final Spinner< Integer > normalizeBaseSpinner = new Spinner<>();
      normalizeBaseSpinner.setPrefWidth( 77 );
      final HBox normalizeOption = new HBox( new Label( "First basis points : " ), normalizeBaseSpinner );
      normalizeOption.setAlignment( Pos.CENTER_LEFT );
      normalizeBaseSpinner.setValueFactory( new SpinnerValueFactory.IntegerSpinnerValueFactory( 5, 20, 5 ) );
      normalizeBaseSpinner.disableProperty().bind(
              Bindings.when(
                      normalize.selectedProperty() ).then( false ).otherwise( true )
      );

      final Spinner< Integer > relativeIntensityMaximaSpinner = new Spinner<>();
      //		relativeIntensityMaximaSpinner.setPrefHeight( 77 );
      final HBox relativeIntensityMaximaOption = new HBox( new Label( "Maxima less than: 0." ), relativeIntensityMaximaSpinner, new Label( "%" ) );
      relativeIntensityMaximaOption.setAlignment( Pos.CENTER_LEFT );
      relativeIntensityMaximaSpinner.setValueFactory( new SpinnerValueFactory.IntegerSpinnerValueFactory( 1, 9, 5 ) );
      relativeIntensityMaximaSpinner.disableProperty().bind(
              Bindings.when(
                      normalize.selectedProperty() ).then( false ).otherwise( true )
      );

      final CheckBox threePointAvg = new CheckBox( "3-point Average" );
      final CheckBox removeOutliers = new CheckBox( "Remove outliers" );

      final Spinner< Integer > outlierRemoveSpinner = new Spinner<>();
      outlierRemoveSpinner.setPrefWidth( 77 );
      final HBox outlierRemoveOption = new HBox( new Label( "Outlier range : " ), outlierRemoveSpinner, new Label( " %" ) );
      outlierRemoveOption.setAlignment( Pos.CENTER_LEFT );
      outlierRemoveSpinner.setValueFactory( new SpinnerValueFactory.IntegerSpinnerValueFactory( 5, 50, 5, 5 ) );
      outlierRemoveSpinner.setEditable( true );
      outlierRemoveSpinner.disableProperty().bind(
              Bindings.when(
                      removeOutliers.selectedProperty() ).then( false ).otherwise( true )
      );

      final Button dataRefine = new Button( "Refine" );
      dataRefine.setOnAction( new EventHandler< ActionEvent >()
      {
         @Override public void handle( ActionEvent event )
         {
            Event.fireEvent( dataTreePane.getCheckTreeView(), new ProcessEvent( ProcessEvent.DATA_REFINED,
                    normalize.isSelected(), normalizeBaseSpinner.getValue(), threePointAvg.isSelected(),
                    removeOutliers.isSelected(), outlierRemoveSpinner.getValue(), validFragments,
                    relativeIntensityMaximaSpinner.getValue() ) );
         }
      } );

      final Button exportData = new Button( "Export" );
      exportData.setOnAction( new EventHandler< ActionEvent >()
      {
         @Override public void handle( ActionEvent actionEvent )
         {
            Event.fireEvent( dataTreePane.getCheckTreeView(), new ProcessEvent( ProcessEvent.DATA_EXPORTED, getScene().getWindow() ) );
         }
      } );

      // Default behaviors
      normalize.setSelected( true );
      threePointAvg.setSelected( true );
      removeOutliers.setSelected( true );

      tp2.setContent( new VBox( 8, normalize, normalizeOption, relativeIntensityMaximaOption, threePointAvg, removeOutliers, outlierRemoveOption, new HBox( 5, dataRefine, exportData ) ) );

      // Specific fragment
      final TitledPane tp3 = new TitledPane();
      tp3.setText( "Specify Fragments" );
      final TabPane tabPane = new TabPane();

      // Get lipidClass information from the resource
      String lipidClass = null;
      String lipidFile = "lipid.txt";

      try
      {
         if ( isExist( lipidFile ) )
            lipidClass = loadFile( lipidFile );
         else
            lipidClass = IOUtils.toString( getClass().getResourceAsStream( lipidFile ) );
      }
      catch ( IOException e )
      {
         e.printStackTrace();
      }

      // Basic tab
      final Tab basicTab = new Tab( "Basic" );
      basicTab.setClosable( false );

      // list up the current loaded classes
      ObservableList< FAAnion > faAnionsList = FXCollections.observableArrayList();
      this.mFaAnionsList = faAnionsList;

      ObservableList< Mass > customMzList = FXCollections.observableArrayList();
      ObservableList< String > analyteList = FXCollections.observableArrayList();
      analyteList.addAll( "FA_anions", "Custom m/z" );

      // Setup lipid class collection with the given FAAnion input
      for ( LipidClass lipidClassItem : LipidClassCollection.get() )
      {
         TreeItem< LipidClass > item = new TreeItem( lipidClassItem );
         lipidCollectionRoot.getChildren().add( item );
      }

      ListView< String > listView = new ListView<>( analyteList );

      listView.setCellFactory( CheckBoxListCell.forListView( new Callback< String, ObservableValue< Boolean > >()
      {
         @Override
         public ObservableValue< Boolean > call( String item )
         {
            BooleanProperty observable = new SimpleBooleanProperty();
            observable.addListener( ( obs, wasSelected, isNowSelected ) ->
            {
               if ( isNowSelected )
               {
                  final TreeSet< Double > set = new TreeSet<>();

                  if ( item.equals( "FA_anions" ) )
                     // send the set to Refined_averages in order to show them
                     faAnionsList.forEach( c -> set.add( c.getMass() ) );
                  else if ( item.equals( "Custom m/z" ) )
                     // send the custom m/z set to Refined_averages in order to show them
                     customMzList.forEach( c -> set.add( c.getMass() ) );

                  Event.fireEvent( dataTreePane.getCheckTreeView(), new ChartEvent( ChartEvent.DRAW_SPECIFIC_MZ_CHART,
                          set ) );
               }
               else
               {
                  Event.fireEvent( dataTreePane.getCheckTreeView(), new ChartEvent( ChartEvent.CLEAR_SPECIFIC_MZ_CHART,
                          null ) );
               }
            } );
            return observable;
         }
      } ) );

      basicTab.setContent( new VBox( listView ) );
      tabPane.getTabs().add( basicTab );

      // FA anions tab
      final Tab faAnionsTab = new Tab( "FA anions" );
      faAnionsTab.setClosable( false );
      final TextArea faAnionsTextArea = new TextArea();

      faAnionsTextArea.textProperty().addListener( new ChangeListener< String >()
      {
         @Override public void changed( ObservableValue< ? extends String > observable, String oldValue, String newValue )
         {
            //Mass.parseTextArea( ta.getParagraphs(), massToleranceSpinner.getValue(), massClassTreeMap, classSet );
            faAnionsList.clear();
            List< FAAnion > faAnionList = FAAnion.tryParseAnalyteInput( newValue, massToleranceSpinner.getValue() );

            if ( faAnionList != null )
            {
               faAnionsList.addAll( faAnionList );
               saveFile( "lipid.txt", newValue );
            }
         }
      } );
      faAnionsTextArea.setText( lipidClass );
      faAnionsTab.setContent( faAnionsTextArea );
      tabPane.getTabs().add( faAnionsTab );

      // Custom m/z
      final Tab customMzTab = new Tab( "Custom m/z" );
      customMzTab.setClosable( false );
      final TextArea customMzTextArea = new TextArea();

      customMzTextArea.textProperty().addListener( new ChangeListener< String >()
      {
         @Override public void changed( ObservableValue< ? extends String > observable, String oldValue, String newValue )
         {
            //Mass.parseTextArea( ta.getParagraphs(), massToleranceSpinner.getValue(), massClassTreeMap, classSet );
            customMzList.clear();
            customMzList.addAll( Mass.tryParseMzInput( newValue, massToleranceSpinner.getValue() ) );
         }
      } );
      customMzTab.setContent( customMzTextArea );
      tabPane.getTabs().add( customMzTab );

      tp3.setContent( tabPane );
      tp3.setExpanded( true );

      // One click for all the process
      final Button batch = new Button( "Do all!" );
      batch.setMinSize( 120, 70 );
      batch.setStyle( "-fx-font: 22 arial; -fx-base: #b6e7c9;" );

      batch.setOnAction( new EventHandler< ActionEvent >()
      {
         @Override public void handle( ActionEvent event )
         {
            createCycles.fire();

            fragmentDone.addListener( new ChangeListener< Boolean >()
            {
               @Override public void changed( ObservableValue< ? extends Boolean > observable, Boolean oldValue, Boolean newValue )
               {
                  if ( newValue )
                  {
                     dataRefine.getOnAction().handle( event );
                     fragmentDone.removeListener( this );
                  }
               }
            } );

            createFragments.fire();
         }
      } );

      final VBox vBoxPipeline = new VBox( tp3, tp1, tp2, batch );

      final Tab dataPrep = new Tab( "Data preparation" );
      dataPrep.setClosable( false );
      dataPrep.setContent( new SplitPane( vBox1, vBoxPipeline ) );

      return dataPrep;
   }

   CalibrationPane mCalibrationPane;

   private Tab makeCalibrationTab( LineChart< Number, Number > chart, LineChart< Number, Number > correctionFactorChart,
           LineChart< Number, Number > extraChart, MasterXmlPane masterXmlPane )
   {
      final Tab calibration = new Tab( "Calibration" );
      calibration.setClosable( false );

      mCalibrationPane = new CalibrationPane( folderList, chart, correctionFactorChart, extraChart, mFaAnionsList, masterXmlPane );

      clearChartEvents.add( event -> Event.fireEvent(
              mCalibrationPane, new ChartEvent( ChartEvent.CLEAR_CHART, null ) ) );

      calibration.setContent( mCalibrationPane );
      return calibration;
   }

   ValidationTab mValidationTab;

   private Tab makeValidationTab( LineChart< Number, Number > chart, LineChart< Number, Number > correctionFactorChart, LineChart< Number, Number > extraChart, MasterXmlPane masterXmlPane, SampleValidationPane quantValidationPane, TransmissionCorrectionPane transmissionCorrectionPane )
   {
      mValidationTab = new ValidationTab( chart, correctionFactorChart, extraChart, masterXmlPane, quantValidationPane, transmissionCorrectionPane, mFaAnionsList );
      return mValidationTab;
   }

   QuantificationTab mQuantificationTab;

   private Tab makeQuantificationTab( LineChart< Number, Number > chart, LineChart< Number, Number > correctionFactorChart, LineChart< Number, Number > extraChart, MasterXmlPane masterXmlPane, QuantificationPane quantificationPane )
   {
      mQuantificationTab = new QuantificationTab( chart, correctionFactorChart, extraChart, masterXmlPane, mFaAnionsList, quantificationPane );
      return mQuantificationTab;
   }

   private void loadFolderList()
   {
      String lru = "lru.txt";
      String pathList = null;
      if ( isExist( lru ) )
         pathList = loadFile( lru );

      if ( pathList != null )
      {
         Scanner scanner = new Scanner( pathList );
         while ( scanner.hasNextLine() )
         {
            String line = scanner.nextLine();
            // process the line
            folderList.add( line );
         }
         scanner.close();
      }
   }

   private void storeFolderList()
   {
      storeFolderList( folderList );
   }

   public static void storeFolderList( LinkedHashSet< String > folderList )
   {
      StringBuilder sb = new StringBuilder();

      for ( String path : folderList )
      {
         sb.append( path );
         sb.append( System.getProperty( "line.separator" ) );
      }

      String lru = "lru.txt";
      saveFile( lru, sb.toString() );
   }

   public static boolean isExist( String filename )
   {
      String path = getUserDataDirectory();
      if ( !new File( path ).exists() )
         new File( path ).mkdirs();

      String file = getUserDataDirectory() + filename;
      return new File( file ).exists();
   }

   public static String loadFile( String filename )
   {
      String str = null;

      try
      {
         str = FileUtils.readFileToString( new File( getUserDataDirectory() + filename ) );
      }
      catch ( IOException e )
      {
         e.printStackTrace();
      }

      return str;
   }

   public static void saveFile( String filename, String content )
   {
      try
      {
         FileUtils.writeStringToFile( new File( getUserDataDirectory() + filename ), content );
      }
      catch ( IOException e )
      {
         e.printStackTrace();
      }
   }

   public static String getUserDataDirectory()
   {
      return System.getProperty( "user.home" ) + File.separator + ".massSpec" + File.separator + getApplicationVersionString() + File.separator;
   }

   public static String getApplicationVersionString()
   {
      return "1.0";
   }
}
