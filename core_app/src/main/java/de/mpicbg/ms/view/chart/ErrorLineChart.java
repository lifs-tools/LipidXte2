package de.mpicbg.ms.view.chart;

import javafx.beans.NamedArg;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.chart.Axis;
import javafx.scene.chart.LineChart;

import javafx.scene.chart.NumberAxis;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Author: HongKee Moon (moon@mpi-cbg.de), Scientific Computing Facility
 * Organization: MPI-CBG Dresden
 * Date: March 2017
 * (reference: https://gist.github.com/james-d/7252698)
 */
public class ErrorLineChart<X, Y> extends LineChart<X, Y>
{
	static String DEFAULT_COLOR = "default-color";
	private final BitSet colorBits = new BitSet(8);
	final Map<Series<X,Y>, Integer> seriesColorMap = new HashMap<>();
	final Set<Series<X,Y>> seriesErrorSet = new HashSet<>();

	public ErrorLineChart( @NamedArg( "xAxis" ) Axis< X > xAxis, @NamedArg( "yAxis" ) Axis< Y > yAxis )
	{
		super( xAxis, yAxis );

		final Rectangle zoomRect = new Rectangle();
		zoomRect.setManaged(false);
		zoomRect.setFill( Color.LIGHTSEAGREEN.deriveColor(0, 1, 1, 0.5));
		getChildren().add(zoomRect);

		setUpZooming(zoomRect, this);
	}

	public ErrorLineChart( @NamedArg( "xAxis" ) Axis< X > xAxis, @NamedArg( "yAxis" ) Axis< Y > yAxis, @NamedArg( "data" ) ObservableList< Series< X, Y > > data )
	{
		super( xAxis, yAxis, data );
	}

	private void setUpZooming(final Rectangle rect, final Node zoomingNode) {
		final ObjectProperty<Point2D > mouseAnchor = new SimpleObjectProperty<>();
		zoomingNode.setOnMousePressed(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				if(event.getClickCount() > 1)
				{
					rect.setWidth(0);
					rect.setHeight(0);

					getXAxis().setAutoRanging( true );
				}
				else
				{
					mouseAnchor.set( new Point2D( event.getX(), event.getY() ) );
					rect.setWidth( 0 );
					rect.setHeight( 0 );
					getXAxis().setAutoRanging( false );
				}
			}
		});
		zoomingNode.setOnMouseDragged(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				double x = event.getX();
				double y = event.getY();
				rect.setX(Math.min(x, mouseAnchor.get().getX()));
				rect.setY(Math.min(y, mouseAnchor.get().getY()));
				rect.setWidth(Math.abs(x - mouseAnchor.get().getX()));
				rect.setHeight(Math.abs(y - mouseAnchor.get().getY()));
			}
		});
		zoomingNode.setOnMouseReleased( new EventHandler< MouseEvent >()
		{
			@Override public void handle( MouseEvent event )
			{
				if( rect.getWidth() > 0 || rect.getHeight() > 0 )
					doZoom(rect);
			}
		} );
	}

	private void doZoom(Rectangle zoomRect) {
		Point2D zoomTopLeft = new Point2D(zoomRect.getX(), zoomRect.getY());
		Point2D zoomBottomRight = new Point2D(zoomRect.getX() + zoomRect.getWidth(), zoomRect.getY() + zoomRect.getHeight());
		final NumberAxis yAxis = (NumberAxis) getYAxis();
		final NumberAxis xAxis = (NumberAxis) getXAxis();


		double xOffset = zoomTopLeft.getX() - (yAxis.getLayoutX() + yAxis.getWidth());
		double yOffset = zoomBottomRight.getY() - xAxis.getLayoutY();
		double xAxisScale = xAxis.getScale();
		double yAxisScale = yAxis.getScale();

		xAxis.setLowerBound(xAxis.getLowerBound() + xOffset / xAxisScale);
		xAxis.setUpperBound(xAxis.getLowerBound() + zoomRect.getWidth() / xAxisScale);

//		System.out.println(xAxis.getLowerBound() + " " + xAxis.getUpperBound());

		yAxis.setLowerBound(yAxis.getLowerBound() + yOffset / yAxisScale);
		yAxis.setUpperBound(yAxis.getLowerBound() - zoomRect.getHeight() / yAxisScale);

//		System.out.println(yAxis.getLowerBound() + " " + yAxis.getUpperBound());

		zoomRect.setWidth(0);
		zoomRect.setHeight(0);
	}

	/** @inheritDoc */
	@Override protected void updateAxisRange() {
		final Axis<X> xa = getXAxis();
		final Axis<Y> ya = getYAxis();
		List<X> xData = null;
		List<Y> yData = null;
		if(xa.isAutoRanging()) xData = new ArrayList<X>();
		if(ya.isAutoRanging()) yData = new ArrayList<Y>();
		if(xData != null || yData != null) {
			for(Series<X,Y> series : getData()) {
				for(Data<X,Y> data: series.getData()) {
					{
						if(data.getNode() instanceof ErrorBarNode)
						{
							if ( xData != null )
								xData.add( data.getXValue() );

							ErrorBarNode n = (ErrorBarNode) data.getNode();
							yData.add( ( Y ) new Double( n.getMaxError() ) );
							yData.add( ( Y ) new Double( n.getMinError() ) );
						}
						else
						{
							if ( xData != null )
								xData.add( data.getXValue() );
							if ( yData != null )
								yData.add( data.getYValue() );
						}
					}
				}
			}
			// RT-32838 No need to invalidate range if there is one data item - whose value is zero.
			if(xData != null && !(xData.size() == 1 && getXAxis().toNumericValue(xData.get(0)) == 0)) {
				xa.invalidateRange(xData);
			}
			if(yData != null && !(yData.size() == 1 && getYAxis().toNumericValue(yData.get(0)) == 0)) {
				ya.invalidateRange(yData);
			}

		}
	}

	final int getDataSize() {
		final ObservableList<Series<X,Y>> data = getData();
		return (data!=null) ? data.size() : 0;
	}

	private void updateDefaultColorIndex(final Series<X,Y> series)
	{
		int clearIndex = seriesColorMap.remove(series);
		colorBits.clear(clearIndex);
	}

	@Override protected void seriesAdded(Series<X,Y> series, int seriesIndex)
	{
		super.seriesAdded( series, seriesIndex );
		int nextClearBit = colorBits.nextClearBit(0);
		colorBits.set(nextClearBit, true);
		seriesColorMap.put(series, nextClearBit%8);
	}

	@Override protected void seriesRemoved(final Series<X,Y> series)
	{
		updateDefaultColorIndex(series);

		getPlotChildren().remove(series.getNode());
		for (Data<X,Y> d:series.getData()) getPlotChildren().remove(d.getNode());
		removeSeriesFromDisplay(series);

		seriesErrorSet.remove( series );
	}

	@Override protected void seriesChanged(ListChangeListener.Change<? extends Series> c)
	{
		// Do nothing but overriding the parent method
	}

	@Override protected void layoutPlotChildren() {
		List<PathElement> constructedPath = new ArrayList<>(getData().size());
		List<PathElement> constructedFillPath = new ArrayList<>(getData().size());
		List<PathElement> reverseFillPath = new ArrayList<>(getData().size());

		int errorIndex = -1;

		for (int seriesIndex=0; seriesIndex < getData().size(); seriesIndex++) {
			Series<X,Y> series = getData().get(seriesIndex);

			if(series.getNode() instanceof Path ) {
				// Create a new Path for Fill objects
				final ObservableList<PathElement> seriesLine = ((Path)series.getNode()).getElements();
				seriesLine.clear();
				constructedPath.clear();
				constructedFillPath.clear();
				reverseFillPath.clear();

				double lastX = 0, lastMinY = 0;
				boolean hasError = false;

				for( Data<X, Y> item : series.getData() ) {

					Node symbol = item.getNode();

					double x = getXAxis().getDisplayPosition( item.getXValue() );
					double y = getYAxis().getDisplayPosition(
							getYAxis().toRealValue(getYAxis().toNumericValue( item.getYValue() ) ));

					if (Double.isNaN(x) || Double.isNaN(y)) {
						continue;
					}

					if( symbol instanceof ErrorBarNode )
					{
						hasError = true;

						ErrorBarNode n = (ErrorBarNode) symbol;
						double maxErrY = getYAxis().getDisplayPosition( ( Y ) new Double( n.getMaxError() ) );
						double minErrY = getYAxis().getDisplayPosition( ( Y ) new Double( n.getMinError() ) );

						//System.out.println(maxErrY + ", " + minErrY);
						constructedFillPath.add( new LineTo( x, maxErrY ) );
						reverseFillPath.add( new LineTo( x, minErrY ) );

						lastX = x;
						lastMinY = minErrY;

						n.update( minErrY - y, maxErrY - y );
					}
					else
					{
						constructedPath.add(new LineTo(x, y));
					}

					if( symbol instanceof HoveredNode )
					{
						HoveredNode n = (HoveredNode) symbol;
						n.updateStyle( "default-color" + seriesColorMap.get(series), "chart-line-symbol", "chart-series-line" );
					}

					if (symbol != null) {
						final double w = symbol.prefWidth(-1);
						final double h = symbol.prefHeight(-1);
						symbol.resizeRelocate(x-(w/2), y-(h/2),w,h);
					}
				}

				if( hasError )
				{
					series.getNode().getStyleClass().setAll( "chart-series-line-fill", "series" + seriesColorMap.get(series), DEFAULT_COLOR+seriesColorMap.get(series) );

					seriesErrorSet.add( series );
					constructedFillPath.add( new LineTo( lastX, lastMinY ) );
					Collections.reverse( reverseFillPath );
					constructedFillPath.addAll( reverseFillPath );

					LineTo first = (LineTo) constructedFillPath.get(0);
					seriesLine.add(new MoveTo(first.getX(), first.getY()));
					seriesLine.addAll(constructedFillPath);
					seriesLine.add(new ClosePath());
				}
				else
				{
					if(constructedPath.size() > 0)
					{
						series.getNode().getStyleClass().setAll( "chart-series-line", "series" + seriesColorMap.get( series ), DEFAULT_COLOR + seriesColorMap.get( series ) );

						LineTo first = ( LineTo ) constructedPath.get( 0 );
						seriesLine.add( new MoveTo( first.getX(), first.getY() ) );
						seriesLine.addAll( constructedPath );
					}
					else
					{
						System.err.println( "Something went wrong with the chart. Please, restart the application." );
					}
				}
			}
		}
	}
}
