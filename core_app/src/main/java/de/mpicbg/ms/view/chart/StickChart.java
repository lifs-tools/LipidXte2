package de.mpicbg.ms.view.chart;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.Axis;
import javafx.scene.chart.LineChart;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;

/**
 * StickChart
 */
public class StickChart<X,Y> extends LineChart<X, Y>
{
	/**
	 * Constructs a XYChart given the two axes. The initial content for the chart
	 * plot background and plot area that includes vertical and horizontal grid
	 * lines and fills, are added.
	 * @param numberAxis X Axis for this XY chart
	 * @param numberAxis2 Y Axis for this XY chart
	 */
	public StickChart( Axis< X > numberAxis, Axis< Y > numberAxis2 )
	{
		this( numberAxis, numberAxis2, FXCollections.< Series< X, Y > >observableArrayList() );
	}

	public StickChart(Axis<X> xAxis, Axis<Y> yAxis, ObservableList<Series<X,Y>> data) {
		super( xAxis, yAxis );
		setData(data);
		setAnimated( false );
	}

	/** @inheritDoc */
	@Override protected void layoutPlotChildren() {
		for (int seriesIndex=0; seriesIndex < getData().size(); seriesIndex++) {
			Series<X,Y> series = getData().get(seriesIndex);

			//final DoubleProperty seriesYAnimMultiplier = seriesYMultiplierMap.get(series);
			if(series.getNode() instanceof Path) {
				Path seriesLine = (Path)series.getNode();
				seriesLine.getElements().clear();

				for( Data<X, Y> item : series.getData().sorted() ) {
					double x = getXAxis().getDisplayPosition( item.getXValue() );
					double y = getYAxis().getDisplayPosition(
							getYAxis().toRealValue(getYAxis().toNumericValue( item.getYValue() ) ));
//					double y = getYAxis().getDisplayPosition(
//							getYAxis().toRealValue(getYAxis().toNumericValue(item.getCurrentY()) * seriesYAnimMultiplier.getValue()));

					double zero = getYAxis().getDisplayPosition( ( Y ) new Double( 0 ) );

					seriesLine.getElements().add(new MoveTo(x, zero));
					seriesLine.getElements().add(new LineTo(x, y));

//					Node symbol = item.getNode();
//					if (symbol != null) {
//						final double w = symbol.prefWidth(-1);
//						final double h = symbol.prefHeight(-1);
//						symbol.resizeRelocate(x-(w/2), y-(h/2),w,h);
//					}
				}
			}
		}
	}
}
