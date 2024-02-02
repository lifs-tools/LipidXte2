package de.mpicbg.ms.view.pipeline.validation;

import de.mpicbg.ms.model.event.ProcessEvent;
import de.mpicbg.ms.model.data.BA;
import de.mpicbg.ms.model.data.BARow;
import de.mpicbg.ms.model.data.CO;
import de.mpicbg.ms.model.data.FA;
import de.mpicbg.ms.model.data.FAAnion;
import de.mpicbg.ms.model.data.Sample;
import de.mpicbg.ms.view.pane.MasterXmlPane;
import de.mpicbg.ms.view.pane.component.LabeledPane;
import de.mpicbg.ms.view.pipeline.common.Experiment;
import de.mpicbg.ms.view.treecell.FilteredTreeItem;
import de.mpicbg.ms.util.TableViewUtil;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.chart.LineChart;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Callback;
import org.apache.commons.math3.util.Precision;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static de.mpicbg.ms.model.event.ProcessEvent.COMMAND_VALIDATION;

/**
 * Author: HongKee Moon (moon@mpi-cbg.de), Scientific Computing Facility
 * Organization: MPI-CBG Dresden
 * Date: May 2017
 */
public class ValidationTab extends Tab
{
//	final static HashSet<String> defaultPriRefs = new HashSet<>();
//
//	static {
//		defaultPriRefs.add( "PC 25:0:0" );
//		defaultPriRefs.add( "PC 34:0:0" );
//		defaultPriRefs.add( "PC 37:4:0" );
//		defaultPriRefs.add( "PC 43:6:0" );
//	}

	private HashSet< Experiment.StandardReference > defaultPriRefs = new HashSet<>(  );


	protected final TreeTableView<BARow> treeTableView;
	private final TableView<String[]> collisionEnergyTableView;

	protected final FilteredTreeItem<BARow> root;

	private final HashMap<TreeItem< BARow >, BA> baMap;

	private final ListView<String> sampleListView;
	private final ObservableList<String> samples;

	private final ObservableList< Experiment.PRIRef > priRefs;

	private final TreeTableView<String[]> groupTreeView;
	private final TreeItem<String[]> groupRoot;

	private File loadingFile;
	private String outputPath;

	private File machinePerformanceFile;
	private File transmissionFunctionFile;

	private final ObservableList<String> cleanSpecies;
	private final ObservableList<String> excludedSpecies;

	public ValidationTab( LineChart< Number, Number > chart, LineChart< Number, Number > correctionFactorChart,
			LineChart< Number, Number > extraChart, MasterXmlPane masterXmlPane, SampleValidationPane quantValidationPane,
			TransmissionCorrectionPane transmissionCorrectionPane, ObservableList< FAAnion > mFaAnionsList )
	{
		baMap = new HashMap<>();
		samples = FXCollections.observableArrayList();
		cleanSpecies = FXCollections.observableArrayList();
		excludedSpecies = FXCollections.observableArrayList();

		priRefs = FXCollections.observableArrayList();
		ListView< Experiment.PRIRef > priRefListView = new ListView<>();

		priRefListView.setCellFactory( CheckBoxListCell.forListView( new Callback< Experiment.PRIRef, ObservableValue<Boolean>>() {
			@Override
			public ObservableValue<Boolean> call( Experiment.PRIRef item) {
				return item.onProperty();
			}
		}));

		priRefListView.setItems( priRefs );

		groupRoot = new TreeItem<String[]>( new String[] {"",""} );

		groupTreeView = TableViewUtil.createTreeDataViewForSamplePattern( new String[] { "Group", "Pattern"} );
		groupTreeView.setEditable( true );
		groupTreeView.setShowRoot( false );
		groupTreeView.setRoot( groupRoot );

		treeTableView = new TreeTableView< BARow >();
		treeTableView.setEditable( false );

		root = new FilteredTreeItem<>(new BARow( "" ));
		root.setExpanded(true);

		//Creating a column1
		TreeTableColumn<BARow, String> column1 = new TreeTableColumn<>("");
		column1.setPrefWidth(150);
		column1.setEditable( false );
		//Defining cell content
		column1.setCellValueFactory((param) ->
				new ReadOnlyStringWrapper( param.getValue().getValue().getTitle() )
		);

		//Creating a column2
		TreeTableColumn<BARow, String> column2 = new TreeTableColumn<>("MZ");
		column2.setPrefWidth(80);
		column2.setEditable( false );
		//Defining cell content
		column2.setCellValueFactory( (param) ->
				new ReadOnlyStringWrapper( param.getValue().getValue().getMassString() ) );

		//Creating a column3
		TreeTableColumn<BARow, String> column3 = new TreeTableColumn<>("C");
		column3.setPrefWidth(50);
		column3.setEditable( false );
		//Defining cell content
		column3.setCellValueFactory( (param) ->
				new ReadOnlyStringWrapper( param.getValue().getValue().getCarbon() + "" ) );

		//Creating a column4
		TreeTableColumn<BARow, String > column4 = new TreeTableColumn<>("DB");
		column4.setPrefWidth(50);
		column4.setEditable( false );
		column4.setCellValueFactory( ( param ) ->
				new ReadOnlyStringWrapper( param.getValue().getValue().getDb() + "" )
		);

		//Creating a column5
		TreeTableColumn<BARow, String> column5 = new TreeTableColumn<>("CO");
		column5.setPrefWidth(80);
		column5.setEditable( false );
		column5.setCellValueFactory( (param) ->
				new ReadOnlyStringWrapper( param.getValue().getValue().getCoValidString() )
		);

		treeTableView.setEditable( true );
		treeTableView.setRoot( root );

		treeTableView.setShowRoot( false );
		treeTableView.getColumns().setAll( column1, column2, column3, column4, column5 );


		collisionEnergyTableView = new TableView<String[]>();

		TableColumn tableColumn1 = new TableColumn( "Sample" );
		tableColumn1.setPrefWidth( 150 );
		tableColumn1.setCellValueFactory( new Callback<TableColumn.CellDataFeatures<String[], String>, ObservableValue<String> >() {
			@Override
			public ObservableValue<String> call(TableColumn.CellDataFeatures<String[], String> p) {
				return new SimpleStringProperty((p.getValue()[0]));
			}
		} );
		TableColumn tableColumn2 = new TableColumn( "CE" );
		tableColumn2.setPrefWidth( 150 );
		tableColumn2.setCellValueFactory( new Callback<TableColumn.CellDataFeatures<String[], String>, ObservableValue<String> >() {
			@Override
			public ObservableValue<String> call(TableColumn.CellDataFeatures<String[], String> p) {
				return new SimpleStringProperty((p.getValue()[1]));
			}
		} );
		TableColumn tableColumn3 = new TableColumn( "INT" );
		tableColumn3.setPrefWidth( 150 );
		tableColumn3.setCellValueFactory( new Callback<TableColumn.CellDataFeatures<String[], String>, ObservableValue<String>>() {
			@Override
			public ObservableValue<String> call(TableColumn.CellDataFeatures<String[], String> p) {
				return new SimpleStringProperty((p.getValue()[2]));
			}
		} );
		TableColumn tableColumn4 = new TableColumn( "COI" );
		tableColumn4.setPrefWidth( 150 );
		tableColumn4.setCellValueFactory( new Callback<TableColumn.CellDataFeatures<String[], String>, ObservableValue<String>>() {
			@Override
			public ObservableValue<String> call(TableColumn.CellDataFeatures<String[], String> p) {
				return new SimpleStringProperty((p.getValue()[3]));
			}
		} );

		collisionEnergyTableView.getColumns().addAll( tableColumn1, tableColumn2, tableColumn3, tableColumn4);

		treeTableView.getSelectionModel().selectedItemProperty().addListener( ( observable, oldValue, newValue ) ->
				updateCollisionEnergyTableView() );

		Button loadWorkspace = new Button( "Load previous workspace" );
		Button saveWorkspace = new Button( "Save current workspace" );

		Button loadExperiment = new Button( "Load experiment" );
		loadExperiment.setMinHeight( 35 );
		loadExperiment.setStyle("-fx-base: #a5e787;");

		loadExperiment.setOnDragOver( new EventHandler< DragEvent >()
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

		TextField expName = new TextField(  );

		loadExperiment.setOnDragDropped( new EventHandler< DragEvent >()
		{
			@Override public void handle( DragEvent event )
			{
				Dragboard db = event.getDragboard();
				boolean success = false;
				if ( db.hasFiles() )
				{
					baMap.clear();
					samples.clear();
					cleanSpecies.clear();
					excludedSpecies.clear();
					priRefs.clear();

					success = true;

					for ( File file : db.getFiles() )
					{
						// Process file
						//						System.out.println(file);
						final String suggestedName = preprocessData(file);
						expName.setText( suggestedName );
					}
				}
				event.setDropCompleted( success );
				event.consume();
			}
		} );

		loadExperiment.setOnAction( new EventHandler< ActionEvent >()
		{
			@Override public void handle( ActionEvent event )
			{
				if(null != loadingFile)
				{
					Experiment.process(outputPath, loadingFile, expName.getText(), root, samples, baMap, priRefs, defaultPriRefs, mFaAnionsList, false);
				}
			}
		} );

		Button findCleanSpeciesButton = new Button( "Find Clean Species" );
		findCleanSpeciesButton.setMinHeight( 35 );
		findCleanSpeciesButton.setStyle("-fx-base: #e7e798;");

//		HBox expNameBox = new HBox( new Label( "Experiment Name: " ), expName );
//		expNameBox.setAlignment( Pos.CENTER_LEFT );
//
//		Button autoGroup = new Button( "Group samples" );
//
//		autoGroup.setOnAction( new EventHandler< ActionEvent >()
//		{
//			@Override public void handle( ActionEvent event )
//			{
//				// Iterate them and put sample id into proper group
//				groupRoot.getChildren().forEach( c ->
//				{
//					String pattern = c.getValue()[1];
//					Pattern r = Pattern.compile(pattern);
//
//					for(String sample : samples)
//					{
//						if( r.matcher( sample ).matches() )
//						{
//							c.getChildren().add( new TreeItem<>( new String[] { sample, "" } ) );
//						}
//					}
//				});
//			}
//		} );

		Button checkIntRatioButton = new Button( "Check Intensity Ratio" );
		checkIntRatioButton.setMinHeight( 35 );
		checkIntRatioButton.setStyle( "-fx-base: #e7ac91;" );

		sampleListView = new ListView<String>();
		sampleListView.setItems( samples );
		sampleListView.getSelectionModel().setSelectionMode( SelectionMode.MULTIPLE );

		sampleListView.setOnDragDetected( new EventHandler< MouseEvent >()
		{
			@Override public void handle( MouseEvent event )
			{
				ObservableList<String> items = sampleListView.getSelectionModel().getSelectedItems();

				Dragboard dragboard = sampleListView.startDragAndDrop( TransferMode.COPY );
				ClipboardContent content = new ClipboardContent();
				content.putString( "^Samples:" + items.stream().collect( Collectors.joining(",") ) );
				dragboard.setContent(content);

				event.consume();
			}
		} );

		//		sampleListView.setCellFactory( tv ->
		//		{
		//			DragSelectionCell cell = new DragSelectionCell();
		//			cell.addEventFilter( MouseEvent.MOUSE_PRESSED, e -> {
		//				if (e.getButton() == MouseButton.SECONDARY) {
		//					e.consume();
		//				}
		//			});
		//			return cell ;
		//		});

		groupTreeView.setOnDragOver( new EventHandler< DragEvent >()
		{
			@Override public void handle( DragEvent event )
			{
				Dragboard db = event.getDragboard();
				if ( db.hasString() )
				{
					event.acceptTransferModes( TransferMode.COPY );
				}
				else
				{
					event.consume();
				}
			}
		} );

		groupTreeView.setOnDragDropped( new EventHandler< DragEvent >()
		{
			@Override public void handle( DragEvent event )
			{
				Dragboard db = event.getDragboard();
				boolean success = false;

				if ( db.hasString() )
				{
					if( db.getString().startsWith( "^Samples:" ) && event.getTarget() instanceof Text )
					{
						// In case of drag & drop samples under the previous group
						String groupName = ((Text) event.getTarget()).getText();

						groupRoot.getChildren().forEach( c ->
						{
							if ( groupName.equals( c.getValue()[ 0 ] ) )
							{
								String sampleString = db.getString().replace( "^Samples:", "" );
								String[] tokens = sampleString.split( "," );

								for ( String token : tokens )
								{
									c.getChildren().add( new TreeItem<>( new String[] { token, "" } ) );
								}
							}
						} );
					}
					else
					{
						if( db.getString().startsWith( "^Samples:" ) )
						{
							// In case of just drag & drop samples under nothing since we need to create new group

							// Find the first available new group name
							final String groupName = "Group-" + (groupRoot.getChildren().size() + 1);

							final TreeItem c = new TreeItem<>( new String[] { groupName, "" } );
							c.setExpanded( true );

							final String sampleString = db.getString().replace( "^Samples:", "" );
							final String[] tokens = sampleString.split( "," );

							for ( String token : tokens )
							{
								c.getChildren().add( new TreeItem<>( new String[] { token, "" } ) );
							}

							groupRoot.getChildren().add( c );
						}
						else
						{
							// Case of drag & drop "group name with patterns"
							BufferedReader bufReader = new BufferedReader(new StringReader(db.getString()));
							String line = null;

							try
							{
								while ( ( line = bufReader.readLine() ) != null )
								{
									String[] tokens = line.split( "\t" );
									if(tokens.length == 2)
									{
										groupRoot.getChildren().add( new TreeItem< String[] >( new String[]{tokens[0], tokens[1]} ) );
									}
									else
									{
										groupRoot.getChildren().add( new TreeItem< String[] >( new String[]{tokens[0], ""} ) );
									}
								}
							}
							catch ( IOException exception )
							{

							}
						}
					}
				}

				event.setDropCompleted( success );
				event.consume();
			}
		} );

		groupTreeView.getSelectionModel().selectedItemProperty().addListener( ( observable, oldValue, newValue ) -> updateCollisionEnergyTableView() );

		MenuItem createItem = new MenuItem( "Create" );
		createItem.setOnAction( new EventHandler< ActionEvent >()
		{
			@Override public void handle( ActionEvent event )
			{
				groupRoot.getChildren().add( new TreeItem< String[] >( new String[]{ "New Item", ""} ) );
			}
		} );

		MenuItem removeItem = new MenuItem( "Remove" );
		removeItem.setOnAction( new EventHandler< ActionEvent >()
		{
			@Override public void handle( ActionEvent event )
			{
				String name = groupTreeView.getSelectionModel().getSelectedItem().getValue()[0];
				groupRoot.getChildren().removeIf( c -> name.equals( c.getValue()[0] ) );
			}
		} );

		MenuItem resetItem = new MenuItem( "Reset" );
		resetItem.setOnAction( new EventHandler< ActionEvent >()
		{
			@Override public void handle( ActionEvent event )
			{
				groupRoot.getChildren().clear();
			}
		} );

		MenuItem sendGroup = new MenuItem( "Send Group" );
		sendGroup.setOnAction( new EventHandler< ActionEvent >()
		{
			@Override public void handle( ActionEvent event )
			{
				TreeItem<String[]> item = groupTreeView.getSelectionModel().getSelectedItem();
				String groupName;

				if( item.isLeaf() )
				{
					TreeItem<String[]> groupItem = item.getParent();
					groupName = groupItem.getValue()[0];
				}
				else
				{
					groupName = item.getValue()[0];
				}

				//				System.out.println( groupName );

				sendGroupSample( groupName, quantValidationPane,
						treeTableView, collisionEnergyTableView, mFaAnionsList );
			}
		} );

		MenuItem resetGroup = new MenuItem( "Reset Group" );
		resetGroup.setOnAction( new EventHandler< ActionEvent >()
		{
			@Override public void handle( ActionEvent event )
			{
				if( null != collisionEnergyTableView.getSelectionModel().getSelectedItems() )
				{
					collisionEnergyTableView.getItems().forEach( c ->
					{
						// Fire Reset to selectedSamplesDataView
						Event.fireEvent( quantValidationPane, new ProcessEvent( ProcessEvent.VALIDATION_GROUPING, "Reset" ) );
					} );
					collisionEnergyTableView.getSelectionModel().clearSelection();
				}
			}
		} );

		MenuItem txCorrect = new MenuItem( "Correct Transmission" );
		txCorrect.setOnAction( new EventHandler< ActionEvent >()
		{
			@Override public void handle( ActionEvent event )
			{
				System.out.println( "Correct Transmission" );

				if( null == groupTreeView.getSelectionModel().getSelectedItem() ) return;

				// Check the all the clean species
				if(priRefs.stream().filter( c -> c.isOn() ).count() == 0) {
					System.err.println("Please, check the references or click 'Find Clean Species' button.");
					return;
				}

				TreeMap< String, ArrayList<String> > groupMap = new TreeMap< String, ArrayList< String > >(  );

				ArrayList<String> sampleIds = new ArrayList<>();

				if( groupTreeView.getSelectionModel().getSelectedItem().isLeaf() )
				{
					sampleIds.add( groupTreeView.getSelectionModel().getSelectedItem().getValue()[ 0 ] );
					groupMap.put( groupTreeView.getSelectionModel().getSelectedItem().getParent().getValue()[ 0 ], sampleIds );
				}
				else
				{
					groupTreeView.getSelectionModel().getSelectedItem().getChildren().forEach( c -> sampleIds.add( c.getValue()[ 0 ] ) );
					groupMap.put( groupTreeView.getSelectionModel().getSelectedItem().getValue()[ 0 ], sampleIds );
				}

				for( String key : groupMap.keySet() )
				{
					System.out.println( key + ":" + groupMap.get(key) );
				}

				ObservableList<TreeItem<BARow>> species = treeTableView.getRoot().getChildren();

				Map<String, Float> refPRIMap = priRefs.stream().filter( c -> c.isOn() ).collect( Collectors.toMap( Experiment.PRIRef::getName, c -> 0f));

//				System.out.println(refPRIMap);

				Event.fireEvent( transmissionCorrectionPane, new ProcessEvent( ProcessEvent.VALIDATION_TX_CORRECTION, groupMap,
						species, baMap, refPRIMap, mFaAnionsList ) );
			}
		} );

		MenuItem saveTXFunctionParams = new MenuItem( "Show TX function parameters" );
		saveTXFunctionParams.setOnAction( new EventHandler< ActionEvent >()
		{
			@Override public void handle( ActionEvent event )
			{
				System.out.println( "Show TX function parameters" );

				TreeMap< String, ArrayList<String> > groupMap = new TreeMap< String, ArrayList< String > >(  );

				ArrayList<String> sampleIds = new ArrayList<>();

				if( groupTreeView.getSelectionModel().getSelectedItem().isLeaf() )
				{
					sampleIds.add( groupTreeView.getSelectionModel().getSelectedItem().getValue()[ 0 ] );
					groupMap.put( groupTreeView.getSelectionModel().getSelectedItem().getParent().getValue()[ 0 ], sampleIds );
				}
				else
				{
					groupTreeView.getSelectionModel().getSelectedItem().getChildren().forEach( c -> sampleIds.add( c.getValue()[ 0 ] ) );
					groupMap.put( groupTreeView.getSelectionModel().getSelectedItem().getValue()[ 0 ], sampleIds );
				}

				for( String key : groupMap.keySet() )
				{
					System.out.println( key + ":" + groupMap.get(key) );
				}

				ObservableList<TreeItem<BARow>> species = treeTableView.getRoot().getChildren();

				Map<String, Float> refPRIMap = priRefs.stream().filter( c -> c.isOn() ).collect( Collectors.toMap( Experiment.PRIRef::getName, c -> 0f));

				//				System.out.println(refPRIMap);
				HashMap<String, HashSet<TreeItem< BARow >>> map = new HashMap<>(  );

				for(TreeItem< BARow > pre : root.getChildren())
				{
					String title = pre.getValue().getTitle();

					if ( !map.containsKey( pre.getValue().getTitle() ) )
					{
						map.put( title, new HashSet<>() );
					}

					for( TreeItem< BARow > fa : pre.getChildren() )
					{
						map.get( title ).add( fa );
					}
				}

				Event.fireEvent( transmissionCorrectionPane, new ProcessEvent( ProcessEvent.COMMAND_TX_CORRECTION, groupMap,
						species, baMap, refPRIMap, mFaAnionsList, map, transmissionFunctionFile, machinePerformanceFile ) );
			}
		} );

		MenuItem resetTxCorrect = new MenuItem( "Reset Transmission Correction" );
		resetTxCorrect.setOnAction( new EventHandler< ActionEvent >()
		{
			@Override public void handle( ActionEvent event )
			{
				Event.fireEvent( transmissionCorrectionPane, new ProcessEvent( ProcessEvent.VALIDATION_RESET_TX_CORRECTION ) );
			}
		} );

		MenuItem machinPerfCheck = new MenuItem( "Machine Performance" );
		machinPerfCheck.setOnAction( new EventHandler< ActionEvent >()
		{
			@Override public void handle( ActionEvent event )
			{
				if( null == groupTreeView.getSelectionModel().getSelectedItem() ) return;

				// Check if the references are checked
				if(priRefs.stream().filter( c -> c.isOn() ).count() == 0) {
					System.err.println("Please, check the references or click 'Find Clean Species' button.");
					return;
				}

				TreeMap< String, ArrayList<String> > groupMap = new TreeMap< String, ArrayList< String > >(  );

				ArrayList<String> sampleIds = new ArrayList<>();

				if( groupTreeView.getSelectionModel().getSelectedItem().isLeaf() )
				{
					sampleIds.add( groupTreeView.getSelectionModel().getSelectedItem().getValue()[ 0 ] );
					groupMap.put( groupTreeView.getSelectionModel().getSelectedItem().getParent().getValue()[ 0 ], sampleIds );
				}
				else
				{
					groupTreeView.getSelectionModel().getSelectedItem().getChildren().forEach( c -> sampleIds.add( c.getValue()[ 0 ] ) );
					groupMap.put( groupTreeView.getSelectionModel().getSelectedItem().getValue()[ 0 ], sampleIds );
				}

				for( String key : groupMap.keySet() )
				{
					System.out.println( key + ":" + groupMap.get(key) );
				}

				ObservableList<TreeItem<BARow>> species = treeTableView.getRoot().getChildren();

				Map<String, Float> refPRIMap = priRefs.stream().filter( c -> c.isOn() ).collect( Collectors.toMap( Experiment.PRIRef::getName, c -> 0f));

				HashMap<String, HashSet<TreeItem< BARow >>> map = new HashMap<>(  );

				for(TreeItem< BARow > pre : root.getChildren())
				{
					String title = pre.getValue().getTitle();

					if ( !map.containsKey( pre.getValue().getTitle() ) )
					{
						map.put( title, new HashSet<>() );
					}

					for( TreeItem< BARow > fa : pre.getChildren() )
					{
						map.get( title ).add( fa );
					}
				}

				// Change offset and
				Event.fireEvent( transmissionCorrectionPane, new ProcessEvent( ProcessEvent.VALIDATION_MACHINE_PERFORMANCE, groupMap,
						species, baMap, refPRIMap, mFaAnionsList, map, machinePerformanceFile ) );
			}
		} );

		groupTreeView.setContextMenu( new ContextMenu( createItem, removeItem, resetItem, new SeparatorMenuItem(), sendGroup, resetGroup, new SeparatorMenuItem(),
				txCorrect, saveTXFunctionParams, resetTxCorrect, new SeparatorMenuItem(), machinPerfCheck ) );

		sendGroup = new MenuItem( "Send Group" );
		sendGroup.setOnAction( new EventHandler< ActionEvent >()
		{
			@Override public void handle( ActionEvent event )
			{
				TreeItem<String[]> item = groupTreeView.getSelectionModel().getSelectedItem();

				if( null != item )
				{
					String groupName;

					if( item.isLeaf() )
					{
						TreeItem<String[]> groupItem = item.getParent();
						groupName = groupItem.getValue()[0];
					}
					else
					{
						groupName = item.getValue()[0];
					}

					//				System.out.println( groupName );

					sendGroupSample( groupName, quantValidationPane,
							treeTableView, collisionEnergyTableView, mFaAnionsList );
				}
			}
		} );

		resetGroup = new MenuItem( "Reset Group" );
		resetGroup.setOnAction( new EventHandler< ActionEvent >()
		{
			@Override public void handle( ActionEvent event )
			{
				if( null != collisionEnergyTableView.getSelectionModel().getSelectedItems() )
				{
					collisionEnergyTableView.getItems().forEach( c ->
					{
						// Fire Reset to selectedSamplesDataView
						Event.fireEvent( quantValidationPane, new ProcessEvent( ProcessEvent.VALIDATION_GROUPING, "Reset" ) );
					} );
					collisionEnergyTableView.getSelectionModel().clearSelection();
				}
			}
		} );

		treeTableView.setContextMenu( new ContextMenu( sendGroup, resetGroup ) );

		ListView<String> cleanSpeciesList = new ListView<String>( cleanSpecies );

		Tab fragmentsTab = new Tab("Fragments", treeTableView);
		fragmentsTab.setClosable( false );

		Tab speciesTab = new Tab("Species", cleanSpeciesList);
		speciesTab.setClosable( false );

		findCleanSpeciesButton.setOnAction( new EventHandler< ActionEvent >()
		{
			@Override public void handle( ActionEvent event )
			{
				if( baMap.size() > 0 )
				{
					HashMap<String, BA> preMap = new HashMap<>(  );
					LinkedHashMap<String, HashSet<TreeItem< BARow >>> map = new LinkedHashMap<>(  );

					for(TreeItem< BARow > pre : root.getChildren())
					{
						String title = pre.getValue().getTitle();

						if ( !map.containsKey( pre.getValue().getTitle() ) )
						{
							map.put( title, new HashSet<>() );
							preMap.put( title, baMap.get(pre));
						}

						for( TreeItem< BARow > fa : pre.getChildren() )
						{
							map.get( title ).add( fa );
						}
					}

					findCleanSpecies( mFaAnionsList, map, preMap );

					priRefs.stream().forEach( c -> c.setOn( cleanSpecies.contains( c.getName() ) ) );

					speciesTab.getTabPane().getSelectionModel().select( speciesTab );
				}
			}
		} );

		ListView<String> excludedSpeciesList = new ListView<String>( excludedSpecies );

		Tab excludedSpeciesTab = new Tab("Excluded", excludedSpeciesList);
		excludedSpeciesTab.setClosable( false );

		checkIntRatioButton.setOnAction( new EventHandler< ActionEvent >()
		{
			@Override public void handle( ActionEvent event )
			{
				System.out.println("Check Intensity Ratio");
				// 1. Iterate only two FAs
				if( baMap.size() > 0 )
				{
					LinkedHashSet< TreeItem< BARow > > baSet = new LinkedHashSet<>(  );

					for(TreeItem< BARow > pre : root.getChildren())
					{
						if(pre.getChildren().size() == 2) {

							baSet.add( pre );
						}
					}

					// 2. Based on XML database calculate FA_ratio at each NCE with 20%
					Event.fireEvent( quantValidationPane, new ProcessEvent( ProcessEvent.VALIDATION_INTENSITY_RATIO_CHECK, baSet, baMap, mFaAnionsList, excludedSpecies, samples ) );
				}

				speciesTab.getTabPane().getSelectionModel().select( excludedSpeciesTab );
			}
		} );

		MenuItem plus = new MenuItem( "+" );
		plus.setOnAction( new EventHandler< ActionEvent >()
		{
			@Override public void handle( ActionEvent event )
			{
				HashMap<String, HashSet<TreeItem< BARow >>> map = new HashMap<>(  );

				for(TreeItem< BARow > pre : root.getChildren())
				{
					String title = pre.getValue().getTitle();

					if ( !map.containsKey( pre.getValue().getTitle() ) )
					{
						map.put( title, new HashSet<>() );
					}

					for( TreeItem< BARow > fa : pre.getChildren() )
					{
						map.get( title ).add( fa );
					}
				}

				setOffset(1, map);
			}
		} );

		MenuItem minus = new MenuItem( "-" );
		minus.setOnAction( new EventHandler< ActionEvent >()
		{
			@Override public void handle( ActionEvent event )
			{
				HashMap<String, HashSet<TreeItem< BARow >>> map = new HashMap<>(  );

				for(TreeItem< BARow > pre : root.getChildren())
				{
					String title = pre.getValue().getTitle();

					if ( !map.containsKey( pre.getValue().getTitle() ) )
					{
						map.put( title, new HashSet<>() );
					}

					for( TreeItem< BARow > fa : pre.getChildren() )
					{
						map.get( title ).add( fa );
					}
				}

				setOffset(-1, map);
			}
		} );


		cleanSpeciesList.setContextMenu( new ContextMenu( plus, minus ) );

		SplitPane leftSplit = new SplitPane( new VBox(20, loadExperiment, new HBox(10, findCleanSpeciesButton, checkIntRatioButton)),
				new LabeledPane( "Samples", sampleListView ));
		leftSplit.setOrientation( Orientation.VERTICAL );
		leftSplit.setDividerPositions( 0.2 );

		SplitPane rightSplit = new SplitPane( new LabeledPane( "PRI references", priRefListView ),
				new LabeledPane( "Group", groupTreeView ));
		rightSplit.setOrientation( Orientation.VERTICAL );

		SplitPane groupSplitPane = new SplitPane( leftSplit, rightSplit );

		SplitPane treeTable = new SplitPane( groupSplitPane,
				new TabPane( fragmentsTab, speciesTab, excludedSpeciesTab ),
				new TitledPane( "Sample Detail", collisionEnergyTableView ) );

		treeTable.setOrientation( Orientation.VERTICAL );
		treeTable.setDividerPositions( 0.5, 0.9 );

		priRefListView.setPrefHeight( 60 );

		this.setText( "Validation" );
		this.setClosable( false );
		this.setContent( treeTable );

		tabPaneProperty().addListener(( observable, oldValue, newValue ) -> {
			if(newValue != null)
				newValue.addEventHandler( COMMAND_VALIDATION, new EventHandler< ProcessEvent >()
				{
					@Override public void handle( ProcessEvent event )
					{
						Object[] params = event.getParam();

						System.out.println(":COMMAND_VALIDATION Received");
						Map<String, String> namedParameters = (Map<String, String>) params[0];
						List<String> unnamedParameters = (List<String>) params[1];

						System.out.println ("\nParameters -");
						for (Map.Entry<String, String> entry : namedParameters.entrySet ())
							System.out.println (entry.getKey () + " : " + entry.getValue ());

						for (String entry : unnamedParameters)
							System.out.print (entry + ", ");
						System.out.println();

						if( namedParameters.containsKey( "merged-file" ) )
						{
							loadingFile = new File( namedParameters.get( "merged-file" ) );

							Experiment.process(outputPath, loadingFile, expName.getText(), root, samples, baMap, priRefs, defaultPriRefs, mFaAnionsList, false);

							final String groupName = "Group-" + (groupRoot.getChildren().size() + 1);

							final TreeItem c = new TreeItem<>( new String[] { groupName, "" } );
							c.setExpanded( true );

							sampleListView.getItems().stream().forEach( item ->
							{
								c.getChildren().add( new TreeItem<>( new String[] { item, "" } ) );
							});

							groupRoot.getChildren().add( c );

							groupTreeView.getSelectionModel().select( c );

							if( namedParameters.containsKey( "output-path" ) )
							{
								outputPath = namedParameters.get( "output-path" );

								String outputFileString = namedParameters.get( "output-path" ).trim() + File.separator + "machine_performance.tsv";

								machinePerformanceFile = new File( outputFileString );

								outputFileString = namedParameters.get( "output-path" ).trim() + File.separator + "txfunction_parameter.tsv";

								transmissionFunctionFile = new File( outputFileString );
							}

							// Check the all the clean species
							findCleanSpeciesButton.fire();

							machinPerfCheck.fire();

							// Store TX.CF function parameters
							saveTXFunctionParams.fire();
						}
					}
				} );
		});
	}

	private void setOffset( float offset, HashMap<String, HashSet<TreeItem<BARow>>> map )
	{
		for( String pre : map.keySet() )
		{
			Optional<BA> ba = baMap.values().stream().filter( c -> pre.equals( c.toString() ) ).findFirst();
			if(ba.isPresent())
				ba.get().setOffset( offset );

			for( TreeItem<BARow> fa : map.get( pre ))
			{
				baMap.get(fa).setOffset( offset );
			}
		}
	}

	private void findCleanSpecies( ObservableList< FAAnion > mFaAnionsList,
			LinkedHashMap< String, HashSet< TreeItem< BARow > > > map, HashMap< String, BA > preMap )
	{
		cleanSpecies.clear();

		Double previousMass = 0d;
		Float previousIntensity = 0f;
		Double cutoff = 2.0d;

		for( String pre : map.keySet() )
		{
			Double currentMass = preMap.get(pre).getMass();
			Optional<Sample> sample = preMap.get(pre).getSamples().stream().findFirst();
			Float ce = sample.get().getFirstKey();

			if( containsMultipleIsomers( pre, map.get(pre), mFaAnionsList ) )
			{
				previousMass = currentMass;
				previousIntensity = sample.get().get(ce);
				continue;
			}

			if( !previousMass.equals( 0d ) ) {
				Double diff = Precision.round( currentMass - previousMass, 1 );
				if( cutoff.equals( diff ) ) {
					if (sample.get().get(ce) < (0.25 * previousIntensity)) {
						System.out.println( pre + " is excluded due to the logic (less than 0.25 * PRI1)");
						previousMass = currentMass;
						previousIntensity = sample.get().get(ce);
						continue;
					}
				}
			}

			previousMass = currentMass;
			previousIntensity = sample.get().get(ce);

			final int size = map.get(pre).size();
			if( size == 1 || size == 2 )
			{
				// Only check Isomer constraint
				System.out.println(pre + " contains 1 or 2 FA(s)");
				cleanSpecies.add( pre );
			}
			else if( size > 2 )
			{
				// Combination check
				System.out.println(pre + " contais 3 FAs");
				if( !containsMultipleValidFAs(map.get(pre)) )
					cleanSpecies.add( pre );
			}
			else
			{
				System.err.println( "Error in findCleanSpecies" );
			}
		}

		cleanSpecies.sort( new Comparator< String >()
		{
			@Override public int compare( String o1, String o2 )
			{
				return o1.compareTo( o2 );
			}
		} );
	}

	private boolean containsMultipleIsomers( String pre, HashSet<TreeItem< BARow >> set, ObservableList< FAAnion > mFaAnionsList )
	{
		for(TreeItem< BARow > row : set)
		{
			if( getFAAnions( mFaAnionsList, row.getValue() ).size() > 1 ) {
				System.out.println(pre + " contains multiple Isomers.");
				return true;
			}
		}

		return false;
	}

	private boolean containsMultipleValidFAs( HashSet<TreeItem< BARow >> set )
	{
		// Check for the first sample right now
		Float sum = 0f;
		HashSet<TreeItem< BARow >> parents = new HashSet<>(  );

		for(TreeItem< BARow > row : set)
		{
			parents.add( row.getParent() );
			BA ba = baMap.get(row);
			Optional<Sample> sample = ba.getSamples().stream().findFirst();

			if(sample.isPresent())
			{
				Float ce = sample.get().getFirstKey();
				sum += sample.get().get( ce );
			}
		}

		int count = 0;
		for(TreeItem< BARow > parent : parents)
		{
			Float faSum = 0f;
			String id = "";
			for(TreeItem< BARow > row : parent.getChildren())
			{
				BA ba = baMap.get(row);
				Optional<Sample> sample = ba.getSamples().stream().findFirst();

				if(sample.isPresent())
				{
					Float ce = sample.get().getFirstKey();
					faSum += sample.get().get( ce );
					id = id + row.getValue().toString() + " ";
				}
			}

			System.out.println( id + "-> sum(FA) / sum(PRI) = " + (faSum / sum) );
			if( faSum / sum > 0.05f ) count++;
		}

		System.out.println( "The count of sum(FA) / sum(PRI) > 0.05 : " + count );
		if(count > 1)
			System.out.println( "The count is bigger than one. The PRI is dropped out." );
		else
			System.out.println( "The count is one. The PRI is accepted." );

		return count > 1;
	}

	private void sendGroupSample( String groupName, SampleValidationPane quantValidationPane, TreeTableView< BARow > baRowTreeTableView, TableView< String[] > tableView, ObservableList< FAAnion > mFaAnionsList )
	{
		if( null != tableView.getSelectionModel().getSelectedItems() && null != baRowTreeTableView.getSelectionModel().getSelectedItem() )
		{
			BARow baRow = baRowTreeTableView.getSelectionModel().getSelectedItem().getValue();
			if(baRow.getTitle().startsWith( "FA-" ))
			{
				String specie = baRowTreeTableView.getSelectionModel().getSelectedItem().getParent().getValue().getTitle();
				Double prMass = baRowTreeTableView.getSelectionModel().getSelectedItem().getParent().getValue().getMass();

				boolean isSym = false;

				// Decide if the selected fragment is symmetric or asymmetric
				if( baRowTreeTableView.getSelectionModel().getSelectedItem().getParent().getChildren().size() == 1 )
					isSym = true;

				// Get the index of FAanion from Fragments
				int carbon = baRowTreeTableView.getSelectionModel().getSelectedItem().getValue().getCarbon();
				int db = baRowTreeTableView.getSelectionModel().getSelectedItem().getValue().getDb();
				double mass = Double.parseDouble( baRow.getMassString() );

				final int index = getIndex( mFaAnionsList, mass, carbon, db );

				boolean finalIsSym = isSym;

				final ArrayList<String[]> sampleData = new ArrayList<>(  );

				tableView.getItems().forEach( c ->
				{
					if ( c != null )
					{
						sampleData.add(
								new String[] {
										groupName,
										specie,
										baRow.getMassString(),
										c[0],
										c[1],
										c[2],
										c[3],
										"0.0", "0.0", "0.0"
								}
						);
					}
				} );

				Event.fireEvent( quantValidationPane, new ProcessEvent( ProcessEvent.VALIDATION_GROUPING, groupName,
						index, finalIsSym, specie, baRow.getTitle(), sampleData, mFaAnionsList, prMass) );
			}
		}
	}

	private void updateCollisionEnergyTableView()
	{
		collisionEnergyTableView.getItems().clear();

		HashSet<String> sampleIds = new HashSet< String >();

		if(null == groupTreeView.getSelectionModel().getSelectedItem() ||
				null == treeTableView.getSelectionModel().getSelectedItem() ) return;

		if( groupTreeView.getSelectionModel().getSelectedItem().isLeaf() )
			sampleIds.add( groupTreeView.getSelectionModel().getSelectedItem().getValue()[0] );
		else
			groupTreeView.getSelectionModel().getSelectedItem().getChildren().forEach( c -> sampleIds.add( c.getValue()[0]) );

		TreeItem< BARow > selectedItem = treeTableView.getSelectionModel().getSelectedItem();

		CO co = null;
		if( selectedItem.getValue().isCoValid() )
		{
			co = ((FA ) baMap.get( selectedItem )).getCO();
		}

		BA ba = baMap.get( selectedItem );

		for( Sample sample : ba.getSamples() )
		{
			if( sampleIds.contains( sample.getId() ))
				for( Float ce : sample.getKeys() )
				{
					collisionEnergyTableView.getItems().add(
							new String[]
									{
											sample.getId(),
											ce.toString(),
											sample.get( ce ).toString(),
											(co == null)? "" : co.getSample( sample.getId() ).get( ce ).toString()
									}
					);
				}
		}
	}

	public static int getIndex( ObservableList< FAAnion > mFaAnionsList, double mass, int carbon, int db )
	{
//		System.out.println( mass + ":carbon - " + carbon + ":db - " + db);

		int found = 0;
		//
		//		for(FAAnion faAnion : mFaAnionsList)
		//		{
		//			if( faAnion.getMass().equals( mass ) && faAnion.getFACarbon().equals( carbon ) && faAnion.getFADoubleBonds().equals( db ) )
		//			{
		//				found = faAnion.getIndex();
		//				break;
		//			}
		//		}
		//
		//		return found;


		Optional<FAAnion> faAnion = mFaAnionsList.stream().filter( c ->
				c.getMass().equals( mass ) &&
						c.getFACarbon().equals( carbon ) &&
						c.getFADoubleBonds().equals( db ) ).findFirst();

		if(faAnion.isPresent())
			found = faAnion.get().getIndex();
		else
			System.err.println( mass + ":carbon - " + carbon + ":db - " + db + " => Not found in FAAnion List!" );

		return found;
	}

	private List<FAAnion> getFAAnions( ObservableList< FAAnion > mFaAnionsList, BARow row )
	{
		return mFaAnionsList.stream().filter( c ->
				c.getMass().equals( row.getMass() ) &&
						c.getFACarbon().equals( row.getCarbon() ) &&
						c.getFADoubleBonds().equals( row.getDb() ) ).collect( Collectors.toList() );
	}

	//	private TableView getTableView()
	//	{
	//		TableView<SampleRow> tableView = new TableView<SampleRow >();
	//
	//		TableColumn columnGrp = new TableColumn("Group");
	//		columnGrp.setCellValueFactory(
	//				new PropertyValueFactory<SampleRow, String>("group"));
	//
	//		columnGrp.setCellFactory( column ->
	//				new TableCell<SampleRow, String>() {
	//					@Override
	//					protected void updateItem(String item, boolean empty) {
	//						super.updateItem(item, empty);
	//
	//						setText(empty ? "" : getItem().toString());
	//						setGraphic(null);
	//
	//						TableRow<SampleRow > currentRow = getTableRow();
	//
	//						if (!isEmpty() && currentRow != null) {
	//							if(item.equals("1"))
	//								currentRow.setStyle("-fx-background-color:lightcoral");
	//							else if(item.equals("2"))
	//								currentRow.setStyle("-fx-background-color:lightgreen");
	//							else if(item.equals(""))
	//								currentRow.setStyle("");
	//						}
	//					}
	//				}
	//		);
	//
	//		TableColumn columnId = new TableColumn("ID");
	//		columnId.setCellValueFactory(
	//				new PropertyValueFactory<SampleRow, String>("id"));
	//
	//		tableView.getColumns().addAll( columnGrp, columnId );
	//
	//		ObservableList<SampleRow > data = FXCollections.observableArrayList();
	//
	//		data.add( new SampleRow( "", "M1_01") );
	//		data.add( new SampleRow( "", "M1_02") );
	//		data.add( new SampleRow( "", "M1_03") );
	//		data.add( new SampleRow( "", "M1_04") );
	//
	//		data.add( new SampleRow( "", "M2_01") );
	//		data.add( new SampleRow( "", "M2_02") );
	//		data.add( new SampleRow( "", "M2_03") );
	//		data.add( new SampleRow( "", "M2_04") );
	//
	//
	//		tableView.setItems( data );
	//
	//		tableView.getSelectionModel().setSelectionMode( SelectionMode.MULTIPLE );
	//
	//		final ContextMenu menu = new ContextMenu();
	//
	//		final MenuItem reset = new MenuItem("Reset");
	//		reset.setOnAction(new EventHandler<ActionEvent >() {
	//			@Override
	//			public void handle(ActionEvent event) {
	//				if( null != tableView.getSelectionModel().getSelectedItems() )
	//				{
	//					tableView.getItems().forEach( c ->
	//					{
	//						if ( c != null )
	//							c.groupProperty().set( "" );
	//					} );
	//					tableView.getSelectionModel().clearSelection();
	//				}
	//			}
	//		});
	//
	//		final MenuItem setGrp1 = new MenuItem("Group 1");
	//		setGrp1.setOnAction(new EventHandler<ActionEvent >() {
	//			@Override
	//			public void handle(ActionEvent event) {
	//				if( null != tableView.getSelectionModel().getSelectedItems() )
	//					tableView.getSelectionModel().getSelectedItems().forEach( c -> {
	//						if(c != null)
	//							c.groupProperty().set( "1" );
	//					} );
	//			}
	//		});
	//
	//		final MenuItem setGrp2 = new MenuItem("Group 2");
	//		setGrp2.setOnAction(new EventHandler<ActionEvent>() {
	//			@Override
	//			public void handle(ActionEvent event) {
	//				if( null != tableView.getSelectionModel().getSelectedItems() )
	//					tableView.getSelectionModel().getSelectedItems().forEach( c -> {
	//
	//						if(c != null)
	//						{
	//							c.groupProperty().set( "2" );
	//						}
	//					} );
	//			}
	//		});
	//
	//		tableView.setRowFactory(tv -> {
	//			DragSelectionRow row = new DragSelectionRow();
	//			row.addEventFilter( MouseEvent.MOUSE_PRESSED, e -> {
	//				if (e.getButton() == MouseButton.SECONDARY) {
	//					e.consume();
	//				}
	//			});
	//			return row ;
	//		});
	//
	//		// disable this menu item if nothing is selected:
	//		//		deleteAllSelectedItem.disableProperty().bind(
	//		//				Bindings.isEmpty(tableView.getSelectionModel().getSelectedItems()));
	//
	//		menu.getItems().addAll(reset, setGrp1, setGrp2);
	//		tableView.setContextMenu(menu);
	//
	//		return tableView;
	//	}

	private String preprocessData( File file )
	{
		loadingFile = file;
		return "";
	}

	public static class DragSelectionRow extends TableRow<String[]>
	{

		public DragSelectionRow() {

			setOnDragDetected( event ->
			{
				startFullDrag();
				setSelection(ValidationTab.DragSelectionRow.this);
			} );

			setOnMouseDragEntered( event ->
			{
				setSelection(ValidationTab.DragSelectionRow.this);
				getTableView().getSelectionModel().clearSelection( getIndex() + 1 );
			} );
		}

		private void setSelection(IndexedCell cell) {
			getTableView().getSelectionModel().select(cell.getIndex());
		}
	}

	public static class DragSelectionCell extends ListCell<String>
	{
		public DragSelectionCell() {

			setOnDragDetected( event ->
			{
				startFullDrag();
				setSelection(ValidationTab.DragSelectionCell.this);
			} );

			setOnMouseDragEntered( event ->
			{
				setSelection(ValidationTab.DragSelectionCell.this);
				getListView().getSelectionModel().clearSelection( getIndex() + 1 );
			} );
		}

		private void setSelection(ListCell cell) {
			getListView().getSelectionModel().select(cell.getIndex());
		}
	}
}
