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
package ortus.boxlang.runtime.scopes;

/**
 * Represents a case-insenstive key, while retaining the original case too.
 */
public class Key {

	private String name;
	private String nameNoCase;

	public Key( String name ) {
		this.name		= name;
		this.nameNoCase	= name.toUpperCase();
	}

	public String getNameNoCase() {
		return this.nameNoCase;
	}

	public String getName() {
		return this.name;
	}

	@Override
	public boolean equals( Object obj ) {
		if ( this == obj )
			return true;
		return false;
	}

	@Override
	public int hashCode() {
		return nameNoCase.hashCode();
	}

	public static Key of( String name ) {
		return new Key( name );
	}

}
