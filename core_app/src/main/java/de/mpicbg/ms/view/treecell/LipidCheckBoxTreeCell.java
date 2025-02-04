package de.mpicbg.ms.view.treecell;

import de.mpicbg.ms.util.FileConvert;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.event.EventHandler;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.util.Callback;
import javafx.util.StringConverter;

import java.io.File;
import java.util.List;

/**
 * This is a kind of SelectiveCheckBoxTreeCell with Drag & Drop support for files
 */
public class LipidCheckBoxTreeCell< T > extends SelectiveCheckBoxTreeCell< T >
{
   static < T > StringConverter< TreeItem< T > > defaultTreeItemStringConverter()
   {
      return ( StringConverter< TreeItem< T > > ) defaultTreeItemStringConverter;
   }

   public LipidCheckBoxTreeCell( Callback< TreeItem< T >, ObservableValue< Boolean > > getSelectedProperty, StringConverter< TreeItem< T > > converter )
   {
      this( getSelectedProperty, converter, null );
   }

   public LipidCheckBoxTreeCell( Callback< TreeItem< T >, ObservableValue< Boolean > > getSelectedProperty, StringConverter< TreeItem< T > > converter, ObservableMap< String, ObservableList< File > > items )
   {
      super( getSelectedProperty, converter );

      setOnDragOver( new EventHandler< DragEvent >()
      {
         @Override public void handle( DragEvent event )
         {
            Dragboard db = event.getDragboard();
            if ( db.hasFiles() )
            {
               event.acceptTransferModes( TransferMode.COPY );
            }
            else
            {
               event.consume();
            }
         }
      } );

      setOnDragDropped( new EventHandler< DragEvent >()
      {
         @Override public void handle( DragEvent event )
         {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if ( db.hasFiles() )
            {
               success = true;
               List< File > xmlFiles;

               ObservableList< File > addedFiles = FXCollections.observableArrayList();

               for ( File file : db.getFiles() )
               {
                  if ( file.isDirectory() )
                  {
                     xmlFiles = FileConvert.checkXmlFiles( file );
                     if ( xmlFiles.size() == 0 )
                     {
                        List< File > list = FileConvert.checkRawFiles( file );
                        for ( File rawFile : list )
                        {
                           FileConvert.convertRaw( rawFile );
                        }
                     }

                     addedFiles.addAll( FileConvert.checkXmlFiles( file ) );
                  }
                  else if ( file.isFile() )
                  {
                     if ( file.getName().toLowerCase().endsWith( ".mzxml" ) )
                     {
                        addedFiles.add( file );
                     }
                     else if ( file.getName().toLowerCase().endsWith( ".raw" ) )
                     {
                        addedFiles.add( FileConvert.convertRaw( file ) );
                     }
                  }
               }

               final String clazz = getTreeItem().getValue().toString();

               if ( !items.containsKey( clazz ) )
                  items.put( clazz, FXCollections.observableArrayList() );

               items.get( clazz ).addAll( addedFiles );

               for ( File file : addedFiles )
               {
                  // Make tree item
                  getTreeItem().getChildren().add( ( TreeItem< T > ) new TreeItem<>( file.getName() ) );
               }
            }
            event.setDropCompleted( success );
            event.consume();
         }
      } );

   }

   public static < T > Callback< TreeView< T >, TreeCell< T > > forTreeView(
           final Callback< TreeItem< T >, ObservableValue< Boolean > > getSelectedProperty,
           final StringConverter< TreeItem< T > > converter,
           ObservableMap< String, ObservableList< File > > items )
   {
      return tree -> new LipidCheckBoxTreeCell< T >( getSelectedProperty, converter, items );
   }

   public static < T > Callback< TreeView< T >, TreeCell< T > > forTreeView( ObservableMap< String, ObservableList< File > > items )
   {
      Callback< TreeItem< T >, ObservableValue< Boolean > > getSelectedProperty =
              item -> {
                 if ( item instanceof CheckBoxTreeItem< ? > )
                 {
                    return ( ( CheckBoxTreeItem< ? > ) item ).selectedProperty();
                 }
                 return null;
              };
      return forTreeView( getSelectedProperty,
              LipidCheckBoxTreeCell.< T >defaultTreeItemStringConverter(), items );

   }

   @Override public void updateItem( T item, boolean empty )
   {
      super.updateItem( item, empty );

      TreeItem< T > treeItem = getTreeItem();

      if ( treeItem instanceof CheckBoxTreeItem )
      {
         //			super.updateItem( item, empty );
         //			setStyle("-fx-background-color: yellow");
         //			Node disclosure = lookup( ".tree-cell > .tree-disclosure-node" );
         //			disclosure.setStyle( "-fx-padding: 24, 6, 24, 8" );
         //setStyle("-fx-graphic-text-gap: 30");

      }
      else
      {
         if ( !empty && item != null )
         {
            setText( item.toString() );
            setGraphic( getTreeItem().getGraphic() );
         }
         else
         {
            setText( null );
            setGraphic( null );
         }
      }
   }
}
