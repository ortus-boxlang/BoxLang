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

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.runnables.accessors.GeneratedGetter;
import ortus.boxlang.runtime.runnables.accessors.GeneratedSetter;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.util.DuplicationUtil;

/**
 * Represents a class property
 *
 * @param name              The name of the property
 * @param type              The type of the argument
 * @param defaultValue      The default value of the argument
 * @param defaultExpression The default value of the argument as a Lambda to be evaluated at runtime
 * @param annotations       Annotations for the property
 * @param documentation     Documentation for the property
 * @param generatedGetter   The generated getter Function
 * @param generatedSetter   The generated setter Function
 * @param sourceType        The source type of the property
 * @param declaringClass    The class that declares this property
 *
 */
public record Property( Key name, String type, Object defaultValue, DefaultExpression defaultExpression, IStruct annotations, IStruct documentation,
    Key getterName, Key setterName,
    UDF generatedGetter, UDF generatedSetter, BoxSourceType sourceType, Class<?> declaringClass ) {

	public Property( Key name, String type, Object defaultValue, DefaultExpression defaultExpression, IStruct annotations, IStruct documentation,
	    BoxSourceType sourceType, Class<?> declaringClass ) {
		// Pre-calculate the getter and setter names
		this(
		    name,
		    type,
		    defaultValue,
		    defaultExpression,
		    annotations,
		    documentation,
		    Key.of( "get" + name.getName() ),
		    Key.of( "set" + name.getName() ),
		    new GeneratedGetter( Key.of( "get" + name.getName() ), name,
		        sourceType.equals( BoxSourceType.CFSCRIPT ) || sourceType.equals( BoxSourceType.CFSCRIPT ) ? "any" : type ),
		    new GeneratedSetter( Key.of( "set" + name.getName() ), name, type ),
		    sourceType,
		    declaringClass
		);
	}

	public Object getDefaultValue( IBoxContext context ) {
		if ( defaultExpression != null ) {
			return defaultExpression.evaluate( context );
		}
		return DuplicationUtil.duplicate( defaultValue, false );
	}

	public Object getDefaultValueForMeta() {
		if ( defaultValue != null ) {
			return defaultValue;
		}
		if ( defaultExpression != null ) {
			return "[Runtime Expression]";
		}
		return null;
	}

	public boolean hasDefaultValue() {
		return defaultValue != null || defaultExpression != null;
	}

}
