package de.mpicbg.ms.view.treecell;

import de.mpicbg.ms.model.data.FAAnionRow;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;

import javafx.scene.control.TreeTableView;

import javafx.util.Callback;
import javafx.util.StringConverter;

/**
* Created by moon on 5/23/16.
*/
public class SelectiveCheckBoxTreeTableCell<S, T> extends TreeTableCell<S, T>
{
	private final static StringConverter<?> defaultStringConverter = new StringConverter<Object>() {
		@Override public String toString(Object t) {
			if(t != null && t instanceof NamedBoolean)
			{
				return ((NamedBoolean) t).getName();
			}
			return t == null ? null : t.toString();
		}

		@Override public Object fromString(String string) {
			return (Object) string;
		}
	};

	static <T> StringConverter<T> defaultStringConverter() {
		return (StringConverter<T>) defaultStringConverter;
	}

	public static <S> Callback<TreeTableColumn<S,NamedBoolean>, TreeTableCell<S,NamedBoolean>> forTreeTableTitleColumn(
			final TreeTableView<S> treeTableView) {

		Callback<Integer, ObservableValue<Boolean> > getSelectedProperty =
				item -> {
					if (treeTableView != null && treeTableView.getTreeItem( item ).getValue() instanceof FAAnionRow ) {
						return ((FAAnionRow) treeTableView.getTreeItem( item ).getValue()).getTitle();
					}
					return null;
				};

		return forTreeTableColumn(getSelectedProperty, true);
	}

	public static <S> Callback<TreeTableColumn<S,NamedBoolean>, TreeTableCell<S,NamedBoolean>> forTreeTableSn1Column(
			final TreeTableView<S> treeTableView) {

		Callback<Integer, ObservableValue<Boolean> > getSelectedProperty =
				item -> {
					if (treeTableView != null && treeTableView.getTreeItem( item ).getValue() instanceof FAAnionRow ) {
						return ((FAAnionRow) treeTableView.getTreeItem( item ).getValue()).getSn1();
					}
					return null;
				};

		return forTreeTableColumn(getSelectedProperty, false);
	}

	public static <S> Callback<TreeTableColumn<S,NamedBoolean>, TreeTableCell<S,NamedBoolean>> forTreeTableSn2Column(
			final TreeTableView<S> treeTableView) {

		Callback<Integer, ObservableValue<Boolean> > getSelectedProperty =
				item -> {
					if (treeTableView != null && treeTableView.getTreeItem( item ).getValue() instanceof FAAnionRow ) {
						return ((FAAnionRow) treeTableView.getTreeItem( item ).getValue()).getSn2();
					}
					return null;
				};

		return forTreeTableColumn(getSelectedProperty, false);
	}

	public static <S> Callback<TreeTableColumn<S,NamedBoolean>, TreeTableCell<S,NamedBoolean>> forTreeTableSymColumn(
			final TreeTableView<S> treeTableView) {

		Callback<Integer, ObservableValue<Boolean> > getSelectedProperty =
				item -> {
					if (treeTableView != null && treeTableView.getTreeItem( item ).getValue() instanceof FAAnionRow ) {
						return ((FAAnionRow) treeTableView.getTreeItem( item ).getValue()).getSym();
					}
					return null;
				};

		return forTreeTableColumn(getSelectedProperty, false);
	}


	public static <S> Callback<TreeTableColumn<S,NamedBoolean>, TreeTableCell<S,NamedBoolean>> forTreeTableCo2LossColumn(
			final TreeTableView<S> treeTableView) {

		Callback<Integer, ObservableValue<Boolean> > getSelectedProperty =
				item -> {
					if (treeTableView != null && treeTableView.getTreeItem( item ).getValue() instanceof FAAnionRow ) {
						return ((FAAnionRow) treeTableView.getTreeItem( item ).getValue()).getCo2MassProperty();
					}
					return null;
				};

		return forTreeTableColumn(getSelectedProperty, true);
	}

	public static <S,T> Callback<TreeTableColumn<S,T>, TreeTableCell<S,T>> forTreeTableColumn(
			final Callback<Integer, ObservableValue<Boolean>> getSelectedProperty) {
		return forTreeTableColumn(getSelectedProperty, null);
	}

	public static <S,T> Callback<TreeTableColumn<S,T>, TreeTableCell<S,T> > forTreeTableColumn(
			final Callback<Integer, ObservableValue<Boolean>> getSelectedProperty,
			final boolean showLabel) {
		StringConverter<T> converter = ! showLabel ?
				null : SelectiveCheckBoxTreeTableCell.<T>defaultStringConverter();
		return forTreeTableColumn(getSelectedProperty, converter);
	}

	public static <S,T> Callback<TreeTableColumn<S,T>, TreeTableCell<S,T>> forTreeTableColumn(
			final Callback<Integer, ObservableValue<Boolean>> getSelectedProperty,
			final StringConverter<T> converter) {
		return list -> new SelectiveCheckBoxTreeTableCell<S,T>(getSelectedProperty, converter);
	}

	private final CheckBox checkBox;
	private boolean showLabel;
	private ObservableValue<Boolean> booleanProperty;

	public SelectiveCheckBoxTreeTableCell() {
		this(null, null);
	}

	public SelectiveCheckBoxTreeTableCell(
			final Callback<Integer, ObservableValue<Boolean>> getSelectedProperty) {
		this(getSelectedProperty, null);
	}

	public SelectiveCheckBoxTreeTableCell(
			final Callback<Integer, ObservableValue<Boolean>> getSelectedProperty,
			final StringConverter<T> converter) {
		// we let getSelectedProperty be null here, as we can always defer to the
		// TreeTableColumn
		this.getStyleClass().add("check-box-tree-table-cell");

		this.checkBox = new CheckBox();

		// by default the graphic is null until the cell stops being empty
		setGraphic(null);

		setSelectedStateCallback(getSelectedProperty);
		setConverter(converter);

		//        // alignment is styleable through css. Calling setAlignment
		//        // makes it look to css like the user set the value and css will not
		//        // override. Initializing alignment by calling set on the
		//        // CssMetaData ensures that css will be able to override the value.
		//        final CssMetaData prop = CssMetaData.getCssMetaData(alignmentProperty());
		//        prop.set(this, Pos.CENTER);
	}

	/***************************************************************************
	 *                                                                         *
	 * Properties                                                              *
	 *                                                                         *
	 **************************************************************************/

	// --- converter
	private ObjectProperty<StringConverter<T>> converter =
			new SimpleObjectProperty<StringConverter<T>>(this, "converter") {
				protected void invalidated() {
					updateShowLabel();
				}
			};

	/**
	 * The {@link StringConverter} property.
	 */
	public final ObjectProperty<StringConverter<T>> converterProperty() {
		return converter;
	}

	/**
	 * Sets the {@link StringConverter} to be used in this cell.
	 */
	public final void setConverter(StringConverter<T> value) {
		converterProperty().set(value);
	}

	/**
	 * Returns the {@link StringConverter} used in this cell.
	 */
	public final StringConverter<T> getConverter() {
		return converterProperty().get();
	}



	// --- selected state callback property
	private ObjectProperty<Callback<Integer, ObservableValue<Boolean>>>
			selectedStateCallback =
			new SimpleObjectProperty<Callback<Integer, ObservableValue<Boolean>>>(
					this, "selectedStateCallback");

	/**
	 * Property representing the {@link Callback} that is bound to by the
	 * CheckBox shown on screen.
	 */
	public final ObjectProperty<Callback<Integer, ObservableValue<Boolean>>> selectedStateCallbackProperty() {
		return selectedStateCallback;
	}

	/**
	 * Sets the {@link Callback} that is bound to by the CheckBox shown on screen.
	 */
	public final void setSelectedStateCallback(Callback<Integer, ObservableValue<Boolean>> value) {
		selectedStateCallbackProperty().set(value);
	}

	/**
	 * Returns the {@link Callback} that is bound to by the CheckBox shown on screen.
	 */
	public final Callback<Integer, ObservableValue<Boolean>> getSelectedStateCallback() {
		return selectedStateCallbackProperty().get();
	}

	@Override public void updateItem( T item, boolean empty)
	{
		//super.updateItem( item, empty );

		if (empty) {
			setText(null);
			setGraphic(null);
		} else {
			StringConverter<T> c = getConverter();

			if (showLabel) {
				setText(c.toString(item));
			}

			if( item instanceof CheckBoxNamedBoolean )
			{
				setGraphic(checkBox);
			}
			else
				setGraphic( null );

			if (booleanProperty instanceof BooleanProperty) {
				checkBox.selectedProperty().unbindBidirectional((BooleanProperty)booleanProperty);
			}
			ObservableValue<?> obsValue = getSelectedProperty();
			if (obsValue instanceof BooleanProperty) {
				booleanProperty = (ObservableValue<Boolean>) obsValue;
				checkBox.selectedProperty().bindBidirectional((BooleanProperty)booleanProperty);
			}

			checkBox.disableProperty().bind(Bindings.not(
					getTreeTableView().editableProperty().and(
							getTableColumn().editableProperty()).and(
							editableProperty())
			));
		}
	}


	/***************************************************************************
	 *                                                                         *
	 * Private implementation                                                  *
	 *                                                                         *
	 **************************************************************************/

	private void updateShowLabel() {
		this.showLabel = converter != null;
		this.checkBox.setAlignment(showLabel ? Pos.CENTER_LEFT : Pos.CENTER);
	}

	private ObservableValue<?> getSelectedProperty() {
		return getSelectedStateCallback() != null ?
				getSelectedStateCallback().call(getIndex()) :
				getTableColumn().getCellObservableValue(getIndex());
	}
}
