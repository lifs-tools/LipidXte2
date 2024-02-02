package de.mpicbg.ms.view.treecell;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.SimpleBooleanProperty;

/**
 * Created by moon on 5/24/16.
 */
public class NamedBoolean extends SimpleBooleanProperty
{
	private String name;

	public NamedBoolean(String name)
	{
		super(name, name);
		this.name = name;
	}

	public String getName()
	{
		return name;
	}

	public void setName( String name )
	{
		this.name = name;
	}

	// --- Valid
	private ReadOnlyBooleanWrapper valid;

	public void setValid(boolean value) {
		if(value && name.equals( "0" )) this.name = "1";
		validPropertyImpl().set(value);
	}

	/**
	 * Represents whether this property is currently valid or not.
	 */
	public final boolean isValid() {
		return valid == null ? false : valid.get();
	}

	/**
	 * Property representing whether this is currently valid.
	 */
	public final ReadOnlyBooleanProperty validProperty() {
		return validPropertyImpl().getReadOnlyProperty();
	}

	private ReadOnlyBooleanWrapper validPropertyImpl() {
		if (valid == null) {
			valid = new ReadOnlyBooleanWrapper(this, "valid");
		}
		return valid;
	}
}
