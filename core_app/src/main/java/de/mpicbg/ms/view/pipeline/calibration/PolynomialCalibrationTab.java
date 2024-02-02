package de.mpicbg.ms.view.pipeline.calibration;

import de.mpicbg.ms.model.Fragment;
import de.mpicbg.ms.model.data.CO2;
import de.mpicbg.ms.model.data.FAAnion;
import de.mpicbg.ms.model.data.FAAnionRow;
import de.mpicbg.ms.model.data.Pos;
import de.mpicbg.ms.model.event.ProcessEvent;
import de.mpicbg.ms.model.fitter.NewtonPolynomialFunction;
import de.mpicbg.ms.view.pane.MasterXmlPane;
import de.mpicbg.ms.view.treecell.CheckBoxNamedBoolean;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.cell.ComboBoxTreeTableCell;
import javafx.scene.layout.VBox;
import org.apache.commons.math3.util.Precision;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Author: HongKee Moon (moon@mpi-cbg.de), Scientific Computing Facility
 * Organization: MPI-CBG Dresden
 * Date: September 2022
 */
@SuppressWarnings( "Duplicates" )
public class PolynomialCalibrationTab extends CalibrationTab
{
   TreeMap<String, TreeMap<String, TreeMap< Double, Fragment > > > fragmentCollection;

   TreeMap<String, TreeMap<String, TreeMap< String, Fragment > > > calibrateFragmentCollection;

   TreeMap<String, TreeSet< FAAnion > > selectedFAanionMap;

   TreeMap<String, TreeMap< String, Fragment > > correctionFactor;

   TreeMap<String, TreeMap<String, TreeMap< String, Fragment > > > correctedCollection;

   final protected MasterXmlPane masterXmlPane;

   public PolynomialCalibrationTab( ObservableList< FAAnion > faAnionsList,
           LineChart< Number, Number > chart,
           LineChart< Number, Number > correctionFactorChart,
           LineChart< Number, Number > extraChart,
           MasterXmlPane masterXmlPane, ButtonBase[] menuButtons )
   {
      super( chart, correctionFactorChart, extraChart, menuButtons );
      setText( "Polynomial Cal." );

      mFaAnionsList = faAnionsList;
      selectedFAanionMap = new TreeMap<>();

      this.masterXmlPane = masterXmlPane;

      // Creating isomer column
      TreeTableColumn<FAAnionRow, String> column2 = new TreeTableColumn<>("Isomer");
      column2.setPrefWidth(60);

      //Defining cell content
      column2.setCellValueFactory( (param) ->
              new ReadOnlyStringWrapper( param.getValue().getValue().getIsomer() + "" ) );


      // Getting the initial isomer information from mFaAnionsList;
      TreeSet<Float> isomers = new TreeSet<>();
      mFaAnionsList.stream().forEach( c -> isomers.add( c.getFAIsomer() ) );

      ObservableList<String> isomerValues = FXCollections.observableArrayList();
      isomers.forEach( c -> isomerValues.add( c + "" ) );

      column2.setCellFactory( ComboBoxTreeTableCell.forTreeTableColumn( isomerValues ) );

      column2.setOnEditCommit( event -> {
         final FAAnionRow item = event.getRowValue().getValue();

         Float isomer = Float.parseFloat( event.getNewValue() );

         {
            item.setIsomer( isomer );

            String group = event.getRowValue().getParent().getValue().getName();
            //            System.out.println(selectedFAanionMap.get(group));
            for(FAAnion faAnion : selectedFAanionMap.get(group)) {
               if(item.getName().equals( faAnion.getName() )) {
                  faAnion.setFAIsomer( isomer );
               }
            }
         }

         // Call store the global lipid.txt
      } );

      column2.setEditable( true );

      treeTableView.getColumns().add( 2, column2 );

      treeTableView.getColumns().get( 3 ).setEditable( false );
      treeTableView.getColumns().get( 4 ).setEditable( false );

      makeFAAnionTree();
   }

   @Override protected Node getDetailNode()
   {
      Button resetButton = new Button("Reset");
      resetButton.setOnAction( new EventHandler< ActionEvent >()
      {
         @Override public void handle( ActionEvent event )
         {
            Event.fireEvent( getTabPane(), new ProcessEvent( ProcessEvent.MZ_CALIBRATION,
                    fragmentCollection, mFaAnionsList ) );
         }
      } );

      Button newtButton = new Button("Compute correction factors.");
      newtButton.setOnAction( new EventHandler< ActionEvent >()
      {
         @Override public void handle( ActionEvent event )
         {
            if ( calibrateFragmentCollection != null )
            {
               mergeMakeTree( calibrateFragmentCollection );
            }
         }
      } );

      TitledPane preparationPane = new TitledPane( "Preparation", new VBox( 10, resetButton ) );
      preparationPane.setCollapsible( false );

      TitledPane correctionPane = new TitledPane( "Correction", new VBox( 10, newtButton ) );
      correctionPane.setCollapsible( false );

      //      TitledPane validationPane = new TitledPane( "Validation", new VBox( 10, interpolateBox, interpolateHBox ) );

      return new VBox( 10, preparationPane, correctionPane  );
   }

   @Override
   protected void removeCurrentTreeItem()
   {
      // remove
      TreeItem<FAAnionRow> row = treeTableView.getSelectionModel().getSelectedItem();
      if(!row.getValue().getMassString().equals( "" ))
      {
         final TreeItem<FAAnionRow> groupItem = row.getParent();
         final TreeItem<FAAnionRow> clazzItem = groupItem.getParent();

         final String clazz = clazzItem.getValue().getName();
         final String group = groupItem.getValue().getName();

         calibrateFragmentCollection.get(clazz).get(group).remove( row.getValue().getMass() );

         if(null != correctedCollection &&
                 correctedCollection.containsKey( clazz ) &&
                 correctedCollection.get( clazz ).containsKey( group ))
            correctedCollection.get(clazz).get(group).remove( row.getValue().getMass() );

         if( selectedFAanionMap.containsKey( group ) )
            selectedFAanionMap.get( group ).removeIf( c -> c.getMass().equals( row.getValue().getMass() ) );

         if( faanionHashMap.containsKey( group ) )
            faanionHashMap.get(group).remove( row );

         groupItem.getChildren().remove( row );
         if(groupItem.getChildren().size() == 0)
         {
            clazzItem.getChildren().remove( groupItem );
            faanionHashMap.remove( group );
         }
      }
   }

   void makeFAAnionTree() {
      uncheckNode( root );

      root.getChildren().forEach( c -> c.getChildren().clear() );
      root.getChildren().clear();
      root.setExpanded( true );

      if(!selectedFAanionMap.isEmpty())
         selectedFAanionMap.values().forEach( TreeSet::clear );

      selectedFAanionMap.clear();

      faanionHashMap.clear();

      calibrateFragmentCollection = new TreeMap< String, TreeMap< String, TreeMap< String, Fragment > > >();

      String[] classes = new String[] {"PA", "PC", "PCO", "PCO-FANL", "PCO-M-60", "PCO-PR", "PE", "PEO", "PG", "PI", "PS"};

      for(final String clazz : classes)
      {
         calibrateFragmentCollection.put( clazz, new TreeMap<>(  ) );

         //System.out.println(clazz);
         final TreeItem<FAAnionRow> clazzNode = new TreeItem<>(new FAAnionRow( clazz ));
         clazzNode.setExpanded( true );

         TreeSet<FAAnion> candidates = new TreeSet< FAAnion >(  );

         for(FAAnion faAnion : mFaAnionsList) {
            candidates.add( faAnion );

            String group = faAnion.getKey();

            calibrateFragmentCollection.get( clazz ).put( group, new TreeMap<>(  ) );

            final TreeItem<FAAnionRow> groupNode = new TreeItem<>(new FAAnionRow( group, clazz ));
            groupNode.setExpanded( true );

            // Consider SN1, SN2, SYM here
            for(Pos pos : Pos.values()) {
               if((clazz.startsWith( "PCO" ) || clazz.equals( "PEO" )) && !pos.equals( Pos.SN2 )) continue;

               Fragment fragment = new Fragment( faAnion.getMass() );

               fragment.setCarbon( faAnion.getFACarbon() );
               fragment.setDoubleBond( faAnion.getFADoubleBonds() );
               fragment.setIsomer( faAnion.getFAIsomer() );
               fragment.setPosition( pos );

               NewtonPolynomialFunction newt = getNewtonFunction( clazz, pos,false );

               double[] nce = newt.getXvals();
               double[] point = new double[] { 10,  fragment.getCarbon(), fragment.getDoubleBond(), fragment.getIsomer() };

               for(double ce : nce)
               {
                  point[0] = ce;
                  float a = (float) Math.pow( 2, newt.value( point ) );
                  // Limit the precision in order to avoid infinity value in CF
                  if(a > 1e-35)
                     fragment.put( (float) ce, a );
                  else fragment.put( (float) ce, 0f );
               }

               calibrateFragmentCollection.get( clazz ).get( group ).put( faAnion.getMass() + "-" + pos, fragment );

               FAAnionRow row = new FAAnionRow( faAnion.getMass() + "-" + pos, faAnion );

               final TreeItem<FAAnionRow> mzNode = new TreeItem<>(row);

               if( !faanionHashMap.containsKey( group ) )
                  faanionHashMap.put( group, new ArrayList<>() );
               faanionHashMap.get(group).add( row );

               // Consider CO2 loss item
               if( faAnion.getFADoubleBonds() > 2 && !clazz.startsWith( "PCO-" ) ){
                  double co2 = Precision.round( faAnion.getMass() - Precision.round( 43.99, 2 ), 2 );

                  Double mz = co2;
                  Fragment co2fragment = new Fragment( fragment );
                  co2fragment.setMz( mz );
                  co2fragment.setCo2loss( true );

                  newt = getNewtonFunction( clazz, pos,true );

                  nce = newt.getXvals();
                  point = new double[] { 10,  fragment.getCarbon(), fragment.getDoubleBond(), fragment.getIsomer() };

                  for(double ce : nce)
                  {
                     point[0] = ce;
                     float a;
                     if(ce > 50)
                        a = 0f;
                     else
                        a = (float) Math.pow( 2, newt.value( point ) );

                     co2fragment.put( (float) ce, a );
                  }

                  String co2name = co2 + "-" + pos;

                  mzNode.getValue().setCo2name( co2name );

                  calibrateFragmentCollection.get( clazz ).get( group ).put( co2name, co2fragment );

                  mzNode.getValue().setCo2mass( new CO2(co2) );

                  mzNode.getValue().getCo2MassProperty().addListener( new ChangeListener< Boolean >()
                  {
                     @Override public void changed( ObservableValue< ? extends Boolean > observable, Boolean oldValue, Boolean newValue )
                     {
                        // Setup the seriesName
                        String seriesName = mzNode.getValue().getName() + ":" + pos + ":" + co2;

                        if(newValue)
                        {
                           createSeries( chart,
                                   co2fragment,
                                   seriesName + ".Poly" );
                        }
                        else
                        {
                           chart.getData().removeIf( series -> series.getName().startsWith( seriesName ) );
                        }
                     }
                  } );
               }

               switch ( pos ) {
                  case SN1:
                     mzNode.getValue().getSn1().set( true );
                     break;
                  case SN2:
                     mzNode.getValue().getSn2().set( true );
                     break;
                  case SYM:
                     mzNode.getValue().getSn1().set( true );
                     mzNode.getValue().getSn2().set( true );
                     break;
               }

               mzNode.getValue().getTitle().addListener( ( observable, oldValue, newValue ) -> {

                  // Setup the seriesName
                  String seriesName = mzNode.getValue().getName() + ":" + pos + ":" + faAnion.getMass();

                  if(newValue)
                  {
                     createSeries( chart,
                             fragment,
                             seriesName + ".Poly" );
                  }
                  else
                  {
                     chart.getData().removeIf( series -> series.getName().startsWith( seriesName ) );
                  }

               } );

               groupNode.getChildren().add(mzNode);
            }

            selectedFAanionMap.put(group, candidates);

            if(groupNode.getChildren().size() > 0)
               clazzNode.getChildren().add(groupNode);
         }

         root.getChildren().add( clazzNode );
      }

      treeTableView.refresh();
   }

   @Override protected void handleProcessEvent( ProcessEvent event )
   {
      if(event.getEventType() == ProcessEvent.MZ_CALIBRATION)
      {
         System.out.println(event.getEventType());
         event.consume();
      }
   }

   public void mergeMakeTree(TreeMap<String, TreeMap<String, TreeMap< String, Fragment > > > collection)
   {
      mergeMakeTree(collection, true);
   }

   public void mergeMakeTree(TreeMap<String, TreeMap<String, TreeMap< String, Fragment > > > collection, boolean correct)
   {
      uncheckNode(root);

      TreeMap<String, TreeMap<String, TreeMap< String, Fragment > > > calibratedCollection = new TreeMap<>(  );

      for(String clazz : collection.keySet())
      {
         calibratedCollection.put( clazz, new TreeMap<>(  ) );

         for(String group : collection.get(clazz).keySet())
         {
            calibratedCollection.get(clazz).put( group, new TreeMap<>(  ));

            TreeMap< String, Fragment > treeMap = collection.get( clazz ).get( group );

            // Create calibrated collection
            for( String item : treeMap.keySet() )
            {
               Fragment originalFragment = treeMap.get( item );
               Fragment fragment = new Fragment( originalFragment );

               calibratedCollection.get( clazz ).get( group ).put( item, fragment );

               for(Float ce : treeMap.get( item ).keys())
                  calibratedCollection.get( clazz ).get( group ).get( item ).put( ce, treeMap.get( item ).get( ce ) );
            }
         }
      }

      // correction factor
      if( correct )
      {
         correctionFactor = correct( collection, calibratedCollection );
         correctedCollection = calibratedCollection;
         System.out.println("Corrected.");
      }

      String msg;
      if( correct ) {
         msg = ".xPol";
         makeTree( root, calibratedCollection, msg, correctionFactor );
      }
   }

   public static void setupNormBasis(String clazz, Fragment[] fragments) {
      NewtonPolynomialFunction newt = new NewtonPolynomialFunction();

      double[] nce = newt.getXvals();
      double[] point = new double[] { 10, 16, 0, 0 };

      for(Fragment fragment : fragments) {
         if (clazz.equals( "PCO-FANL" ) || clazz.equals( "PCO-M-60" ) || clazz.equals( "PCO-PR" )) {
            // For the cases of PCO-sn2_FANL_poly, PCO-sn2_M-60_poly, PCO-sn2_PR_poly
            newt.loadParameters( "PCO/sn2_FA_poly.json" );
         } else {
            if(fragment.isCo2loss()) {
               point[1] = 22.0;
               point[2] = 6.0;
               point[3] = 4.0;

               switch ( fragment.getPosition() )
               {
                  case SN1:
                     newt.loadParameters( clazz + "/sn1_CO2_poly.json" );
                     break;
                  case SN2:
                     newt.loadParameters( clazz + "/sn2_CO2_poly.json" );
                     break;
                  case SYM:
                     newt.loadParameters( clazz + "/sym_CO2_poly.json" );
                     break;
               }
            } else {
               switch ( fragment.getPosition() )
               {
                  case SN1:
                     newt.loadParameters( clazz + "/sn1_FA_poly.json" );
                     break;
                  case SN2:
                     newt.loadParameters( clazz + "/sn2_FA_poly.json" );
                     break;
                  case SYM:
                     newt.loadParameters( clazz + "/sym_FA_poly.json" );
                     break;
               }
            }
         }
         for(double ce : nce)
         {
            point[0] = ce;
            float a;
            if(fragment.isCo2loss() && ce > 50)
               a = 0f;
            else
               a = (float) Math.pow( 2, newt.value( point ) );
//            System.out.println( Arrays.toString(point) + ":" + a );
            fragment.put( (float) ce, a );
         }

         if( clazz.equals( "PI" ) && fragment.isCo2loss()){
            System.out.println(clazz + ", " + fragment.getPosition());
            for(double ce : nce)
            {
               point[0] = ce;
//               float a = (float) Math.pow( 2, newt.value( point ) );
               System.out.println( Arrays.toString(point) + ":" + fragment.get((float) ce) );
//               System.out.println(ce + "," + fragment.get((float) ce));
            }
         }
      }
   }

   private NewtonPolynomialFunction getNewtonFunction(String clazz, Pos pos, boolean co2loss) {
      NewtonPolynomialFunction newt = new NewtonPolynomialFunction();

      if (clazz.equals( "PCO-FANL" ) || clazz.equals( "PCO-M-60" ) || clazz.equals( "PCO-PR" )) {
         // For the cases of PCO-sn2_FANL_poly, PCO-sn2_M-60_poly, PCO-sn2_PR_poly
         switch ( clazz ) {
            case "PCO-FANL":
               newt.loadParameters( "PCO/sn2_FANL_poly.json" );
               break;
            case "PCO-M-60":
               newt.loadParameters( "PCO/sn2_M-60_poly.json" );
               break;
            case "PCO-PR":
               newt.loadParameters( "PCO/sn2_PR_poly.json" );
               break;
            default:
               throw new IllegalArgumentException("Cannot find Newton Polynomial Parameter");
         }
      } else {
         if(co2loss) {
            switch ( pos ) {
               case SYM:
                  newt.loadParameters( clazz + "/sym_CO2_poly.json" );
                  break;
               case SN1:
                  newt.loadParameters( clazz + "/sn1_CO2_poly.json" );
                  break;
               case SN2:
                  newt.loadParameters( clazz + "/sn2_CO2_poly.json" );
                  break;
               default:
                  throw new IllegalArgumentException("Cannot find Newton Polynomial Parameter");
            }
         }
         else
         {
            switch ( pos ) {
               case SYM:
                  newt.loadParameters( clazz + "/sym_FA_poly.json" );
                  break;
               case SN1:
                  newt.loadParameters( clazz + "/sn1_FA_poly.json" );
                  break;
               case SN2:
                  newt.loadParameters( clazz + "/sn2_FA_poly.json" );
                  break;
               default:
                  throw new IllegalArgumentException("Cannot find Newton Polynomial Parameter");
            }
         }
      }

      return newt;
   }

   public TreeMap<String, TreeMap< String, Fragment > > correct(
           TreeMap<String, TreeMap<String, TreeMap< String, Fragment > > > originalCollection,
           TreeMap<String, TreeMap<String, TreeMap< String, Fragment > > > calibratedCollection )
   {
      TreeMap<String, TreeMap< String, Fragment > > correctionFactorMap = new TreeMap<>(  );

      for(String clazz : originalCollection.keySet())
      {
         // Normalization basis fragment
         final Fragment symNormBasis;
         final Fragment sn1NormBasis;
         final Fragment sn2NormBasis;

         final Fragment symCO2NormBasis;
         final Fragment sn1CO2NormBasis;
         final Fragment sn2CO2NormBasis;

         symNormBasis = new Fragment( 255.23d, Pos.SYM );
         sn1NormBasis = new Fragment( 255.23d, Pos.SN1 );
         sn2NormBasis = new Fragment( 255.23d, Pos.SN2 );

         symCO2NormBasis = new Fragment( 327.23d, Pos.SYM, true );
         sn1CO2NormBasis = new Fragment( 327.23d, Pos.SN1, true );
         sn2CO2NormBasis = new Fragment( 327.23d, Pos.SN2, true);

         if(clazz.startsWith( "PCO" ) || clazz.equals( "PEO" ))
            setupNormBasis(clazz, new Fragment[] { sn2NormBasis, sn2CO2NormBasis});
         else
            setupNormBasis(clazz, new Fragment[] { symNormBasis, sn1NormBasis, sn2NormBasis, symCO2NormBasis, sn1CO2NormBasis, sn2CO2NormBasis});

         for ( String group : originalCollection.get( clazz ).keySet() )
         {
            TreeMap< String, Fragment > treeMap = originalCollection.get( clazz ).get( group );
            correctionFactorMap.put(group, new TreeMap<>(  ));

            for( String item : treeMap.keySet() )
            {
               correctionFactorMap.get(group).put(item, new Fragment( treeMap.get( item ) ));

               Fragment correctedFragment = calibratedCollection.get(clazz).get(group).get( item );

               if(correctedFragment.getPosition() != null) {
                  Fragment normBasis = null;
                  if(correctedFragment.isCo2loss()) {
                     switch ( correctedFragment.getPosition() ) {
                        case SYM: normBasis = symCO2NormBasis;
                           break;
                        case SN1: normBasis = sn1CO2NormBasis;
                           break;
                        case SN2: normBasis = sn2CO2NormBasis;
                           break;
                     }
                  } else {
                     switch ( correctedFragment.getPosition() ) {
                        case SYM: normBasis = symNormBasis;
                           break;
                        case SN1: normBasis = sn1NormBasis;
                           break;
                        case SN2: normBasis = sn2NormBasis;
                           break;
                     }
                  }


                  for(Float ce : normBasis.keys())
                  {
                     if(correctedFragment.get( ce ) != 0)
                     {
                        // Correction factor calculation
                        float factor = normBasis.get( ce ) / correctedFragment.get( ce );

                        // Put the factor to the factor map
                        correctionFactorMap.get( group ).get( item ).put( ce, factor );

                        // Apply the factor to the actual experimental data
//                        correctedFragment.put( ce, correctedFragment.get( ce ) * factor);

                        // Store correction factor in Fragment
                        correctedFragment.putCF( ce, factor );

                        correctionFactorMap.get( group ).get( item ).putCF( ce, factor );
                     }
                  }
               }

               // Store correction dbcf in Fragment
               //               FillDBCF(correctedFragment, correctionFactorMap.get( group ).get( mz ));
            }
         }
      }

      Event.fireEvent( masterXmlPane, new ProcessEvent( ProcessEvent.UPDATE_XML_MASTER, calibratedCollection, mFaAnionsList, 4 ));

      return correctionFactorMap;
   }

   public void makeTree( TreeItem<FAAnionRow> rootNode,
           TreeMap<String, TreeMap<String, TreeMap< String, Fragment > > > calibratedCollection,
           String postFix,
           TreeMap<String, TreeMap< String, Fragment > > correctionFactorMap)
   {
      HashMap<String, Float> maxFitMap = new HashMap<>();

      for(TreeItem<FAAnionRow> classNode : rootNode.getChildren())
      {
         String clazz = classNode.getValue().getName();

         // Normalization basis fragment
         final Fragment symNormBasis;
         final Fragment sn1NormBasis;
         final Fragment sn2NormBasis;

         final Fragment symCO2NormBasis;
         final Fragment sn1CO2NormBasis;
         final Fragment sn2CO2NormBasis;

         symNormBasis = new Fragment( 255.23d, Pos.SYM );
         sn1NormBasis = new Fragment( 255.23d, Pos.SN1 );
         sn2NormBasis = new Fragment( 255.23d, Pos.SN2 );

         symCO2NormBasis = new Fragment( 327.23d, Pos.SYM, true );
         sn1CO2NormBasis = new Fragment( 327.23d, Pos.SN1, true );
         sn2CO2NormBasis = new Fragment( 327.23d, Pos.SN2, true);

         if(clazz.startsWith( "PCO" ) || clazz.equals( "PEO" ))
            setupNormBasis(clazz, new Fragment[] { sn2NormBasis, sn2CO2NormBasis});
         else
            setupNormBasis(clazz, new Fragment[] { symNormBasis, sn1NormBasis, sn2NormBasis, symCO2NormBasis, sn1CO2NormBasis, sn2CO2NormBasis});

         for(TreeItem<FAAnionRow> groupNode : classNode.getChildren())
         {
            String group = groupNode.getValue().getName();

            for(TreeItem<FAAnionRow> mzNode : groupNode.getChildren())
            {
               Float fitMaxInt = calibratedCollection.get( clazz ).get( group ).get(mzNode.getValue().getName()).getMaxMz().floatValue();
               maxFitMap.put( mzNode.getValue().getName(), fitMaxInt);

               mzNode.getValue().newTitle( mzNode.getValue().getTitle() );

               mzNode.getValue().getTitle().addListener( new ChangeListener< Boolean >()
               {
                  @Override public void changed( ObservableValue< ? extends Boolean > observable, Boolean oldValue, Boolean newValue )
                  {
                     TreeMap< String, Fragment > treeMap = calibratedCollection.get( classNode.getValue().getName() )
                             .get( group );

                     String seriesName = mzNode.getValue().getName();

                     Fragment actualFragment = treeMap.get( seriesName );

                     Fragment normBasis = null;

                     switch ( actualFragment.getPosition() ) {
                        case SYM: normBasis = symNormBasis;
                           break;
                        case SN1: normBasis = sn1NormBasis;
                           break;
                        case SN2: normBasis = sn2NormBasis;
                           break;
                     }

                     createSeries( chart,
                             normBasis,
                             seriesName + ".Norm" );

                     updateChartsWithMzFactor( newValue, chart, correctionFactorChart, errorChart,
                             treeMap, maxFitMap, correctionFactorMap.get(group),
                             actualFragment.getMaxMz(), seriesName, postFix );
                  }
               } );

               if( mzNode.getValue().getCo2MassProperty() instanceof CheckBoxNamedBoolean )
               {
                  mzNode.getValue().newCo2Mass( mzNode.getValue().getCo2MassProperty() );

                  TreeMap< String, Fragment > treeMap = calibratedCollection.get( classNode.getValue().getName() )
                          .get( group );

                  String seriesName = mzNode.getValue().getCo2name();

                  maxFitMap.put( seriesName, treeMap.get(seriesName).getMaxMz().floatValue() );

                  mzNode.getValue().getCo2MassProperty().addListener( new ChangeListener< Boolean >()
                  {
                     @Override public void changed( ObservableValue< ? extends Boolean > observable, Boolean oldValue, Boolean newValue )
                     {
                        Fragment actualFragment = calibrateFragmentCollection.get(clazz).get(group).get( seriesName );

                        Fragment normBasis = null;

                        switch ( actualFragment.getPosition() ) {
                           case SYM: normBasis = symCO2NormBasis;
                              break;
                           case SN1: normBasis = sn1CO2NormBasis;
                              break;
                           case SN2: normBasis = sn2CO2NormBasis;
                              break;
                        }

                        createSeries( chart,
                                normBasis,
                                seriesName + ".Norm" );

                        updateChartsWithMzFactor( newValue, chart, correctionFactorChart, errorChart,
                                treeMap, maxFitMap, correctionFactorMap.get(group),
                                actualFragment.getMaxMz(), seriesName, postFix );
                     }
                  } );
               }

            }
         }
      }

      treeTableView.refresh();
   }

   protected void clearChart()
   {
      if(faanionHashMap != null)
      {
         for(String group : faanionHashMap.keySet())
         {
            for(FAAnionRow row : faanionHashMap.get(group))
            {
               if(row.getCo2MassProperty() != null)
                  row.getCo2MassProperty().set( false );
            }
         }
      }
   }
}
