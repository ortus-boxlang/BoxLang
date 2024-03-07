package ortus.boxlang.runtime.validation;

import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;

import java.util.Set;

public interface Validatable {

	public Key name();

	public String type();

	public Object defaultValue();

	public Set<Validator> validators();

	/**
	 * Validate myself
	 *
	 * @param context
	 * @param records
	 */
	default void validate( IBoxContext context, Key caller, IStruct records ) {
		// Automatically enforce type, if set. This always happens first.
		Validator.TYPE.validate( context, caller, this, records );
		// loop over validators and call
		for ( Validator validator : this.validators() ) {
			validator.validate( context, caller, this, records );
		}
		// Automatically enforce default value, if set. This always happens last.
		Validator.DEFAULT_VALUE.validate( context, caller, this, records );
	}
}
