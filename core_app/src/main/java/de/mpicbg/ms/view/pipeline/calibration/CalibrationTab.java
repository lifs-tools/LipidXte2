package de.mpicbg.ms.view.pipeline.calibration;

import de.mpicbg.ms.model.event.ChartEvent;
import de.mpicbg.ms.model.Fragment;
import de.mpicbg.ms.model.event.ProcessEvent;
import de.mpicbg.ms.model.SplineComposite;
import de.mpicbg.ms.model.data.FAAnion;
import de.mpicbg.ms.model.data.FAAnionRow;
import de.mpicbg.ms.model.fitter.ExponentialFitter;
import de.mpicbg.ms.view.chart.HoveredNode;
import de.mpicbg.ms.view.treecell.CheckBoxNamedBoolean;
import de.mpicbg.ms.view.treecell.NamedBoolean;
import de.mpicbg.ms.view.treecell.SelectiveCheckBoxTreeTableCell;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.exception.NonMonotonicSequenceException;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.MathArrays;
import org.controlsfx.control.MasterDetailPane;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * Abstract Tab class for Calibration
 */
public abstract class CalibrationTab extends Tab
{
	final protected TreeTableView<FAAnionRow> treeTableView;
	final protected LineChart< Number, Number > chart;
	final protected LineChart< Number, Number > correctionFactorChart;
	final protected LineChart< Number, Number > errorChart;

	final protected TreeItem<FAAnionRow> root;

	final protected MasterDetailPane masterDetailPane;

	protected BooleanProperty sn1EditableProperty;

	protected ObservableList< FAAnion > mFaAnionsList;

	final protected HashMap<String, ArrayList<FAAnionRow> > faanionHashMap;

	//The pseudo classes 'valid' that was defined in the css file.
	final protected PseudoClass validItem = PseudoClass.getPseudoClass( "valid" );

	enum Interpolate {
		Polynomial2nd,
		PolynomialSpline,
		Gaussian,
		SimpleCurve,
		Harmonic,
		GammaVariate
	}

	protected CalibrationTab( LineChart< Number, Number > chart,
			LineChart< Number, Number > correctionFactorChart,
			LineChart< Number, Number > extraChart,
			ButtonBase[] menuButtons )
	{
		this.faanionHashMap = new HashMap<>();

		this.chart = chart;
		this.correctionFactorChart = correctionFactorChart;
		this.errorChart = extraChart;

		setClosable( false );
		treeTableView = new TreeTableView< FAAnionRow >();
		treeTableView.setEditable( true );

		root = new TreeItem<>(new FAAnionRow( "" ));
		root.setExpanded(true);

		TreeTableColumn<FAAnionRow, NamedBoolean> column1 = new TreeTableColumn<>("");
		column1.setPrefWidth(150);

		//Defining cell content
		column1.setCellFactory( SelectiveCheckBoxTreeTableCell.forTreeTableTitleColumn( treeTableView ) );
		column1.setCellValueFactory((param) ->
						new ReadOnlyObjectWrapper<>( param.getValue().getValue().getTitle() )
		);
		column1.setCellValueFactory( (param) ->
				new ReadOnlyObjectWrapper( param.getValue().getValue().getTitle() ) );

		//Creating a column2
		TreeTableColumn<FAAnionRow, String> column2 = new TreeTableColumn<>("mz");
		column2.setPrefWidth(100);

		//Defining cell content
		column2.setCellValueFactory( (param) ->
				new ReadOnlyStringWrapper( param.getValue().getValue().getMassString() ) );


		//Creating a column3
		TreeTableColumn<FAAnionRow, NamedBoolean > column3 = new TreeTableColumn<>("sn1");
		column3.setPrefWidth(60);

		column3.setCellFactory( SelectiveCheckBoxTreeTableCell.forTreeTableSn1Column( treeTableView ) );
		column3.setCellValueFactory( ( param ) ->
						new ReadOnlyObjectWrapper<>( param.getValue().getValue().getSn1() )
		);

		sn1EditableProperty = column3.editableProperty();

		TreeTableColumn<FAAnionRow, NamedBoolean> column4 = new TreeTableColumn<>("sn2");
		column4.setPrefWidth(60);

		column4.setCellFactory( SelectiveCheckBoxTreeTableCell.forTreeTableSn2Column( treeTableView ) );
		column4.setCellValueFactory( (param) ->
						new ReadOnlyObjectWrapper<>( param.getValue().getValue().getSn2() )
		);

		TreeTableColumn<FAAnionRow, NamedBoolean> column5 = new TreeTableColumn<>("CO2Loss");
		column5.setPrefWidth(100);

		column5.setCellFactory( SelectiveCheckBoxTreeTableCell.forTreeTableCo2LossColumn( treeTableView ) );
		column5.setCellValueFactory( (param) ->
						new ReadOnlyObjectWrapper<>( param.getValue().getValue().getCo2MassProperty() )
		);

		treeTableView.setRoot( root );
		treeTableView.setShowRoot( false );
		treeTableView.getColumns().setAll(column1, column2, column3, column4, column5 );

		masterDetailPane = new MasterDetailPane();
		masterDetailPane.setMasterNode( treeTableView );
		masterDetailPane.setDetailNode( getDetailNode() );

		masterDetailPane.setDetailSide( Side.BOTTOM );
		masterDetailPane.setShowDetailNode( true );
		masterDetailPane.setDividerPosition( 0.4 );

		if(menuButtons == null)
			setContent( masterDetailPane );
		else
		{
			HBox hbox = new HBox();
			hbox.setPadding(new Insets(8, 4, 8, 4));
			hbox.setSpacing(10);
			hbox.setStyle("-fx-background-color: #5a7d99;");

			hbox.getChildren().addAll( menuButtons );

			BorderPane borderPane = new BorderPane();

			borderPane.setTop( hbox );
			borderPane.setCenter( masterDetailPane );

			setContent( borderPane );
		}

		tabPaneProperty().addListener( new ChangeListener< TabPane >()
		{
			@Override public void changed( ObservableValue< ? extends TabPane > observable, TabPane oldValue, TabPane newValue )
			{
				newValue.addEventHandler( ProcessEvent.ANY, event -> handleProcessEvent( event ) );

				newValue.addEventHandler( ChartEvent.ANY, event -> handleChartEvent( event ) );


				tabPaneProperty().removeListener( this );
			}
		} );

		// Remove currnet tree item
		final MenuItem removeCurrentTreeItem = new MenuItem( "Remove" );
		removeCurrentTreeItem.setOnAction( new EventHandler< ActionEvent >()
		{
			@Override public void handle( ActionEvent event )
			{
				removeCurrentTreeItem();
			}
		} );

		final ContextMenu treeContextMenu = new ContextMenu( removeCurrentTreeItem );

		treeTableView.setOnMouseClicked( new EventHandler< MouseEvent >()
		{
			@Override public void handle( MouseEvent event )
			{
				if( MouseButton.SECONDARY.equals( event.getButton() )) {
					treeContextMenu.show( treeTableView, event.getScreenX(), event.getScreenY() );
				}
				else
				{
					treeContextMenu.hide();
				}
			}
		} );
	}

	protected abstract Node getDetailNode();

	protected abstract void handleProcessEvent( ProcessEvent event );

	protected abstract void removeCurrentTreeItem();

	protected void uncheckNode( TreeItem<FAAnionRow> item )
	{
		if(item.getValue().getTitle() instanceof CheckBoxNamedBoolean)
			item.getValue().getTitle().set( false );
		else
			item.getChildren().forEach( this::uncheckNode );
	}

	protected abstract void clearChart();

	private void handleChartEvent( ChartEvent event )
	{
		ChartEvent.ChartEventType eventType = ChartEvent.getChartEventType( event.getEventType() );

		switch ( eventType )
		{
			case CLEAR_CHART:
				uncheckNode( root );
				clearChart();
				break;
		}
	}

	public static int compareSN( String sn1sn2 )
	{
//		System.out.println(sn1sn2);
		String snString = sn1sn2.replaceAll( "[^0-9]+", "" );

//		System.out.println(snString);
		String sn1 = snString.substring( 0, 3 );
		String sn2 = snString.substring( 3, 6 );

		return Integer.compare( Integer.parseInt( sn1 ), Integer.parseInt( sn2 ) );
	}

	public static Float[] splitFAIndexes( String string )
	{
		String[] split = string.split( "," );
		ArrayList<Float> array = new ArrayList<>();

		for(int i = 0; i < split.length; i++)
		{
			if(!split[i].isEmpty())
				array.add( Float.parseFloat( split[i] ) );
		}

		return array.toArray(new Float[]{});
	}

	public int compareSN( String sn1sn2, Double mz )
	{
		Optional<FAAnion> faAnion = mFaAnionsList.stream().filter( c -> c.getMass().equals( mz ) ).findFirst();

		int ret = 0;

		if(faAnion.isPresent())
		{
			String CAD = faAnion.get().getFACarbon() + "" + faAnion.get().getFADoubleBonds();

			//		System.out.println(sn1sn2);
			String snString = sn1sn2.replaceAll( "[^0-9]+", "" );

			//		System.out.println(snString);
			String sn1 = snString.substring( 0, 3 );
			String sn2 = snString.substring( 3, 6 );

			if( CAD.equals( sn1 ) && CAD.equals( sn2 ) )
				ret = 0;
			else if( CAD.equals( sn1 ) )
				ret = -1;
			else if( CAD.equals( sn2 ) )
				ret = 1;
		}

		return ret;
	}

	public void checkSN( String sn1sn2, TreeItem<FAAnionRow> node, int check )
	{
		if( check == -1 )
		{
			if(compareSN( sn1sn2, node.getValue().getMass() ) == -1)
			{
				node.getValue().getSn1().set( true );
				node.getValue().validProperty().set( true );
			}
			else
			{
				node.getValue().getSn2().set( false );
			}
		}
		else if( check == 1 )
		{
			if(compareSN( sn1sn2, node.getValue().getMass() ) == 1)
			{
				node.getValue().getSn2().set( true );
				node.getValue().validProperty().set( true );
			}
			else
			{
				node.getValue().getSn1().set( false );
			}
		}
		else if( check == 0 )
		{
			node.getValue().getSn1().set( true );
			node.getValue().validProperty().set( true );
		}
	}

	public void checkSN( String sn1sn2, FAAnionRow node, int check )
	{
		if( check == -1 )
		{
			if(compareSN( sn1sn2, node.getMass() ) == -1)
			{
				node.getSn1().set( true );
			}
			else
			{
				node.getSn2().set( false );
			}
		}
		else if( check == 1 )
		{
			if(compareSN( sn1sn2, node.getMass() ) == 1)
			{
				node.getSn2().set( true );
			}
			else
			{
				node.getSn1().set( false );
			}
		}
		else if( check == 0 )
		{
			node.getSn1().set( true );
		}
	}

	public void checkSNWithFAIndex( String sn1sn2, FAAnionRow node, int check )
	{
		if( check == -1 )
		{
			if(compareSN( sn1sn2, node.getMass() ) == -1)
			{
				node.getSn1().set( true );
			}
			else
			{
				node.getSn2().set( false );
			}
		}
		else if( check == 1 )
		{
			if(compareSN( sn1sn2, node.getMass() ) == 1)
			{
				node.getSn1().set( true );
			}
			else
			{
				node.getSn1().set( false );
			}
		}
		else if( check == 0 )
		{
			node.getSn1().set( true );
		}
	}

	public static boolean isDoubleBond( String group )
	{
		String snString = group.replaceAll( "[^0-9]+", "" );
		String sn2 = snString.substring( 3, 6 );

		return sn2.endsWith( "0" );
	}

   public static boolean isSym( String group )
   {
      String snString = group.replaceAll( "[^0-9]+", "" );
      String sn1 = snString.substring( 0, 3 );
      String sn2 = snString.substring( 3, 6 );

      return sn1.equals( sn2 );
   }

	protected static double computeR2(XYChart.Series<Number, Number> series1, XYChart.Series<Number, Number> series2)
	{
		XYChart.Data<Number, Number>[] data1 = series1.getData().toArray(new XYChart.Data[]{});
		XYChart.Data<Number, Number>[] data2 = series2.getData().toArray(new XYChart.Data[]{});

		double rSquared = 0f;
		double fitted  = 0f;

		float sum = 0;
		for (int i = 0; i < data2.length; i++){
			sum = sum + data2[i].getYValue().floatValue();
		}
		// calculate average
		double average = data2.length == 1? 0: sum / data2.length;

		for(int i = 0; i < data2.length; i++)
		{
			float x = data2[ i ].getXValue().floatValue();
			Optional< XYChart.Data< Number, Number > > fit = series1.getData().stream().filter( c -> c.getXValue().floatValue() == x ).findFirst();

			if(fit.isPresent())
			{
				rSquared += FastMath.pow( data2[ i ].getYValue().doubleValue() - fit.get().getYValue().doubleValue(), 2d );
				fitted += FastMath.pow( fit.get().getYValue().doubleValue() - average, 2d );
			}
			else
			{
				rSquared += FastMath.pow( data2[ i ].getYValue().doubleValue() - data1[ i ].getYValue().doubleValue(), 2d );
				fitted += FastMath.pow( data1[i].getYValue().doubleValue() - average, 2d );
			}
		}

		rSquared = 1 - (rSquared / fitted);

		//System.out.println( "R² = " + rSquared);
		return rSquared;
	}

	protected static void createSeries( LineChart< Number, Number > chart, Fragment fragment, String seriesName )
	{
		XYChart.Series series = new XYChart.Series();
		series.setName(seriesName);

		for ( Float key: fragment.keys() )
		{
			XYChart.Data node = new XYChart.Data( key, fragment.get( key ) );
			node.setNode( new HoveredNode( fragment.get( key ) ) );
			series.getData().add( node );
		}
		chart.getData().add( series );
	}

   protected static void createSeriesWithMzCF( LineChart< Number, Number > chart, Fragment fragment, String seriesName )
   {
      XYChart.Series series = new XYChart.Series();
      series.setName(seriesName);

      for ( Float key: fragment.keys() )
      {
         XYChart.Data node = new XYChart.Data( key, fragment.getCF( key ) );
         node.setNode( new HoveredNode( fragment.get( key ) ) );
         series.getData().add( node );
      }
      chart.getData().add( series );
   }

	protected static void createSeries( LineChart< Number, Number > chart, Fragment fragment, SplineComposite function, String seriesName )
	{
		XYChart.Series seriesAct = new XYChart.Series();
		seriesAct.setName( seriesName + ".Actual" );

		XYChart.Series seriesFit = new XYChart.Series();
		seriesFit.setName( seriesName + ".Fitted");

		for ( Float key: fragment.keys() )
		{
			XYChart.Data node = new XYChart.Data( key, fragment.get( key ) );
			node.setNode( new HoveredNode( fragment.get( key ) ) );
			seriesAct.getData().add( node );

			double fittedValue = function.value( key );
			XYChart.Data fitNode = new XYChart.Data( key, fittedValue );
			fitNode.setNode( new HoveredNode( fittedValue ) );
			seriesFit.getData().add( fitNode );
		}

		chart.getData().add( seriesAct );
		chart.getData().add( seriesFit );
	}

	protected static void createSeries( LineChart< Number, Number > chart,
			Set<Float> keys, SplineComposite function, String seriesName )
	{
		XYChart.Series series = new XYChart.Series();
		series.setName( seriesName );

		for ( Float key: keys )
		{
			double val = function.value( key );

			XYChart.Data node = new XYChart.Data( key,  val );
			node.setNode( new HoveredNode( val ) );
			series.getData().add( node );
		}

		chart.getData().add( series );
	}


	protected static void createSeries( LineChart< Number, Number > chart,
			LineChart< Number, Number > correctionFactorChart,
			Fragment fragment, SplineComposite function, String seriesName )
	{
		createSeries( chart, correctionFactorChart, fragment, function, null, seriesName );
	}

	protected static void createSeries( LineChart< Number, Number > chart,
			LineChart< Number, Number > correctionFactorChart,
			Fragment fragment, SplineComposite function, Fragment fitted, String seriesName )
	{
		XYChart.Series seriesAct = new XYChart.Series();
		seriesAct.setName( seriesName + ".Actual" );

		XYChart.Series seriesFit = new XYChart.Series();
		seriesFit.setName( seriesName + ".Fitted");

		XYChart.Series seriesFactor = new XYChart.Series();
		seriesFactor.setName( seriesName + ".cFactor" );

		for ( Float key: fragment.keys() )
		{
			XYChart.Data node = new XYChart.Data( key, fragment.get( key ) );
			node.setNode( new HoveredNode( fragment.get( key ) ) );
			seriesAct.getData().add( node );

			double fittedValue = function.value( key );
			XYChart.Data fitNode = new XYChart.Data( key, fittedValue );
			fitNode.setNode( new HoveredNode( fittedValue ) );
			seriesFit.getData().add( fitNode );

			float correctionFactor = (null != fitted) ? fitted.getCF( key ) : fragment.getCF( key );
			XYChart.Data factorNode = new XYChart.Data( key, correctionFactor );
			factorNode.setNode( new HoveredNode( correctionFactor ) );
			seriesFactor.getData().add( factorNode );
		}

		chart.getData().add( seriesAct );
		chart.getData().add( seriesFit );

		correctionFactorChart.getData().add( seriesFactor );
	}

	protected static void create4Series( LineChart< Number, Number > chart,
			LineChart< Number, Number > correctionFactorChart,
			Fragment fragment, SplineComposite function, Fragment fitted, String seriesName )
	{
		// Actual experiment data
		XYChart.Series seriesAct = new XYChart.Series();
		seriesAct.setName( seriesName + ".Actual" );

		// Corrected by correction factor
		XYChart.Series seriesCorrected = new XYChart.Series();
		seriesCorrected.setName( seriesName + ".Corrected" );

		// Extrapolated with CQ based curve
		XYChart.Series seriesFit = new XYChart.Series();
		seriesFit.setName( seriesName + ".Fitted");

		// Correction Factor
		XYChart.Series seriesFactor = new XYChart.Series();
		seriesFactor.setName( seriesName + ".cFactor" );

		for ( Float key: fragment.keys() )
		{
			XYChart.Data node = new XYChart.Data( key, fragment.get( key ) );
			node.setNode( new HoveredNode( fragment.get( key ) ) );
			seriesAct.getData().add( node );

			double fittedValue = function.value( key );
			XYChart.Data fitNode = new XYChart.Data( key, fittedValue );
			fitNode.setNode( new HoveredNode( fittedValue ) );
			seriesFit.getData().add( fitNode );

			float correctionFactor = (null != fitted) ? fitted.getCF( key ) : fragment.getCF( key );

			float correctedValue = correctionFactor * fragment.get( key );
			XYChart.Data correctedNode = new XYChart.Data( key, correctedValue );
			correctedNode.setNode( new HoveredNode( correctedValue ) );
			seriesCorrected.getData().add( correctedNode );

			XYChart.Data factorNode = new XYChart.Data( key, correctionFactor );
			factorNode.setNode( new HoveredNode( correctionFactor ) );
			seriesFactor.getData().add( factorNode );
		}

		chart.getData().add( seriesAct );
		chart.getData().add( seriesFit );
		chart.getData().add( seriesCorrected );

		correctionFactorChart.getData().add( seriesFactor );
	}

	protected static void createSeries( LineChart< Number, Number > chart, Fragment fragment, Fragment fitted, String seriesName )
	{
		XYChart.Series seriesAct = new XYChart.Series();
		seriesAct.setName( seriesName + ".Actual" );

		XYChart.Series seriesFit = new XYChart.Series();
		seriesFit.setName( seriesName + ".Fitted");

		for ( Float key: fragment.keys() )
		{
			XYChart.Data node = new XYChart.Data( key, fragment.get( key ) );
			node.setNode( new HoveredNode( fragment.get( key ) ) );
			seriesAct.getData().add( node );

			double fittedValue = fitted.get( key );
			XYChart.Data fitNode = new XYChart.Data( key, fittedValue );
			fitNode.setNode( new HoveredNode( fittedValue ) );
			seriesFit.getData().add( fitNode );
		}

		chart.getData().add( seriesAct );
		chart.getData().add( seriesFit );
	}

	protected static void createSeries( LineChart< Number, Number > chart,
			LineChart< Number, Number > correctionFactorChart, Fragment fragment, String seriesName )
	{
		XYChart.Series series = new XYChart.Series();
		series.setName(seriesName);

		XYChart.Series seriesFactor = new XYChart.Series();
		seriesFactor.setName( seriesName + ".cFactor" );

		for ( Float key: fragment.keys() )
		{
			XYChart.Data node = new XYChart.Data( key, fragment.get( key ) );
			node.setNode( new HoveredNode( fragment.get( key ) ) );
			series.getData().add( node );

			float correctionFactor = fragment.getCF( key );
			XYChart.Data factorNode = new XYChart.Data( key, correctionFactor );
			factorNode.setNode( new HoveredNode( correctionFactor ) );
			seriesFactor.getData().add( factorNode );
		}

		chart.getData().add( series );
		correctionFactorChart.getData().add( seriesFactor );
	}

	protected static void updateCharts( boolean newValue, LineChart< Number, Number > chart,
			Fragment fragment, String seriesName, String postFix )
	{
		if( newValue )
		{
			createSeries( chart, fragment, seriesName + postFix );
		}
		else
		{
			chart.getData().removeIf( series -> series.getName().startsWith( seriesName ) );
		}
	}

	/**
	 * UpdateCharts function for MzCalibration tab
	 *
	 * @param newValue					show/hidden boolean value
	 * @param chart						The curve chart
	 * @param correctionFactorChart		The correction factor chart
	 * @param errorChart				The error chart
	 * @param treeMap					TreeMap containing fragments
	 * @param maxFitMap					Max fitted value map
	 * @param correctionFactorMap		Correction factor map
	 * @param maxMz						Maximum Mz
	 * @param group						Fragment group
	 * @param mass						Mass
	 * @param seriesName				Series name
	 * @param cq						FA-index
	 * @param postFix					Post fix for the series
	 */
	protected static void updateCharts( boolean newValue, LineChart< Number, Number > chart,
			LineChart< Number, Number > correctionFactorChart, LineChart< Number, Number > errorChart,
			TreeMap< Double, Fragment > treeMap, HashMap<Float, Float> maxFitMap,
			TreeMap< Double, Fragment > correctionFactorMap,
			Double maxMz, String group, Double mass, String seriesName, Float cq, String postFix )
	{
		if ( newValue )
		{
			// Update main chart
			{
				createSeries( chart, treeMap.get( mass ), seriesName + postFix );
			}

			// Update correction factor chart
			if(correctionFactorMap != null && correctionFactorMap.containsKey( mass ))
			{
				createSeries( correctionFactorChart, correctionFactorMap.get( mass ),
						seriesName + ".cFactor" );
			}

			// Update error chart
			if(treeMap.keySet().size() > 0)
			{
				createErrorSeries(errorChart, maxFitMap, cq, group + mass, maxMz, "FAanion" );
			}
		}
		else
		{
			chart.getData().removeIf( series -> series.getName().startsWith( seriesName ) );
			correctionFactorChart.getData().removeIf( series -> series.getName().startsWith( seriesName ) );

			removeErrorSeries( errorChart, group + mass );
		}
	}

   /**
    * UpdateCharts function for MzCalibration tab
    *
    * @param newValue					show/hidden boolean value
    * @param chart						The curve chart
    * @param correctionFactorChart		The correction factor chart
    * @param errorChart				The error chart
    * @param treeMap					TreeMap containing fragments
    * @param maxFitMap					Max fitted value map
    * @param correctionFactorMap		Correction factor map
    * @param maxMz						Maximum Mz
    * @param group						Fragment group
    * @param mass						Mass
    * @param seriesName				Series name
    * @param cq						FA-index
    * @param postFix					Post fix for the series
    */
   protected static void updateChartsWithTwoFactors( boolean newValue, LineChart< Number, Number > chart,
           LineChart< Number, Number > correctionFactorChart, LineChart< Number, Number > errorChart,
           TreeMap< Double, Fragment > treeMap, HashMap<Float, Float> maxFitMap,
           TreeMap< Double, Fragment > correctionFactorMap,
           Double maxMz, String group, Double mass, String seriesName, Float cq, String postFix )
   {
      if ( newValue )
      {
         // Update main chart
         {
            createSeries( chart, treeMap.get( mass ), seriesName + postFix );
         }

         // Update correction factor chart
         if(correctionFactorMap != null && correctionFactorMap.containsKey( mass ))
         {
            createSeriesWithMzCF( correctionFactorChart, correctionFactorMap.get( mass ),
                    seriesName + ".cf" );
         }

         // Update error chart
         if(treeMap.keySet().size() > 0)
         {
            createErrorSeries(errorChart, maxFitMap, cq, group + mass, maxMz, "FAanion" );
         }
      }
      else
      {
         chart.getData().removeIf( series -> series.getName().startsWith( seriesName ) );
         correctionFactorChart.getData().removeIf( series -> series.getName().startsWith( seriesName ) );

         removeErrorSeries( errorChart, group + mass );
      }
   }

   /**
    * UpdateCharts function for MzCalibration tab
    *
    * @param newValue					show/hidden boolean value
    * @param chart						The curve chart
    * @param correctionFactorChart		The correction factor chart
    * @param errorChart				The error chart
    * @param treeMap					TreeMap containing fragments
    * @param maxFitMap					Max fitted value map
    * @param correctionFactorMap		Correction factor map
    * @param maxMz						Maximum Mz
    * @param seriesName				Series name
    * @param postFix					Post fix for the series
    */
   protected static void updateChartsWithMzFactor( boolean newValue, LineChart< Number, Number > chart,
           LineChart< Number, Number > correctionFactorChart, LineChart< Number, Number > errorChart,
           TreeMap< String, Fragment > treeMap, HashMap<String, Float> maxFitMap,
           TreeMap< String, Fragment > correctionFactorMap,
           Double maxMz, String seriesName, String postFix )
   {
      if ( newValue )
      {
         // Update main chart
         {
            createSeries( chart, treeMap.get( seriesName ), seriesName + postFix );
         }

         // Update correction factor chart
         if(correctionFactorMap != null && correctionFactorMap.containsKey( seriesName ))
         {
            createSeriesWithMzCF( correctionFactorChart, correctionFactorMap.get( seriesName ),
                    seriesName + ".mzcf" );
         }

//         // Update error chart
//         if(treeMap.keySet().size() > 0)
//         {
//            createErrorSeries(errorChart, maxFitMap, seriesName, maxMz, "FAanion" );
//         }
      }
      else
      {
         chart.getData().removeIf( series -> series.getName().startsWith( seriesName ) );
         correctionFactorChart.getData().removeIf( series -> series.getName().startsWith( seriesName ) );

//         removeErrorSeries( errorChart, seriesName );
      }
   }

	protected static void updateCharts( boolean newValue, LineChart< Number, Number > chart,
			LineChart< Number, Number > correctionFactorChart, LineChart< Number, Number > errorChart,
			TreeMap< Double, Fragment > treeMap, TreeMap<String, SplineComposite > functionMap, HashMap<Float, Float> maxFitMap,
			Double maxMz, String group, Double mass, String seriesName, Float cq, String postFix )
	{
		if ( newValue )
		{
			// Update main chart
			{
				createSeries( chart, correctionFactorChart, treeMap.get( mass ), functionMap.get( group + mass ), seriesName + postFix );
			}

			// Update error chart
			if(treeMap.keySet().size() > 0)
			{
				createErrorSeries(errorChart, maxFitMap, cq, group + mass, maxMz, "FAanion" );
			}
		}
		else
		{
			chart.getData().removeIf( series -> series.getName().startsWith( seriesName ) );
			correctionFactorChart.getData().removeIf( series -> series.getName().startsWith( seriesName ) );

			removeErrorSeries( errorChart, group + mass );
		}
	}

	protected static void updateCharts4Series( boolean newValue, LineChart< Number, Number > chart,
			LineChart< Number, Number > correctionFactorChart, LineChart< Number, Number > errorChart,
			TreeMap< Double, Fragment > treeMap, TreeMap<String, SplineComposite > functionMap,
			TreeMap< Double, Fragment > fittedMap,
			HashMap<Float, Float> maxFitMap,
			Double maxMz, String group, Double mass, String seriesName, Float cq, String postFix )
	{
		if ( newValue )
		{
			// Update main chart
			{
				create4Series( chart, correctionFactorChart, treeMap.get( mass ), functionMap.get( group + mass ), fittedMap.get(mass), seriesName + postFix );
			}

			// Update error chart
			if(treeMap.keySet().size() > 0)
			{
				createErrorSeries(errorChart, maxFitMap, cq, group + mass, maxMz, "FAanion" );
			}
		}
		else
		{
			chart.getData().removeIf( series -> series.getName().startsWith( seriesName ) );
			correctionFactorChart.getData().removeIf( series -> series.getName().startsWith( seriesName ) );

			removeErrorSeries( errorChart, group + mass );
		}
	}

	protected static void updateCharts( boolean newValue, LineChart< Number, Number > chart,
			LineChart< Number, Number > correctionFactorChart, LineChart< Number, Number > errorChart,
			TreeMap< Double, Fragment > treeMap, TreeMap<String, SplineComposite > functionMap,
			TreeMap< Double, Fragment > fittedMap,
			HashMap<Float, Float> maxFitMap,
			Double maxMz, String group, Double mass, String seriesName, Float cq, String postFix )
	{
		if ( newValue )
		{
			// Update main chart
			{
				createSeries( chart, correctionFactorChart, treeMap.get( mass ), functionMap.get( group + mass ), fittedMap.get(mass), seriesName + postFix );
			}

			// Update error chart
			if(treeMap.keySet().size() > 0)
			{
				createErrorSeries(errorChart, maxFitMap, cq, group + mass, maxMz, "FAanion" );
			}
		}
		else
		{
			chart.getData().removeIf( series -> series.getName().startsWith( seriesName ) );
			correctionFactorChart.getData().removeIf( series -> series.getName().startsWith( seriesName ) );

			removeErrorSeries( errorChart, group + mass );
		}
	}

	protected static void updateCO2FittedCharts( boolean newValue, LineChart< Number, Number > chart, LineChart< Number, Number > errorChart,
			TreeMap< Double, Fragment > treeMap, TreeMap< Double, Fragment > fittedMap, HashMap<Float, Float> maxFitMap, Double maxMz, String group, Double mass, String seriesName, Float cq, String postFix )
	{
		if ( newValue )
		{
			// Update main chart
			{
				createSeries( chart, treeMap.get( mass ), fittedMap.get(mass), seriesName + postFix );
			}

			// Update error chart
			if(treeMap.keySet().size() > 0)
			{
				createErrorSeries( errorChart, maxFitMap, cq, group + mass, maxMz, "CO2Loss" );
			}
		}
		else
		{
			chart.getData().removeIf( series -> series.getName().startsWith( seriesName ) );

			removeErrorSeries( errorChart, group + mass );
		}
	}

	protected static void updateCO2Charts( boolean newValue, LineChart< Number, Number > chart, LineChart< Number, Number > errorChart,
			TreeMap< Double, Fragment > treeMap, TreeMap<String, SplineComposite > functionMap, HashMap<Float, Float> maxFitMap, Double maxMz, String group, Double mass, String seriesName, Float cq, String postFix )
	{
		if ( newValue )
		{
			// Update main chart
			{
				createSeries( chart, treeMap.get( mass ), functionMap.get( group + mass ), seriesName + postFix );
			}

			// Update error chart
			if(treeMap.keySet().size() > 0)
			{
				createErrorSeries( errorChart, maxFitMap, cq, group + mass, maxMz, "CO2Loss" );
			}
		}
		else
		{
			chart.getData().removeIf( series -> series.getName().startsWith( seriesName ) );

			removeErrorSeries( errorChart, group + mass );
		}
	}

	protected static void createErrorSeries( LineChart< Number, Number > errorChart, HashMap<Float, Float> maxFitMap, Float cq, String name, Double maxMz, String category )
	{
		if(errorChart.getData().size() == 0) {
			errorChart.getData().addAll( new XYChart.Series(), new XYChart.Series() );

			XYChart.Series fitSeries = errorChart.getData().get(0);
			fitSeries.setName( category + ".Fitted" );

			for(Float faIndex : maxFitMap.keySet())
			{
				XYChart.Data node = new XYChart.Data( faIndex, maxFitMap.get(faIndex) );
				node.setExtraValue( faIndex );
				node.setNode( new HoveredNode( maxFitMap.get(faIndex) ));
				fitSeries.getData().add( node );
			}

			XYChart.Series actSeries = errorChart.getData().get(1);
			actSeries.setName( category + ".Actual" );
		}

		XYChart.Series fitSeries = errorChart.getData().get(0);
		XYChart.Series actSeries = errorChart.getData().get(1);

		XYChart.Data node = new XYChart.Data( cq, maxMz );
		node.setExtraValue( name );
		node.setNode( new HoveredNode( maxMz ) );
		actSeries.getData().add( node );

		// Calculate R^2
		errorChart.setTitle( "R² = " + computeR2( fitSeries, actSeries ) );
	}

//   protected static void createErrorSeries( LineChart< Number, Number > errorChart, HashMap<String, Float> maxFitMap, String name, Double maxMz, String category )
//   {
//      if(errorChart.getData().size() == 0) {
//         errorChart.getData().addAll( new XYChart.Series(), new XYChart.Series() );
//
//         XYChart.Series fitSeries = errorChart.getData().get(0);
//         fitSeries.setName( category + ".Fitted" );
//
//         for(String faIndex : maxFitMap.keySet())
//         {
//            XYChart.Data node = new XYChart.Data( faIndex, maxFitMap.get(faIndex) );
//            node.setExtraValue( faIndex );
//            node.setNode( new HoveredNode( maxFitMap.get(faIndex) ));
//            fitSeries.getData().add( node );
//         }
//
//         XYChart.Series actSeries = errorChart.getData().get(1);
//         actSeries.setName( category + ".Actual" );
//      }
//
//      XYChart.Series fitSeries = errorChart.getData().get(0);
//      XYChart.Series actSeries = errorChart.getData().get(1);
//
//      XYChart.Data node = new XYChart.Data( 1f, maxMz );
//      node.setExtraValue( name );
//      node.setNode( new HoveredNode( maxMz ) );
//      actSeries.getData().add( node );
//
//      // Calculate R^2
//      errorChart.setTitle( "R² = " + computeR2( fitSeries, actSeries ) );
//   }

	protected static void removeErrorSeries( LineChart< Number, Number > errorChart, String name )
	{
		if(errorChart.getData().size() != 0) {
			XYChart.Series fitSeries = errorChart.getData().get(0);
			XYChart.Series actSeries = errorChart.getData().get(1);

			actSeries.getData().removeIf( c -> ((XYChart.Data) c).getExtraValue().equals( name ) );

			if(actSeries.getData().size() == 0)
			{
				errorChart.getData().clear();
				errorChart.setTitle( null );
			}
			else
			{
				errorChart.setTitle( "R² = " + computeR2( fitSeries, actSeries ) );
			}
		}
	}


	protected static void fillLinearRegressionValues( boolean is2ndPoly, ArrayList< Float[] > lrNCE,
			ArrayList< Float[] > lrIntensity, int[] height, Double value, double[] x, double[] y )
	{
		for(int idx = 0; idx < height.length; idx++)
		{
			if(is2ndPoly)
			{
				double ce =  value * value * lrNCE.get(idx)[3] + value * lrNCE.get(idx)[2] + lrNCE.get(idx)[1];
				double intensity =  value * value * lrIntensity.get(idx)[3] + value * lrIntensity.get(idx)[2] + lrIntensity.get(idx)[1];

				x[idx] = ce;
				y[idx] = intensity;
			}
			else
			{
				double ce =  value * lrNCE.get(idx)[2] + lrNCE.get(idx)[1];
				double intensity =  value * lrIntensity.get(idx)[2] + lrIntensity.get(idx)[1];

				x[idx] = ce;
				y[idx] = intensity;
			}
		}
	}

	protected static ArrayList< Float[] > buildLinearRegressionArrayList( boolean is2ndPoly, TreeMap< Double, ArrayList< Float[] > > interpolateList,
			int[] height, int pos )
	{
		ArrayList< Float[] > arrayList = new ArrayList<>(  );

		int dataSize = interpolateList.keySet().size();
		int percent = 0;

		OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();

		//		System.out.println("Linear regression data for Intensity");
		for ( int point : height )
		{
			if( is2ndPoly )
			{
				double[] data = new double[ dataSize * 3 ];
				int itemIdx = 0;

				for ( Double mass : interpolateList.keySet() )
				{
					ArrayList< Float[] > list = interpolateList.get( mass );

					data[ itemIdx++ ] = list.get( point )[ pos ].doubleValue();
					data[ itemIdx++ ] = mass;
					data[ itemIdx++ ] = mass * mass;

					percent = list.get( point )[ 2 ].intValue();
				}

				regression.newSampleData( data, dataSize, 2 );
				double[] params = regression.estimateRegressionParameters();

				arrayList.add( new Float[] { ( float ) percent, ( float ) params[ 0 ], ( float ) params[ 1 ],( float ) params[ 2 ] } );
			}
			else
			{
				double[] data = new double[ dataSize * 2 ];
				int itemIdx = 0;

				for ( Double mass : interpolateList.keySet() )
				{
					ArrayList< Float[] > list = interpolateList.get( mass );

					data[ itemIdx++ ] = list.get( point )[ pos ].doubleValue();
					data[ itemIdx++ ] = mass;

					percent = list.get( point )[ 2 ].intValue();
				}

				regression.newSampleData( data, dataSize, 1 );
				double[] params = regression.estimateRegressionParameters();

				arrayList.add( new Float[] { ( float ) percent, ( float ) params[ 0 ], ( float ) params[ 1 ] } );
			}
		}

		return arrayList;
	}


	protected static SplineComposite getInterpolateFunction( boolean is2ndPoly,
			int[] height,
			ArrayList< Float[] > lrNCE,
			ArrayList< Float[] > lrIntensity, float val )
	{
//      System.out.println("Interpolated function called.");

		double[] x = new double[height.length];
		double[] y = new double[height.length];

		fillLinearRegressionValues( is2ndPoly, lrNCE, lrIntensity, height, (double) val, x, y );

		// Check order of x
		try {
			MathArrays.checkOrder( x );
		} catch ( NonMonotonicSequenceException exp )
		{
			System.err.print( "Linear Regression could give more consistent sequence for " + val  );
			System.err.println( " if you add a higher mass as reference to cover all the curves." );
			MathArrays.sortInPlace( x, y );
		}

		final WeightedObservedPoints obs = new WeightedObservedPoints();

		obs.clear();
		for(int idx = 0; idx < 5; idx++)
		{
			obs.add( x[idx], y[idx] );
		}

		ExponentialFitter left = new ExponentialFitter( obs.toList() );

		obs.clear();
		for(int idx = 50; idx < height.length; idx++)
		{
			obs.add( x[idx], y[idx] );
		}

		ExponentialFitter right = new ExponentialFitter( obs.toList() );

		HashSet<Double> set = new HashSet<>(  );

		// Update the splineFunction
		for(int idx = 0; idx < 4; idx++)
		{
			y[idx] = (float) left.value( x[idx] );

			if( set.contains( x[idx] ) )
			{
				x[idx] += 0.1;
				y[idx] = (float) left.value( x[idx] );
			}

			set.add( x[idx] );
		}

		for(int idx = 51; idx < height.length; idx++)
		{
			y[idx] = (float) right.value( x[idx] );
		}

		for(int idx = 0; idx < y.length; idx++)
		{
			if( Double.isNaN( y[idx] ) ) y[idx] = 0;
		}

		PolynomialSplineFunction splineFunction = new LinearInterpolator().interpolate( x, y );

//      PolynomialSplineFunction splineFunction = new SplineInterpolator().interpolate( x, y );

		return new SplineComposite( left, right, splineFunction, x[4], x[51] );
	}
}
