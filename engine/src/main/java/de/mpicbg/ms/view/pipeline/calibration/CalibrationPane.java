package de.mpicbg.ms.view.pipeline.calibration;

import de.mpicbg.ms.Pipeline;
import de.mpicbg.ms.model.event.ProcessEvent;
import de.mpicbg.ms.model.data.FAAnion;
import de.mpicbg.ms.view.pane.MasterXmlPane;

import de.mpicbg.ms.view.pipeline.validation.TxCorrectionTab;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.chart.LineChart;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.TabPane;
import javafx.stage.FileChooser;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * Create Calibration TreeTable
 */
@SuppressWarnings( "Duplicates" )
public class CalibrationPane extends TabPane
{
   final ObservableList< FAAnion > faAnionsList;
   final LinkedHashSet< String > folderList;

   public CalibrationPane( LinkedHashSet< String > folderList, LineChart< Number, Number > chart, LineChart< Number, Number > correctionFactorChart, LineChart< Number, Number > extraChart, ObservableList< FAAnion > faAnionsList, MasterXmlPane masterXmlPane )
   {
      this.folderList = folderList;
      this.faAnionsList = faAnionsList;

      masterXmlPane.setFaAnionsList( faAnionsList );
      masterXmlPane.setCalibrationTabPane( this );

      PolynomialCalibrationTab polynomialCalibrationTab = getPolynomialCalibrationTab( chart, correctionFactorChart, extraChart, faAnionsList, masterXmlPane );

      addEventHandler( ProcessEvent.STORE_LIPID_MASTER, new EventHandler< ProcessEvent >()
      {
         @Override public void handle( ProcessEvent event )
         {
            List< FAAnion > faAnionList = ( List< FAAnion > ) event.getParam()[ 0 ];

            if ( faAnionList != null )
            {
               String current = Pipeline.loadFile( "lipid.txt" );

               String updated = FAAnion.update( current, faAnionList );
               //System.out.println(updated);

               Pipeline.saveFile( "lipid.txt", updated );

               System.out.println( "lipid.txt is updated." );
            }
         }
      } );

      // Receive the virtual fragment request from dnSn1CalibrationTab
      //		addEventHandler( ProcessEvent.DB_VIRT_REQ, new EventHandler< ProcessEvent >()
      //		{
      //			@Override public void handle( ProcessEvent event )
      //			{
      //				Event.fireEvent( dbSn2CalibrationTab, event );
      //				Event.fireEvent( dbSymCalibrationTab, event );
      //			}
      //		} );

      //		// Toss the virtual fragment to dnSn1CalibrationTab
      //		addEventHandler( ProcessEvent.DB_SN2_VIRT_RESP, new EventHandler< ProcessEvent >()
      //		{
      //			@Override public void handle( ProcessEvent event )
      //			{
      //				Fragment fragment = (Fragment) event.getParam()[0];
      //				Event.fireEvent( dbSn1CalibrationTab, new ProcessEvent( ProcessEvent.DB_SN2_VIRT_RESP, fragment ) );
      //			}
      //		} );
      //
      //		addEventHandler( ProcessEvent.DB_SYM_VIRT_RESP, new EventHandler< ProcessEvent >()
      //		{
      //			@Override public void handle( ProcessEvent event )
      //			{
      //				Fragment fragment = (Fragment) event.getParam()[0];
      //				Event.fireEvent( dbSn1CalibrationTab, new ProcessEvent( ProcessEvent.DB_SYM_VIRT_RESP, fragment ) );
      //			}
      //		} );

      TxCorrectionTab txCorrectionTab = new TxCorrectionTab();

      //		getTabs().addAll( newtCalibrationTab, dbSn2CalibrationTab, dbSymCalibrationTab, dbSn1CalibrationTab, txCorrectionTab );
      getTabs().addAll( polynomialCalibrationTab, txCorrectionTab );
   }

   private PolynomialCalibrationTab getPolynomialCalibrationTab( LineChart< Number, Number > chart, LineChart< Number, Number > correctionFactorChart, LineChart< Number, Number > extraChart, ObservableList< FAAnion > faAnionsList, MasterXmlPane masterXmlPane )
   {
      return new PolynomialCalibrationTab( faAnionsList, chart, correctionFactorChart, extraChart, masterXmlPane, null );
   }

   private void openCalibrationFile( String fileTitle, String fileName, EventType< ProcessEvent > calibration )
   {
      // Open file dialog
      FileChooser fileChooser = new FileChooser();
      fileChooser.setTitle( "Open " + fileTitle );

      // Set extension filter
      FileChooser.ExtensionFilter extFilter =
              new FileChooser.ExtensionFilter( fileTitle, fileName );
      fileChooser.getExtensionFilters().add( extFilter );

      File file = fileChooser.showOpenDialog( getScene().getWindow() );

      if ( file != null && file.getName().equals( fileName ) )
      {
         Object collection = loadCollection( file.getParent(), file.getName() );
         folderList.remove( file.getParent() );
         folderList.add( file.getParent() );

         // Store lru when user click the menu according to the latest usage
         Pipeline.storeFolderList( folderList );

         if ( collection != null )
            Event.fireEvent( this, new ProcessEvent( calibration, collection, faAnionsList ) );
      }
      else
      {
         System.err.println( "The opening file is not a " + fileTitle + ". Please, open " + fileName + "." );
      }
   }

   private LinkedList< MenuItem > getCalibrationMenuItems( EventType< ProcessEvent > eventType, String fileName )
   {
      LinkedList< MenuItem > menuItems = new LinkedList<>();

      List< String > list = new ArrayList<>( folderList );
      Collections.reverse( list );

      // Get only 10 recent items
      for ( String folder : list.subList( 0, Math.min( list.size(), 10 ) ) )
      {
         MenuItem item = new MenuItem( folder );
         item.setOnAction( event ->
                 {
                    //						System.out.println(folder);
                    Object collection = loadCollection( folder, fileName );
                    folderList.remove( folder );
                    folderList.add( folder );

                    // Store lru when user click the menu according to the latest usage
                    Pipeline.storeFolderList( folderList );

                    if ( collection != null )
                       Event.fireEvent( this, new ProcessEvent( eventType, collection, faAnionsList ) );
                 }
         );
         menuItems.add( item );
      }
      return menuItems;
   }

   private Object loadCollection( String lastAccessedFolder, String fileName )
   {
      XMLDecoder e = null;
      try
      {
         e = new XMLDecoder(
                 new BufferedInputStream(
                         new FileInputStream( lastAccessedFolder + File.separator + fileName ) ) );
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

   private void storeCollection( String lastAccessedFolder, String fileName, Object collection )
   {
      if ( lastAccessedFolder == null )
      {
         //System.err.println( "Last accessed folder is not specified." );
         return;
      }

      XMLEncoder e = null;
      try
      {
         e = new XMLEncoder(
                 new BufferedOutputStream(
                         new FileOutputStream( lastAccessedFolder + File.separator + fileName ) ) );
      }
      catch ( FileNotFoundException e1 )
      {
         e1.printStackTrace();
      }

      e.writeObject( collection );

      e.close();
   }

   public static void main( final String[] args )
   {
      final OLSMultipleLinearRegression regression2 = new OLSMultipleLinearRegression();
      double[] y = {
              4,
              8,
              13,
              18
      };
      double[][] x2 =
              {
                      { 1, 1, 1 },
                      { 1, 2, 4 },
                      { 1, 3, 9 },
                      { 1, 4, 16 },
              };

      regression2.newSampleData( y, x2 );
      regression2.setNoIntercept( true );
      regression2.newSampleData( y, x2 );
      double[] beta = regression2.estimateRegressionParameters();
      for ( double d : beta )
      {
         System.out.println( "D: " + d );
      }
   }
}


