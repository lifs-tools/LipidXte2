package de.mpicbg.ms.view.treecell;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.Cell;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import javafx.util.StringConverter;
import javafx.util.converter.DefaultStringConverter;

/**
 * Author: HongKee Moon (moon@mpi-cbg.de), Scientific Computing Facility
 * Organization: MPI-CBG Dresden
 * Date: February 2017
 */
public class SamplePatternTreeTableCell<S,T> extends TreeTableCell<S,T>
{

	/***************************************************************************
	 *                                                                         *
	 * Static cell factories                                                   *
	 *                                                                         *
	 **************************************************************************/

	public static <S> Callback<TreeTableColumn<S,String>, TreeTableCell<S,String>> forTreeTableColumn() {
		return forTreeTableColumn(new DefaultStringConverter());
	}

	public static <S,T> Callback<TreeTableColumn<S,T>, TreeTableCell<S,T>> forTreeTableColumn(
			final StringConverter<T> converter) {
		return list -> new SamplePatternTreeTableCell<S,T>(converter);
	}


	/***************************************************************************
	 *                                                                         *
	 * Fields                                                                  *
	 *                                                                         *
	 **************************************************************************/

	private TextField textField;



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
	public SamplePatternTreeTableCell() {
		this(null);
	}

	/**
	 * Creates a TextFieldTreeTableCell that provides a {@link TextField} when put
	 * into editing mode that allows editing of the cell content. This method
	 * will work on any TreeTableColumn instance, regardless of its generic type.
	 * However, to enable this, a {@link StringConverter} must be provided that
	 * will convert the given String (from what the user typed in) into an
	 * instance of type T. This item will then be passed along to the
	 * {@link TreeTableColumn#onEditCommitProperty()} callback.
	 *
	 * @param converter A {@link StringConverter converter} that can convert
	 *      the given String (from what the user typed in) into an instance of
	 *      type T.
	 */
	public SamplePatternTreeTableCell(StringConverter<T> converter) {
		this.getStyleClass().add("text-field-tree-table-cell");
		setConverter(converter);
	}



	/***************************************************************************
	 *                                                                         *
	 * Properties                                                              *
	 *                                                                         *
	 **************************************************************************/

	// --- converter
	private ObjectProperty<StringConverter<T>> converter =
			new SimpleObjectProperty<StringConverter<T>>(this, "converter");

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



	/***************************************************************************
	 *                                                                         *
	 * Public API                                                              *
	 *                                                                         *
	 **************************************************************************/

	/** {@inheritDoc} */
	@Override public void startEdit() {
		if (! isEditable()
				|| ! getTreeTableView().isEditable()
				|| ! getTableColumn().isEditable()) {
			return;
		}

		if( getTreeTableView().getTreeItemLevel( getTreeTableRow().getTreeItem() ) == 2 ) return;

		super.startEdit();

		if (isEditing()) {
			if (textField == null) {
				textField = createTextField(this, getConverter());
			}

			if (textField != null) {
				textField.setText( getItemText(this, getConverter()) );
			}

			startEdit(this, getConverter(), null, null, textField);
		}
	}

	/** {@inheritDoc} */
	@Override public void cancelEdit() {
		super.cancelEdit();
		cancelEdit(this, getConverter(), null);
	}

	/** {@inheritDoc} */
	@Override public void updateItem(T item, boolean empty) {
		super.updateItem(item, empty);
		updateItem(this, getConverter(), null, null, textField);
	}

	static <T> TextField createTextField(final Cell<T> cell, final StringConverter<T> converter) {
		final TextField textField = new TextField(getItemText(cell, converter));

		// Use onAction here rather than onKeyReleased (with check for Enter),
		// as otherwise we encounter RT-34685
		textField.setOnAction(event -> {
			if (converter == null) {
				throw new IllegalStateException(
						"Attempting to convert text input into Object, but provided "
								+ "StringConverter is null. Be sure to set a StringConverter "
								+ "in your cell factory.");
			}
			cell.commitEdit(converter.fromString(textField.getText()));
			event.consume();
		});
		textField.setOnKeyReleased(t -> {
			if (t.getCode() == KeyCode.ESCAPE) {
				cell.cancelEdit();
				t.consume();
			}
		});
		return textField;
	}

	static <T> void updateItem(final Cell<T> cell,
			final StringConverter<T> converter,
			final HBox hbox,
			final Node graphic,
			final TextField textField) {
		if (cell.isEmpty()) {
			cell.setText(null);
			cell.setGraphic(null);
		} else {
			if (cell.isEditing()) {
				if (textField != null) {
					textField.setText(getItemText(cell, converter));
				}
				cell.setText(null);

				if (graphic != null) {
					hbox.getChildren().setAll(graphic, textField);
					cell.setGraphic(hbox);
				} else {
					cell.setGraphic(textField);
				}
			} else {
				cell.setText(getItemText(cell, converter));
				cell.setGraphic(graphic);
			}
		}
	}

	static <T> void startEdit(final Cell<T> cell,
			final StringConverter<T> converter,
			final HBox hbox,
			final Node graphic,
			final TextField textField) {
		if (textField != null) {
			textField.setText(getItemText(cell, converter));
		}
		cell.setText(null);

		if (graphic != null) {
			hbox.getChildren().setAll(graphic, textField);
			cell.setGraphic(hbox);
		} else {
			cell.setGraphic(textField);
		}

		textField.selectAll();

		// requesting focus so that key input can immediately go into the
		// TextField (see RT-28132)
		textField.requestFocus();
	}

	static <T> void cancelEdit(Cell<T> cell, final StringConverter<T> converter, Node graphic) {
		cell.setText(getItemText(cell, converter));
		cell.setGraphic(graphic);
	}

	private static <T> String getItemText(Cell<T> cell, StringConverter<T> converter) {
		return converter == null ?
				cell.getItem() == null ? "" : cell.getItem().toString() :
				converter.toString(cell.getItem());
	}
}