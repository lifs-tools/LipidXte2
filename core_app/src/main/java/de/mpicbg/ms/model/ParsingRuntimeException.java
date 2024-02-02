package de.mpicbg.ms.model;


/**
 * Parsing runtime exception class.
 */
public class ParsingRuntimeException extends java.lang.RuntimeException
{

	private static final long serialVersionUID = 1L;

	/**
	 * <p>
	 * Constructor for ParsingRuntimeException.
	 * </p>
	 *
	 * @param msg
	 *            a {@link java.lang.String} object.
	 */
	public ParsingRuntimeException( String msg ) {
		super(msg);
	}

	/**
	 * <p>
	 * Constructor for ParsingRuntimeException.
	 * </p>
	 *
	 * @param s
	 * @param exception
	 *            a {@link Throwable} object.
	 */
	public ParsingRuntimeException( String s, Throwable exception ) {
		super(exception);
	}

	public ParsingRuntimeException( Exception e )
	{
		super(e);
	}
}
