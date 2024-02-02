package de.mpicbg.ms.model.event;

import javafx.event.Event;
import javafx.event.EventType;

/**
 * Process Event for event-driven pipeline processes
 */

public class ProcessEvent extends Event
{
	public enum ProcessEventType
	{
		CYCLES_DETECTED,
		FRAGMENTS_CREATED,
		DATA_REFINED,
      DATA_EXPORTED,
		SPECIFIC_FRAGMENTS_ADDED,
		REMOVE_TREE_ITEM,
		SAVE_FRAGMENTS,
		LOAD_FRAGMENTS,
		CONVERT_FRAGMENTS_CSV,

		MZ_CALIBRATION,
		DB_SN2_CALIBRATION,
		DB_SYM_CALIBRATION,
		DB_SN1_CALIBRATION,
	}

	public static final EventType<ProcessEvent> ANY = new EventType<>( "PROCESS" );

	public static final EventType<ProcessEvent> DATA_PREP = new EventType<>( ANY, "DATA_PREP" );

	public static final EventType<ProcessEvent> CALIBRATION = new EventType<>( ANY, "CALIBRATION" );

	public static final EventType<ProcessEvent> STORE = new EventType<>( ANY, "STORE" );

	public static final EventType<ProcessEvent> VALIDATION = new EventType<>( ANY, "VALIDATION" );

	public static final EventType<ProcessEvent> QUANTIFICATION = new EventType<>( ANY, "QUANTIFICATION" );

	// Data preparation specific events
	public static final EventType<ProcessEvent> CYCLES_DETECTED = new EventType<>( DATA_PREP, "CYCLES_DETECTED" );
	public static final EventType<ProcessEvent> FRAGMENTS_CREATED = new EventType<>( DATA_PREP, "FRAGMENTS_CREATED" );
	public static final EventType<ProcessEvent> DATA_REFINED = new EventType<>( DATA_PREP, "DATA_REFINED" );
   public static final EventType<ProcessEvent> DATA_EXPORTED = new EventType<>( DATA_PREP, "DATA_EXPORTED" );
	public static final EventType<ProcessEvent> SPECIFIC_FRAGMENTS_ADDED = new EventType<>( DATA_PREP, "SPECIFIC_FRAGMENTS_ADDED" );
	public static final EventType<ProcessEvent> REMOVE_TREE_ITEM = new EventType<>( DATA_PREP, "REMOVE_TREE_ITEM" );
	public static final EventType<ProcessEvent> SAVE_FRAGMENTS = new EventType<>( DATA_PREP, "SAVE_FRAGMENTS" );
	public static final EventType<ProcessEvent> LOAD_FRAGMENTS = new EventType<>( DATA_PREP, "LOAD_FRAGMENTS" );
	public static final EventType<ProcessEvent> CONVERT_FRAGMENTS_CSV = new EventType<>( DATA_PREP, "CONVERT_FRAGMENTS_CSV" );

	// Calibration specific events
	public static final EventType<ProcessEvent> MZ_CALIBRATION = new EventType<>( CALIBRATION, "MZ_CALIBRATION" );
	public static final EventType<ProcessEvent> DB_SN2_CALIBRATION = new EventType<>( CALIBRATION, "DB_SN2_CALIBRATION" );
	public static final EventType<ProcessEvent> DB_SN2_CALIBRATION_STORE = new EventType<>( CALIBRATION, "DB_SN2_CALIBRATION_STORE" );
	public static final EventType<ProcessEvent> DB_SYM_CALIBRATION = new EventType<>( CALIBRATION, "DB_SYM_CALIBRATION" );
	public static final EventType<ProcessEvent> DB_SYM_CALIBRATION_STORE = new EventType<>( CALIBRATION, "DB_SYM_CALIBRATION_STORE" );
	public static final EventType<ProcessEvent> DB_SN1_CALIBRATION = new EventType<>( CALIBRATION, "DB_SN1_CALIBRATION" );
	public static final EventType<ProcessEvent> DB_SN1_CALIBRATION_STORE = new EventType<>( CALIBRATION, "DB_SN1_CALIBRATION_STORE" );

	// Request/response virtual fragment with FA-Index
	public static final EventType<ProcessEvent> DB_VIRT_REQ = new EventType<>( CALIBRATION, "DB_VIRT_REQ" );

	public static final EventType<ProcessEvent> DB_SN2_VIRT_RESP = new EventType<>( CALIBRATION, "DB_SN2_VIRT_RESP" );
	public static final EventType<ProcessEvent> DB_SYM_VIRT_RESP = new EventType<>( CALIBRATION, "DB_SYM_VIRT_RESP" );

	// For the Master Database
	public static final EventType<ProcessEvent> DB_FRAG_REQ = new EventType<>( CALIBRATION, "DB_FRAG_REQ" );

	public static final EventType<ProcessEvent> DB_SN1_FRAG_RESP = new EventType<>( CALIBRATION, "DB_SN1_FRAG_RESP" );
	public static final EventType<ProcessEvent> DB_SN2_FRAG_RESP = new EventType<>( CALIBRATION, "DB_SN2_FRAG_RESP" );
	public static final EventType<ProcessEvent> DB_SYM_FRAG_RESP = new EventType<>( CALIBRATION, "DB_SYM_FRAG_RESP" );

	// Update XML master sheet
	public static final EventType<ProcessEvent> STORE_LIPID_MASTER = new EventType<>( STORE, "STORE_LIPID_MASTER" );
	public static final EventType<ProcessEvent> UPDATE_XML_MASTER = new EventType<>( STORE, "UPDATE_XML_MASTER" );

	// For Validation Events
	public static final EventType<ProcessEvent> VALIDATION_MZ_CORRECTION = new EventType<>( VALIDATION, "VALIDATION_MZ_CORRECTION" );
	public static final EventType<ProcessEvent> VALIDATION_GROUPING = new EventType<>( VALIDATION, "VALIDATION_GROUPING" );
	public static final EventType<ProcessEvent> VALIDATION_TX_CORRECTION = new EventType<>( VALIDATION, "VALIDATION_TX_CORRECTION" );
	public static final EventType<ProcessEvent> VALIDATION_MACHINE_PERFORMANCE = new EventType<>( VALIDATION, "VALIDATION_MACHINE_PERFORMANCE" );
	public static final EventType<ProcessEvent> VALIDATION_RESET_TX_CORRECTION = new EventType<>( VALIDATION, "VALIDATION_RESET_TX_CORRECTION" );
	public static final EventType<ProcessEvent> VALIDATION_SAMPLE = new EventType<>( VALIDATION, "VALIDATION_SAMPLE" );
	public static final EventType<ProcessEvent> VALIDATION_INTENSITY_RATIO_CHECK = new EventType<>( VALIDATION, "VALIDATION_INTENSITY_RATIO_CHECK" );


	// For Validation Events from command line
	public static final EventType<ProcessEvent> COMMAND_VALIDATION = new EventType<>( VALIDATION, "COMMAND_VALIDATION" );
	public static final EventType<ProcessEvent> COMMAND_TX_CORRECTION = new EventType<>( VALIDATION, "COMMAND_TX_CORRECTION" );

	// For Quantification Events
	public static final EventType<ProcessEvent> QUANTIFICATION_RESET = new EventType<>( QUANTIFICATION, "QUANTIFICATION_RESET" );
	public static final EventType<ProcessEvent> QUANTIFICATION_PROCESS = new EventType<>( QUANTIFICATION, "QUANTIFICATION_PROCESS" );

	// For Quantification Events from command line
	public static final EventType<ProcessEvent> COMMAND_QUANTIFICATION = new EventType<>( QUANTIFICATION, "COMMAND_QUANTIFICATION" );

	final private EventType<ProcessEvent> eventType;
	final private Object[] param;

	public ProcessEvent(EventType<ProcessEvent> eventType, Object... param)
	{
		super(eventType);
		this.eventType = eventType;
		this.param = param;
	}

	public ProcessEvent(EventType<ProcessEvent> eventType)
	{
		this(eventType, null);
	}

	public static <T extends ProcessEvent> ProcessEventType getProcessEventType( EventType<T> type )
	{
		return ProcessEventType.valueOf( type.getName() );
	}

	@Override
	public EventType<ProcessEvent> getEventType()
	{
		return eventType;
	}

	public Object[] getParam()
	{
		return param;
	}
}
