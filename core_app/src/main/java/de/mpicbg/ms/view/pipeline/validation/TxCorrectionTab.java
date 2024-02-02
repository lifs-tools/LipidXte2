package de.mpicbg.ms.view.pipeline.validation;

import de.mpicbg.ms.Pipeline;
import de.mpicbg.ms.model.SampleEstimation;
import de.mpicbg.ms.view.pane.component.LabeledPane;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.TreeMap;

import static de.mpicbg.ms.model.SampleEstimation.getSelectedTxCFunction;
import static de.mpicbg.ms.model.SampleEstimation.setSelectedTxCFunction;
import static de.mpicbg.ms.model.SampleEstimation.setTxCFunctionParamMap;

/**
 * Author: HongKee Moon (moon@mpi-cbg.de), Scientific Computing Facility
 * Organization: MPI-CBG Dresden
 * Date: May 2017
 */
public class TxCorrectionTab extends Tab
{
	public TxCorrectionTab()
	{
		setText( "Tx Correct" );
		setClosable( false );

		final TextArea txFunctionsTextArea = new TextArea();

		String tcFunctionsString = null;
		String tcFunctionsFile = "tcfunctions.txt";

		HashMap<SampleEstimation.TxCorrectionFunc, String> functionParamMap = new HashMap<>(  );

		try
		{
			if( Pipeline.isExist( tcFunctionsFile ) )
				tcFunctionsString = Pipeline.loadFile( tcFunctionsFile );
			else
				tcFunctionsString = IOUtils.toString( getClass().getResourceAsStream( tcFunctionsFile ) );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}

		boolean bSaveFirst = false;
		if( tcFunctionsString.indexOf( '$' ) < 0 )
		{
			bSaveFirst = true;
			try
			{
				tcFunctionsString = IOUtils.toString( getClass().getResourceAsStream( tcFunctionsFile ) );
			}
			catch ( IOException e )
			{
				e.printStackTrace();
			}
		}

		String[] functions = tcFunctionsString.split( "\\$" );

		for(String function : functions)
		{
			if(function.indexOf( '|' ) < 0)
				continue;

			String[] funcMap = function.split( "\\|" );

			functionParamMap.put( SampleEstimation.TxCorrectionFunc.valueOf( funcMap[0].trim() ), funcMap[1].trim() );
		}

		if( bSaveFirst )
		{
			saveFunctionFile( tcFunctionsFile, functionParamMap );
		}

		txFunctionsTextArea.setText( tcFunctionsString );

		final ToggleGroup functionGroup = new ToggleGroup();
		GridPane functionsPane = new GridPane();
		functionsPane.setHgap( 15 );
		functionsPane.setVgap( 10 );

		RadioButton expDecayFunction = new RadioButton( "Exp. Decay Function : A * Exp( - x / B)" );
		expDecayFunction.setToggleGroup( functionGroup );

		RadioButton simpleExpFunction = new RadioButton( "Simple Exp. Function : A * X ^ B");
		simpleExpFunction.setToggleGroup( functionGroup );

		functionGroup.selectedToggleProperty().addListener( new ChangeListener< Toggle >()
		{
			@Override public void changed( ObservableValue< ? extends Toggle > observable, Toggle oldValue, Toggle newValue )
			{
				SampleEstimation.TxCorrectionFunc func = null;
				if( newValue.equals( expDecayFunction ) )
				{
					func = SampleEstimation.TxCorrectionFunc.ExpDecay;
				}
				else if( newValue.equals( simpleExpFunction ) )
				{
					func = SampleEstimation.TxCorrectionFunc.SimpleExp;
				}

				txFunctionsTextArea.setText( functionParamMap.get( func ) );

				TreeMap<Float, double[]> parameterMap = tryParseTxFunctionString( functionParamMap.get( func ) );
				setTxCFunctionParamMap( parameterMap ) ;
				setSelectedTxCFunction( func );
			}
		} );

		//expDecayFunction.setSelected( true );
		simpleExpFunction.setSelected( true );

		functionsPane.add( expDecayFunction, 0, 0);
		functionsPane.setMargin( expDecayFunction, new Insets( 9 ) );
		functionsPane.add( simpleExpFunction, 0, 1);
		functionsPane.setMargin( simpleExpFunction, new Insets( 9 ) );

		Button applyBtn = new Button( "Apply" );
		applyBtn.setOnAction( new EventHandler< ActionEvent >()
		{
			@Override public void handle( ActionEvent event )
			{

				SampleEstimation.TxCorrectionFunc func = null;
				if( expDecayFunction.isSelected() )
				{
					func = SampleEstimation.TxCorrectionFunc.ExpDecay;
				}
				else if( simpleExpFunction.isSelected() )
				{
					func = SampleEstimation.TxCorrectionFunc.SimpleExp;
				}

				functionParamMap.put( func, txFunctionsTextArea.getText().trim() );

				TreeMap<Float, double[]> parameterMap = tryParseTxFunctionString( functionParamMap.get( func ) );

				if( null != parameterMap )
				{
					setTxCFunctionParamMap( parameterMap ) ;

					saveFunctionFile( tcFunctionsFile, functionParamMap );
				}

				System.out.println( "Parameters are applied for " + getSelectedTxCFunction() + "." );
			}
		} );

		SplitPane txCorrectionSplitPane = new SplitPane(
				new LabeledPane( "Functions", functionsPane ),
				new LabeledPane( "Parameters", new VBox( 9.0, applyBtn, txFunctionsTextArea ) ) );

		txCorrectionSplitPane.setOrientation( Orientation.VERTICAL );
		txCorrectionSplitPane.setDividerPositions( 0.3 );

		setContent( txCorrectionSplitPane );
	}

	private void saveFunctionFile( String tcFunctionsFile, HashMap< SampleEstimation.TxCorrectionFunc, String > functionParamMap )
	{
		StringBuilder sb = new StringBuilder();
		for ( SampleEstimation.TxCorrectionFunc func : functionParamMap.keySet() )
		{
			sb.append( func );
			sb.append( "|\n" );
			sb.append( functionParamMap.get( func ) );
			sb.append( "\n$\n" );
		}
		Pipeline.saveFile( tcFunctionsFile, sb.toString() );
	}

	public static TreeMap< Float, double[] > tryParseTxFunctionString( String newValue )
	{
		TreeMap< Float, double[] > parameterMap = new TreeMap<>(  );

		StringReader reader = new StringReader( newValue );
		CSVParser parser = null;

		try
		{
			parser = CSVFormat.TDF.withHeader().withNullString( "" ).parse( reader );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}

		if ( null != parser )
		{
			parser.forEach( c ->
			{
				Float ce = Float.parseFloat( c.get( "CE" ) );
				double a = Double.parseDouble( c.get( "A" ) );
				double b = Double.parseDouble( c.get( "B" ) );

				parameterMap.put( ce, new double[]{ a, b } );

				//System.out.println( ce + " A=" + a + ", B=" + b );
			});
		}

		if( parameterMap.size() > 0 ) return parameterMap;
		else return null;
	}
}
