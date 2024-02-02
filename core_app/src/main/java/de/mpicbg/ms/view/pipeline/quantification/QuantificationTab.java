package de.mpicbg.ms.view.pipeline.quantification;

import de.mpicbg.ms.util.Data;
import de.mpicbg.ms.model.event.ProcessEvent;
import de.mpicbg.ms.model.data.BA;
import de.mpicbg.ms.model.data.BARow;
import de.mpicbg.ms.model.data.FAAnion;
import de.mpicbg.ms.model.event.Quant;
import de.mpicbg.ms.view.pane.MasterXmlPane;
import de.mpicbg.ms.view.pane.component.LabeledPane;
import de.mpicbg.ms.view.pipeline.common.Experiment;

import de.mpicbg.ms.util.TableViewUtil;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.chart.LineChart;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableRow;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;

import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Callback;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static de.mpicbg.ms.model.SampleEstimation.setTxCFunctionMapForSimpleExp;

/**
 * Author: HongKee Moon (moon@mpi-cbg.de), Scientific Computing Facility
 * Organization: MPI-CBG Dresden
 * Date: January 2017
 */
public class QuantificationTab extends Tab
{
	private final HashMap<TreeItem< BARow >, BA> baMap;

	private final ListView<String> sampleListView;
	private final ObservableList<String> samples;

	private final ObservableList< Experiment.PRIRef > priRefs;

	private final TreeTableView<String[]> groupTreeView;
	private final TreeItem<String[]> groupRoot;

	private TreeItem<BARow> root;
	private HashSet< Experiment.StandardReference > defaultPriRefs = new HashSet<>(  );

	private File loadingFile;
	private File outputFile;
	private File txfunctionParameterFile;
	private String outputPath;

	public QuantificationTab( LineChart< Number, Number > chart, LineChart< Number, Number > correctionFactorChart,
			LineChart< Number, Number > extraChart, MasterXmlPane masterXmlPane, ObservableList< FAAnion > mFaAnionsList, QuantificationPane quantificationPane )
	{
		baMap = new HashMap<>();
		samples = FXCollections.observableArrayList();

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

		root = new TreeItem<>(new BARow( "" ));

		groupTreeView = TableViewUtil.createTreeDataViewForSamplePattern( new String[] { "Group", "Pattern"} );
		groupTreeView.setEditable( true );
		groupTreeView.setShowRoot( false );
		groupTreeView.setRoot( groupRoot );

		CheckBox removeIsomerInfoCheckBox = new CheckBox( "Remove Isomer Info" );

		Button loadExperiment = new Button( "Load experiment" );
		loadExperiment.setMinHeight( 35 );
		loadExperiment.setStyle("-fx-base: #76e7bf;");

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

					Experiment.process( outputPath, loadingFile, expName.getText(), root, samples, baMap, priRefs, defaultPriRefs, mFaAnionsList, removeIsomerInfoCheckBox.isSelected());
				}
			}
		} );

		VBox expNameBox = new VBox( 1.2, new Label( "Experiment Name: " ), expName );
		expNameBox.setAlignment( Pos.CENTER_LEFT );

		Button autoGroup = new Button( "Group samples" );

		autoGroup.setOnAction( new EventHandler< ActionEvent >()
		{
			@Override public void handle( ActionEvent event )
			{
				// Iterate them and put sample id into proper group
				groupRoot.getChildren().forEach( c ->
				{
					String pattern = c.getValue()[1];
					Pattern r = Pattern.compile(pattern);

					for(String sample : samples)
					{
						if( r.matcher( sample ).matches() )
						{
							c.getChildren().add( new TreeItem<>( new String[] { sample, "" } ) );
						}
					}
				});
			}
		} );

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


		Button clearBtn = new Button( "Clear" );
		clearBtn.setMinHeight( 35 );
		clearBtn.setStyle("-fx-base: #e79497;");
		clearBtn.setOnAction( new EventHandler< ActionEvent >()
		{
			@Override public void handle( ActionEvent event )
			{
				Event.fireEvent( quantificationPane, new ProcessEvent( ProcessEvent.QUANTIFICATION_RESET ) );

				root.getChildren().clear();
				groupRoot.getChildren().clear();
				samples.clear();
				baMap.clear();

				priRefs.clear();
				defaultPriRefs.clear();
			}
		} );


		final ToggleGroup processGroup = new ToggleGroup();

		RadioButton intensityRadioButton = new RadioButton( "Intensity" );
		intensityRadioButton.setToggleGroup( processGroup );
		intensityRadioButton.setSelected( true );

		RadioButton profileRadioButton = new RadioButton( "Profile" );
		profileRadioButton.setToggleGroup( processGroup );

		RadioButton quantityRadioButton = new RadioButton( "Quantity" );
		quantityRadioButton.setToggleGroup( processGroup );

		CheckBox removeReferenceCheckBox = new CheckBox( "Remove Ref." );
		CheckBox summarizeNCECheckBox = new CheckBox( "Summarize NCE" );
		CheckBox noCorrectionCheckBox = new CheckBox( "No Correction" );
		CheckBox groupOnlyCheckBox = new CheckBox( "Group Only" );
//		CheckBox mergeUnspecifiedIsomerCheckBox = new CheckBox( "Merge if isomer unspecified" );
		CheckBox intensityRatioCheckRemoveCheckBox = new CheckBox( "Intensity Ratio Check/Remove" );
		CheckBox mergeGlobalHomogeneousCheckBox = new CheckBox( "Global Homogeneous Merge" );
		//CheckBox applyTXCFinSummary = new CheckBox( "TX.CF applied in Summary" );

		final ToggleGroup outputGroup = new ToggleGroup();

		RadioButton processAll = new RadioButton( "All" );
		processAll.setToggleGroup( outputGroup );
		processAll.setSelected( true );

		RadioButton processSum = new RadioButton( "Sum");
		processSum.setToggleGroup( outputGroup );

		RadioButton processMspecies = new RadioButton( "Mspecies" );
		processMspecies.setToggleGroup( outputGroup );

		Button quantifyButton = new Button( "Quantify" );
		quantifyButton.setMinSize( 120, 40 );
		quantifyButton.setStyle("-fx-font: 18 arial; -fx-base: #43a5e7;");

		final ProgressIndicator piQuantify = new ProgressIndicator( 0 );
		piQuantify.setMinSize( 40, 40 );

		quantifyButton.setOnAction( new EventHandler< ActionEvent >()
		{
			@Override public void handle( ActionEvent event )
			{
				TreeMap< String, ArrayList<String> > groupMap = new TreeMap< String, ArrayList< String > >(  );

				for ( TreeItem< String[] > groupItem : groupTreeView.getRoot().getChildren() )
				{
					String groupKey = groupItem.getValue()[0];
					groupMap.put( groupKey, new ArrayList<String>() );

					for ( TreeItem< String[] > sampleItem : groupItem.getChildren() )
					{
						groupMap.get( groupKey ).add( sampleItem.getValue()[0] );
					}
				}

				ObservableList<TreeItem<BARow>> species = root.getChildren();

				Map<String, Float> refPRIMap = priRefs.stream().filter( c -> c.isOn() ).collect( Collectors.toMap( Experiment.PRIRef::getName, c -> {
					if( c instanceof Experiment.StandardReference )
						return ( ( Experiment.StandardReference ) c ).quantity;
					else return 0f;
				}));

				RadioButton toggle = (RadioButton) processGroup.getSelectedToggle();
				String processName = toggle.getText();

				Quant.Option processOption = Quant.Option.valueOf( processName );

				toggle = (RadioButton) outputGroup.getSelectedToggle();
				processName = toggle.getText();

				Quant.Output outputOption = Quant.Output.valueOf( processName );

				EnumSet<Quant.AdditionalOption> additionalOptions = EnumSet.noneOf( Quant.AdditionalOption.class );

				if(removeReferenceCheckBox.isSelected()) additionalOptions.add( Quant.AdditionalOption.RemoveReference );
				if(summarizeNCECheckBox.isSelected()) additionalOptions.add( Quant.AdditionalOption.SummarizeNCE );
				if(noCorrectionCheckBox.isSelected()) additionalOptions.add( Quant.AdditionalOption.NoCorrection );
				if(groupOnlyCheckBox.isSelected()) additionalOptions.add( Quant.AdditionalOption.GroupOnly );
//				if(mergeUnspecifiedIsomerCheckBox.isSelected()) additionalOptions.add( Quant.AdditionalOption.MergeUnspecifiedIsomer );
				if(intensityRatioCheckRemoveCheckBox.isSelected()) additionalOptions.add( Quant.AdditionalOption.IntensityCheckRemove );
				if(mergeGlobalHomogeneousCheckBox.isSelected()) additionalOptions.add( Quant.AdditionalOption.MergeGlobalHomogeneous );
//				if(applyTXCFinSummary.isSelected()) additionalOptions.add( Quant.AdditionalOption.ApplyTXCFinSummary );
				if(removeIsomerInfoCheckBox.isSelected()) additionalOptions.add( Quant.AdditionalOption.RemoveIsomerInfo );

				Event.fireEvent( quantificationPane, new ProcessEvent( ProcessEvent.QUANTIFICATION_PROCESS, groupMap,
						species, baMap, refPRIMap, mFaAnionsList, processOption, outputOption, additionalOptions, piQuantify, loadingFile, outputFile ) );
			}
		} );

		Button resetButton = new Button( "Reset" );
		resetButton.setMinSize( 120, 40 );
		resetButton.setStyle("-fx-font: 18 arial; -fx-base: #e77d8c;");

		resetButton.setOnAction( new EventHandler< ActionEvent >()
		{
			@Override public void handle( ActionEvent event )
			{
				Event.fireEvent( quantificationPane, new ProcessEvent( ProcessEvent.QUANTIFICATION_RESET ) );
			}
		} );

		GridPane quantOptions = new GridPane();
		quantOptions.setHgap( 15 );
		quantOptions.setVgap( 20 );

		// Process methods
		quantOptions.add( intensityRadioButton, 0, 0);
		quantOptions.add( profileRadioButton, 0, 1);
		quantOptions.add( quantityRadioButton, 0, 2);

		// Process options
		quantOptions.add( removeReferenceCheckBox, 1, 0);
		quantOptions.add( summarizeNCECheckBox, 1, 1);
		quantOptions.add( noCorrectionCheckBox, 1, 2);
		quantOptions.add( groupOnlyCheckBox, 1, 3);
//		quantOptions.add( mergeUnspecifiedIsomerCheckBox, 1, 4);
		quantOptions.add( intensityRatioCheckRemoveCheckBox, 1, 4);
		quantOptions.add( mergeGlobalHomogeneousCheckBox, 1, 5);
		//quantOptions.add( applyTXCFinSummary, 1, 3);


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


		MenuItem quantProcess = new MenuItem( "Process" );
		quantProcess.setOnAction( new EventHandler< ActionEvent >()
		{
			@Override public void handle( ActionEvent event )
			{
				quantifyButton.fire();
			}
		} );

		MenuItem quantReset = new MenuItem( "Reset" );
		quantReset.setOnAction( new EventHandler< ActionEvent >()
		{
			@Override public void handle( ActionEvent event )
			{
				resetButton.fire();
			}
		} );

		groupTreeView.setContextMenu( new ContextMenu( createItem, removeItem, resetItem, new SeparatorMenuItem(), quantProcess, quantReset ) );

		SplitPane groupSplitPane = new SplitPane(
				new LabeledPane( "Samples", sampleListView ),
				new LabeledPane( "Group", groupTreeView ) );

		TitledPane quantPane = new TitledPane( "Quantification Options", quantOptions );
		quantPane.setCollapsible( false );

		VBox outputOptions = new VBox( 20, processAll, processSum, processMspecies );
		TitledPane outputPane = new TitledPane( "Output Options", outputOptions );
		outputPane.setCollapsible( false );

		SplitPane contentPane = new SplitPane( groupSplitPane, new VBox( 20, new HBox( quantPane, outputPane ), new HBox( 10, quantifyButton, piQuantify), resetButton ) );
		contentPane.setOrientation( Orientation.VERTICAL );
		contentPane.setDividerPositions( 0.7 );

		priRefListView.setPrefHeight( 150 );
		priRefListView.setOnDragOver( new EventHandler< DragEvent >()
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

		priRefListView.setOnDragDropped( new EventHandler< DragEvent >()
		{
			@Override public void handle( DragEvent event )
			{
				Dragboard db = event.getDragboard();
				boolean success = false;
				if ( db.hasFiles() )
				{
					success = true;

					for ( File file : db.getFiles() )
					{
						// Process file
						//						System.out.println(file);
						processPriReference( file );
					}
				}
				event.setDropCompleted( success );
				event.consume();
			}
		} );

		VBox startButtons = new VBox(10, removeIsomerInfoCheckBox, new HBox( 5, loadExperiment, clearBtn), expNameBox, autoGroup);
		startButtons.setPadding( new Insets( 10 ) );

		SplitPane topPane = new SplitPane(
				startButtons,
				new LabeledPane( "PRI references(Drag & Drop the csv file)", priRefListView )
		);

		BorderPane borderPane = new BorderPane();
		borderPane.setTop( topPane );
		borderPane.setCenter( contentPane );

		this.setText( "Quantification" );
		this.setClosable( false );
		this.setContent( borderPane );

		tabPaneProperty().addListener( new ChangeListener< TabPane >()
		{
			@Override public void changed( ObservableValue< ? extends TabPane > observable, TabPane oldValue, TabPane newValue )
			{
				if(newValue != null)
					newValue.addEventHandler( ProcessEvent.COMMAND_QUANTIFICATION, new EventHandler< ProcessEvent >()
					{
						@Override public void handle( ProcessEvent event )
						{
							Object[] params = event.getParam();
//							System.out.println(":COMMAND_QUANTIFICATION Received");
							Map<String, String> namedParameters = (Map<String, String>) params[0];
							List<String> unnamedParameters = (List<String>) params[1];

							System.out.println ("\nParameters -");
							for (Map.Entry<String, String> entry : namedParameters.entrySet ())
								System.out.println (entry.getKey () + " : " + entry.getValue ());

							for (String entry : unnamedParameters)
								System.out.print (entry + ", ");
							System.out.println();

							// Handle the options
							String outputOptionString = "";
							if( namedParameters.containsKey( "output-option" ) )
							{
								switch ( namedParameters.get( "output-option" ) )
								{
									case "Sum":
										processSum.setSelected( true );
										break;
									case "Mspecies":
										processMspecies.setSelected( true );
										break;
									case "All":
										processAll.setSelected( true );
										break;
								}
								outputOptionString = "_" + namedParameters.get( "output-option" );
							}

							String quantOptionString = "";
							if( namedParameters.containsKey( "quant-option" ) )
							{
								switch ( namedParameters.get( "quant-option" ) )
								{
									case "Intensity":
										intensityRadioButton.setSelected( true );
										break;
									case "Profile":
										profileRadioButton.setSelected( true );
										break;
									case "Quantity":
										quantityRadioButton.setSelected( true );
										break;
								}
								quantOptionString = "_" + namedParameters.get( "quant-option" );
							}

							StringBuilder processOptionStringBuilder = new StringBuilder(  );
							if( unnamedParameters.size() > 0 )
							{
								processOptionStringBuilder.append( "(" );
								for(String opt : unnamedParameters)
								{
									if(!processOptionStringBuilder.toString().equals( "(" ))
										processOptionStringBuilder.append( "_" );

									switch ( opt )
									{
										case "RemoveRef":
											removeReferenceCheckBox.setSelected( true );
											break;
										case "SummarizeNCE":
											summarizeNCECheckBox.setSelected( true );
											break;
										case "NoCorrection":
											noCorrectionCheckBox.setSelected( true );
											break;
										case "GroupOnly":
											groupOnlyCheckBox.setSelected( true );
											break;
//										case "IntensityRatioCheck":
//											intensityRatioCheckRemoveCheckBox.setSelected( true );
//											break;
									}

									processOptionStringBuilder.append( opt );
								}
								processOptionStringBuilder.append( ")" );
							}

							// IntensityRatioCheck is the default behavior for command line process
                     // IntensityRatioCheck
//							intensityRatioCheckRemoveCheckBox.setSelected( true );

							if( namedParameters.containsKey( "standard-list" ) )
							{
								processPriReference( new File(namedParameters.get("standard-list") ) );

								if( namedParameters.containsKey( "merged-file" ) && namedParameters.containsKey( "output-path" ) )
								{
									loadingFile = new File( namedParameters.get( "merged-file" ) );
									outputPath = namedParameters.get( "output-path" ).trim() + File.separator;

									Experiment.process(outputPath, loadingFile, expName.getText(), root, samples, baMap, priRefs, defaultPriRefs, mFaAnionsList, true);

									if ( namedParameters.containsKey( "group1" )
											|| namedParameters.containsKey( "group2" )
											|| namedParameters.containsKey( "group3" )) {

										for(int i = 1; i < 4; i++) {
											String group = "group" + i;
											if ( namedParameters.containsKey( group ) ) {
												String items = namedParameters.get( group );
												addGroupNode( groupRoot, sampleListView, i, items );
											}
										}
									}
									else
									{
										final String groupName = "Group-" + (groupRoot.getChildren().size() + 1);

										final TreeItem c = new TreeItem<>( new String[] { groupName, "" } );
										c.setExpanded( true );

										sampleListView.getItems().stream().forEach( item ->
										{
											c.getChildren().add( new TreeItem<>( new String[] { item, "" } ) );
										});

										groupRoot.getChildren().add( c );
									}

									// Define output File
									String outputFileString = outputPath + "output" + quantOptionString + outputOptionString + processOptionStringBuilder.toString() + ".tsv";

									outputFile = new File( outputFileString );

									if( checkMachinePerformance() )
									{
										txfunctionParameterFile = new File( outputPath + "txfunction_parameter.tsv" );
										updateTxFunctionParametersWithFile( txfunctionParameterFile );
									}

									quantifyButton.fire();
								}
							}
						}
					});
			}
		} );
	}

	private static void updateTxFunctionParametersWithFile(final File txfunctionParameterFile)
	{
		if ( txfunctionParameterFile.exists() ) {
			System.out.println("Transmission Correction Parameter File is found");

			String str = null;

			try
			{
				str = FileUtils.readFileToString( txfunctionParameterFile );
			}
			catch ( IOException e )
			{
				e.printStackTrace();
			}

			if ( str != null )
			{
				System.out.println("The below parameters are used:");
				System.out.println(str);
				setTxCFunctionMapForSimpleExp(str);
			}
		} else {
			System.err.println("Transmission Correction Parameter File does not exist.");
		}
	}

	private static void addGroupNode( TreeItem< String[] > groupRoot, ListView< String > sampleListView, int groupNo, String items )
	{
		String[] tokens = items.split( "," );

		final String groupName = "Group-" + groupNo;
		final TreeItem c = new TreeItem<>( new String[] { groupName, "" } );
		c.setExpanded( true );

		for(String token : tokens)
		{
			if( !token.trim().equals( "" ) )
			{
				String exp = token.trim();
				if( sampleListView.getItems().contains( exp ) ) {
					c.getChildren().add( new TreeItem<>( new String[] { exp, "" } ) );
				}
			}
		}

		if(c.getChildren().size() > 0)
			groupRoot.getChildren().add( c );
	}

	private void processPriReference( File file )
	{
		priRefs.clear();

		Data data = new Data( file );

		for( CSVRecord record : data.getRecords() )
		{
			Float qty = 0f;
			if( !record.get( "QUANTITY").isEmpty() )
				qty = Float.parseFloat( record.get( "QUANTITY") );

			String specie = record.get( "SPECIE" );
			String molSpecie = record.get( "MolSPECIE" );

			if( specie.startsWith( "PC O-" )) {
				specie = specie.replace( "PC O-", "PCO" );
				molSpecie = molSpecie.replace( "PC O-", "PCO" );
			} if( specie.startsWith( "PE O-" )) {
				specie = specie.replace( "PE O-", "PEO" );
				molSpecie = molSpecie.replace( "PE O-", "PEO" );
			}

			Experiment.StandardReference ref = new Experiment.StandardReference( specie, molSpecie, qty );
			defaultPriRefs.add( ref );

//			System.out.println( record.get( "SPECIE" ) );
			if( !priRefs.contains( ref ) )
				priRefs.add( ref );
		}

		// Fill up the GUI with the precursor data
//		for( PR precursor : experiment.values() )
//		{
//			// Add PRI reference item into the ListView of PRI
//			ValidationTab.PRIRef priRef = new ValidationTab.PRIRef( precursor.getSpecie(), false );
//			if( defaultPriRefs.contains( priRef.getName() ) ) priRef.setOn( true );
//			priRefs.add( priRef );
//
//			// Create a PRI tree item
//			final TreeItem< BARow > prItem = new TreeItem<>( new BARow( precursor ) );
//			baMap.put( prItem, precursor );
//
//			int i = 1;
//			for( FA faanion : precursor.getFAs())
//			{
//				final TreeItem< BARow > faItem = new TreeItem<>( new BARow( faanion, i++ ) );
//				baMap.put( faItem, faanion );
//
//				prItem.getChildren().add( faItem );
//			}
//		}
	}

	private void updateCollisionEnergyTableView()
	{
//		collisionEnergyTableView.getItems().clear();
//
//		HashSet<String> sampleIds = new HashSet< String >();
//
//		if(null == groupTreeView.getSelectionModel().getSelectedItem() ||
//				null == treeTableView.getSelectionModel().getSelectedItem() ) return;
//
//		if( groupTreeView.getSelectionModel().getSelectedItem().isLeaf() )
//			sampleIds.add( groupTreeView.getSelectionModel().getSelectedItem().getValue()[0] );
//		else
//			groupTreeView.getSelectionModel().getSelectedItem().getChildren().forEach( c -> sampleIds.add( c.getValue()[0]) );
//
//		TreeItem< BARow > selectedItem = treeTableView.getSelectionModel().getSelectedItem();
//
//		CO co = null;
//		if( selectedItem.getValue().isCoValid() )
//		{
//			co = ((FA) baMap.get( selectedItem )).getCO();
//		}
//
//		BA ba = baMap.get( selectedItem );
//
//		for( Sample sample : ba.getSamples() )
//		{
//			if( sampleIds.contains( sample.getId() ))
//				for( Float ce : sample.getKeys() )
//				{
//					collisionEnergyTableView.getItems().add(
//							new String[]
//									{
//											sample.getId(),
//											ce.toString(),
//											sample.get( ce ).toString(),
//											(co == null)? "" : co.getSample( sample.getId() ).get( ce ).toString()
//									}
//					);
//				}
//		}
	}

	private String preprocessData( File file )
	{
		loadingFile = file;
		Set<String> headers = Data.getHeaders( file );

		for(String s : headers)
		{
			if(s.startsWith( "PRI:" ))
			{
//				if( s.indexOf( ".mzXML@" ) > 0 )
//					return s.substring( 4, s.indexOf( ".mzXML@" ) );
//				else
					return "";
			}
		}
		return "";
	}

	private boolean checkMachinePerformance()
	{
		File machinePerformanceFile = new File(outputPath + "machine_performance.tsv");

		if(machinePerformanceFile.exists())
		{
			String firstLine = Data.getFirstLine( machinePerformanceFile );
			//				System.out.println(firstLine);

			String[] split = firstLine.split( "\t" );

			if( !split[2].equals( "-Infinity" )  )
			{
				return true;
			}
		}

		return false;
	}

	public static class DragSelectionRow extends TableRow<String[]> {

		public DragSelectionRow() {

			setOnDragDetected( event ->
			{
				startFullDrag();
				setSelection(DragSelectionRow.this);
			} );

			setOnMouseDragEntered( event ->
			{
				setSelection(DragSelectionRow.this);
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
				setSelection(DragSelectionCell.this);
			} );

			setOnMouseDragEntered( event ->
			{
				setSelection(DragSelectionCell.this);
				getListView().getSelectionModel().clearSelection( getIndex() + 1 );
			} );
		}

		private void setSelection(ListCell cell) {
			getListView().getSelectionModel().select(cell.getIndex());
		}
	}
}
