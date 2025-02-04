package de.mpicbg.ms;

import com.brsanthu.googleanalytics.AppViewHit;
import com.brsanthu.googleanalytics.EventHit;
import com.brsanthu.googleanalytics.GoogleAnalytics;
import de.mpicbg.ms.model.event.ProcessEvent;
import de.mpicbg.ms.view.pane.MasterXmlPane;
import de.mpicbg.ms.view.pipeline.quantification.QuantificationPane;
import de.mpicbg.ms.view.pipeline.validation.SampleValidationPane;
import de.mpicbg.ms.view.pane.StdOutputCaptureConsole;
import de.mpicbg.ms.view.pipeline.validation.TransmissionCorrectionPane;
import de.mpicbg.ms.view.chart.ErrorLineChart;
import de.mpicbg.ms.util.Data;
import javafx.application.Application;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Cursor;

import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import de.mpicbg.ms.view.chart.StickChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Callback;
import org.apache.commons.math3.analysis.function.Gaussian;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.fitting.GaussianCurveFitter;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.controlsfx.control.textfield.TextFields;
import org.dockfx.DockNode;
import org.dockfx.DockPane;
import org.dockfx.DockPos;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Mass Spectrometry Analysis Pipeline Software
 */
public class MainApplication extends Application
{
   private LineChart< Number, Number > chart;
   private LineChart< Number, Number > correctionFactorChart;
   private LineChart< Number, Number > extraChart;
   private StickChart< Number, Number > stickChart;

   private MasterXmlPane masterXmlPane;

   @Override
   public void start( Stage primaryStage ) throws Exception
   {
      primaryStage.setTitle( "Mass Spectrometry Analysis" );

      // create a dock pane that will manage our dock nodes and handle the layout
      DockPane dockPane = new DockPane();

      // Define windows for functional properties
      //		Data data = new Data( "overview.cestat-1.csv" );
      //System.out.println( getClass().getResource( "overview.csv" ).toString() );
      Data data = new Data( getClass().getResourceAsStream( "overview.csv" ) );

      /*********************************************************************************
       * Data and control pane
       *********************************************************************************/

      ObservableList< ListItem > observableList = FXCollections.observableArrayList();
      data.getHeaders().stream().forEach( e -> observableList.add( new ListItem( e ) ) );

      FilteredList< ListItem > filteredData = new FilteredList<>( observableList, s -> true );

      ListView< ListItem > listView = new ListView<>( filteredData );

      TextField searchText = TextFields.createClearableTextField();
      //		TextFields.bindAutoCompletion( searchText, data.getHeaders() );

      searchText.textProperty().addListener( obs -> {
         String filter = searchText.getText();
         if ( filter == null || filter.length() == 0 )
         {
            filteredData.setPredicate( s -> true );
         }
         else
         {
            filteredData.setPredicate( s -> s.contains( filter ) );
         }
      } );

      /*********************************************************************************
       * Chart pane
       *********************************************************************************/

      chart = new ErrorLineChart<>( new NumberAxis(), new NumberAxis() );

      correctionFactorChart = new LineChart<>( new NumberAxis(), new NumberAxis() );

      extraChart = new LineChart<>( new NumberAxis(), new NumberAxis() );

      stickChart = new StickChart<>( new NumberAxis(), new NumberAxis() );

      ArrayList< Double > xSeries = data.getValues( 1 );

      for ( ListItem item : listView.getItems() )
      {
         item.selectedProperty().addListener( new ChangeListener< Boolean >()
         {
            @Override public void changed( ObservableValue< ? extends Boolean > observable, Boolean oldValue, Boolean newValue )
            {
               if ( newValue )
               {
                  int index = data.getHeaders().indexOf( item.getName() );

                  XYChart.Series series = new XYChart.Series();
                  series.setName( item.getName() );

                  ArrayList< Double > plot = data.getValues( index );
                  for ( int i = 0; i < xSeries.size(); i++ )
                  {
                     XYChart.Data node = new XYChart.Data( xSeries.get( i ), plot.get( i ) );
                     node.setNode( new HoveredThresholdNode( chart.getData().size(),
                             ( i == 0 ) ? 0 : plot.get( i - 1 ), plot.get( i ) ) );
                     series.getData().add( node );
                  }
                  chart.getData().add( series );
               }
               else
               {
                  chart.getData().removeIf( series -> series.getName().equals( item.getName() ) );
               }
            }
         } );
      }

      listView.setCellFactory( CheckBoxListCell.forListView( new Callback< ListItem, ObservableValue< Boolean > >()
      {
         @Override
         public ObservableValue< Boolean > call( ListItem item )
         {
            return item.selectedProperty();
         }
      } ) );

      CheckBox polynomial = new CheckBox( "Polynomial Curve Fitting" );
      polynomial.selectedProperty().addListener( new ChangeListener< Boolean >()
      {
         final String seriesName = "Polynomial Curve Fit";

         @Override public void changed( ObservableValue< ? extends Boolean > observable, Boolean oldValue, Boolean newValue )
         {
            if ( newValue )
            {
               XYChart.Series series = new XYChart.Series();
               series.setName( seriesName );

               final WeightedObservedPoints obs = new WeightedObservedPoints();

               for ( ListItem item : listView.getItems() )
               {
                  if ( item.getSelected() )
                  {
                     int index = data.getHeaders().indexOf( item.getName() );
                     ArrayList< Double > plot = data.getValues( index );
                     for ( int i = 0; i < xSeries.size(); i++ )
                     {
                        obs.add( xSeries.get( i ), plot.get( i ) );
                     }
                  }
               }

               // PolynomialCurveFitter
               final PolynomialCurveFitter fitter = PolynomialCurveFitter.create( 4 );
               final double[] coeff = fitter.fit( obs.toList() );

               PolynomialFunction function = new PolynomialFunction( coeff );

               for ( int i = 0; i < xSeries.size(); i++ )
               {
                  double fitted = function.value( xSeries.get( i ) );
                  series.getData().add( new XYChart.Data( xSeries.get( i ), fitted ) );
               }

               chart.getData().add( series );

               for ( String styleClass : series.getNode().getStyleClass() )
               {
                  if ( styleClass.startsWith( "series" ) )
                  {
                     //							System.out.println(	styleClass );
                     for ( javafx.scene.Node n : chart.lookupAll( "." + styleClass ) )
                     {
                        n.setStyle( " -fx-stroke-width: 5px; " );
                     }
                     continue;
                  }

                  if ( styleClass.startsWith( "default-color" ) )
                  {
                     for ( javafx.scene.Node n : chart.lookupAll( "." + styleClass + ".chart-line-symbol" ) )
                     {
                        //								System.out.println(	n.getStyle() );

                        n.setStyle( " visibility: hidden" );
                     }
                     continue;
                  }
               }
            }
            else
            {
               chart.getData().removeIf( series -> series.getName().equals( seriesName ) );
            }
         }
      } );

      CheckBox gaussian = new CheckBox( "Gaussian Curve Fitting" );
      gaussian.selectedProperty().addListener( new ChangeListener< Boolean >()
      {
         final String seriesName = "Gaussian Curve Fit";

         @Override public void changed( ObservableValue< ? extends Boolean > observable, Boolean oldValue, Boolean newValue )
         {
            if ( newValue )
            {
               XYChart.Series series = new XYChart.Series();
               series.setName( seriesName );

               final WeightedObservedPoints obs = new WeightedObservedPoints();

               for ( ListItem item : listView.getItems() )
               {
                  if ( item.getSelected() )
                  {
                     int index = data.getHeaders().indexOf( item.getName() );
                     ArrayList< Double > plot = data.getValues( index );
                     for ( int i = 0; i < xSeries.size(); i++ )
                     {
                        obs.add( xSeries.get( i ), plot.get( i ) );
                     }
                  }
               }

               // GaussianFitter
               final GaussianCurveFitter fitter = GaussianCurveFitter.create();
               final double[] coeff = fitter.fit( obs.toList() );

               Gaussian function = new Gaussian( coeff[ 0 ], coeff[ 1 ], coeff[ 2 ] );

               for ( int i = 0; i < xSeries.size(); i++ )
               {
                  double fitted = function.value( xSeries.get( i ) );
                  series.getData().add( new XYChart.Data( xSeries.get( i ), fitted ) );
               }

               chart.getData().add( series );

               for ( String styleClass : series.getNode().getStyleClass() )
               {
                  if ( styleClass.startsWith( "series" ) )
                  {
                     //							System.out.println(	styleClass );
                     for ( javafx.scene.Node n : chart.lookupAll( "." + styleClass ) )
                     {
                        n.setStyle( " -fx-stroke-width: 5px; " );
                     }
                     continue;
                  }

                  if ( styleClass.startsWith( "default-color" ) )
                  {
                     for ( javafx.scene.Node n : chart.lookupAll( "." + styleClass + ".chart-line-symbol" ) )
                     {
                        // System.out.println( n.getStyle() );

                        n.setStyle( " visibility: hidden" );
                     }
                     continue;
                  }
               }

               // System.out.println( coeff.length );
            }
            else
            {
               chart.getData().removeIf( series -> series.getName().equals( seriesName ) );
            }
         }
      } );

      GoogleAnalytics ga = new GoogleAnalytics( "UA-97933915-1" );

      LocalDate date = LocalDateTime.now().toLocalDate();
      int dateInt = date.getYear() * 10000 + date.getMonthValue() * 100 + date.getDayOfMonth();

      AppViewHit appViewHit = new AppViewHit( "LipidXte", "1.0beta", "experimental" );
      ga.post( appViewHit );

      EventHit hit = new EventHit( "Application", "Start", "Date", dateInt );
      ga.post( hit );

      BorderPane borderPane = new BorderPane();
      borderPane.setTop( new VBox( gaussian, polynomial, searchText ) );
      borderPane.setCenter( listView );

      //		DockNode listNode = new DockNode( borderPane, "Items" );
      //		listNode.dock( dockPane, DockPos.LEFT );
      masterXmlPane = new MasterXmlPane( chart );

      SampleValidationPane sampleValidationPane = new SampleValidationPane( chart );

      TransmissionCorrectionPane transmissionCorrectionPane = new TransmissionCorrectionPane( chart );

      QuantificationPane quantificationPane = new QuantificationPane( sampleValidationPane, chart );

      DockNode pipeline = new DockNode( new Pipeline( chart, correctionFactorChart, extraChart, stickChart, masterXmlPane, sampleValidationPane,
              transmissionCorrectionPane, quantificationPane ), "Process" );

      // Process command parameters
      Parameters parameters = getParameters();

      Map< String, String > namedParameters = parameters.getNamed();

      final boolean isCommandLine = namedParameters.containsKey( "op" );

      if ( isCommandLine )
      {
         List< String > unnamedParameters = parameters.getUnnamed();
         //		for( String param : unnamedParameters )
         //		{
         //			System.out.println(param);
         //		}

         // Example extended command line
         // --quant-option [Intensity, Profile, Quantity]
         // --output-option [All, Sum, Mspecies]
         // [RemoveRef, SummarizeNCE, NoCorrection, GroupOnly]

         if ( namedParameters.containsKey( "op" ) )
         {
            if ( namedParameters.get( "op" ).equals( "quant" ) )
            {
               // Example command line
               // java -jar LipidXte-1.0-SNAPSHOT-jfx.jar --op=quant --standard-list=/Users/moon/Desktop/KAI/pc/20171129_internal_standard_list.csv --merged-file=/Users/moon/Desktop/KAI/pc/PC_-merged_v1.csv --output-file=/Users/moon/Desktop/KAI/pc/ > quant.log
               Event.fireEvent( pipeline.getContents(), new ProcessEvent( ProcessEvent.COMMAND_QUANTIFICATION, namedParameters, unnamedParameters ) );
            }
            else if ( namedParameters.get( "op" ).equals( "valid" ) )
            {
               // Example command line
               // java -jar LipidXte-1.0-SNAPSHOT-jfx.jar --op=valid --merged-file=/Users/moon/Desktop/KAI/pc/PC_-merged_v1.csv --output-file=/Users/moon/Desktop/KAI/pc/ > valid.log
               Event.fireEvent( pipeline.getContents(), new ProcessEvent( ProcessEvent.COMMAND_VALIDATION, namedParameters, unnamedParameters ) );
            }
         }
      }
      else
      {
         pipeline.setPrefSize( 400, 500 );
         pipeline.dock( dockPane, DockPos.LEFT );

         DockNode chartNode = new DockNode( chart, "Line Chart" );
         chartNode.dock( dockPane, DockPos.RIGHT );

         DockNode chart2ndNode = new DockNode( correctionFactorChart, "Correction Factor Chart" );
         chart2ndNode.dock( dockPane, DockPos.CENTER, chartNode );

         DockNode chart3rddNode = new DockNode( extraChart, "Extra Chart" );
         chart3rddNode.dock( dockPane, DockPos.CENTER, chartNode );

         DockNode barChartNode = new DockNode( stickChart, "Stick Chart" );
         barChartNode.dock( dockPane, DockPos.CENTER, chartNode );

         DockNode masterXmlNode = new DockNode( masterXmlPane, "Master XML" );
         masterXmlNode.dock( dockPane, DockPos.CENTER, chartNode );

         DockNode sampleValidationNode = new DockNode( sampleValidationPane, "Sample Validation" );
         sampleValidationNode.dock( dockPane, DockPos.CENTER, masterXmlNode );

         DockNode txCfNode = new DockNode( transmissionCorrectionPane, "Transmission Correction" );
         txCfNode.dock( dockPane, DockPos.CENTER, masterXmlNode );

         DockNode sampleQuantificationNode = new DockNode( quantificationPane, "Sample Quantification" );
         sampleQuantificationNode.dock( dockPane, DockPos.CENTER, masterXmlNode );

         StdOutputCaptureConsole console = new StdOutputCaptureConsole();
         console.setPrefSize( 200, 200 );
         console.dock( dockPane, DockPos.BOTTOM, chartNode );

         SplitPane split = ( SplitPane ) dockPane.getChildren().get( 0 );
         SplitPane split2 = ( SplitPane ) split.getItems().get( 1 );
         split2.setDividerPositions( 0.8 );

         //		MenuItem saveMenuItem = new MenuItem("Save");
         //		saveMenuItem.setOnAction( new EventHandler< ActionEvent >()
         //		{
         //			@Override public void handle( ActionEvent event )
         //			{
         //				dockPane.storePreference( getUserDataDirectory() + "layout.pref" );
         //			}
         //		} );
         //
         //		MenuItem restoreMenuItem = new MenuItem("Restore");
         //		restoreMenuItem.setOnAction( new EventHandler< ActionEvent >()
         //		{
         //			@Override public void handle( ActionEvent event )
         //			{
         //				dockPane.loadPreference( getUserDataDirectory() + "layout.pref" );
         //			}
         //		} );
         //
         //		Menu fileMenu = new Menu("File");
         //		MenuBar menuBar = new MenuBar(fileMenu);
         //		fileMenu.getItems().addAll(saveMenuItem, restoreMenuItem);

         BorderPane mainBorderPane = new BorderPane();
         //		mainBorderPane.setTop( menuBar );
         mainBorderPane.setCenter( dockPane );

         primaryStage.setOnShown( new EventHandler< WindowEvent >()
         {
            @Override public void handle( WindowEvent event )
            {
               if ( new File( getUserDataDirectory() + "layout.pref" ).exists() )
                  dockPane.loadPreference( getUserDataDirectory() + "layout.pref" );
            }
         } );

         primaryStage.setOnCloseRequest( new EventHandler< WindowEvent >()
         {
            @Override
            public void handle( WindowEvent event )
            {
               dockPane.storePreference( getUserDataDirectory() + "layout.pref" );
            }
         } );

         Scene scene = new Scene( mainBorderPane, 1280, 900 );
         scene.getStylesheets().add( "de/mpicbg/ms/style.css" );

         primaryStage.getIcons().add( new Image( this.getClass().getResourceAsStream( "icon.png" ) ) );
         primaryStage.setTitle( "LipidXte" );
         primaryStage.setScene( scene );
         primaryStage.show();

         // test the look and feel with both Caspian and Modena
         Application.setUserAgentStylesheet( Application.STYLESHEET_MODENA );

         // initialize the default styles for the dock pane and undocked nodes using the DockFX
         // library's internal Default.css stylesheet
         // unlike other custom control libraries this allows the user to override them globally
         // using the style manager just as they can with internal JavaFX controls
         // this must be called after the primary stage is shown
         // https://bugs.openjdk.java.net/browse/JDK-8132900
         DockPane.initializeDefaultUserAgentStylesheet();
      }
   }

   class ListItem
   {
      private final SimpleBooleanProperty selected;
      private final SimpleStringProperty name;

      public ListItem( String name )
      {
         this.selected = new SimpleBooleanProperty( false );
         this.name = new SimpleStringProperty( name );
      }

      public boolean getSelected()
      {
         return selected.get();
      }

      public void setSelected( boolean selected )
      {
         this.selected.set( selected );
      }

      public String getName()
      {
         return name.get();
      }

      public void setName( String fName )
      {
         name.set( fName );
      }

      public SimpleBooleanProperty selectedProperty()
      {
         return selected;
      }

      @Override
      public String toString()
      {
         return getName();
      }

      /**
       * Return true if the name contains keyword (case-insensitive)
       * @param key the key
       * @return the boolean
       */
      public boolean contains( String key )
      {
         return getName().toLowerCase().contains( key.toLowerCase() );
      }
   }

   /**
    * a node which displays a value on hover, but is otherwise empty
    */
   class HoveredThresholdNode extends StackPane
   {
      HoveredThresholdNode( int maxSize, double priorValue, Double value )
      {
         setPrefSize( 10, 10 );

         final Label label = createDataThresholdLabel( maxSize, priorValue, value );

         setOnMouseEntered( new EventHandler< MouseEvent >()
         {
            @Override public void handle( MouseEvent mouseEvent )
            {
               getChildren().setAll( label );
               setCursor( Cursor.NONE );
               toFront();
            }
         } );
         setOnMouseExited( new EventHandler< MouseEvent >()
         {
            @Override public void handle( MouseEvent mouseEvent )
            {
               getChildren().clear();
               setCursor( Cursor.CROSSHAIR );
            }
         } );
      }

      private Label createDataThresholdLabel( int maxSize, double priorValue, double value )
      {
         final Label label = new Label( value + "" );
         label.getStyleClass().addAll( "default-color" + maxSize, "chart-line-symbol", "chart-series-line" );
         label.setStyle( "-fx-font-size: 10; -fx-font-weight: bold;" );

         if ( priorValue == 0 )
         {
            label.setTextFill( Color.DARKGRAY );
         }
         else if ( value > priorValue )
         {
            label.setTextFill( Color.FORESTGREEN );
         }
         else
         {
            label.setTextFill( Color.FIREBRICK );
         }

         label.setMinSize( Label.USE_PREF_SIZE, Label.USE_PREF_SIZE );
         return label;
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

   public static void main( String[] args )
   {
      //		String os = System.getProperty("os.name").toLowerCase();
      //		if(!os.startsWith( "win" ))
      //		{
      //			System.out.println("Sorry, Mass Spectrometry Analysis is only run in Windows platform.");
      //			return;
      //		}

      launch( args );
   }
}
