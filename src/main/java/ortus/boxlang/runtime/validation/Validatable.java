package ortus.boxlang.runtime.validation;

import ortus.boxlang.runtime.scopes.Key;

import java.util.Set;

public interface Validatable {

	public Key name();

	public String type();

	public Object defaultValue();

	public Set<Validator> validators();

}
