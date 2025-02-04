package de.mpicbg.ms.view.treecell;

import de.mpicbg.ms.model.data.FAAnionRow;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.css.PseudoClass;
import javafx.scene.control.Cell;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import javafx.util.StringConverter;

/**
 * Author: HongKee Moon (moon@mpi-cbg.de), Scientific Computing Facility
 * Organization: MPI-CBG Dresden
 * Date: November 2016
 */
public class CheckBoxTextFieldTreeTableCell< S, T > extends TreeTableCell< S, T >
{
   private final static StringConverter< ? > defaultStringConverter = new StringConverter< Object >()
   {
      @Override public String toString( Object t )
      {
         if ( t != null && t instanceof NamedBoolean )
         {
            return ( ( NamedBoolean ) t ).getName();
         }
         return t == null ? "" : t.toString();
      }

      @Override public Object fromString( String string )
      {
         return new CheckBoxNamedBoolean( string );
      }
   };

   /***************************************************************************
    *                                                                         *
    * Static cell factories                                                   *
    *                                                                         *
    **************************************************************************/

   public static < S > Callback< TreeTableColumn< S, NamedBoolean >, TreeTableCell< S, NamedBoolean > > forTreeTableSn1Column(
           final TreeTableView< S > treeTableView )
   {

      Callback< Integer, NamedBoolean > getSelectedProperty =
              item -> {
                 if ( treeTableView != null && treeTableView.getTreeItem( item ).getValue() instanceof FAAnionRow )
                 {
                    return ( ( FAAnionRow ) treeTableView.getTreeItem( item ).getValue() ).getSn1();
                 }
                 return null;
              };

      return forTreeTableColumn( getSelectedProperty );
   }

   public static < S > Callback< TreeTableColumn< S, NamedBoolean >, TreeTableCell< S, NamedBoolean > > forTreeTableSn2Column(
           final TreeTableView< S > treeTableView )
   {

      Callback< Integer, NamedBoolean > getSelectedProperty =
              item -> {
                 if ( treeTableView != null && treeTableView.getTreeItem( item ).getValue() instanceof FAAnionRow )
                 {
                    return ( ( FAAnionRow ) treeTableView.getTreeItem( item ).getValue() ).getSn2();
                 }
                 return null;
              };

      return forTreeTableColumn( getSelectedProperty );
   }

   public static < S > Callback< TreeTableColumn< S, NamedBoolean >, TreeTableCell< S, NamedBoolean > > forTreeTableSymColumn(
           final TreeTableView< S > treeTableView )
   {

      Callback< Integer, NamedBoolean > getSelectedProperty =
              item -> {
                 if ( treeTableView != null && treeTableView.getTreeItem( item ).getValue() instanceof FAAnionRow )
                 {
                    return ( ( FAAnionRow ) treeTableView.getTreeItem( item ).getValue() ).getSym();
                 }
                 return null;
              };

      return forTreeTableColumn( getSelectedProperty );
   }

   public static < S, T > Callback< TreeTableColumn< S, T >, TreeTableCell< S, T > > forTreeTableColumn(
           final Callback< Integer, NamedBoolean > getSelectedProperty )
   {
      return forTreeTableColumn( getSelectedProperty, CheckBoxTextFieldTreeTableCell.< T >defaultStringConverter() );
   }

   /**
    * Provides a {@link javafx.scene.control.TextField} that allows editing of the cell content when
    * the cell is double-clicked, or when
    * {@link javafx.scene.control.TreeTableView#edit(int, javafx.scene.control.TreeTableColumn) } is called.
    * This method will work  on any {@link TreeTableColumn} instance, regardless of
    * its generic type. However, to enable this, a {@link javafx.util.StringConverter} must
    * be provided that will convert the given String (from what the user typed
    * in) into an instance of type T. This item will then be passed along to the
    * {@link TreeTableColumn#onEditCommitProperty()} callback.
    * @param converter A {@link javafx.util.StringConverter} that can convert the given String
    * (from what the user typed in) into an instance of type T.
    * @return A {@link Callback} that can be inserted into the
    * {@link TreeTableColumn#cellFactoryProperty() cell factory property} of a
    * TreeTableColumn, that enables textual editing of the content.
    */
   public static < S, T > Callback< TreeTableColumn< S, T >, TreeTableCell< S, T > > forTreeTableColumn(
           final Callback< Integer, NamedBoolean > getSelectedProperty,
           final StringConverter< T > converter )
   {
      return list -> new CheckBoxTextFieldTreeTableCell< S, T >( getSelectedProperty, converter );
   }

   //The pseudo classes 'valid' that was defined in the css file.
   final protected PseudoClass validItem = PseudoClass.getPseudoClass( "valid" );

   /***************************************************************************
    *                                                                         *
    * Fields                                                                  *
    *                                                                         *
    **************************************************************************/

   private HBox hbox;
   private TextField textField;
   private final CheckBox checkBox;
   private ObservableValue< Boolean > booleanProperty;
   private final BooleanProperty validProperty;

   /***************************************************************************
    *                                                                         *
    * Constructors                                                            *
    *                                                                         *
    **************************************************************************/

   /**
    * Creates a default TextFieldTreeTableCell with a null converter. Without a
    * {@link StringConverter} specified, this cell will not be able to accept
    * input from the TextField (as it will not know how to convert this back
    * to the domain object). It is therefore strongly encouraged to not use
    * this constructor unless you intend to set the converter separately.
    */
   public CheckBoxTextFieldTreeTableCell()
   {
      this( null, null );
   }

   public CheckBoxTextFieldTreeTableCell(
           final Callback< Integer, NamedBoolean > getSelectedProperty )
   {
      this( getSelectedProperty, null );
   }

   public CheckBoxTextFieldTreeTableCell(
           final StringConverter< T > converter )
   {
      this( null, converter );
   }

   /**
    * Creates a TextFieldTreeTableCell that provides a {@link TextField} when put
    * into editing mode that allows editing of the cell content. This method
    * will work on any TreeTableColumn instance, regardless of its generic type.
    * However, to enable this, a {@link StringConverter} must be provided that
    * will convert the given String (from what the user typed in) into an
    * instance of type T. This item will then be passed along to the
    * {@link TreeTableColumn#onEditCommitProperty()} callback.
    * @param converter A {@link StringConverter converter} that can convert
    * the given String (from what the user typed in) into an instance of
    * type T.
    */
   public CheckBoxTextFieldTreeTableCell(
           final Callback< Integer, NamedBoolean > getSelectedProperty,
           final StringConverter< T > converter )
   {
      this.getStyleClass().add( "checkbox-text-field-tree-table-cell" );

      this.checkBox = new CheckBox();

      validProperty = new SimpleBooleanProperty( false );
      validProperty.addListener( new ChangeListener< Boolean >()
      {
         @Override public void changed( ObservableValue< ? extends Boolean > observable, Boolean oldValue, Boolean newValue )
         {
            pseudoClassStateChanged( validItem, newValue );
            setText( getConverter().toString( getItem() ) );
         }
      } );

      setSelectedStateCallback( getSelectedProperty );
      setConverter( converter );
      setEditable( true );
   }

   /***************************************************************************
    *                                                                         *
    * Properties                                                              *
    *                                                                         *
    **************************************************************************/

   // --- converter
   private ObjectProperty< StringConverter< T > > converter =
           new SimpleObjectProperty<>( this, "converter" );

   /**
    * The {@link StringConverter} property.
    */
   public final ObjectProperty< StringConverter< T > > converterProperty()
   {
      return converter;
   }

   /**
    * Sets the {@link StringConverter} to be used in this cell.
    */
   public final void setConverter( StringConverter< T > value )
   {
      converterProperty().set( value );
   }

   /**
    * Returns the {@link StringConverter} used in this cell.
    */
   public final StringConverter< T > getConverter()
   {
      return converterProperty().get();
   }

   // --- selected state callback property
   private ObjectProperty< Callback< Integer, NamedBoolean > >
           selectedStateCallback =
           new SimpleObjectProperty< Callback< Integer, NamedBoolean > >(
                   this, "selectedStateCallback" );

   /**
    * Property representing the {@link Callback} that is bound to by the
    * CheckBox shown on screen.
    */
   public final ObjectProperty< Callback< Integer, NamedBoolean > > selectedStateCallbackProperty()
   {
      return selectedStateCallback;
   }

   /**
    * Sets the {@link Callback} that is bound to by the CheckBox shown on screen.
    */
   public final void setSelectedStateCallback( Callback< Integer, NamedBoolean > value )
   {
      selectedStateCallbackProperty().set( value );
   }

   /**
    * Returns the {@link Callback} that is bound to by the CheckBox shown on screen.
    */
   public final Callback< Integer, NamedBoolean > getSelectedStateCallback()
   {
      return selectedStateCallbackProperty().get();
   }

   private ObservableValue< ? > getSelectedProperty()
   {
      return getSelectedStateCallback() != null ?
              getSelectedStateCallback().call( getIndex() ) :
              getTableColumn().getCellObservableValue( getIndex() );
   }

   /***************************************************************************
    *                                                                         *
    * Public API                                                              *
    *                                                                         *
    **************************************************************************/

   /**
    * {@inheritDoc}
    */
   @Override public void updateItem( T item, boolean empty )
   {
      super.updateItem( item, empty );

      if ( empty )
      {
         setText( null );
         setGraphic( null );
         pseudoClassStateChanged( validItem, false );
      }
      else
      {
         StringConverter< T > c = getConverter();

         if ( item instanceof CheckBoxNamedBoolean )
         {
            if ( isEditing() )
            {
               if ( textField != null )
               {
                  textField.setText( getItemText( this, c ) );
               }
               setText( null );

               hbox.getChildren().setAll( checkBox, textField );
               setGraphic( hbox );
            }
            else
            {
               setText( getItemText( this, c ) );
               //					setGraphic( checkBox );
               setGraphic( null );
            }

            pseudoClassStateChanged( validItem, ( ( CheckBoxNamedBoolean ) item ).isValid() );
         }
         else
         {
            setText( null );
            setGraphic( null );
            pseudoClassStateChanged( validItem, false );
         }

         if ( booleanProperty instanceof BooleanProperty )
         {
            checkBox.selectedProperty().unbindBidirectional( ( BooleanProperty ) booleanProperty );
         }
         ObservableValue< ? > obsValue = getSelectedProperty();
         if ( obsValue instanceof BooleanProperty )
         {
            booleanProperty = ( ObservableValue< Boolean > ) obsValue;
            checkBox.selectedProperty().bindBidirectional( ( BooleanProperty ) booleanProperty );
         }

         validProperty.unbind();
         if ( obsValue instanceof NamedBoolean )
         {
            validProperty.bind( ( ( NamedBoolean ) obsValue ).validProperty() );
         }

         //			checkBox.disableProperty().bind( Bindings.not(
         //					getTreeTableView().editableProperty().and(
         //							getTableColumn().editableProperty() ).and(
         //							editableProperty() )
         //			));
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override public void startEdit()
   {
      if ( !isEditable()
              || !getTreeTableView().isEditable()
              || !getTableColumn().isEditable()
              || getItem() instanceof String )
      {
         return;
      }
      super.startEdit();

      if ( isEditing() )
      {
         if ( textField == null )
         {
            textField = createTextField( this, getConverter() );
         }

         if ( textField != null )
         {
            textField.setText( getItemText( this, getConverter() ) );
         }
         setText( null );

         if ( hbox == null )
         {
            hbox = new HBox();
         }

         hbox.getChildren().setAll( checkBox, textField );
         setGraphic( hbox );

         textField.selectAll();

         // requesting focus so that key input can immediately go into the
         // TextField (see RT-28132)
         textField.requestFocus();
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override public void cancelEdit()
   {
      super.cancelEdit();
      setText( getItemText( this, getConverter() ) );
      setGraphic( checkBox );
   }

   /***************************************************************************
    *                                                                         *
    * TextField convenience                                                   *
    *                                                                         *
    **************************************************************************/

   static < T > TextField createTextField( final Cell< T > cell, final StringConverter< T > converter )
   {
      final TextField textField = new TextField( getItemText( cell, converter ) );

      // Use onAction here rather than onKeyReleased (with check for Enter),
      // as otherwise we encounter RT-34685
      textField.setOnAction( event -> {
         if ( converter == null )
         {
            throw new IllegalStateException(
                    "Attempting to convert text input into Object, but provided "
                            + "StringConverter is null. Be sure to set a StringConverter "
                            + "in your cell factory." );
         }
         T item = cell.getItem();
         if ( item instanceof NamedBoolean )
         {
            ( ( NamedBoolean ) item ).setName( textField.getText() );
         }
         cell.commitEdit( item );
         event.consume();
      } );
      textField.setOnKeyReleased( t -> {
         if ( t.getCode() == KeyCode.ESCAPE )
         {
            cell.cancelEdit();
            t.consume();
         }
      } );
      return textField;
   }

   /***************************************************************************
    *                                                                         *
    * General convenience                                                     *
    *                                                                         *
    **************************************************************************/

   /*
    * Simple method to provide a StringConverter implementation in various cell
    * implementations.
    */
   @SuppressWarnings( "unchecked" )
   static < T > StringConverter< T > defaultStringConverter()
   {
      return ( StringConverter< T > ) defaultStringConverter;
   }

   private static < T > String getItemText( Cell< T > cell, StringConverter< T > converter )
   {
      return converter == null ?
              cell.getItem() == null ? "" : cell.getItem().toString() :
              converter.toString( cell.getItem() );
   }

}
