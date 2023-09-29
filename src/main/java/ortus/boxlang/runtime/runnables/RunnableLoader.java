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
package ortus.boxlang.runtime.runnables;

import java.nio.file.Path;
import java.nio.file.Paths;

import ortus.boxlang.parser.BoxFileType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.runnables.compiler.BoxJavaCompiler;
import ortus.boxlang.runtime.types.exceptions.MissingIncludeException;

/**
 * This class is responsible for taking a template on disk or arbitrary set of statements
 * and compiling them into an invokable class and loading that class.
 */
public class RunnableLoader {

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Singleton instance
	 */
	private static RunnableLoader instance;

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Private constructor
	 */
	private RunnableLoader() {
	}

	/**
	 * Get the singleton instance
	 *
	 * @return TemplateLoader
	 */
	public static synchronized RunnableLoader getInstance() {
		if ( instance == null ) {
			instance = new RunnableLoader();
		}
		return instance;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Public Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Load the class for a template, JIT compiling if needed
	 *
	 * @param path Absolute path on disk to the template
	 *
	 * @return
	 */
	public BoxTemplate loadTemplateAbsolute( IBoxContext context, Path path ) {
		if ( !path.toFile().exists() ) {
			throw new MissingIncludeException( "The template path [" + path.toString() + "] could not be found.", path.toString() );
		}
		// TODO: enforce valid include extensions (.cfm, .cfs, .bxs, .bxm, .bx)
		Class<BoxTemplate> clazz = BoxJavaCompiler.getInstance().compileTemplate( path );
		return ( BoxTemplate ) DynamicObject.of( clazz ).invokeStatic( "getInstance" ).get();
	}

	/**
	 * * Load the class for a template, JIT compiling if needed
	 *
	 * @param path Relative path on disk to the template
	 *
	 * @return
	 */
	public BoxTemplate loadTemplateRelative( IBoxContext context, String path ) {
		// Determine what this path is relative to
		BoxTemplate	template	= context.findClosestTemplate();
		String		relativeBase;
		// We our current context is executing a template, then we are relative to that template
		if ( template != null ) {
			relativeBase = template.getRunnablePath().getParent().toString();
		} else {
			// Otherwise we are relative to the root of the runtime (the / mapping, or the working dir of the process)
			// TODO: Get config via the context, not directly from the runtime to allow overrides
			Object rootMapping = BoxRuntime.getInstance().getConfiguration().runtime.mappings.getOrDefault( "/", System.getProperty( "user.dir" ) );
			relativeBase = ( String ) rootMapping;
		}

		// Make absolute
		return loadTemplateAbsolute( context, Paths.get( relativeBase, path ) );
	}

	/**
	 * Load the class for an ad-hoc script, JIT compiling if needed
	 *
	 * @param source The BoxLang or CFML source to compile
	 * @param type   The type of source to parse
	 *
	 * @return
	 */
	public BoxScript loadSource( String source, BoxFileType type ) {
		Class<BoxScript> clazz = BoxJavaCompiler.getInstance().compileSource( source, type );
		return ( BoxScript ) DynamicObject.of( clazz ).invokeStatic( "getInstance" ).get();
	}

	/**
	 * Load the class for an ad-hoc script, JIT compiling if needed
	 *
	 * @param source The BoxLang or CFML source to compile
	 *
	 * @return
	 */
	public BoxScript loadSource( String source ) {
		// TODO: Change to BoxLangScript once implemented
		return loadSource( source, BoxFileType.CFSCRIPT );
	}

	/**
	 * Load the class for a script, JIT compiling if needed
	 *
	 * @param source The source to load
	 *
	 * @return
	 */
	public BoxScript loadScript( IBoxContext context, String source ) {
		return null;
	}
}
