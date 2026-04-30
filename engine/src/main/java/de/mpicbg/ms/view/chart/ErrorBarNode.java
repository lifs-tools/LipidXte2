package de.mpicbg.ms.view.chart;

import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

/**
 * Author: HongKee Moon (moon@mpi-cbg.de), Scientific Computing Facility
 * Organization: MPI-CBG Dresden
 * Date: March 2017
 */
public class ErrorBarNode extends HoveredNode
{
   private Number value;
   private Number stdErr;
   private Number stdDev;
   private Line errorLine = new Line();

   public ErrorBarNode( Number value, Number stdErr, Number stdDev )
   {
      super( value );

      this.value = value;
      this.stdErr = stdErr == null ? 0f : stdErr;
      this.stdDev = stdDev;

      errorLine.setStrokeWidth( 1d );
      getChildren().add( errorLine );
   }

   public double getMaxError()
   {
      return value.doubleValue() + stdErr.doubleValue();
   }

   public double getMinError()
   {
      return value.doubleValue() - stdErr.doubleValue();
   }

   public Number getStdDev()
   {
      return stdDev;
   }

   public void update( double min, double max )
   {
      errorLine.setStartY( max );
      errorLine.setEndY( min );
   }

   public void updateStyle( String... elements )
   {
      super.updateStyle( elements );
      errorLine.getStyleClass().addAll( elements );
   }
}
