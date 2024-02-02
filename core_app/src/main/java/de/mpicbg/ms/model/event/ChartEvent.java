package de.mpicbg.ms.model.event;

import javafx.event.Event;
import javafx.event.EventType;

/**
 * ChartEvent
 */
public class ChartEvent extends Event
{
	public enum ChartEventType
	{
		UPDATE_CE,
		CLEAR,
		CLEAR_CHART,
		DRAW_CHART,
		DRAW_SPECIFIC_MZ_CHART,
		CLEAR_SPECIFIC_MZ_CHART
	}

	public static final EventType<ChartEvent> ANY = new EventType<>( "CHART" );

	// Update collision energy
	public static final EventType<ChartEvent> UPDATE_CE = new EventType<>( ANY, "UPDATE_CE" );
	public static final EventType<ChartEvent> CLEAR = new EventType<>( ANY, "CLEAR" );
	public static final EventType<ChartEvent> CLEAR_CHART = new EventType<>( ANY, "CLEAR_CHART" );
	public static final EventType<ChartEvent> DRAW_CHART = new EventType<>( ANY, "DRAW_CHART" );

	public static final EventType<ChartEvent> DRAW_SPECIFIC_MZ_CHART = new EventType<>( ANY, "DRAW_SPECIFIC_MZ_CHART" );
	public static final EventType<ChartEvent> CLEAR_SPECIFIC_MZ_CHART = new EventType<>( ANY, "CLEAR_SPECIFIC_MZ_CHART" );


	final private EventType<ChartEvent> eventType;
	final private Object targetValue;

	public ChartEvent(EventType<ChartEvent> eventType, Object targetValue)
	{
		super(eventType);
		this.eventType = eventType;
		this.targetValue = targetValue;
	}

	public static <T extends ChartEvent> ChartEventType getChartEventType( EventType<T> type )
	{
		return ChartEventType.valueOf( type.getName() );
	}

	@Override
	public EventType<ChartEvent> getEventType()
	{
		return eventType;
	}

	public Object getTargetValue()
	{
		return targetValue;
	}
}
