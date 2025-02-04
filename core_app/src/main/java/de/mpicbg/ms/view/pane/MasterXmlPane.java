package de.mpicbg.ms.view.pane;

import de.mpicbg.ms.db.Calib;
import de.mpicbg.ms.db.MasterDatabase;
import de.mpicbg.ms.model.Fragment;
import de.mpicbg.ms.model.LipidClassCollection;
import de.mpicbg.ms.model.data.FAAnion;
import de.mpicbg.ms.model.data.FAAnionRow;
import de.mpicbg.ms.model.data.Pos;
import de.mpicbg.ms.model.event.ChartEvent;
import de.mpicbg.ms.model.event.ProcessEvent;
import de.mpicbg.ms.util.TableViewUtil;
import de.mpicbg.ms.view.chart.HoveredNode;
import de.mpicbg.ms.view.treecell.CheckBoxNamedBoolean;
import de.mpicbg.ms.view.treecell.CheckBoxTextFieldTreeTableCell;
import de.mpicbg.ms.view.treecell.FilteredTreeItem;
import de.mpicbg.ms.view.treecell.NamedBoolean;
import de.mpicbg.ms.view.treecell.SelectiveCheckBoxTreeTableCell;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Side;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.util.Precision;
import org.controlsfx.control.MasterDetailPane;
import org.controlsfx.control.textfield.TextFields;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Predicate;

import static de.mpicbg.ms.view.pipeline.calibration.PolynomialCalibrationTab.setupNormBasis;

/**
 * Author: HongKee Moon (moon@mpi-cbg.de), Scientific Computing Facility
 * Organization: MPI-CBG Dresden
 * Date: September 2022
 */
public class MasterXmlPane extends MasterDetailPane
{
   final protected TreeTableView< FAAnionRow > treeTableView;
   final private TableView collisionEnergyTableView;

   final protected FilteredTreeItem< FAAnionRow > root;

   final TreeMap< String, FilteredTreeItem< FAAnionRow > > classMap;

   final TreeMap< String, TreeMap< Integer, TreeItem > > masterDB;

   final ObservableList< FilteredTreeItem< FAAnionRow > > treeItems = FXCollections.observableArrayList();

   final HashMap< FAAnionRow, Fragment > fragmentMap;
   final HashMap< FAAnionRow, Fragment > co2LossFragmentMap;

   String currentClazz;

   final HashMap< Float, Fragment > normBasisMap;

   protected ObservableList< FAAnion > mFaAnionsList;

   protected TabPane mCalibrationTabPane;

   public MasterXmlPane( LineChart< Number, Number > chart )
   {
      classMap = new TreeMap<>();
      masterDB = new TreeMap<>();

      fragmentMap = new HashMap<>();
      co2LossFragmentMap = new HashMap<>();

      normBasisMap = new HashMap<>();

      addEventHandler( ProcessEvent.UPDATE_XML_MASTER, new EventHandler< ProcessEvent >()
      {
         @Override public void handle( ProcessEvent event )
         {
            //				root.getChildren().forEach( c -> c.getChildren().clear() );
            //				root.getChildren().clear();

            Object[] params = event.getParam();
            TreeMap< String, TreeMap< String, TreeMap< String, Fragment > > > fragmentCollection
                    = ( TreeMap< String, TreeMap< String, TreeMap< String, Fragment > > > ) params[ 0 ];

            ObservableList< FAAnion > mFaAnionsList = ( ObservableList< FAAnion > ) params[ 1 ];

            TreeMap< String, TreeMap< String, Fragment > > classCollection = new TreeMap<>();
            ;

            for ( String clazz : fragmentCollection.keySet() )
            {
               if ( !classCollection.containsKey( clazz ) )
                  classCollection.put( clazz, new TreeMap<>() );

               if ( !masterDB.containsKey( clazz ) )
               {
                  currentClazz = clazz;
                  masterDB.put( clazz, new TreeMap<>() );
               }

               //System.out.println(clazz);
               if ( !classMap.containsKey( clazz ) )
               {
                  classMap.put( clazz, new FilteredTreeItem<>( new FAAnionRow( clazz ) ) );
                  treeItems.add( classMap.get( clazz ) );
               }

               final FilteredTreeItem< FAAnionRow > clazzNode = classMap.get( clazz );
               clazzNode.setExpanded( true );

               // Setup NormBasis
               setupNorms( clazz );

               for ( String group : fragmentCollection.get( clazz ).keySet() )
               {
                  TreeMap< String, Fragment > collection = fragmentCollection.get( clazz ).get( group );

                  FAAnion faAnion = null;
                  TreeItem< FAAnionRow > faAnionRowNode = null;
                  for ( String position : collection.keySet() )
                  {
                     System.out.println( position + ":" + collection.get( position ).getIsomer() );
                     classCollection.get( clazz ).put( position, collection.get( position ) );
                     Fragment fragment = collection.get( position );

                     faAnion = getFaAnion( mFaAnionsList, fragment.getMz(), fragment.getIsomer(), fragment.getCarbon(), fragment.getDoubleBond() );
                     if ( faAnion == null )
                        continue;

                     if ( !masterDB.get( clazz ).containsKey( faAnion.getIndex() ) )
                     {
                        final FAAnionRow faAnionRow = new FAAnionRow( faAnion.getIndex(), faAnion );
                        final FilteredTreeItem< FAAnionRow > faAnionRowNodeCreated = new FilteredTreeItem< FAAnionRow >( faAnionRow );

                        masterDB.get( clazz ).put( faAnion.getIndex(), faAnionRowNodeCreated );
                        clazzNode.add( faAnionRowNodeCreated );
                        clazzNode.getChildren().sort( Comparator.comparing( t -> Integer.parseInt( t.getValue().getName() ) ) );
                     }

                     faAnionRowNode = masterDB.get( clazz ).get( faAnion.getIndex() );

                     final FAAnionRow row = new FAAnionRow( faAnion.getIndex(), faAnion );
                     final TreeItem< FAAnionRow > mzNode = new TreeItem<>( row );
                     faAnionRowNode.getChildren().add( mzNode );

                     row.newTitle( row.getTitle() );
                     NamedBoolean nb = row.getTitle();

                     if ( nb != null )
                     {
                        switch ( fragment.getPosition() )
                        {
                           case SN2:
                              row.getSn2().setValid( true );
                              break;
                           case SN1:
                              row.getSn1().setValid( true );
                              break;
                           case SYM:
                              row.getSym().setValid( true );
                              break;
                        }

                        fragmentMap.put( row, fragment );

                        nb.addListener( ( observable, oldValue, newValue ) -> {

                           collisionEnergyTableView.getItems().clear();

                           final String seriesName = clazz + ":" + mzNode.getValue().getName() + ":SN1=" + row.getSn1().getName() + ",SN2=" + row.getSn2().getName();

                           if ( newValue )
                           {
                              Fragment mz = fragmentMap.get( row );

                              Fragment co2mz = null;

                              if ( row.getCo2Mass() != null && row.getCo2Mass().getMass() != 0d )
                              {
                                 co2mz = co2LossFragmentMap.get( row );
                              }

                              XYChart.Series seriesFit = new XYChart.Series();
                              seriesFit.setName( seriesName + ".Fitted" );

                              for ( Float ce : mz.keys() )
                              {
                                 String[] row1;

                                 if ( co2mz != null && co2mz.contains( ce ) )
                                    row1 = new String[] { ce.toString(), mz.get( ce ).toString(),
                                            mz.getCF( ce ).toString(),
                                            co2mz.get( ce ).toString() };
                                 else
                                    row1 = new String[] { ce.toString(), mz.get( ce ).toString(),
                                            mz.getCF( ce ).toString(),
                                            "" };

                                 collisionEnergyTableView.getItems().add( row1 );

                                 XYChart.Data actNode = new XYChart.Data( ce, mz.get( ce ) );
                                 actNode.setNode( new HoveredNode( mz.get( ce ) ) );
                                 seriesFit.getData().add( actNode );
                              }

                              chart.getData().add( seriesFit );
                           }
                           else
                           {
                              chart.getData().removeIf( series -> series.getName().startsWith( seriesName ) );
                           }
                        } );
                     }

                     if ( faAnion.getFADoubleBonds() > 2 )
                     {
                        double co2 = Precision.round( fragment.getMz() - Precision.round( 43.99, 2 ), 2 );
                        String co2name = co2 + "-" + fragment.getPosition();

                        if ( faAnionRowNode.getValue().getCo2Mass() == null )
                        {
                           faAnionRowNode.getValue().setCo2MassString( "" + co2 );
                        }

                        row.setCo2mass( faAnion.getCo2mass() );
                        co2LossFragmentMap.put( row, classCollection.get( clazz ).get( co2name ) );

                        row.getCo2MassProperty().addListener( new ChangeListener< Boolean >()
                        {
                           @Override public void changed( ObservableValue< ? extends Boolean > observable, Boolean oldValue, Boolean newValue )
                           {
                              final String seriesName = clazz + ":" + mzNode.getValue().getName() + ":CO2Loss-" + fragment.getPosition();

                              if ( newValue )
                              {
                                 Fragment co2mz = co2LossFragmentMap.get( row );

                                 XYChart.Series seriesFit = new XYChart.Series();
                                 seriesFit.setName( seriesName + ".Fitted" );

                                 for ( Float ce : co2mz.keys() )
                                 {
                                    XYChart.Data actNode = new XYChart.Data( ce, co2mz.get( ce ) );
                                    actNode.setNode( new HoveredNode( co2mz.get( ce ) ) );
                                    seriesFit.getData().add( actNode );
                                 }

                                 chart.getData().add( seriesFit );
                              }
                              else
                              {
                                 chart.getData().removeIf( series -> series.getName().startsWith( seriesName ) );
                              }
                           }
                        } );
                     }
                  }

                  // Add additional sn1 and sn2 varied series
                  if ( !clazz.equals( "PCO" ) && !clazz.equals( "PEO" ) && containSn1Sn2( faAnionRowNode ) )
                  {
                     // sn1 = 0.5, sn2 = 0.5
                     {
                        final float sn1 = 0.5f, sn2 = 0.5f;
                        addOptionalNode( sn1, sn2, clazz, faAnion, faAnionRowNode );
                     }

                     // sn1 = 0.75, sn2 =0.25
                     {
                        final float sn1 = 0.75f, sn2 = 0.25f;
                        addOptionalNode( sn1, sn2, clazz, faAnion, faAnionRowNode );
                     }

                     // sn1 = 0.25, sn2 = 0.75
                     {
                        final float sn1 = 0.25f, sn2 = 0.75f;
                        addOptionalNode( sn1, sn2, clazz, faAnion, faAnionRowNode );
                     }
                  }
               }
            }

            event.consume();
         }

         private void setupNorms( String clazz )
         {
            Fragment sn1NormBasis = new Fragment( 255.23d, Pos.SN1 );
            Fragment sn2NormBasis = new Fragment( 255.23d, Pos.SN2 );

            Fragment[] fragments;

            if ( clazz.equals( "PCO" ) || clazz.equals( "PEO" ) )
               fragments = new Fragment[] { sn2NormBasis };
            else
               fragments = new Fragment[] { sn1NormBasis, sn2NormBasis };

            setupNormBasis( clazz, fragments );

            for ( float sn1 = 0.25f; sn1 < 1f; sn1 += 0.25f )
            {
               if ( !normBasisMap.containsKey( sn1 ) )
               {
                  // NormBasis
                  final PolynomialSplineFunction func = computeSn2Fragment( fragments, 1.0f - sn1 );

                  final Fragment fragment = new Fragment();

                  for ( Float ce : fragments[ 1 ].keys() )
                  {
                     float intensity = ( float ) func.value( ce );
                     fragment.put( ce, intensity );
                  }

                  normBasisMap.put( sn1, fragment );
               }
            }
         }

         // For normal FAAnion
         private void addOptionalNode( final float sn1, final float sn2, String clazz, FAAnion faAnion, TreeItem< FAAnionRow > faAnionRowNode )
         {
            final FAAnionRow row = new FAAnionRow( faAnion.getIndex(), faAnion );
            row.getSn1().setName( sn1 + "" );
            row.getSn2().setName( sn2 + "" );

            final TreeItem< FAAnionRow > mzNode = new TreeItem<>( row );
            faAnionRowNode.getChildren().add( mzNode );

            row.newTitle( row.getTitle() );
            final NamedBoolean nb = row.getTitle();

            nb.addListener( ( observable, oldValue, newValue ) -> {

               final String seriesName = clazz + ":" + mzNode.getValue().getName() + ":SN1=" + sn1 + ",SN2=" + sn2;

               collisionEnergyTableView.getItems().clear();

               if ( newValue )
               {
                  final XYChart.Series seriesFit = new XYChart.Series();
                  seriesFit.setName( seriesName + ".Fitted" );

                  final PolynomialSplineFunction func = computeSn2Fragment( getSn1Sn2( fragmentMap, faAnionRowNode ), sn2 );

                  PolynomialSplineFunction funcCo2Loss = null;
                  Fragment co2LossFragment = null;
                  if ( !faAnionRowNode.getValue().getCo2MassString().equals( "" ) )
                  {
                     funcCo2Loss = computeSn2Fragment( getSn1Sn2( co2LossFragmentMap, faAnionRowNode ), sn2 );
                     co2LossFragment = getSn1Sn2( co2LossFragmentMap, faAnionRowNode )[ 1 ];
                  }

                  Fragment fragment = getSn1Sn2( fragmentMap, faAnionRowNode )[ 1 ];

                  for ( Float ce : fragment.keys() )
                  {
                     float intensity = ( float ) func.value( ce );
                     float cf = getCf( ce, intensity, sn1 );
                     final String[] row1 = new String[] { ce.toString(), intensity + "",
                             cf + "",
                             "" };

                     if ( funcCo2Loss != null && funcCo2Loss.isValidPoint( ce ) )
                     {
                        row1[ 3 ] = funcCo2Loss.value( ce ) + "";
                     }

                     collisionEnergyTableView.getItems().add( row1 );

                     XYChart.Data actNode = new XYChart.Data( ce, func.value( ce ) );
                     actNode.setNode( new HoveredNode( func.value( ce ) ) );
                     seriesFit.getData().add( actNode );
                  }

                  chart.getData().add( seriesFit );
               }
               else
               {
                  chart.getData().removeIf( series -> series.getName().startsWith( seriesName ) );
               }
            } );

            // For CO2Loss
            if ( !faAnionRowNode.getValue().getCo2MassString().equals( "" ) )
            {
               row.setCo2mass( faAnion.getCo2mass() );
               row.getCo2MassProperty().addListener( new ChangeListener< Boolean >()
               {
                  @Override public void changed( ObservableValue< ? extends Boolean > observable, Boolean oldValue, Boolean newValue )
                  {
                     final String seriesName = clazz + ":" + mzNode.getValue().getName() + ":CO2Loss-" + "SN1=" + sn1 + ",SN2=" + sn2;

                     if ( newValue )
                     {
                        final XYChart.Series seriesFit = new XYChart.Series();
                        seriesFit.setName( seriesName + ".Fitted" );

                        Fragment[] fragments = getSn1Sn2( co2LossFragmentMap, faAnionRowNode );
                        final PolynomialSplineFunction func = computeSn2Fragment( fragments, sn2 );

                        for ( Float ce : fragments[ 1 ].keys() )
                        {
                           XYChart.Data actNode = new XYChart.Data( ce, func.value( ce ) );
                           actNode.setNode( new HoveredNode( func.value( ce ) ) );
                           seriesFit.getData().add( actNode );
                        }

                        chart.getData().add( seriesFit );
                     }
                     else
                     {
                        chart.getData().removeIf( series -> series.getName().startsWith( seriesName ) );
                     }
                  }
               } );
            }

         }
      } );

      TreeMap< Integer, Fragment[] > faIndexMap = new TreeMap<>();
      TreeMap< Integer, Fragment[] > co2LossFaIndexMap = new TreeMap<>();

      addEventHandler( ProcessEvent.CALIBRATION, new EventHandler< ProcessEvent >()
      {
         @Override public void handle( ProcessEvent event )
         {
            Integer index = ( Integer ) event.getParam()[ 0 ];
            Fragment fragment = ( Fragment ) event.getParam()[ 1 ];
            //				Float faIndex = fragment.getFaIndex();

            if ( !faIndexMap.containsKey( index ) )
               faIndexMap.put( index, new Fragment[ 3 ] );

            if ( event.getEventType() == ProcessEvent.DB_SYM_FRAG_RESP )
               faIndexMap.get( index )[ 0 ] = fragment;
            else if ( event.getEventType() == ProcessEvent.DB_SN1_FRAG_RESP )
               faIndexMap.get( index )[ 1 ] = fragment;
            else if ( event.getEventType() == ProcessEvent.DB_SN2_FRAG_RESP )
               faIndexMap.get( index )[ 2 ] = fragment;

            if ( event.getParam().length == 3 )
            {
               // CO2Loss is available
               fragment = ( Fragment ) event.getParam()[ 2 ];

               if ( !co2LossFaIndexMap.containsKey( index ) )
                  co2LossFaIndexMap.put( index, new Fragment[ 3 ] );

               if ( event.getEventType() == ProcessEvent.DB_SYM_FRAG_RESP )
                  co2LossFaIndexMap.get( index )[ 0 ] = fragment;
               else if ( event.getEventType() == ProcessEvent.DB_SN1_FRAG_RESP )
                  co2LossFaIndexMap.get( index )[ 1 ] = fragment;
               else if ( event.getEventType() == ProcessEvent.DB_SN2_FRAG_RESP )
                  co2LossFaIndexMap.get( index )[ 2 ] = fragment;
            }

            if ( LipidClassCollection.isSym( currentClazz ) )
            {
               if ( faIndexMap.containsKey( mFaAnionsList.size() ) &&
                       null != faIndexMap.get( mFaAnionsList.size() )[ 0 ] &&
                       null != faIndexMap.get( mFaAnionsList.size() )[ 1 ] &&
                       null != faIndexMap.get( mFaAnionsList.size() )[ 2 ] )
               {
                  processAllFAanions( chart, faIndexMap, co2LossFaIndexMap );
               }
            }
            else
            {
               if ( faIndexMap.containsKey( mFaAnionsList.size() ) &&
                       null != faIndexMap.get( mFaAnionsList.size() )[ 2 ] )
               {
                  processAllFAanions( chart, faIndexMap, co2LossFaIndexMap );
               }
            }
         }
      } );

      chart.addEventHandler( ChartEvent.CLEAR_CHART, new EventHandler< ChartEvent >()
      {
         @Override public void handle( ChartEvent event )
         {
            uncheckNode( root );
         }
      } );

      treeTableView = new TreeTableView< FAAnionRow >();
      treeTableView.setEditable( true );

      root = new FilteredTreeItem<>( new FAAnionRow( "" ) );
      root.setExpanded( true );

      TreeTableColumn< FAAnionRow, NamedBoolean > column1 = new TreeTableColumn<>( "" );
      column1.setPrefWidth( 150 );

      //Defining cell content
      column1.setCellFactory( SelectiveCheckBoxTreeTableCell.forTreeTableTitleColumn( treeTableView ) );
      column1.setCellValueFactory( ( param ) ->
              new ReadOnlyObjectWrapper<>( param.getValue().getValue().getTitle() )
      );

      //Creating a column2
      TreeTableColumn< FAAnionRow, String > column2 = new TreeTableColumn<>( "mz" );
      column2.setPrefWidth( 80 );

      //Defining cell content
      column2.setCellValueFactory( ( param ) ->
              new ReadOnlyStringWrapper( param.getValue().getValue().getMassString() ) );

      //Creating a column3
      TreeTableColumn< FAAnionRow, String > column3 = new TreeTableColumn<>( "Iso" );
      column3.setPrefWidth( 50 );

      //Defining cell content
      column3.setCellValueFactory( ( param ) ->
              new ReadOnlyStringWrapper( param.getValue().getValue().getIsomer() ) );

      //Creating a column4
      TreeTableColumn< FAAnionRow, NamedBoolean > column4 = new TreeTableColumn<>( "sn1" );
      column4.setPrefWidth( 100 );
      column4.setEditable( false );
      column4.setCellFactory( CheckBoxTextFieldTreeTableCell.forTreeTableSn1Column( treeTableView ) );
      column4.setCellValueFactory( ( param ) ->
              new ReadOnlyObjectWrapper<>( param.getValue().getValue().getSn1() )
      );

      //Creating a column5
      TreeTableColumn< FAAnionRow, NamedBoolean > column5 = new TreeTableColumn<>( "sn2" );
      column5.setPrefWidth( 100 );
      column5.setEditable( false );
      column5.setCellFactory( CheckBoxTextFieldTreeTableCell.forTreeTableSn2Column( treeTableView ) );
      column5.setCellValueFactory( ( param ) ->
              new ReadOnlyObjectWrapper<>( param.getValue().getValue().getSn2() )
      );

      //Creating a column6
      TreeTableColumn< FAAnionRow, NamedBoolean > column6 = new TreeTableColumn<>( "sym" );
      column6.setPrefWidth( 100 );
      column6.setEditable( false );
      column6.setCellFactory( CheckBoxTextFieldTreeTableCell.forTreeTableSymColumn( treeTableView ) );
      column6.setCellValueFactory( ( param ) ->
              new ReadOnlyObjectWrapper<>( param.getValue().getValue().getSym() )
      );

      //Creating a column7
      TreeTableColumn< FAAnionRow, NamedBoolean > column7 = new TreeTableColumn<>( "CO2Loss" );
      column7.setPrefWidth( 100 );

      column7.setCellFactory( SelectiveCheckBoxTreeTableCell.forTreeTableCo2LossColumn( treeTableView ) );
      column7.setCellValueFactory( ( param ) ->
              new ReadOnlyObjectWrapper<>( param.getValue().getValue().getCo2MassProperty() )
      );

      treeTableView.setEditable( true );
      treeTableView.setRoot( root );

      treeTableView.setShowRoot( false );
      treeTableView.getColumns().setAll( column1, column2, column3, column4, column5, column6, column7 );

      collisionEnergyTableView = new TableView();
      //		collisionEnergyTableView.setEditable( true );

      TableViewUtil.addContextMenu( collisionEnergyTableView );

      TableColumn tableColumn1 = new TableColumn( "CE" );
      tableColumn1.setPrefWidth( 150 );
      tableColumn1.setCellValueFactory( new Callback< TableColumn.CellDataFeatures< String[], String >, ObservableValue< String > >()
      {
         @Override
         public ObservableValue< String > call( TableColumn.CellDataFeatures< String[], String > p )
         {
            return new SimpleStringProperty( ( p.getValue()[ 0 ] ) );
         }
      } );
      TableColumn tableColumn2 = new TableColumn( "INT" );
      tableColumn2.setPrefWidth( 150 );
      tableColumn2.setCellValueFactory( new Callback< TableColumn.CellDataFeatures< String[], String >, ObservableValue< String > >()
      {
         @Override
         public ObservableValue< String > call( TableColumn.CellDataFeatures< String[], String > p )
         {
            return new SimpleStringProperty( ( p.getValue()[ 1 ] ) );
         }
      } );
      TableColumn tableColumn3 = new TableColumn( "CF" );
      tableColumn3.setPrefWidth( 150 );
      tableColumn3.setCellValueFactory( new Callback< TableColumn.CellDataFeatures< String[], String >, ObservableValue< String > >()
      {
         @Override
         public ObservableValue< String > call( TableColumn.CellDataFeatures< String[], String > p )
         {
            return new SimpleStringProperty( ( p.getValue()[ 2 ] ) );
         }
      } );
      TableColumn tableColumn4 = new TableColumn( "CO2 INT" );
      tableColumn4.setPrefWidth( 150 );
      tableColumn4.setCellValueFactory( new Callback< TableColumn.CellDataFeatures< String[], String >, ObservableValue< String > >()
      {
         @Override
         public ObservableValue< String > call( TableColumn.CellDataFeatures< String[], String > p )
         {
            return new SimpleStringProperty( ( p.getValue()[ 3 ] ) );
         }
      } );

      collisionEnergyTableView.getColumns().addAll( tableColumn1, tableColumn2, tableColumn3, tableColumn4 );

      //		treeTableView.getSelectionModel().selectedItemProperty().addListener( ( observable, oldValue, newValue ) -> {
      //			collisionEnergyTableView.getItems().clear();
      //
      //			TreeItem< FAAnionRow > selectedItem = treeTableView.getSelectionModel().getSelectedItem();
      //
      //			FAAnionRow faAnionRow = treeTableView.getSelectionModel().getSelectedItem().getValue();
      //
      //			TreeItem< FAAnionRow > clazzItem = selectedItem.getParent();
      //			String clazz = clazzItem.getValue().getName();
      //
      //			if(!clazzItem.getValue().getName().equals( "" ))
      //			{
      //				Fragment mz = classCollection.get( clazz ).get( faAnionRow.getMass() );
      //
      //				Fragment co2mz = null;
      //
      //				if(faAnionRow.getCo2Mass() != null && faAnionRow.getCo2Mass().getMass() != 0d)
      //				{
      //					co2mz = classCollection.get(clazz).get( faAnionRow.getCo2Mass().getMass() );
      //				}
      //
      //				for(Float ce : mz.keys())
      //				{
      //					String[] row;
      //
      //					if(co2mz != null && co2mz.contains( ce ))
      //						row = new String[] { ce.toString(), mz.get( ce ).toString(), co2mz.get(ce).toString(), co2mz.get(ce) / mz.get( ce ) + "",
      //								mz.get( ce ) / co2mz.get(ce) + "" };
      //					else
      //						row = new String[] { ce.toString(), mz.get( ce ).toString(), "", "",
      //								"" };
      //
      //					collisionEnergyTableView.getItems().add( row );
      //				}
      //			}
      //
      //		} );

      final FilteredList< FilteredTreeItem< FAAnionRow > > filteredData = new FilteredList< FilteredTreeItem< FAAnionRow > >( treeItems, s -> true );

      filteredData.addListener( new ListChangeListener< TreeItem< FAAnionRow > >()
      {
         @Override public void onChanged( Change< ? extends TreeItem< FAAnionRow > > c )
         {
            root.getChildren().setAll( filteredData );
         }
      } );

      TextField searchText = TextFields.createClearableTextField();

      searchText.textProperty().addListener( obs -> {
         String filter = searchText.getText();
         if ( filter == null || filter.length() == 0 )
         {

            filteredData.setPredicate( s -> true );
            filteredData.forEach( c -> c.setPredicate( s -> true ) );
         }
         else
         {
            Predicate< TreeItem< FAAnionRow > > predicate = new Predicate< TreeItem< FAAnionRow > >()
            {
               @Override public boolean test( TreeItem< FAAnionRow > faAnionRowTreeItem )
               {
                  return faAnionRowTreeItem.getChildren().stream().anyMatch( t -> t.getValue().getMassString().contains( filter ) ||
                          t.getValue().getCo2MassString().contains( filter ) );
               }
            };
            filteredData.setPredicate( predicate );
            filteredData.forEach( c -> c.setPredicate( predicate ) );
         }

         root.getChildren().forEach( a -> a.getChildren().sort( Comparator.comparing( t -> Integer.parseInt( t.getValue().getName() ) ) ) );
      } );

      Button loadButton = new Button( "Load" );
      loadButton.setOnAction( new EventHandler< ActionEvent >()
      {
         @Override public void handle( ActionEvent event )
         {
            MasterDatabase db = new MasterDatabase();
            db.connect();

            TreeMap< String, TreeMap< Integer, FAAnion > > masterDBSet = db.getMasterDB();

            if ( null == masterDBSet )
               return;

            for ( String clazz : masterDBSet.keySet() )
            {
               if ( !masterDB.containsKey( clazz ) )
                  masterDB.put( clazz, new TreeMap<>() );
               else
                  continue;

               if ( !classMap.containsKey( clazz ) )
               {
                  classMap.put( clazz, new FilteredTreeItem<>( new FAAnionRow( clazz ) ) );
                  treeItems.add( classMap.get( clazz ) );
               }

               final FilteredTreeItem< FAAnionRow > clazzNode = classMap.get( clazz );
               clazzNode.setExpanded( true );

               for ( Integer index : masterDBSet.get( clazz ).keySet() )
               {
                  FAAnion faAnion = masterDBSet.get( clazz ).get( index );
                  if ( !masterDB.get( clazz ).containsKey( index ) )
                  {
                     final FAAnionRow faAnionRow = new FAAnionRow( index, faAnion );
                     final FilteredTreeItem< FAAnionRow > faAnionRowNode = new FilteredTreeItem< FAAnionRow >( faAnionRow );

                     masterDB.get( clazz ).put( index, faAnionRowNode );
                     clazzNode.add( faAnionRowNode );
                     clazzNode.getChildren().sort( Comparator.comparing( t -> Integer.parseInt( t.getValue().getName() ) ) );
                  }

                  final TreeItem< FAAnionRow > faAnionRowNode = masterDB.get( clazz ).get( index );

                  for ( FAAnionRow row : db.getFAAnionRows( clazz, index ) )
                  {
                     final TreeItem< FAAnionRow > mzNode = new TreeItem<>( row );
                     faAnionRowNode.getChildren().add( mzNode );

                     row.newTitle( row.getTitle() );
                     NamedBoolean nb = row.getTitle();

                     nb.addListener( ( observable, oldValue, newValue ) -> {

                        collisionEnergyTableView.getItems().clear();

                        final String seriesName = clazz + ":" + mzNode.getValue().getName() + ":SN1-" + row.getSn1().getName() + ",SN2-" + row.getSn2().getName();

                        if ( newValue )
                        {
                           XYChart.Series seriesFit = new XYChart.Series();
                           seriesFit.setName( seriesName + ".Fitted" );

                           final MasterDatabase masterDatabase = new MasterDatabase();
                           masterDatabase.connect();

                           for ( String[] detailRow : masterDatabase.getDetails( index, clazz, row.getSn1().getName(), row.getSn2().getName(), row.getSym().getName() ) )
                           {
                              collisionEnergyTableView.getItems().add( detailRow );

                              Float ce = Float.parseFloat( detailRow[ 0 ] );
                              Float intensity = Float.parseFloat( detailRow[ 1 ] );

                              XYChart.Data actNode = new XYChart.Data( ce, intensity );
                              actNode.setNode( new HoveredNode( intensity ) );
                              seriesFit.getData().add( actNode );
                           }

                           masterDatabase.close();

                           chart.getData().add( seriesFit );
                        }
                        else
                        {
                           chart.getData().removeIf( series -> series.getName().startsWith( seriesName ) );
                        }
                     } );

                     if ( !row.getCo2MassString().equals( "" ) )
                     {
                        if ( faAnionRowNode.getValue().getCo2MassString().equals( "" ) )
                        {
                           faAnionRowNode.getValue().setCo2MassString( row.getCo2MassString() );
                        }

                        row.setCo2mass();
                        row.getCo2MassProperty().addListener( new ChangeListener< Boolean >()
                        {
                           @Override public void changed( ObservableValue< ? extends Boolean > observable, Boolean oldValue, Boolean newValue )
                           {
                              final String seriesName = clazz + ":" + mzNode.getValue().getName() + "CO2Loss:SN1-" + row.getSn1().getName() + ",SN2-" + row.getSn2().getName();

                              if ( newValue )
                              {
                                 XYChart.Series seriesFit = new XYChart.Series();
                                 seriesFit.setName( seriesName + ".Fitted" );

                                 final MasterDatabase masterDatabase = new MasterDatabase();
                                 masterDatabase.connect();

                                 for ( String[] detailRow : masterDatabase.getDetails( index, clazz, row.getSn1().getName(), row.getSn2().getName(), row.getSym().getName() ) )
                                 {
                                    if ( !detailRow[ 3 ].equals( "" ) )
                                    {
                                       Float ce = Float.parseFloat( detailRow[ 0 ] );
                                       Float intensity = Float.parseFloat( detailRow[ 3 ] );

                                       XYChart.Data actNode = new XYChart.Data( ce, intensity );
                                       actNode.setNode( new HoveredNode( intensity ) );
                                       seriesFit.getData().add( actNode );
                                    }
                                 }

                                 masterDatabase.close();

                                 chart.getData().add( seriesFit );
                              }
                              else
                              {
                                 chart.getData().removeIf( series -> series.getName().startsWith( seriesName ) );
                              }
                           }
                        } );
                     }
                  }
               }
            }

            db.close();
         }
      } );

      Button storeButton = new Button( "Reset/Store" );
      storeButton.setOnAction( new EventHandler< ActionEvent >()
      {
         @Override public void handle( ActionEvent event )
         {
            startStoreDatabase( true );
         }
      } );

      BorderPane borderPane = new BorderPane();
      borderPane.setTop( new HBox( searchText, loadButton, storeButton ) );
      borderPane.setCenter( treeTableView );

      setMasterNode( borderPane );
      setDetailNode( collisionEnergyTableView );

      setDetailSide( Side.BOTTOM );
      setShowDetailNode( true );
      setDividerPosition( 0.6 );
   }

   private void processAllFAanions( LineChart< Number, Number > chart, TreeMap< Integer, Fragment[] > faIndexMap, TreeMap< Integer, Fragment[] > co2LossFaIndexMap )
   {
      final FilteredTreeItem< FAAnionRow > clazzNode = classMap.get( currentClazz );

      for ( FAAnion faAnion : mFaAnionsList )
      {
         if ( !masterDB.get( currentClazz ).containsKey( faAnion.getIndex() ) )
         {
            final FAAnionRow faAnionRow = new FAAnionRow( faAnion );
            final FilteredTreeItem< FAAnionRow > faAnionRowNode = new FilteredTreeItem< FAAnionRow >( faAnionRow );

            masterDB.get( currentClazz ).put( faAnion.getIndex(), faAnionRowNode );
            clazzNode.add( faAnionRowNode );
            clazzNode.getChildren().sort( Comparator.comparing( t -> Integer.parseInt( t.getValue().getName() ) ) );
         }

         final TreeItem< FAAnionRow > faAnionRowNode = masterDB.get( currentClazz ).get( faAnion.getIndex() );

         // Have to check if Co2Loss is attached later even though we could not find any CO2Loss in the calibration steps.

         if ( LipidClassCollection.isSym( currentClazz ) )
         {
            if ( !contain( faAnionRowNode, "1", "0", "0" ) )
               createTreeItem( currentClazz, chart, faAnion, faAnionRowNode, faIndexMap, co2LossFaIndexMap, 0 );
            else
               checkCo2Loss( faAnionRowNode, chart, faAnion, co2LossFaIndexMap, "1", "0", "0", 0 );
         }

         if ( LipidClassCollection.isSn1( currentClazz ) )
         {
            if ( !contain( faAnionRowNode, "0", "1", "0" ) )
               createTreeItem( currentClazz, chart, faAnion, faAnionRowNode, faIndexMap, co2LossFaIndexMap, 1 );
            else
               checkCo2Loss( faAnionRowNode, chart, faAnion, co2LossFaIndexMap, "0", "1", "0", 1 );
         }

         if ( LipidClassCollection.isSn2( currentClazz ) )
         {

            if ( !contain( faAnionRowNode, "0", "0", "1" ) )
               createTreeItem( currentClazz, chart, faAnion, faAnionRowNode, faIndexMap, co2LossFaIndexMap, 2 );
            else
               checkCo2Loss( faAnionRowNode, chart, faAnion, co2LossFaIndexMap, "0", "0", "1", 2 );
         }

         if ( LipidClassCollection.isSym( currentClazz ) )
         {
            Fragment[] sn1sn2 = new Fragment[] { faIndexMap.get( faAnion.getIndex() )[ 1 ],
                    faIndexMap.get( faAnion.getIndex() )[ 2 ] };

            Fragment[] sn1sn2Co2Loss = co2LossFaIndexMap.containsKey( faAnion.getIndex() ) ?
                    new Fragment[] { co2LossFaIndexMap.get( faAnion.getIndex() )[ 1 ],
                            co2LossFaIndexMap.get( faAnion.getIndex() )[ 2 ] } : null;

            // sn1 = 0.5, sn2 = 0.5
            if ( !contain( faAnionRowNode, "0", "0.5", "0.5" ) )
            {
               final float sn1 = 0.5f, sn2 = 0.5f;
               addOptionalTreeItem( currentClazz, chart, sn1, sn2, sn1sn2, sn1sn2Co2Loss, faAnion, faAnionRowNode );
            }

            // sn1 = 0.75, sn2 =0.25
            if ( !contain( faAnionRowNode, "0", "0.75", "0.25" ) )
            {
               final float sn1 = 0.75f, sn2 = 0.25f;
               addOptionalTreeItem( currentClazz, chart, sn1, sn2, sn1sn2, sn1sn2Co2Loss, faAnion, faAnionRowNode );
            }

            // sn1 = 0.25, sn2 = 0.75
            if ( !contain( faAnionRowNode, "0", "0.25", "0.75" ) )
            {
               final float sn1 = 0.25f, sn2 = 0.75f;
               addOptionalTreeItem( currentClazz, chart, sn1, sn2, sn1sn2, sn1sn2Co2Loss, faAnion, faAnionRowNode );
            }
         }
      }
   }

   private void checkCo2Loss( TreeItem< FAAnionRow > faAnionRowNode, LineChart< Number, Number > chart,
           FAAnion faAnion, TreeMap< Integer, Fragment[] > co2LossFaIndexMap, String sym, String sn1, String sn2, int dbType )
   {
      Optional< TreeItem< FAAnionRow > > row = faAnionRowNode.getChildren().filtered( c ->
              c.getValue().getSn1().getName().equals( sn1 ) &&
                      c.getValue().getSn2().getName().equals( sn2 ) &&
                      c.getValue().getSym().getName().equals( sym ) ).stream().findAny();

      if ( row.isPresent() )
      {
         TreeItem< FAAnionRow > faAnionRow = row.get();
         final Fragment co2loss = co2LossFaIndexMap.containsKey( faAnion.getIndex() ) ?
                 co2LossFaIndexMap.get( faAnion.getIndex() )[ dbType ] : null;

         if ( co2loss != null && faAnionRow.getValue().getCo2Mass() == null )
         {
            co2LossFragmentMap.put( faAnionRow.getValue(), co2loss );

            faAnionRow.getValue().setCo2MassString( faAnion.getCo2mass().getMass().toString() );

            faAnionRow.getValue().setCo2mass( faAnion.getCo2mass() );
            faAnionRow.getValue().getCo2MassProperty().addListener( createCO2LossChangeListener( chart, faAnionRow, co2loss, dbType ) );
         }
      }
   }

   private ChangeListener< Boolean > createCO2LossChangeListener( LineChart< Number, Number > chart,
           TreeItem< FAAnionRow > faAnionRow, Fragment co2loss, int dbType )
   {
      return ( observable, oldValue, newValue ) ->
      {
         final String seriesName = faAnionRow.getValue().getName() + ":CO2Loss-" + Calib.values()[ dbType ];

         if ( newValue )
         {
            XYChart.Series seriesFit = new XYChart.Series();
            seriesFit.setName( seriesName + ".Fitted" );

            for ( Float ce : co2loss.keys() )
            {
               XYChart.Data actNode = new XYChart.Data( ce, co2loss.get( ce ) );
               actNode.setNode( new HoveredNode( co2loss.get( ce ) ) );
               seriesFit.getData().add( actNode );
            }

            chart.getData().add( seriesFit );
         }
         else
         {
            chart.getData().removeIf( series -> series.getName().startsWith( seriesName ) );
         }
      };
   }

   private void addOptionalTreeItem( String clazz, LineChart< Number, Number > chart,
           float sn1, float sn2, Fragment[] fragments, Fragment[] co2LossFragments, FAAnion faAnion, TreeItem< FAAnionRow > faAnionRowNode )
   {
      final FAAnionRow row = new FAAnionRow( faAnion.getIndex(), faAnion );
      row.getSn1().setName( sn1 + "" );
      row.getSn2().setName( sn2 + "" );

      final TreeItem< FAAnionRow > mzNode = new TreeItem<>( row );
      faAnionRowNode.getChildren().add( mzNode );

      row.newTitle( row.getTitle() );
      final NamedBoolean nb = row.getTitle();

      nb.addListener( ( observable, oldValue, newValue ) -> {

         final String seriesName = currentClazz + ":" + mzNode.getValue().getName() + ":SN1=" + sn1 + ",SN2=" + sn2;

         collisionEnergyTableView.getItems().clear();

         if ( newValue )
         {
            final XYChart.Series seriesFit = new XYChart.Series();
            seriesFit.setName( seriesName + ".Fitted" );

            //				final SplineComposite func = computeLinearRegression( fragments, sn1 );
            //
            //				final SplineComposite funcCo2Loss = null != co2LossFragments ?
            //						computeLinearRegression( co2LossFragments, sn1 ) : null;

            final PolynomialSplineFunction func = computeSn2Fragment( fragments, sn2 );

            final PolynomialSplineFunction funcCo2Loss = null != co2LossFragments ?
                    computeSn2Fragment( co2LossFragments, sn2 ) : null;

            int idx = getCEIndex( fragments );
            Fragment fragment = fragments[ idx ];

            for ( Float ce : getCE( fragments ) )
            {
               float intensity = ( float ) func.value( ce );
               float cf = getCf( ce, intensity, sn1 );
               final String[] row1 =
                       new String[] { ce.toString(), intensity + "",
                               cf + "",
                               "" };

               if ( null != funcCo2Loss )
               {
                  row1[ 4 ] = funcCo2Loss.value( ce ) + "";
               }

               collisionEnergyTableView.getItems().add( row1 );

               XYChart.Data actNode = new XYChart.Data( ce, func.value( ce ) );
               actNode.setNode( new HoveredNode( func.value( ce ) ) );
               seriesFit.getData().add( actNode );
            }

            chart.getData().add( seriesFit );
         }
         else
         {
            chart.getData().removeIf( series -> series.getName().startsWith( seriesName ) );
         }
      } );

      if ( co2LossFragments != null )
      {
         row.setCo2mass( faAnion.getCo2mass() );
         row.getCo2MassProperty().addListener( new ChangeListener< Boolean >()
         {
            @Override public void changed( ObservableValue< ? extends Boolean > observable, Boolean oldValue, Boolean newValue )
            {
               final String seriesName = clazz + ":" + mzNode.getValue().getName() + ":CO2Loss-" + "SN1=" + sn1 + ",SN2=" + sn2;

               if ( newValue )
               {
                  final XYChart.Series seriesFit = new XYChart.Series();
                  seriesFit.setName( seriesName + ".Fitted" );

                  //						final SplineComposite funcCo2Loss = computeLinearRegression( co2LossFragments, sn1 );
                  final PolynomialSplineFunction funcCo2Loss = computeSn2Fragment( co2LossFragments, sn2 );

                  for ( Float ce : getCE( co2LossFragments ) )
                  {
                     XYChart.Data actNode = new XYChart.Data( ce, funcCo2Loss.value( ce ) );
                     actNode.setNode( new HoveredNode( funcCo2Loss.value( ce ) ) );
                     seriesFit.getData().add( actNode );
                  }

                  chart.getData().add( seriesFit );
               }
               else
               {
                  chart.getData().removeIf( series -> series.getName().startsWith( seriesName ) );
               }
            }
         } );
      }
   }

   private void createTreeItem( String clazz, LineChart< Number, Number > chart, FAAnion faAnion,
           TreeItem< FAAnionRow > faAnionRowNode, TreeMap< Integer, Fragment[] > faIndexMap, TreeMap< Integer, Fragment[] > co2LossFaIndexMap, int dbType )
   {
      final Fragment mz = faIndexMap.get( faAnion.getIndex() )[ dbType ];

      final FAAnionRow row = new FAAnionRow( faAnion.getIndex(), faAnion );
      final TreeItem< FAAnionRow > mzNode = new TreeItem<>( row );
      faAnionRowNode.getChildren().add( mzNode );

      row.newTitle( row.getTitle() );
      NamedBoolean nb = row.getTitle();

      fragmentMap.put( row, mz );

      switch ( dbType )
      {
         case 2:
            row.getSn2().setValid( true );
            break;
         case 1:
            row.getSn1().setValid( true );
            break;
         case 0:
            row.getSym().setValid( true );
            break;
      }

      final Fragment co2loss = co2LossFaIndexMap.containsKey( faAnion.getIndex() ) ? co2LossFaIndexMap.get( faAnion.getIndex() )[ dbType ] : null;

      if ( null != co2loss )
         co2LossFragmentMap.put( row, co2loss );

      nb.addListener( ( observable, oldValue, newValue ) -> {

         collisionEnergyTableView.getItems().clear();

         final String seriesName = clazz + ":" + mzNode.getValue().getName() + ":SN1=" + row.getSn1().getName() + ",SN2=" + row.getSn2().getName();

         if ( newValue )
         {
            XYChart.Series seriesFit = new XYChart.Series();
            seriesFit.setName( seriesName + ".Fitted" );

            for ( Float ce : mz.keys() )
            {
               final String[] row1 = new String[] { ce.toString(), mz.get( ce ).toString(),
                       mz.getCF( ce ).toString(),
                       "" };

               if ( null != co2loss )
               {
                  row1[ 4 ] = co2loss.get( ce ) + "";
               }

               collisionEnergyTableView.getItems().add( row1 );

               XYChart.Data actNode = new XYChart.Data( ce, mz.get( ce ) );
               actNode.setNode( new HoveredNode( mz.get( ce ) ) );
               seriesFit.getData().add( actNode );
            }

            chart.getData().add( seriesFit );
         }
         else
         {
            chart.getData().removeIf( series -> series.getName().startsWith( seriesName ) );
         }
      } );

      if ( null != co2loss )
      {
         if ( faAnionRowNode.getValue().getCo2Mass() == null )
         {
            faAnionRowNode.getValue().setCo2MassString( faAnion.getCo2mass().getMass().toString() );
         }

         row.setCo2mass( faAnion.getCo2mass() );
         row.getCo2MassProperty().addListener( createCO2LossChangeListener( chart, faAnionRowNode, co2loss, dbType ) );
      }
   }

   private boolean contain( TreeItem< FAAnionRow > faAnionRowNode, String sym, String sn1, String sn2 )
   {
      return faAnionRowNode.getChildren().stream().anyMatch( c -> c.getValue().getSn1().getName().equals( sn1 ) &&
              c.getValue().getSn2().getName().equals( sn2 ) &&
              c.getValue().getSym().getName().equals( sym )
      );
   }

   private void startStoreDatabase( boolean bNew )
   {
      System.err.println( "Database store process starts" );

      if ( bNew )
         System.err.println( "Database is reset." );

      new Thread( new Runnable()
      {
         @Override public void run()
         {
            MasterDatabase db = new MasterDatabase();
            db.connect();
            db.initTables( bNew );

            for ( String clazz : masterDB.keySet() )
            {
               for ( Integer index : masterDB.get( clazz ).keySet() )
               {
                  System.err.println( clazz + "-FAAnion " + index + "/" + masterDB.get( clazz ).keySet().size() );
                  TreeItem< FAAnionRow > faAnionRowNode = masterDB.get( clazz ).get( index );

                  Fragment[] fragments = new Fragment[ 2 ];
                  Fragment[] co2LossFragments = new Fragment[ 2 ];

                  for ( TreeItem< FAAnionRow > faAnionRowItem : faAnionRowNode.getChildren() )
                  {
                     FAAnionRow row = faAnionRowItem.getValue();

                     if ( row.isMaster() )
                        continue;

                     if ( row.getSn2().getName().equals( "1" ) ||
                             row.getSn1().getName().equals( "1" ) ||
                             row.getSym().getName().equals( "1" ) )
                     {
                        Fragment mz = fragmentMap.get( row );

                        if ( row.getSn1().getName().equals( "1" ) )
                           fragments[ 0 ] = mz;
                        else if ( row.getSn2().getName().equals( "1" ) )
                           fragments[ 1 ] = mz;

                        Fragment co2mz = null;

                        if ( row.getCo2Mass() != null && row.getCo2Mass().getMass() != 0d )
                        {
                           co2mz = co2LossFragmentMap.get( row );
                           if ( row.getSn1().getName().equals( "1" ) )
                              co2LossFragments[ 0 ] = co2mz;
                           else if ( row.getSn2().getName().equals( "1" ) )
                              co2LossFragments[ 1 ] = co2mz;

                        }

                        insertMasterData( db, row, clazz );

                        ArrayList< String[] > rows = new ArrayList< String[] >();

                        for ( Float ce : mz.keys() )
                        {
                           String[] detailRow;

                           if ( co2mz != null && co2mz.contains( ce ) )
                              detailRow = new String[] { ce.toString(), mz.get( ce ).toString(),
                                      mz.getCF( ce ).toString(),
                                      co2mz.get( ce ).toString() };
                           else
                              detailRow = new String[] { ce.toString(), mz.get( ce ).toString(),
                                      mz.getCF( ce ).toString(),
                                      "" };

                           rows.add( detailRow );
                        }

                        insertDetailData( db, row, clazz, rows );
                     }
                     else
                     {
                        float sn1 = Float.parseFloat( row.getSn1().getName() );
                        insertMasterData( db, row, clazz, sn1 );
                        insertDetailData( db, row, clazz, fragments, co2LossFragments, sn1 );
                     }
                  }
               }
            }

            db.close();
            System.err.println( "Master database saved!" );
         }
      } ).start();
   }

   private int getCEIndex( Fragment[] fragments )
   {
      final Fragment sn1 = fragments[ 0 ];

      final Fragment sn2 = fragments[ 1 ];

      if ( sn1.keys().size() > sn2.keys().size() )
         return 0;
      else
         return 1;
   }

   private void insertMasterData( MasterDatabase db, FAAnionRow row, String clazz )
   {
      if ( row.getCo2Mass() != null && row.getCo2Mass().getMass() != 0d )
      {
         // Database insert with co2loss
         db.insertMasterData( row.getName(), clazz, row.getMassString(), row.getIsomer(), row.getSn1().getName(),
                 row.getSn2().getName(), row.getSym().getName(), row.getCo2MassString() );
      }
      else
      {
         // Database insert without co2loss
         db.insertMasterData( row.getName(), clazz, row.getMassString(), row.getIsomer(), row.getSn1().getName(),
                 row.getSn2().getName(), row.getSym().getName() );
      }
   }

   private void insertMasterData( MasterDatabase db, FAAnionRow row, String clazz, float sn1 )
   {
      if ( row.getCo2Mass() != null && row.getCo2Mass().getMass() != 0d )
      {
         // Database insert with co2loss
         db.insertMasterData( row.getName(), clazz, row.getMassString(), row.getIsomer(), sn1 + "",
                 ( 1f - sn1 ) + "", "0", row.getCo2MassString() );
      }
      else
      {
         // Database insert without co2loss
         db.insertMasterData( row.getName(), clazz, row.getMassString(), row.getIsomer(), sn1 + "",
                 ( 1f - sn1 ) + "", "0" );
      }
   }

   private void insertDetailData( MasterDatabase db, FAAnionRow row, String clazz, ArrayList< String[] > dataRows )
   {
      db.insertDetailData( row.getName(), clazz, row.getSn1().getName(), row.getSn2().getName(), row.getSym().getName(), dataRows );
   }

   private void insertDetailData( MasterDatabase db, FAAnionRow row, String clazz, Fragment[] fragments, Fragment[] co2LossFragments, float sn1 )
   {
      //		final SplineComposite func = computeLinearRegression( fragments, sn1 );
      float sn2 = ( 1f - sn1 );
      final PolynomialSplineFunction func = computeSn2Fragment( fragments, sn2 );
      final ArrayList< String[] > rows = new ArrayList< String[] >();

      Fragment fragment = fragments[ 1 ];

      if ( row.getCo2MassString().equals( "" ) )
      {
         double ret;

         for ( Float ce : getCE( fragments ) )
         {
            ret = 0;

            if ( func.isValidPoint( ce ) )
               ret = func.value( ce );

            float cf = getCf( ce, ret, sn1 );

            rows.add(
                    new String[] { ce.toString(), ret + "",
                            cf + "", "" }
            );
         }
      }
      else
      {
         //			final SplineComposite funcCo2Loss = computeLinearRegression( co2LossFragments, sn1 );
         final PolynomialSplineFunction funcCo2Loss = computeSn2Fragment( co2LossFragments, sn2 );

         int idx = getCEIndex( fragments );
         double ret, co2ret;

         for ( Float ce : getCE( fragments ) )
         {
            ret = co2ret = 0;

            if ( func.isValidPoint( ce ) )
               ret = func.value( ce );

            if ( funcCo2Loss.isValidPoint( ce ) )
               co2ret = funcCo2Loss.value( ce );

            float cf = getCf( ce, ret, sn1 );

            rows.add(
                    new String[] { ce.toString(), ret + "",
                            cf + "", co2ret + "" }
            );
         }
      }

      db.insertDetailData( row.getName(), clazz, row.getSn1().getName(), row.getSn2().getName(), row.getSym().getName(), rows );
   }

   public void setCalibrationTabPane( TabPane calibrationTabPane )
   {
      mCalibrationTabPane = calibrationTabPane;
   }

   public void setFaAnionsList( ObservableList< FAAnion > faAnionsList )
   {
      if ( null == mFaAnionsList )
         this.mFaAnionsList = faAnionsList;
   }

   private boolean containSn1Sn2( TreeItem< FAAnionRow > faAnionRowNode )
   {
      return faAnionRowNode.getChildren().stream().filter( c -> c.getValue().getSn1().getName().equals( "1" ) ||
              c.getValue().getSn2().getName().equals( "1" ) ||
              c.getValue().getSn2().getName().equals( "0.5" ) ).count() == 2;
   }

   protected void uncheckNode( TreeItem< FAAnionRow > item )
   {
      if ( item.getValue().getTitle() instanceof CheckBoxNamedBoolean )
      {
         item.getValue().getTitle().set( false );
         if ( item.getValue().getCo2MassProperty() != null )
            item.getValue().getCo2MassProperty().set( false );
      }
      else
      {
         item.getChildren().forEach( this::uncheckNode );
      }
   }

   PolynomialSplineFunction computeSn2Fragment( Fragment[] fragments, float sn2 )
   {
      final Fragment snOne = fragments[ 0 ], snTwo = fragments[ 1 ];

      // snOne -> lower bound [ 1, 2, 3, 4, 5, 4, 3, 2, 1 ]
      // snTwo -> upper bound [ 2, 4, 6, 8, 10, 8, 6, 4, 2 ]
      // snOut -> snOne + ( snTwo - snOne ) * sn1

      Float[] ce = getCE( fragments );

      double[] x = new double[ ce.length ];
      double[] y = new double[ ce.length ];

      for ( int i = 0; i < ce.length; i++ )
      {
         x[ i ] = ce[ i ];

         if ( snOne.contains( ce[ i ] ) && snTwo.contains( ce[ i ] ) )
         {
            y[ i ] = snOne.get( ce[ i ] ) + ( snTwo.get( ce[ i ] ) - snOne.get( ce[ i ] ) ) * sn2;
         }
         //			else
         //			{
         //				System.err.println( "CE " + ce[i] + " is not available in the fragments.");
         //			}
      }

      return new LinearInterpolator().interpolate( x, y );
   }

   private Float[] getCE( Fragment[] fragments )
   {
      final Fragment sn1 = fragments[ 0 ];

      final Fragment sn2 = fragments[ 1 ];

      if ( sn1.keys().size() > sn2.keys().size() )
         return sn1.keys().toArray( new Float[] {} );
      else
         return sn2.keys().toArray( new Float[] {} );
      //		final Fragment sn1 = fragments[0];
      //
      //		final Fragment sn2 = fragments[1];
      //
      //		Set<Float> set = sn1.keys();
      //		set.retainAll( sn2.keys() );
      //
      //		return set.toArray( new Float[] {} );
   }

   private float getCf( Float ce, double value, float sn1 )
   {
      if ( value == 0d )
         return 1f;
      return normBasisMap.get( sn1 ).get( ce ) / ( float ) value;
   }

   private Fragment[] getSn1Sn2( HashMap< FAAnionRow, Fragment > fragmentMap, TreeItem< FAAnionRow > faAnionRow )
   {
      Optional< TreeItem< FAAnionRow > > sn1Item = faAnionRow.getChildren().stream().filter( c -> c.getValue().getSn1().getName().equals( "1" ) ).findAny();
      Optional< TreeItem< FAAnionRow > > sn2Item = faAnionRow.getChildren().stream().filter( c -> c.getValue().getSn2().getName().equals( "1" ) ).findAny();

      final Fragment sn1Fragment = fragmentMap.get( sn1Item.get().getValue() );
      final Fragment sn2Fragment = fragmentMap.get( sn2Item.get().getValue() );

      return new Fragment[] { sn1Fragment, sn2Fragment };
   }

   public static FAAnion getFaAnion( ObservableList< FAAnion > mFaAnionsList, double mass, float isomer, int carbon, int db )
   {
      FAAnion found = null;

      Optional< FAAnion > faAnion = mFaAnionsList.stream().filter( c ->
              c.getMass().equals( mass ) && c.getFAIsomer().equals( isomer ) &&
                      c.getFACarbon().equals( carbon ) &&
                      c.getFADoubleBonds().equals( db ) ).findFirst();

      if ( faAnion.isPresent() )
         found = faAnion.get();
      else
         System.err.println( mass + ":carbon - " + carbon + ":db - " + db + " => Not found in FAAnion List!" );

      return found;
   }
}
