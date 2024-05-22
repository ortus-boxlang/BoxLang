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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * <code>BoxScriptingFactory</code> is used to describe and instantiate
 * <code>ScriptEngines</code> for BoxLang based on JSR-223.
 *
 * See {@link ScriptEngineManager#ScriptEngineManager()} and
 * {@link ScriptEngineManager#ScriptEngineManager(java.lang.ClassLoader)}.
 *
 */
public class BoxScriptingFactory implements ScriptEngineFactory {

	/**
	 * The version information for the BoxLang runtime
	 */
	private IStruct versionInfo;

	/**
	 * Get a Struct of version information from the version.properties
	 */
	private IStruct getVersionInfo() {
		// Lazy Load the version info
		if ( this.versionInfo == null ) {
			Properties properties = new Properties();
			try ( InputStream inputStream = BoxScriptingFactory.class.getResourceAsStream( "/META-INF/boxlang/version.properties" ) ) {
				properties.load( inputStream );
			} catch ( IOException e ) {
				e.printStackTrace();
			}
			this.versionInfo = Struct.fromMap( properties );
		}
		return this.versionInfo;
	}

	/**
	 * Returns the name of the scripting engine.
	 *
	 * @return The name of the scripting engine.
	 */
	public String getEngineName() {
		return "BoxLang";
	}

	/**
	 * Returns the version of the scripting engine.
	 *
	 * @return The version of the scripting engine.
	 */
	public String getEngineVersion() {
		return getVersionInfo().getAsString( Key.version );
	}

	/**
	 * Returns the name of the scripting language supported by the engine.
	 *
	 * @return The name of the scripting language supported by the engine.
	 */
	public String getLanguageName() {
		return "BoxLang";
	}

	/**
	 * Returns the version of the scripting language supported by the engine.
	 *
	 * @return The version of the scripting language supported by the engine.
	 */
	public String getLanguageVersion() {
		return getVersionInfo().getAsString( Key.version );
	}

	/**
	 * Builds a BoxLang method call using the object and method name and the
	 * arguments
	 *
	 * @param obj  The object to call the method on
	 * @param m    The method to call
	 * @param args The arguments to pass to the method
	 *
	 * @return The constructed BoxLang method call
	 */
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

	/**
	 * Build a BoxLang output statement using <code>writeoutput</code>
	 *
	 * @param toDisplay The string to display
	 *
	 * @return A BoxLang output statement
	 */
	public String getOutputStatement( String toDisplay ) {
		return "writeoutput( '" + toDisplay.replace( "'", "''" ) + "' )";
	}

	/**
	 * Builds out a program from a collection of statemetns by joining them with
	 * semicolons and newlines
	 *
	 * @param statements The statements to be executed
	 *
	 * @return A valid BoxLang program
	 */
	public String getProgram( String... statements ) {
		StringBuilder	sb	= new StringBuilder();
		int				len	= statements.length;
		for ( int i = 0; i < len; i++ ) {
			sb.append( statements[ i ] ).append( ";\n" );
		}
		return sb.toString();
	}

	/**
	 * Build a new BoxLang ScriptEngine
	 *
	 * @return A new BoxLang ScriptEngine
	 */
	@Override
	public ScriptEngine getScriptEngine() {
		return new BoxScriptingEngine( this );
	}

	/**
	 * Get the supported extensions for BoxLang
	 */
	@Override
	public List<String> getExtensions() {
		return List.of( "bx", "cfm", "cfc", "cfs", "bxs", "bxm" );
	}

	/**
	 * Get the supported mime types for BoxLang
	 */
	@Override
	public List<String> getMimeTypes() {
		return List.of();
	}

	/**
	 * Get the supported names for BoxLang
	 */
	@Override
	public List<String> getNames() {
		return List.of( "BoxLang", "BL", "BX" );
	}

	/**
	 * Get the supported parameters for BoxLang
	 *
	 * @param key The key to get the parameter for
	 *
	 * @return The parameter value
	 */
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
