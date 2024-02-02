package de.mpicbg.ms.view.treecell;

/**
 * Created by moon on 6/28/16.
 */
public class CheckBoxNamedBoolean extends NamedBoolean
{
	public CheckBoxNamedBoolean(String name)
	{
		super(name);
	}

	public CheckBoxNamedBoolean(String name, boolean value)
	{
		super(name);
		this.setValue( value );
	}
}
