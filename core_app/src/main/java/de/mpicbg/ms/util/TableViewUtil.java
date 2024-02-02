package de.mpicbg.ms.util;

import com.sun.javafx.scene.control.skin.TableViewSkin;
import de.mpicbg.ms.model.data.EstSample;
import de.mpicbg.ms.view.treecell.SamplePatternTreeTableCell;
import javafx.beans.property.ReadOnlyFloatWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.TextFieldTreeTableCell;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.AbstractCollection;
import java.util.LinkedList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Author: HongKee Moon (moon@mpi-cbg.de), Scientific Computing Facility
 * Organization: MPI-CBG Dresden
 * Date: January 2017
 */
public class TableViewUtil
{
	public static TableView<String[]> createDataView(String[] columns)
	{
		TableView<String[]> tv = new TableView<>();

		for(int i = 0; i < columns.length; i++)
		{
			TableColumn column = new TableColumn( columns[i] );
			column.setPrefWidth( 80 );
			int finalI = i;
			column.setCellValueFactory( new Callback<TableColumn.CellDataFeatures<String[], String>, ObservableValue<String> >() {
				@Override
				public ObservableValue<String> call(TableColumn.CellDataFeatures<String[], String> p) {
					return new SimpleStringProperty((p.getValue()[ finalI ]));
//					if(finalI < p.getValue().length)
//						return new SimpleStringProperty((p.getValue()[ finalI ]));
//					else
//						return new SimpleStringProperty("");
				}
			} );

			tv.getColumns().add( column );
		}

		return tv;
	}

	private static Method columnToFitMethod;

	static {
		try {
			columnToFitMethod = TableViewSkin.class.getDeclaredMethod("resizeColumnToFitContent", TableColumn.class, int.class);
			columnToFitMethod.setAccessible(true);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
	}

	public static void autoFitTable(TableView tableView) {
		for (Object column : tableView.getColumns()) {
			try {
				columnToFitMethod.invoke(tableView.getSkin(), column, -1);
			} catch (IllegalAccessException | InvocationTargetException e) {
				e.printStackTrace();
			}
		}
	}

	public static TreeTableView<String[]> createTreeDataView(String[] columns)
	{
		TreeTableView<String[]> tv = new TreeTableView<>();

		for(int i = 0; i < columns.length; i++)
		{
			TreeTableColumn column = new TreeTableColumn( columns[i] );
			column.setPrefWidth( 120 );
			int finalI = i;
			column.setCellValueFactory( new Callback<TreeTableColumn.CellDataFeatures<String[], String>, ObservableValue<String> >() {
				@Override
				public ObservableValue<String> call(TreeTableColumn.CellDataFeatures<String[], String> p) {
					return new SimpleStringProperty((p.getValue().getValue()[ finalI ]));
				}
			} );

			column.setCellFactory( TextFieldTreeTableCell.forTreeTableColumn() );

			column.setOnEditCommit( new EventHandler< TreeTableColumn.CellEditEvent<String[], String> >()
			{
				@Override public void handle( TreeTableColumn.CellEditEvent<String[], String> event )
				{
					String[] row = event.getRowValue().getValue();
					row[finalI] = event.getNewValue();
				}
			} );

			column.setEditable( true );

			tv.getColumns().add( column );
		}

		return tv;
	}

	public static TreeTableView<String[]> createTreeDataViewForSamplePattern(String[] columns)
	{
		TreeTableView<String[]> tv = new TreeTableView<>();

		for(int i = 0; i < columns.length; i++)
		{
			TreeTableColumn column = new TreeTableColumn( columns[i] );
			column.setPrefWidth( 120 );
			int finalI = i;
			column.setCellValueFactory( new Callback<TreeTableColumn.CellDataFeatures<String[], String>, ObservableValue<String> >() {
				@Override
				public ObservableValue<String> call(TreeTableColumn.CellDataFeatures<String[], String> p) {
					return new SimpleStringProperty((p.getValue().getValue()[ finalI ]));
				}
			} );

			column.setCellFactory( SamplePatternTreeTableCell.forTreeTableColumn() );

			column.setOnEditCommit( new EventHandler< TreeTableColumn.CellEditEvent<String[], String> >()
			{
				@Override public void handle( TreeTableColumn.CellEditEvent<String[], String> event )
				{
					if( tv.getTreeItemLevel( event.getRowValue() ) != 2 )
					{
						String[] row = event.getRowValue().getValue();
						row[ finalI ] = event.getNewValue();
					}
				}
			} );

			column.setEditable( true );

			tv.getColumns().add( column );
		}

		return tv;
	}

	public static void addContextMenu( TableView tableView, EventHandler< ? super MouseEvent > handler )
	{
		// Export csv menu
		final MenuItem exportCsvItem = new MenuItem( "Export to Clipboard" );
		exportCsvItem.setOnAction( new EventHandler< ActionEvent >()
		{
			@Override public void handle( ActionEvent event )
			{
				copyToClipboard( tableView );
			}
		} );

		// Clear data
		final MenuItem clear = new MenuItem( "Clear" );
		clear.setOnAction( new EventHandler< ActionEvent >()
		{
			@Override public void handle( ActionEvent event )
			{
				tableView.getItems().clear();
			}
		} );

		final ContextMenu chartContextMenu = new ContextMenu( exportCsvItem, clear );

		tableView.setOnMouseClicked( new EventHandler< MouseEvent >()
		{
			@Override public void handle( MouseEvent event )
			{
				if( MouseButton.SECONDARY.equals( event.getButton() )) {
					chartContextMenu.show( tableView, event.getScreenX(), event.getScreenY() );
				}
				else if( handler != null )
				{
					handler.handle(event);
				}
			}
		} );
	}

	public static void addContextMenu( TableView tableView )
	{
		addContextMenu( tableView, null );
	}

	public static <T> String tabString( AbstractCollection<T> list )
	{
		StringBuilder sb = new StringBuilder();

		int i = 0;
		for ( T item : list )
		{
			if(i++ > 0) sb.append( '\t' );
			sb.append( item.toString() );
		}
		sb.append( '\n' );

		return sb.toString();
	}

	public static void copyToClipboard( TableView tableView )
	{
		ObservableList list = tableView.getItems();

		if(list.size() > 0)
		{
			final Clipboard clipboard = Clipboard.getSystemClipboard();
			final ClipboardContent content = new ClipboardContent();

			content.putString( exportToString( tableView ) );

			clipboard.setContent( content );
		}
	}

	public static String exportToString( TableView tableView )
	{
		ObservableList list = tableView.getItems();

		if(list.size() > 0)
		{
			LinkedList< String > columns = ((Stream<TableColumn> ) tableView.getColumns().stream())
					.map( TableColumn::getText )
					.collect( Collectors.toCollection( () -> new LinkedList<>() ) );

			// Put series names
			StringBuilder sb = new StringBuilder();
			sb.append( tabString( columns ) );

			for( Object row : list )
			{
				if( row instanceof EstSample )
				{
					sb.append( row.toString() );
					sb.append( '\n' );
				}
				else if(row instanceof String[] )
				{
					for ( String column : (String[]) row )
						sb.append( column + '\t' );
					sb.append( '\n' );
				}
			}

			return sb.toString();
		}
		else
			return "";
	}

	/**
	 * Customized TreeView constructors
	 */

	public static TableView<EstSample> createEstSampleDataView()
	{
		TableView<EstSample> tv = new TableView<>();

		//Creating Group column for String type
		TableColumn<EstSample, String> column = new TableColumn<>("Group");
		column.setPrefWidth(80);

		//Defining cell content
		column.setCellValueFactory( (param) ->
				new ReadOnlyStringWrapper( param.getValue().getGroup() ) );

		// Put color on the row depending on Group name
		column.setCellFactory( (param) ->
				new TableCell<EstSample, String>() {
					@Override
					protected void updateItem(String item, boolean empty) {
						super.updateItem(item, empty);

						setText(empty ? "" : getItem().toString());
						setGraphic(null);

						TableRow<EstSample> currentRow = getTableRow();

						if (!isEmpty() && currentRow != null) {
							if(item.equals("Group-1"))
								currentRow.setStyle("-fx-background-color:lightcoral");
							else if(item.equals("Group-2"))
								currentRow.setStyle("-fx-background-color:lightgreen");
							else if(item.equals("Group-3"))
								currentRow.setStyle("-fx-background-color:lightblue");
							else if(item.equals("Group-4"))
								currentRow.setStyle("-fx-background-color:lightsalmon");
							else if(item.equals("Group-5"))
								currentRow.setStyle("-fx-background-color:lightyellow");
							else if(item.equals("Group-6"))
								currentRow.setStyle("-fx-background-color:lightseagreen");
							else if(item.equals("Group-7"))
								currentRow.setStyle("-fx-background-color:lightskyblue");
							else if(item.equals("Group-8"))
								currentRow.setStyle("-fx-background-color:lightgrey");
							else if(item.equals(""))
								currentRow.setStyle("");
						}
						else
							currentRow.setStyle("");
					}
				} );

		tv.getColumns().add(column);


		//Creating Specie column for String type
		column = new TableColumn<>("Specie");
		column.setPrefWidth(80);

		//Defining cell content
		column.setCellValueFactory( (param) ->
				new ReadOnlyStringWrapper( param.getValue().getSpecie() ) );

		tv.getColumns().add(column);

		//Creating Mz column for String type
		column = new TableColumn<>("Mz");
		column.setPrefWidth(80);

		//Defining cell content
		column.setCellValueFactory( (param) ->
				new ReadOnlyStringWrapper( param.getValue().getMz() ) );

		tv.getColumns().add(column);

		//Creating Sample column for String type
		column = new TableColumn<>("Sample");
		column.setPrefWidth(80);

		//Defining cell content
		column.setCellValueFactory( (param) ->
				new ReadOnlyStringWrapper( param.getValue().getName() ) );

		tv.getColumns().add(column);

		//Creating CE column for Number type
		TableColumn<EstSample, Number> numberColumn = new TableColumn<>("CE");
		numberColumn.setPrefWidth(80);

		//Defining cell content
		numberColumn.setCellValueFactory( (param) ->
			new ReadOnlyFloatWrapper( param.getValue().getCe() )
		);

		tv.getColumns().add(numberColumn);

		//Creating Corrected FA Intensity column for Number type
		numberColumn = new TableColumn<>("C.FAI");
		numberColumn.setPrefWidth(80);

		//Defining cell content
		numberColumn.setCellValueFactory( (param) ->
				new ReadOnlyFloatWrapper( param.getValue().getCorrectedFAI() )
		);

		tv.getColumns().add(numberColumn);

		//Creating Corrected CO Intensity column for Number type
		numberColumn = new TableColumn<>("C.COI");
		numberColumn.setPrefWidth(80);

		//Defining cell content
		numberColumn.setCellValueFactory( (param) ->
				new ReadOnlyFloatWrapper( param.getValue().getCorrectedCOI() )
		);

		tv.getColumns().add(numberColumn);

		//Creating Corrected FA-CO ratio column for Number type
		numberColumn = new TableColumn<>("FA-CO.ratio");
		numberColumn.setPrefWidth(80);

		//Defining cell content
		numberColumn.setCellValueFactory( (param) ->
				new ReadOnlyFloatWrapper( param.getValue().getFaCoRatio() )
		);

		tv.getColumns().add(numberColumn);

		//Creating Isomer column for Range type
		column = new TableColumn<>("FA_Isomer");
		column.setPrefWidth(80);

		//Defining cell content
		column.setCellValueFactory( (param) ->
				new ReadOnlyStringWrapper( param.getValue().getIsomer().toString() )
		);

		tv.getColumns().add(column);

		//Creating Position column for Range type
		column = new TableColumn<>("1st Pos");
		column.setPrefWidth(80);

		//Defining cell content
		column.setCellValueFactory( (param) ->
				new ReadOnlyStringWrapper( param.getValue().getPosition().toString() )
		);

		tv.getColumns().add(column);

      //Creating DB Correction factor
      column = new TableColumn<>("1st CF");
      column.setPrefWidth(80);

      //Defining cell content
      column.setCellValueFactory( (param) ->
              new ReadOnlyStringWrapper( param.getValue().getCF().toString() )
      );

      tv.getColumns().add(column);

      //Creating DB Corrected FAI
      column = new TableColumn<>("1st C.FAI");
      column.setPrefWidth(80);

      //Defining cell content
      column.setCellValueFactory( (param) ->
              new ReadOnlyStringWrapper( param.getValue().getCFCorrectedFAI().toString() )
      );

      tv.getColumns().add(column);

		//Creating rel_FAI
		column = new TableColumn<>("1st rel_FAI");
		column.setPrefWidth(80);

		//Defining cell content
		column.setCellValueFactory( (param) ->
				new ReadOnlyStringWrapper( param.getValue().getRel_FAI().toString() )
		);

		tv.getColumns().add(column);

      //Creating Secondary Position
      column = new TableColumn<>("2nd Pos");
      column.setPrefWidth(80);

      //Defining cell content
      column.setCellValueFactory( (param) ->
              new ReadOnlyStringWrapper( param.getValue().getSecondaryPosition().toString() )
      );

      tv.getColumns().add(column);

      // Create the secondary DBCF
      column = new TableColumn<>( "2nd CF" );
      column.setPrefWidth( 80 );

      column.setCellValueFactory( (param) ->
              new ReadOnlyStringWrapper( param.getValue().getSecondaryCF().toString() )
      );

      tv.getColumns().add(column);

      // Create the secondary DBC.FAI
      column = new TableColumn<>( "2nd C.FAI" );
      column.setPrefWidth( 80 );

      column.setCellValueFactory( (param) ->
              new ReadOnlyStringWrapper( param.getValue().getSecondCFCorrectedFAI().toString() )
      );

      tv.getColumns().add(column);

      // Create the secondary rel_FAI
      column = new TableColumn<>( "2nd rel_FAI" );
      column.setPrefWidth( 80 );

      column.setCellValueFactory( (param) ->
              new ReadOnlyStringWrapper( param.getValue().getSecondaryRel_FAI().toString() )
      );

      tv.getColumns().add(column);

      // Create the Third Position
      column = new TableColumn<>( "3rd Pos" );
      column.setPrefWidth( 80 );

      column.setCellValueFactory( (param) ->
              new ReadOnlyStringWrapper( param.getValue().getThirdPosition().toString() )
      );

      tv.getColumns().add(column);

		// Create TX.CF column
		numberColumn = new TableColumn<>("TX.CF");
		numberColumn.setPrefWidth(80);

		//Defining cell content
		numberColumn.setCellValueFactory( (param) ->
						new ReadOnlyFloatWrapper( param.getValue().getTxCF() )
		);

		tv.getColumns().add(numberColumn);

		return tv;
	}
}
