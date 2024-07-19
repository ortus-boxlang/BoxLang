package ortus.boxlang.runtime.validation;

import java.util.Set;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;

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
		// loop over validators and call
		for ( Validator validator : this.validators() ) {
			validator.validate( context, caller, this, records );
		}
	}
}
