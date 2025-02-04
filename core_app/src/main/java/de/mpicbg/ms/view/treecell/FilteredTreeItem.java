package de.mpicbg.ms.view.treecell;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.TreeItem;

import java.util.function.Predicate;

/**
 * Author: HongKee Moon (moon@mpi-cbg.de), Scientific Computing Facility
 * Organization: MPI-CBG Dresden
 * Date: November 2016
 */
public class FilteredTreeItem< T > extends TreeItem< T >
{
   final private ObservableList< FilteredTreeItem< T > > sourceList;
   private FilteredList< FilteredTreeItem< T > > filteredList;

   public FilteredTreeItem( T value )
   {
      super( value );
      this.sourceList = FXCollections.observableArrayList();
      this.filteredList = new FilteredList<>( this.sourceList );

      this.filteredList.addListener( new ListChangeListener< TreeItem< T > >()
      {
         @Override public void onChanged( Change< ? extends TreeItem< T > > c )
         {
            getChildren().setAll( filteredList );
         }
      } );
   }

   public final void setPredicate( Predicate< ? super TreeItem< T > > predicate )
   {
      filteredList.setPredicate( predicate );
   }

   public final void add( FilteredTreeItem< T > item )
   {
      sourceList.add( item );
   }
}
