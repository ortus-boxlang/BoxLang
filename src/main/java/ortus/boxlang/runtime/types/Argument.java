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

import ortus.boxlang.runtime.scopes.Key;

/**
 * Represents an argument to a function or BIF
 *
 * @param required     Whether the argument is required
 * @param type         The type of the argument
 * @param name         The name of the argument
 * @param defaultValue The default value of the argument
 *
 */
public record Argument( boolean required, String type, Key name, Object defaultValue, Struct annotations, Struct documentation ) {

	public Argument( Key name ) {
		this( false, "any", name );
	}

	public Argument( boolean required, String type, Key name ) {
		this( required, type, name, null, Struct.EMPTY, Struct.EMPTY );
	}

	public 	Argument( boolean required, String type, Key name, Object defaultValue ) {
		this( required, type, name, defaultValue, Struct.EMPTY, Struct.EMPTY );
	}

	public Argument( boolean required, String type, Key name, Object defaultValue, Struct annotations ) {
		this( required, type, name, defaultValue, annotations, Struct.EMPTY );
	}

	public Argument( boolean required, String type, Key name, Object defaultValue, Struct annotations, Struct documentation ) {
		this.required		= required;
		this.type			= type;
		this.name			= name;
		this.defaultValue	= defaultValue;
		this.annotations	= annotations;
		this.documentation	= documentation;
	}

}
