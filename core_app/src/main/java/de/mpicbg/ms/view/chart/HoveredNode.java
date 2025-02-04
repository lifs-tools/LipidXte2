package de.mpicbg.ms.view.chart;

import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

/**
 * Created by moon on 3/21/16.
 */
public class HoveredNode extends StackPane
{
   final Label label;

   public HoveredNode( Number value )
   {
      setPrefSize( 10, 10 );

      label = createDataThresholdLabel( value );

      setOnMouseEntered( new EventHandler< MouseEvent >()
      {
         @Override public void handle( MouseEvent mouseEvent )
         {
            getChildren().add( label );
            setCursor( Cursor.NONE );
            toFront();
         }
      } );
      setOnMouseExited( new EventHandler< MouseEvent >()
      {
         @Override public void handle( MouseEvent mouseEvent )
         {
            getChildren().remove( label );
            setCursor( Cursor.CROSSHAIR );
         }
      } );
   }

   private Label createDataThresholdLabel( Number value )
   {
      final Label label = new Label( value + "" );

      label.getStyleClass().addAll( getStyleClass() );
      label.setStyle( "-fx-font-size: 10; -fx-font-weight: bold;" );
      label.setTextFill( Color.DARKSLATEGREY );

      label.setMinSize( Label.USE_PREF_SIZE, Label.USE_PREF_SIZE );
      return label;
   }

   public void updateStyle( String... elements )
   {
      label.getStyleClass().addAll( elements );
   }
}
