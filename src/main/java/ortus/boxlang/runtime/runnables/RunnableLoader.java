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
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import ortus.boxlang.compiler.IBoxpiler;
import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.config.Configuration;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
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
	 * The BoxPiler implementation the runtime will use. BoxLang provides three core implementations:
	 *
	 * <ol>
	 * <li><b>ASMBoxpiler</b> - Generates bytecode directly via ASM (default when available, high performance)</li>
	 * <li><b>JavaBoxpiler</b> - Generates Java source code and compiles it via the JDK (useful for debugging)</li>
	 * <li><b>NoOpBoxpiler</b> - Loads pre-compiled classes only, does not compile (for fully pre-compiled deployments)</li>
	 * </ol>
	 *
	 * <p>
	 * Developers can create their own Boxpiler implementations and register them with the runtime
	 * via the ServiceLoader mechanism and configuration.
	 * </p>
	 *
	 * <p>
	 * <b>Selection Priority:</b>
	 * </p>
	 * <ol>
	 * <li>Use the compiler specified in configuration (if set)</li>
	 * <li>Prefer ASMBoxpiler if available</li>
	 * <li>Fall back to any other registered Boxpiler</li>
	 * <li>Use NoOpBoxpiler as last resort (requires pre-compiled classes)</li>
	 * </ol>
	 */
	private IBoxpiler					boxpiler;

	/**
	 * Valid template extensions
	 *
	 * @see Configuration#validTemplateExtensions
	 */
	private static final Set<String>	VALID_TEMPLATE_EXTENSIONS	= BoxRuntime.getInstance().getConfiguration().getValidTemplateExtensions();

	/**
	 * The registry of Boxpilers we know about
	 */
	private Map<Key, IBoxpiler>			boxpilers					= new ConcurrentHashMap<Key, IBoxpiler>();

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Private constructor
	 */
	private RunnableLoader() {
		registerBoxpilers();
	}

	/**
	 * Get the singleton instance
	 *
	 * @return TemplateLoader
	 */
	public static RunnableLoader getInstance() {
		if ( instance == null ) {
			synchronized ( RunnableLoader.class ) {
				if ( instance == null ) {
					instance = new RunnableLoader();
				}
			}
		}
		return instance;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Public Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Registers the Boxpilers in a specified class loader using Java's ServiceLoader mechanism.
	 *
	 * <p>
	 * This method gracefully handles cases where a Boxpiler class is listed in the service file
	 * but is not available on the classpath. This allows for modular JAR distributions where
	 * different Boxpiler implementations can be included or excluded.
	 * </p>
	 *
	 * <p>
	 * If no Boxpilers are successfully registered, the runtime will attempt to use NoOpBoxpiler
	 * as a fallback during the selection process.
	 * </p>
	 *
	 * @param cl The class loader to use for loading Boxpiler implementations
	 */
	@SuppressWarnings( "null" )
	public void registerBoxpilers( ClassLoader cl ) {
		BoxRuntime					runtime	= BoxRuntime.getInstance();
		// Use service loader to find Boxpiler implementations
		ServiceLoader<IBoxpiler>	loader	= ServiceLoader.load( IBoxpiler.class, cl );

		// Iterate through available Boxpilers, handling missing classes gracefully
		// The try-catch MUST be inside the loop so that one failure doesn't stop the entire iteration
		for ( IBoxpiler boxpiler : loader ) {
			try {
				this.boxpilers.put( boxpiler.getName(), boxpiler );
				runtime.getLoggingService().RUNTIME_LOGGER.trace(
				    "+ Registered Boxpiler: {} ({})",
				    boxpiler.getName(),
				    boxpiler.getClass().getName()
				);
			} catch ( Throwable e ) {
				// ServiceConfigurationError, ClassNotFoundException, NoClassDefFoundError, etc.
				// This can happen when a Boxpiler is listed in the service file but the class
				// or its dependencies are not on the classpath
				runtime.getLoggingService().RUNTIME_LOGGER.debug(
				    "- Failed to register Boxpiler (class may not be on classpath): {}",
				    e.getMessage()
				);
			}
		}

		// Choose which Boxpiler to use
		chooseBoxpiler();
	}

	/**
	 * Register boxpilers with the default class loader.
	 */
	public void registerBoxpilers() {
		registerBoxpilers( IBoxpiler.class.getClassLoader() );
	}

	/**
	 * Get all boxpilers registered, initting in double check lock if null
	 */
	public Map<Key, IBoxpiler> getBoxpilers() {
		return this.boxpilers;
	}

	/**
	 * Get the Boxpiler for the given name.
	 *
	 * @param name the name of the Boxpiler
	 *
	 * @return the Boxpiler instance
	 */
	public IBoxpiler getBoxpiler( Key name ) {
		IBoxpiler boxpiler = this.boxpilers.get( name );
		if ( boxpiler == null ) {
			String detail = "There are no registered Boxpilers. Please ensure you have a Boxpiler implementation on the classpath.";
			if ( !this.boxpilers.isEmpty() ) {
				detail = "Available Boxpilers are: [" + this.boxpilers.keySet() + "]";
			}
			throw new BoxRuntimeException( "No Boxpiler registered for name: " + name + ". " + detail );
		}
		return boxpiler;
	}

	/**
	 * Select the Boxpiler implementation to use when generating bytecode
	 *
	 * @param boxpiler The Boxpiler interface to use
	 */
	public void selectBoxPiler( Key name ) {
		this.boxpiler = getBoxpiler( name );
	}

	/**
	 * Get the current BoxPiler
	 * Returns null if no BoxPiler is registered.
	 *
	 * @return The current Boxpiler
	 */
	public IBoxpiler getBoxpiler() {
		return this.boxpiler;
	}

	/**
	 * Choose the Boxpiler implementation to use according to the following priority:
	 *
	 * <ol>
	 * <li>Use the compiler explicitly specified in configuration (if set)</li>
	 * <li>Prefer ASMBoxpiler if it's registered (high performance, default)</li>
	 * <li>Use any other registered Boxpiler (e.g., JavaBoxpiler)</li>
	 * <li>Fall back to NoOpBoxpiler (for pre-compiled deployments only)</li>
	 * </ol>
	 *
	 * <p>
	 * <b>Note:</b> NoOpBoxpiler can only load pre-compiled classes and will throw an exception
	 * if asked to compile source code. It's intended for fully pre-compiled application deployments.
	 * </p>
	 *
	 * @throws BoxRuntimeException if the configured Boxpiler is not found or no Boxpilers are available
	 */
	private void chooseBoxpiler() {
		// If we already have a boxpiler, do not change it
		if ( this.boxpiler != null ) {
			return;
		}
		// Determine which Boxpiler to use
		BoxRuntime	runtime		= BoxRuntime.getInstance();
		String		nameToUse	= StringCaster.cast( runtime.getConfiguration().compiler );
		Key			keyToUse	= null;

		if ( nameToUse == null ) {
			// No explicit configuration, use default selection logic
			if ( this.boxpilers.containsKey( Key.asm ) ) {
				// Prefer ASM Boxpiler (high performance default)
				keyToUse = Key.asm;
			} else if ( !this.boxpilers.isEmpty() ) {
				// Use the first registered Boxpiler (e.g., JavaBoxpiler)
				keyToUse = this.boxpilers.keySet().iterator().next();
			} else {
				// No Boxpilers registered at all, fall back to NoOp
				// This will work only for pre-compiled classes
				keyToUse = Key.noOp;
			}
		} else {
			// Use explicitly configured Boxpiler
			keyToUse = Key.of( nameToUse );
		}

		// Attempt to select the chosen Boxpiler
		try {
			selectBoxPiler( keyToUse );
			runtime.getLoggingService().RUNTIME_LOGGER.debug(
			    "+ Activated Boxpiler: {} ({})",
			    keyToUse,
			    getBoxpiler().getClass().getSimpleName()
			);
		} catch ( BoxRuntimeException e ) {
			// The requested Boxpiler is not available
			if ( nameToUse != null ) {
				// User explicitly requested a Boxpiler that isn't available
				throw new BoxRuntimeException(
				    "Configured Boxpiler '" + nameToUse + "' is not available. " + e.getMessage(),
				    e
				);
			} else {
				// Should not happen, but handle gracefully
				throw new BoxRuntimeException(
				    "Failed to activate any Boxpiler. Please ensure at least one Boxpiler implementation is on the classpath. " + e.getMessage(),
				    e
				);
			}
		}
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
			String theMissingPath = resolvedFilePath.relativePath();
			if ( theMissingPath == null ) {
				theMissingPath = resolvedFilePath.absolutePath().toString();
			} else {
				// change to forward slashes
				theMissingPath = theMissingPath.replace( "\\", "/" );
			}
			throw new MissingIncludeException( "The template path [" + theMissingPath + "] could not be found.",
			    theMissingPath );
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

		// This extension check is duplicated in the BaseBoxContext.includeTemplate() right now since some code paths hit the runnableLoader directly
		boolean includeAll = VALID_TEMPLATE_EXTENSIONS.contains( "*" );
		if ( includeAll || VALID_TEMPLATE_EXTENSIONS.contains( ext ) ) {
			Class<IBoxRunnable> clazz = getBoxpiler().compileTemplate( resolvedFilePath );
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
	public BoxTemplate loadTemplateRelative( IBoxContext context, String path, boolean externalOnly ) {
		// Make absolute
		return loadTemplateAbsolute( context, FileSystemUtil.expandPath( context, path, externalOnly ) );
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
		Class<IBoxRunnable> clazz = getBoxpiler().compileScript( source, type );
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
		Class<IBoxRunnable> clazz = getBoxpiler().compileStatement( source, type );
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
		Class<IBoxRunnable> clazz = getBoxpiler().compileClass( source, type );
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
		Class<IBoxRunnable> clazz = getBoxpiler().compileClass( resolvedFilePath );
		return clazz;
	}

}
