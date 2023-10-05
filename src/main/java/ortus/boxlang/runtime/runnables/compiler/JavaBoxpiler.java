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
package ortus.boxlang.runtime.runnables.compiler;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.Node;

import ortus.boxlang.ast.BoxExpr;
import ortus.boxlang.ast.BoxScript;
import ortus.boxlang.ast.BoxStatement;
import ortus.boxlang.ast.Point;
import ortus.boxlang.ast.Position;
import ortus.boxlang.ast.statement.BoxExpression;
import ortus.boxlang.parser.BoxFileType;
import ortus.boxlang.parser.BoxParser;
import ortus.boxlang.parser.ParsingResult;
import ortus.boxlang.runtime.config.util.PlaceholderHelper;
import ortus.boxlang.runtime.runnables.IBoxRunnable;
import ortus.boxlang.runtime.types.exceptions.ApplicationException;
import ortus.boxlang.transpiler.BoxLangTranspiler;

/**
 * This class uses the Java compiler to turn a BoxLang script into a Java class
 */
public class JavaBoxpiler {

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Singleton instance
	 */
	private static JavaBoxpiler		instance;

	private JavaMemoryManager		manager;

	private JavaCompiler			compiler;

	private JavaDynamicClassLoader	classLoader;

	private DiskClassLoader			diskClassLoader;

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Private constructor
	 */
	private JavaBoxpiler() {

		this.compiler			= ToolProvider.getSystemJavaCompiler();
		this.manager			= new JavaMemoryManager( compiler.getStandardFileManager( null, null, null ) );

		this.diskClassLoader	= new DiskClassLoader(
		    new URL[] {},
		    this.getClass().getClassLoader(),
		    Paths.get( System.getProperty( "java.io.tmpdir" ) + File.separator + "boxlang" + File.separator + "cfclasses" ),
		    manager
		);

		this.classLoader		= new JavaDynamicClassLoader(
		    new URL[] {
			// new File( boxRT ).toURI().toURL()
		    },
		    this.getClass().getClassLoader(),
		    manager,
		    this.diskClassLoader
		);

	}

	/**
	 * Get the singleton instance
	 *
	 * @return TemplateLoader
	 */
	public static synchronized JavaBoxpiler getInstance() {
		if ( instance == null ) {
			instance = new JavaBoxpiler();
		}
		return instance;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Public Methods
	 * --------------------------------------------------------------------------
	 */

	// @formatter:off
	String				template	= """
		package ${packageName};

		import ortus.boxlang.runtime.BoxRuntime;
		import ortus.boxlang.runtime.context.*;

		// BoxLang Auto Imports
		import ortus.boxlang.runtime.runnables.BoxTemplate;
		import ortus.boxlang.runtime.runnables.BoxScript;
		import ortus.boxlang.runtime.dynamic.Referencer;
		import ortus.boxlang.runtime.interop.DynamicObject;
		import ortus.boxlang.runtime.loader.ClassLocator;
		import ortus.boxlang.runtime.loader.ImportDefinition;
		import ortus.boxlang.runtime.operators.*;
		import ortus.boxlang.runtime.scopes.Key;
		import ortus.boxlang.runtime.scopes.IScope;
		import ortus.boxlang.runtime.dynamic.casters.*;

		import java.nio.file.Path;
		import java.nio.file.Paths;
		import java.time.LocalDateTime;
		import java.util.List;

		public class ${className} extends ${baseclass} {

			private static ${className} instance;

			private static final List<ImportDefinition>	imports			= List.of();
			private static final Path					path			= Paths.get( "${fileFolderPath}" );
			private static final long					compileVersion	= 1L;
			private static final LocalDateTime			compiledOn		= LocalDateTime.parse( "2023-09-27T10:15:30" );
			private static final Object					ast				= null;

			public ${className}() {
			}

			public static synchronized ${className} getInstance() {
				if ( instance == null ) {
					instance = new ${className}();
				}
				return instance;
			}
			/**
				* Each template must implement the invoke() method which executes the template
				*
				* @param context The execution context requesting the execution
				*/
			public ${returnType} _invoke( IBoxContext context ) {
				// Reference to the variables scope
				IScope variablesScope = context.getScopeNearby( Key.of( "variables" ) );
				ClassLocator JavaLoader = ClassLocator.getInstance();
				IBoxContext			catchContext = null;
				${javaCode}
			}

			// ITemplateRunnable implementation methods

			/**
				* The version of the BoxLang runtime
			*/
			public long getRunnableCompileVersion() {
				return ${className}.compileVersion;
			}

			/**
				* The date the template was compiled
			*/
			public LocalDateTime getRunnableCompiledOn() {
				return ${className}.compiledOn;
			}

			/**
				* The AST (abstract syntax tree) of the runnable
			*/
			public Object getRunnableAST() {
			return ${className}.ast;
			}

			/**
				* The path to the template
			*/
			public Path getRunnablePath() {
			return ${className}.path;
			}

		}
	""";
	// @formatter:on

	Logger	logger		= LoggerFactory.getLogger( JavaBoxpiler.class );

	private String makeClass( String javaCode, String baseclass, String packageName, String className ) {
		Map<String, String> values = Map.ofEntries(
		    Map.entry( "javaCode", javaCode ),
		    Map.entry( "baseclass", baseclass ),
		    Map.entry( "fileFolderPath", "" ),
		    Map.entry( "returnType", baseclass.equals( "BoxScript" ) ? "Object" : "void" ),
		    Map.entry( "packageName", packageName ),
		    Map.entry( "className", className )
		);
		return PlaceholderHelper.resolve( template, values );
	}

	public Class<IBoxRunnable> compileStatement( String source, BoxFileType type ) {
		String	packageName	= "generated";
		String	className	= "Statement_" + MD5( source );
		String	fqn			= packageName + "." + className;

		if ( !classLoader.hasClass( fqn ) ) {

			if ( diskClassLoader.hasClass( fqn ) ) {
				return getDiskClass( fqn );
			} else {
				BoxParser		parser	= new BoxParser();
				ParsingResult	result;
				try {
					result = parser.parseStatement( source );
				} catch ( IOException e ) {
					throw new ApplicationException( "Error compiing source", e );
				} catch ( IllegalStateException e ) {
					try {
						result = parser.parseExpression( source );
					} catch ( IOException e2 ) {
						throw new ApplicationException( "Error compiing source", e2 );
					}
				}
				result.getIssues().forEach( it -> System.out.println( it ) );
				if ( !result.isCorrect() ) {
					throw new ApplicationException( "Error compiing source. " + result.getIssues().get( 0 ).toString() );
				}

				BoxLangTranspiler	transpiler	= new BoxLangTranspiler();

				Position			position	= new Position( new Point( 1, 1 ), new Point( 1, source.length() ) );

				Node				javaAST		= ( Node ) transpiler.transpile(
				    new BoxScript(
				        List.of( result.getRoot() instanceof BoxStatement ? ( BoxStatement ) result.getRoot()
				            : new BoxExpression( ( BoxExpr ) result.getRoot(), position, source ) ),
				        position,
				        source
				    )
				);

				compileSource( makeClass( getStatementsAsStringReturnLast( transpiler ), "BoxScript", packageName, className ), fqn );
			}
		}
		return getClass( fqn );

	}

	public Class<IBoxRunnable> compileScript( String source, BoxFileType type ) {
		String	packageName	= "generated";
		String	className	= "Script_" + MD5( source );
		String	fqn			= packageName + "." + className;

		if ( !classLoader.hasClass( fqn ) ) {
			if ( diskClassLoader.hasClass( fqn ) ) {
				return getDiskClass( fqn );
			} else {
				BoxParser		parser	= new BoxParser();
				ParsingResult	result;
				try {
					result = parser.parse( source, type );
				} catch ( IOException e ) {
					throw new ApplicationException( "Error compiing source", e );
				}

				result.getIssues().forEach( it -> System.out.println( it ) );
				if ( !result.isCorrect() )

				{
					throw new ApplicationException( "Error compiing source. " + result.getIssues().get( 0 ).toString() );
				}

				BoxLangTranspiler	transpiler	= new BoxLangTranspiler();

				Node				javaAST		= ( Node ) transpiler.transpile( ( BoxScript ) result.getRoot() );

				compileSource( makeClass( transpiler.getStatementsAsString() + "\n return null;", "BoxScript", packageName, className ), fqn );
			}
		}
		return getClass( fqn );
	}

	public Class<IBoxRunnable> compileTemplate( Path path, String packagePath ) {
		File	lcaseFile		= new File( packagePath.toString().toLowerCase() );
		String	packageName		= getPackageName( lcaseFile );
		String	className		= getClassName( lcaseFile );
		String	fqn				= packageName + "." + className;
		long	lastModified	= path.toFile().lastModified();

		if ( !classLoader.hasClass( fqn, lastModified ) ) {
			if ( diskClassLoader.hasClass( fqn, lastModified ) ) {
				return getDiskClass( fqn );
			} else {
				BoxParser		parser	= new BoxParser();
				ParsingResult	result;
				try {
					result = parser.parse( path.toFile() );
				} catch ( IOException e ) {
					throw new ApplicationException( "Error compiing source", e );
				}
				result.getIssues().forEach( it -> System.out.println( it ) );
				assert result.isCorrect();

				BoxLangTranspiler	transpiler	= new BoxLangTranspiler();
				Node				javaAST		= ( Node ) transpiler.transpile( result.getRoot() );

				compileSource( makeClass( transpiler.getStatementsAsString(), "BoxTemplate", packageName, className ), fqn );
			}
		}
		return getClass( fqn );
	}

	public void compileSource( String javaSource, String fqn ) {

		// if ( true )
		// throw new ApplicationException( javaSource );

		DiagnosticCollector<JavaFileObject>	diagnostics		= new DiagnosticCollector<>();

		String								javaRT			= System.getProperty( "java.class.path" );

		// String boxRT = "C:/Users/Brad/Documents/GitHub/boxlang/runtime/build/classes/java/main";
		// String compRT = "C:/Users/Brad/Documents/GitHub/boxlang/compiler/build/classes/java/main";

		List<JavaFileObject>				sourceFiles		= Collections.singletonList( new JavaSourceString( fqn, javaSource ) );
		List<String>						options			= new ArrayList<>() {

																{
																	add( "-g" );
																	// add( "-cp" );
																	// add( javaRT + File.pathSeparator + boxRT + File.pathSeparator + File.pathSeparator +
																	// compRT );
																}
															};
		JavaCompiler.CompilationTask		task			= compiler.getTask( null, manager, diagnostics, options, null, sourceFiles );
		boolean								compilerResult	= task.call();

		if ( !compilerResult ) {
			diagnostics.getDiagnostics()
			    .forEach( d -> {
				    throw new RuntimeException( String.valueOf( d ) );
			    } );
		}

	}

	// get class
	public Class<IBoxRunnable> getClass( String fqn ) {
		try {
			return ( Class<IBoxRunnable> ) classLoader.loadClass( fqn );
		} catch ( ClassNotFoundException e ) {
			throw new ApplicationException( "Error compiing source", e );
		}
	}

	public Class<IBoxRunnable> getDiskClass( String fqn ) {
		try {
			return ( Class<IBoxRunnable> ) diskClassLoader.loadClass( fqn );
		} catch ( ClassNotFoundException e ) {
			throw new ApplicationException( "Error compiing source", e );
		}
	}

	public String getStatementsAsStringReturnLast( BoxLangTranspiler transpiler ) {
		StringBuilder result = new StringBuilder();
		// loop over statements
		for ( int i = 0; i < transpiler.getStatements().size(); i++ ) {
			// if last statement, return it
			if ( i == transpiler.getStatements().size() - 1 ) {
				result.append( "return " );
			}
			result.append( transpiler.getStatements().get( i ).toString() );
			if ( i < transpiler.getStatements().size() - 1 ) {
				result.append( ";\n" );
			}
		}
		return result.toString();
	}

	public String MD5( String md5 ) {
		try {
			java.security.MessageDigest	md		= java.security.MessageDigest.getInstance( "MD5" );
			byte[]						array	= md.digest( md5.getBytes() );
			StringBuffer				sb		= new StringBuffer();
			for ( int i = 0; i < array.length; ++i ) {
				sb.append( Integer.toHexString( ( array[ i ] & 0xFF ) | 0x100 ).substring( 1, 3 ) );
			}
			return sb.toString();
		} catch ( java.security.NoSuchAlgorithmException e ) {
			throw new ApplicationException( "Error compiing source", e );
		}
	}

	/**
	 * Transforms the filename into the class name
	 *
	 * @param source
	 *
	 * @return returns the class name according the name conventions Test.ext - Test$ext
	 */
	public static String getClassName( File file ) {
		String name = file.getName().replace( ".", "$" );
		name = name.substring( 0, 1 ).toUpperCase() + name.substring( 1 );
		return name;
	}

	/**
	 * Transforms the path into the package name
	 *
	 * @param source
	 *
	 * @return returns the class name according the name conventions Test.ext - Test$ext
	 */
	public static String getPackageName( File file ) {
		String packg = file.toString().replace( File.separatorChar + file.getName(), "" );
		if ( packg.startsWith( "/" ) ) {
			packg = packg.substring( 1 );
		}
		// TODO: This needs a lot more work. There are tons of disallowed edge cases such as a folder that is a number.
		// We probably need to iterate each path segment and clean or remove as neccessary to make it a valid package name.
		// Also, I'd like cfincluded files to use the relative path as the package name, which will require some refactoring.

		// Take out periods in folder names
		packg	= packg.replaceAll( "\\.", "" );
		// Replace / with .
		packg	= packg.replaceAll( "/", "." );
		// Remove any : from Windows drives
		packg	= packg.replaceAll( ":", "" );
		// Replace \ with .
		packg	= packg.replaceAll( "\\\\", "." );
		// Remove any non alpha-numeric chars.
		packg	= packg.replaceAll( "[^a-zA-Z0-9\\\\.]", "" );
		return packg;

	}

}
