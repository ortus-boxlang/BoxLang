/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.runtime.types;

import java.util.Set;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.validation.Validatable;
import ortus.boxlang.runtime.validation.Validator;

/**
 * Represents an argument to a function or BIF
 *
 * @param required          Whether the argument is required
 * @param type              The type of the argument
 * @param name              The name of the argument
 * @param defaultValue      The default value of the argument
 * @param defaultExpression The default value of the argument as a Lambda to be evaluated at runtime
 * @param annotations       Annotations for the argument
 * @param documentation     Documentation for the argument
 *
 */
public record Argument( boolean required, String type, Key name, Object defaultValue, DefaultExpression defaultExpression, IStruct annotations,
    IStruct documentation, Set<Validator> validators ) implements Validatable {

	// Easy Type Constants
	public static final String ANY = "any";
	public static final String ARRAY = "array";
	public static final String BOOLEAN = "boolean";
	public static final String DATE = "date";
	public static final String DATETIME = "datetime";
	public static final String FILE = "file";
	public static final String LIST = "list";
	public static final String NUMERIC = "numeric";
	public static final String QUERY = "query";
	public static final String STRING = "string";
	public static final String STRUCT = "struct";
	public static final String UDF = "udf";
	public static final String CLOSURE = "closure";
	public static final String LAMBDA = "lambda";
	public static final String XML = "xml";

	@FunctionalInterface
	public static interface DefaultExpression {

		Object evaluate( IBoxContext context );
	}

	public Argument( Key name ) {
		this( false, "any", name );
	}

	public Argument( Key name, Set<Validator> validators ) {
		this( false, "any", name, validators );
	}

	public Argument( boolean required, String type, Key name ) {
		this( required, type, name, null, null, Struct.EMPTY, Struct.EMPTY, Set.of() );
	}

	public Argument( boolean required, String type, Key name, Set<Validator> validators ) {
		this( required, type, name, null, null, Struct.EMPTY, Struct.EMPTY, validators );
	}

	public Argument( boolean required, String type, Key name, Object defaultValue ) {
		this( required, type, name, defaultValue, null, Struct.EMPTY, Struct.EMPTY, Set.of() );
	}

	public Argument( boolean required, String type, Key name, Object defaultValue, Set<Validator> validators ) {
		this( required, type, name, defaultValue, null, Struct.EMPTY, Struct.EMPTY, validators );
	}

	public Argument( boolean required, String type, Key name, Object defaultValue, IStruct annotations ) {
		this( required, type, name, defaultValue, null, annotations, Struct.EMPTY, Set.of() );
	}

	public Argument( boolean required, String type, Key name, Object defaultValue, IStruct annotations, Set<Validator> validators ) {
		this( required, type, name, defaultValue, null, annotations, Struct.EMPTY, validators );
	}

	public Argument( boolean required, String type, Key name, Object defaultValue, DefaultExpression defaultExpression, IStruct annotations,
	    IStruct documentation ) {
		this( required, type, name, defaultValue, defaultExpression, annotations, documentation, Set.of() );
	}

	public Argument( boolean required, String type, Key name, Object defaultValue, DefaultExpression defaultExpression, IStruct annotations,
	    IStruct documentation, Set<Validator> validators ) {
		this.required			= required;
		this.type				= type;
		this.name				= name;
		this.defaultValue		= defaultValue;
		this.defaultExpression	= defaultExpression;
		this.annotations		= annotations;
		this.documentation		= documentation;
		this.validators			= validators;
	}

	public Object getDefaultValue( IBoxContext context ) {
		if ( defaultExpression != null ) {
			return defaultExpression.evaluate( context );
		}
		return defaultValue;
	}

	public boolean hasDefaultValue() {
		return defaultValue != null || defaultExpression != null;
	}

}
