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
package ortus.boxlang.runtime.scripting;

import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

public class BoxScriptingFactory implements ScriptEngineFactory {

	public String getEngineName() {
		return "BoxLang";
	}

	public String getEngineVersion() {
		return "1.0";
	}

	public String getLanguageName() {
		return "BoxLang";
	}

	public String getLanguageVersion() {
		return "1.0";
	}

	public String getMethodCallSyntax( String obj, String m, String... args ) {
		StringBuilder sb = new StringBuilder();
		sb.append( obj + "." + m + "(" );
		// loop over args and append
		int len = args.length;
		for ( int i = 0; i < len; i++ ) {
			sb.append( "'" + args[ i ].replace( "'", "''" ) + "'" );
			if ( i < len - 1 ) {
				sb.append( "," );
			}
		}
		sb.append( ")" );
		return sb.toString();
	}

	public String getOutputStatement( String toDisplay ) {
		return "writeoutput( '" + toDisplay.replace( "'", "''" ) + "' )";
	}

	public String getProgram( String... statements ) {
		String	retval	= "";
		int		len		= statements.length;
		for ( int i = 0; i < len; i++ ) {
			retval += statements[ i ] + ";\n";
		}
		return retval;
	}

	public ScriptEngine getScriptEngine() {
		return new BoxScriptingEngine( this );
	}

	@Override
	public List<String> getExtensions() {
		return List.of( "bx", "cfm", "cfc", "cfs", "bxs", "bxm" );
	}

	@Override
	public List<String> getMimeTypes() {
		return List.of();
	}

	@Override
	public List<String> getNames() {
		return List.of( "BoxLang" );
	}

	@Override
	public Object getParameter( String key ) {

		if ( key.equalsIgnoreCase( ScriptEngine.ENGINE ) ) {
			return getEngineName();
		}

		if ( key.equalsIgnoreCase( ScriptEngine.ENGINE_VERSION ) ) {
			return getEngineVersion();
		}

		if ( key.equalsIgnoreCase( ScriptEngine.LANGUAGE ) ) {
			return getLanguageName();
		}

		if ( key.equalsIgnoreCase( ScriptEngine.LANGUAGE_VERSION ) ) {
			return getLanguageVersion();
		}

		if ( key.equalsIgnoreCase( ScriptEngine.NAME ) ) {
			return getNames().get( 0 );
		}

		if ( key.equalsIgnoreCase( "THREADING" ) ) {
			return "THREAD-ISOLATED";
		}

		return null;
	}
}