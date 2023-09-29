/**
 * [BoxLang]
 * <p>
 * Copyright [2023] [Ortus Solutions, Corp]
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.executor;

import javax.tools.SimpleJavaFileObject;

import java.net.URI;

import static java.util.Objects.requireNonNull;

/**
 * Java Source code as a String
 */
public class JavaSourceString extends SimpleJavaFileObject {

	private String sourceCode;

	public JavaSourceString( String name, String sourceCode ) {
		super( URI.create( "string:///" + name.replace( '.', '/' ) + Kind.SOURCE.extension ),
		    Kind.SOURCE );
		this.sourceCode = requireNonNull( sourceCode, "sourceCode must not be null" );
	}

	@Override
	public CharSequence getCharContent( boolean ignoreEncodingErrors ) {
		return sourceCode;
	}
}
