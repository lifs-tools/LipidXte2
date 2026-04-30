package de.mpicbg.ms.view.treecell;

import javafx.beans.value.ObservableValue;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.util.Callback;
import javafx.util.StringConverter;

/**
 * SelectiveCheckBoxTreeCell class for adaptive use of checkbox
 */
public class SelectiveCheckBoxTreeCell< T > extends CheckBoxTreeCell< T >
{
   protected final static StringConverter< ? > defaultTreeItemStringConverter =
           new StringConverter< TreeItem< ? > >()
           {
              @Override public String toString( TreeItem< ? > treeItem )
              {
                 return ( treeItem == null || treeItem.getValue() == null ) ?
                         "" : treeItem.getValue().toString();
              }

              @Override public TreeItem< ? > fromString( String string )
              {
                 return new TreeItem<>( string );
              }
           };

   static < T > StringConverter< TreeItem< T > > defaultTreeItemStringConverter()
   {
      return ( StringConverter< TreeItem< T > > ) defaultTreeItemStringConverter;
   }

   public SelectiveCheckBoxTreeCell( Callback< TreeItem< T >, ObservableValue< Boolean > > getSelectedProperty, StringConverter< TreeItem< T > > converter )
   {
      super( getSelectedProperty, converter );
   }

   public static < T > Callback< TreeView< T >, TreeCell< T > > forTreeView()
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
              SelectiveCheckBoxTreeCell.< T >defaultTreeItemStringConverter() );
   }

   public static < T > Callback< TreeView< T >, TreeCell< T > > forTreeView(
           final Callback< TreeItem< T >,
                   ObservableValue< Boolean > > getSelectedProperty )
   {
      return forTreeView( getSelectedProperty, SelectiveCheckBoxTreeCell.< T >defaultTreeItemStringConverter() );
   }

   public static < T > Callback< TreeView< T >, TreeCell< T > > forTreeView(
           final Callback< TreeItem< T >, ObservableValue< Boolean > > getSelectedProperty,
           final StringConverter< TreeItem< T > > converter )
   {
      return tree -> new SelectiveCheckBoxTreeCell< T >( getSelectedProperty, converter );
   }

   @Override public void updateItem( T item, boolean empty )
   {
      super.updateItem( item, empty );

      TreeItem< T > treeItem = getTreeItem();

      if ( treeItem instanceof CheckBoxTreeItem )
      {
         //			super.updateItem( item, empty );
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
