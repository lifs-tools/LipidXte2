package de.mpicbg.ms.view.pane.component;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;

/**
 * Author: HongKee Moon (moon@mpi-cbg.de), Scientific Computing Facility
 * Organization: MPI-CBG Dresden
 * Date: March 2017
 */
public class LabeledPane extends BorderPane
{
   public LabeledPane( String title, Node content )
   {
      Label topic = new Label( title );
      topic.setPadding( new Insets( 5 ) );
      topic.setLabelFor( this );

      setTop( topic );
      setCenter( content );
   }
}
