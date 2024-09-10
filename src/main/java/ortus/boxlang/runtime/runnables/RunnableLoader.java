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

import java.util.Set;

import ortus.boxlang.compiler.IBoxpiler;
import ortus.boxlang.compiler.asmboxpiler.ASMBoxpiler;
import ortus.boxlang.compiler.javaboxpiler.JavaBoxpiler;
import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.types.exceptions.BoxValidationException;
import ortus.boxlang.runtime.types.exceptions.MissingIncludeException;
import ortus.boxlang.runtime.util.FileSystemUtil;
import ortus.boxlang.runtime.util.ResolvedFilePath;

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
	private static RunnableLoader	instance;
	private IBoxpiler				boxpiler;
	// TODO: make this configurable and move cf extensions to compat
	private Set<String>				validTemplateExtensions	= Set.of( "cfm", "cfml", "cfs", "bxs", "bxm" );

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Private constructor
	 */
	private RunnableLoader() {
		this.boxpiler = JavaBoxpiler.getInstance();
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
	 * Select the Boxpiler implementation to use when generating bytecode
	 *
	 * @param clazz
	 */
	public void selectBoxPiler( Class clazz ) {
		if ( JavaBoxpiler.class.isAssignableFrom( clazz ) ) {
			this.boxpiler = JavaBoxpiler.getInstance();
		} else if ( ASMBoxpiler.class.isAssignableFrom( clazz ) ) {
			this.boxpiler = ASMBoxpiler.getInstance();
		}
	}

	/**
	 * Get the current BoxPiler
	 * 
	 * @return The current Boxpiler
	 */
	public IBoxpiler getBoxpiler() {
		return this.boxpiler;
	}

	/**
	 * Load the class for a template, JIT compiling if needed
	 *
	 * @param context          The context to use
	 * @param resolvedFilePath The resolved file path to the template
	 *
	 * @return
	 */
	public BoxTemplate loadTemplateAbsolute( IBoxContext context, ResolvedFilePath resolvedFilePath ) {
		// TODO: Make case insensitive
		if ( !resolvedFilePath.absolutePath().toFile().exists() ) {
			throw new MissingIncludeException( "The template path [" + resolvedFilePath.absolutePath().toString() + "] could not be found.",
			    resolvedFilePath.absolutePath().toString() );
		}

		String	ext			= "";
		String	fileName	= resolvedFilePath.absolutePath().getFileName().toString().toLowerCase();
		if ( fileName.contains( "." ) ) {
			ext = fileName.substring( fileName.lastIndexOf( "." ) + 1 );
		}
		if ( ext.equals( "*" ) || validTemplateExtensions.contains( ext ) ) {
			Class<IBoxRunnable> clazz = this.boxpiler.compileTemplate( resolvedFilePath );
			return ( BoxTemplate ) DynamicObject.of( clazz ).invokeStatic( context, "getInstance" );
		} else {
			throw new BoxValidationException(
			    "The template path [" + resolvedFilePath.absolutePath().toString() + "] has an invalid extension to be executed [" + ext + "]." );
		}
	}

	/**
	 * * Load the class for a template, JIT compiling if needed
	 *
	 * @param path Relative path on disk to the template
	 *
	 * @return
	 */
	public BoxTemplate loadTemplateRelative( IBoxContext context, String path ) {
		// Make absolute
		return loadTemplateAbsolute( context, FileSystemUtil.expandPath( context, path ) );
	}

	/**
	 * Load the class for an ad-hoc script, JIT compiling if needed
	 *
	 * @param context The context to use
	 * @param source  The BoxLang or CFML source to compile
	 * @param type    The type of source to parse
	 *
	 * @return
	 */
	public BoxScript loadSource( IBoxContext context, String source, BoxSourceType type ) {
		Class<IBoxRunnable> clazz = this.boxpiler.compileScript( source, type );
		if ( IClassRunnable.class.isAssignableFrom( clazz ) ) {
			throw new RuntimeException( "Cannot define class in an ad-hoc script." );
		}
		return ( BoxScript ) DynamicObject.of( clazz ).invokeStatic( context, "getInstance" );
	}

	/**
	 * Load the class for an ad-hoc script, JIT compiling if needed
	 *
	 * @param context The context to use
	 * @param source  The BoxLang or CFML source to compile
	 *
	 * @return
	 */
	public BoxScript loadSource( IBoxContext context, String source ) {
		return loadSource( context, source, BoxSourceType.BOXSCRIPT );
	}

	/**
	 * Load the class for a script, JIT compiling if needed
	 *
	 * @param context The context to use
	 * @param source  The source to load
	 *
	 * @return
	 */
	public BoxScript loadStatement( IBoxContext context, String source ) {
		Class<IBoxRunnable> clazz = this.boxpiler.compileStatement( source, BoxSourceType.BOXSCRIPT );
		return ( BoxScript ) DynamicObject.of( clazz ).invokeStatic( context, "getInstance" );
	}

	/**
	 * Load the class for a BL class, JIT compiling if needed
	 * Returns the class instantiated and the init() method run
	 *
	 * @param source  The source to load
	 * @param context The context to use
	 * @param type    The type of source to parse
	 *
	 * @return
	 */
	public Class<IBoxRunnable> loadClass( String source, IBoxContext context, BoxSourceType type ) {
		return this.boxpiler.compileClass( source, type );
	}

	/**
	 * Load the class for a BL class, JIT compiling if needed
	 * Returns the class instantiated and the init() method run
	 *
	 * @param resolvedFilePath The path to the source to load
	 * @param context          The context to use
	 *
	 * @return The class
	 */
	public Class<IBoxRunnable> loadClass( ResolvedFilePath resolvedFilePath, IBoxContext context ) {
		return this.boxpiler.compileClass( resolvedFilePath );
	}

}
