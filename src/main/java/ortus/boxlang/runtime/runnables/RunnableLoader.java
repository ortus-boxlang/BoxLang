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
import java.util.Set;

import ortus.boxlang.compiler.IBoxpiler;
import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.config.Configuration;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.StaticClassBoxContext;
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
	private static RunnableLoader		instance;

	/**
	 * The Boxpiler to use: ASM or Java
	 */
	private IBoxpiler					boxpiler;

	/**
	 * Valid template extensions
	 *
	 * @see Configuration#validTemplateExtensions
	 */
	private static final Set<String>	VALID_TEMPLATE_EXTENSIONS	= BoxRuntime.getInstance().getConfiguration().validTemplateExtensions;

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
	 * Select the Boxpiler implementation to use when generating bytecode
	 *
	 * @param boxpiler The Boxpiler interface to use
	 */
	public void selectBoxPiler( IBoxpiler boxpiler ) {
		this.boxpiler = boxpiler;
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
		Path result = FileSystemUtil.pathExistsCaseInsensitive( resolvedFilePath.absolutePath() );
		if ( result == null ) {
			throw new MissingIncludeException( "The template path [" + resolvedFilePath.absolutePath().toString() + "] could not be found.",
			    resolvedFilePath.absolutePath().toString() );
		}
		// If the path found on disk is not the same as the resolved path, then we need to update the resolved path
		// This would happen on a case-sensitive file system where the incoming path had the incorrect case, but we
		// we want our resolvedPath instance to reflect the real path on disk.
		if ( !result.equals( resolvedFilePath.absolutePath() ) ) {
			// relative path can be null
			int lengthToUse = resolvedFilePath.relativePath() != null
			    ? resolvedFilePath.relativePath().length()
			    : resolvedFilePath.absolutePath().toString().length();

			// Check if the result length is less than or equal to the length to use
			if ( result.toString().length() <= lengthToUse ) {
				// Create new relative path with last N chars of the new path
				resolvedFilePath = ResolvedFilePath.of(
				    resolvedFilePath.mappingName(),
				    resolvedFilePath.mappingPath(),
				    resolvedFilePath.absolutePath().toString().substring( result.toString().length() ),
				    result
				);
			} else {
				// Reuse the old relative path
				resolvedFilePath = ResolvedFilePath.of(
				    resolvedFilePath.mappingName(),
				    resolvedFilePath.mappingPath(),
				    resolvedFilePath.relativePath(),
				    result
				);
			}
		}
		String	ext			= "";
		String	fileName	= resolvedFilePath.absolutePath().getFileName().toString().toLowerCase();
		if ( fileName.contains( "." ) ) {
			ext = fileName.substring( fileName.lastIndexOf( "." ) + 1 );
		}
		if ( ext.equals( "*" ) || VALID_TEMPLATE_EXTENSIONS.contains( ext ) ) {
			Class<IBoxRunnable> clazz = this.boxpiler.compileTemplate( resolvedFilePath );
			return ( BoxTemplate ) DynamicObject.of( clazz ).invokeStatic( context, "getInstance" );
		} else {
			throw new BoxValidationException(
			    "The template path [" + resolvedFilePath.absolutePath().toString() + "] has an invalid extension to be executed [" + ext + "]." );
		}
	}

	/**
	 * Load the class for a template, JIT compiling if needed
	 *
	 * @param path Relative path on disk to the template
	 *
	 * @return The BoxTemplate instance
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
	 * @return The BoxScript instance
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
	 * @return The BoxScript instance
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
	 * @return The BoxScript instance
	 */
	public BoxScript loadStatement( IBoxContext context, String source, BoxSourceType type ) {
		Class<IBoxRunnable> clazz = this.boxpiler.compileStatement( source, type );
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
	 * @return The BoxLang class
	 */
	public Class<IBoxRunnable> loadClass( String source, IBoxContext context, BoxSourceType type ) {
		Class<IBoxRunnable> clazz = this.boxpiler.compileClass( source, type );
		runStaticInitializer( clazz, context );
		return clazz;
	}

	/**
	 * Load the class for a BL class, JIT compiling if needed
	 * Returns the class instantiated and the init() method run
	 *
	 * @param resolvedFilePath The path to the source to load
	 * @param context          The context to use
	 *
	 * @return The BoxLang class
	 */
	public Class<IBoxRunnable> loadClass( ResolvedFilePath resolvedFilePath, IBoxContext context ) {
		Class<IBoxRunnable> clazz = this.boxpiler.compileClass( resolvedFilePath );
		runStaticInitializer( clazz, context );
		return clazz;
	}

	/**
	 * Run static initializers for a Box class
	 *
	 * @param clazz   The class to run the static initializer for
	 * @param context The context to use
	 */
	private void runStaticInitializer( Class<IBoxRunnable> clazz, IBoxContext context ) {
		// Static initializers for Box Classes. We need to manually fire these so we can control the context
		if ( !clazz.isInterface() && IClassRunnable.class.isAssignableFrom( clazz ) ) {
			DynamicObject boxClass = DynamicObject.of( clazz );
			if ( !( Boolean ) boxClass.getField( "staticInitialized" ).get() ) {
				synchronized ( clazz ) {
					if ( !( Boolean ) boxClass.getField( "staticInitialized" ).get() ) {
						boxClass.invokeStatic( context, "staticInitializer",
						    new StaticClassBoxContext( context, boxClass, BoxClassSupport.getStaticScope( context, boxClass ) ) );
						boxClass.setField( "staticInitialized", true );
					}
				}
			}
		}
	}

}
