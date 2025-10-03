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
import ortus.boxlang.runtime.context.StaticClassBoxContext;
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
	 * The BoxPiler implementation the runtime will use. At this time we offer two
	 * choices:
	 * 1. JavaBoxpiler - Generates Java source code and compiles it via the JDK
	 * 2. ASMBoxpiler - Generates bytecode directly via ASM
	 * However, developers can create their own Boxpiler implementations and
	 * register them with the runtime
	 * via configuration.
	 */
	private IBoxpiler					boxpiler;

	/**
	 * Valid template extensions
	 *
	 * @see Configuration#validTemplateExtensions
	 */
	private static final Set<String>	VALID_TEMPLATE_EXTENSIONS	= BoxRuntime.getInstance().getConfiguration().getValidTemplateExtensions();

	/**
	 * The registry of Boxpilers
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
	 * Registers the Boxpilers in a specified class loader.
	 * 
	 * @param cl
	 */
	public void registerBoxpilers( ClassLoader cl ) {
		// use service loader to find instance of myself
		ServiceLoader<IBoxpiler> loader = ServiceLoader.load( IBoxpiler.class, cl );
		for ( IBoxpiler boxpiler : loader ) {
			boxpilers.put( boxpiler.getName(), boxpiler );
		}
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
		return boxpilers;
	}

	/**
	 * Get the Boxpiler for the given name.
	 *
	 * @param name the name of the Boxpiler
	 *
	 * @return the Boxpiler instance
	 */
	public IBoxpiler getBoxpiler( Key name ) {
		IBoxpiler boxpiler = boxpilers.get( name );
		if ( boxpiler == null ) {
			String detail = "There are no registered Boxpilers. Please ensure you have a Boxpiler implementation on the classpath.";
			if ( !boxpilers.isEmpty() ) {
				detail = "Available Boxpilers are: [" + boxpilers.keySet() + "]";
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
	 * Choose the Boxpiler implementation to use according to the configuration
	 * We only support two direct implementations, Java and ASM
	 * Later on we need to inspect the class path for specific files if we want this configurable.
	 *
	 * @return The Boxpiler implementation to use
	 */
	private void chooseBoxpiler() {
		if ( boxpiler != null ) {
			// If we already have a boxpiler, do not change it
			return;
		}

		BoxRuntime	runtime		= BoxRuntime.getInstance();
		String		nameToUse	= StringCaster.cast( runtime.getConfiguration().experimental.get( "compiler" ) );
		Key			keyToUse	= null;
		// if there is no config
		if ( nameToUse == null ) {
			// prefer the ASM Boxpiler if it's registered
			if ( boxpilers.containsKey( Key.asm ) ) {
				keyToUse = Key.asm;
			} else if ( !boxpilers.isEmpty() ) {
				// if there is no ASM, juse the first we find
				keyToUse = boxpilers.keySet().iterator().next();
			} else {
				// If there are no boxpilers registered, use the no-op
				// it will still load classes, but not compile them
				keyToUse = Key.noOp;
			}

		} else {
			keyToUse = Key.of( nameToUse );
		}

		selectBoxPiler( keyToUse );
		runtime.getLoggingService().RUNTIME_LOGGER.info( "+ Choosing " + getBoxpiler().getClass().getSimpleName() + " as the Boxpiler implementation" );
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
		if ( ext.equals( "*" ) || VALID_TEMPLATE_EXTENSIONS.contains( ext ) ) {
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
		Class<IBoxRunnable> clazz = getBoxpiler().compileClass( resolvedFilePath );
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
						StaticClassBoxContext	staticContext	= new StaticClassBoxContext( context, boxClass,
						    BoxClassSupport.getStaticScope( context, boxClass ) );
						ResolvedFilePath		staticPath		= ( ResolvedFilePath ) boxClass.getField( "path" ).get();
						staticContext.pushTemplate( staticPath );
						boxClass.invokeStatic( context, "staticInitializer", staticContext );
						staticContext.popTemplate();
						boxClass.setField( "staticInitialized", true );
					}
				}
			}
		}
	}

}
