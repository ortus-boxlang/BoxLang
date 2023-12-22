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
import java.util.stream.Collectors;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import ortus.boxlang.parser.BoxParser;
import ortus.boxlang.parser.BoxScriptType;
import ortus.boxlang.parser.ParsingResult;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.runnables.IBoxRunnable;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.ParseException;
import ortus.boxlang.transpiler.JavaTranspiler;
import ortus.boxlang.transpiler.TranspiledCode;
import ortus.boxlang.transpiler.Transpiler;

/**
 * This class uses the Java compiler to turn a BoxLang script into a Java class
 */
@SuppressWarnings( "unchecked" )
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
		    Paths.get( BoxRuntime.getInstance().getConfiguration().compiler.classGenerationDirectory ),
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

	Logger logger = LoggerFactory.getLogger( JavaBoxpiler.class );

	public Class<IBoxRunnable> compileStatement( String source, BoxScriptType type ) {
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
					result = parser.parse( source, BoxScriptType.CFSCRIPT );
				} catch ( IOException e ) {
					throw new BoxRuntimeException( "Error compiling source", e );
				}

				if ( !result.isCorrect() ) {
					throw new ParseException( result.getIssues() );
				}

				Transpiler transpiler = Transpiler.getTranspiler( null /* Config ? */ );
				transpiler.setProperty( "classname", className );
				transpiler.setProperty( "packageName", packageName );
				transpiler.setProperty( "baseclass", "BoxScript" );
				transpiler.setProperty( "returnType", "Object" );

				TranspiledCode				javaASTs	= transpiler.transpile( result.getRoot() );
				ClassOrInterfaceDeclaration	outerClass	= javaASTs.getEntryPoint().getClassByName( className ).get();

				// Process functions and lamdas
				for ( CompilationUnit callable : javaASTs.getCallables() ) {
					ClassOrInterfaceDeclaration innerClass = callable.findFirst( ClassOrInterfaceDeclaration.class ).get();
					outerClass.addMember( innerClass.setPublic( true ).setStatic( true ) );
				}
				compileSource( javaASTs.getEntryPoint().toString(), fqn );
			}
		}
		return getClass( fqn );

	}

	public Class<IBoxRunnable> compileScript( String source, BoxScriptType type ) {
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
					throw new BoxRuntimeException( "Error compiling source", e );
				}

				if ( !result.isCorrect() ) {
					throw new ParseException( result.getIssues() );
				}

				Transpiler transpiler = Transpiler.getTranspiler( null /* Config ? */ );
				transpiler.setProperty( "classname", className );
				transpiler.setProperty( "packageName", packageName );
				transpiler.setProperty( "baseclass", "BoxScript" );
				transpiler.setProperty( "returnType", "Object" );

				TranspiledCode				javaASTs	= transpiler.transpile( result.getRoot() );
				ClassOrInterfaceDeclaration	outerClass	= javaASTs.getEntryPoint().getClassByName( className ).get();

				// Process functions and lamdas
				for ( CompilationUnit callable : javaASTs.getCallables() ) {
					ClassOrInterfaceDeclaration innerClass = callable.findFirst( ClassOrInterfaceDeclaration.class ).get();
					outerClass.addMember( innerClass.setPublic( true ).setStatic( true ) );
				}
				if ( false )
					throw new BoxRuntimeException( javaASTs.getEntryPoint().toString() );

				compileSource( javaASTs.getEntryPoint().toString(), fqn );

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
					throw new BoxRuntimeException( "Error compiling source", e );
				}

				if ( !result.isCorrect() ) {
					throw new ParseException( result.getIssues() );
				}

				Transpiler transpiler = Transpiler.getTranspiler( null /* Config ? */ );
				transpiler.setProperty( "classname", className );
				transpiler.setProperty( "packageName", packageName );
				transpiler.setProperty( "baseclass", "BoxTemplate" );
				transpiler.setProperty( "returnType", "void" );
				TranspiledCode				javaASTs	= transpiler.transpile( result.getRoot() );
				ClassOrInterfaceDeclaration	outerClass	= javaASTs.getEntryPoint().getClassByName( className ).get();

				// Process functions and lamdas
				for ( CompilationUnit callable : javaASTs.getCallables() ) {
					ClassOrInterfaceDeclaration innerClass = callable.findFirst( ClassOrInterfaceDeclaration.class ).get();
					outerClass.addMember( innerClass.setPublic( true ).setStatic( true ) );
				}

				if ( false )
					throw new BoxRuntimeException( javaASTs.getEntryPoint().toString() );
				compileSource( javaASTs.getEntryPoint().toString(), fqn );

			}
		}
		return getClass( fqn );
	}

	public Class<IClassRunnable> compileClass( String source ) {
		String	packageName	= "generated";
		String	className	= "Class_" + MD5( source );
		String	fqn			= packageName + "." + className;

		if ( !classLoader.hasClass( fqn ) ) {
			if ( diskClassLoader.hasClass( fqn ) ) {
				return getDiskClassClass( fqn );
			} else {
				BoxParser		parser	= new BoxParser();
				ParsingResult	result;
				try {
					result = parser.parse( source, BoxScriptType.CFSCRIPT );
				} catch ( IOException e ) {
					throw new BoxRuntimeException( "Error compiling source", e );
				}

				if ( !result.isCorrect() ) {
					throw new ParseException( result.getIssues() );
				}

				Transpiler transpiler = Transpiler.getTranspiler( null );
				transpiler.setProperty( "classname", className );
				transpiler.setProperty( "packageName", packageName );

				TranspiledCode				javaASTs	= transpiler.transpile( result.getRoot() );
				ClassOrInterfaceDeclaration	outerClass	= javaASTs.getEntryPoint().getClassByName( className ).get();

				// Process functions and lamdas
				for ( CompilationUnit callable : javaASTs.getCallables() ) {
					ClassOrInterfaceDeclaration innerClass = callable.findFirst( ClassOrInterfaceDeclaration.class ).get();
					outerClass.addMember( innerClass.setPublic( true ).setStatic( true ) );
				}
				if ( false )
					throw new BoxRuntimeException( javaASTs.getEntryPoint().toString() );

				compileSource( javaASTs.getEntryPoint().toString(), fqn );

			}
		}
		return getClassClass( fqn );
	}

	public void compileSource( String javaSource, String fqn ) {

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
			String errors = diagnostics.getDiagnostics().stream().map( d -> d.toString() ).collect( Collectors.joining( "\n" ) );
			throw new BoxRuntimeException( errors + "\n" + javaSource );
		}

	}

	// get class
	public Class<IBoxRunnable> getClass( String fqn ) {
		try {
			return ( Class<IBoxRunnable> ) classLoader.loadClass( fqn );
		} catch ( ClassNotFoundException e ) {
			throw new BoxRuntimeException( "Error compiling source", e );
		}
	}

	public Class<IBoxRunnable> getDiskClass( String fqn ) {
		try {
			return ( Class<IBoxRunnable> ) diskClassLoader.loadClass( fqn );
		} catch ( ClassNotFoundException e ) {
			throw new BoxRuntimeException( "Error compiling source", e );
		}
	}

	public Class<IClassRunnable> getClassClass( String fqn ) {
		try {
			return ( Class<IClassRunnable> ) classLoader.loadClass( fqn );
		} catch ( ClassNotFoundException e ) {
			throw new BoxRuntimeException( "Error compiling source", e );
		}
	}

	public Class<IClassRunnable> getDiskClassClass( String fqn ) {
		try {
			return ( Class<IClassRunnable> ) diskClassLoader.loadClass( fqn );
		} catch ( ClassNotFoundException e ) {
			throw new BoxRuntimeException( "Error compiling source", e );
		}
	}

	public String getStatementsAsStringReturnLast( JavaTranspiler transpiler ) {
		StringBuilder	result		= new StringBuilder();
		boolean			returned	= false;
		// loop over statements
		for ( int i = 0; i < transpiler.getStatements().size(); i++ ) {
			// if last statement, return it
			if ( ( i == transpiler.getStatements().size() - 1 )
			    && ! ( transpiler.getStatements().get( i ).toString().contains( "ExceptionUtil.throwException(" ) ) ) {
				result.append( "return " );
				returned = true;
			}
			result.append( transpiler.getStatements().get( i ).toString() );
			if ( i < transpiler.getStatements().size() - 1 ) {
				result.append( ";\n" );
			}
		}
		if ( !returned ) {
			result.append( "\nreturn null;" );
		}
		return result.toString();
	}

	public String MD5( String md5 ) {
		try {
			java.security.MessageDigest	md		= java.security.MessageDigest.getInstance( "MD5" );
			byte[]						array	= md.digest( md5.getBytes() );
			StringBuilder				sb		= new StringBuilder();
			for ( int i = 0; i < array.length; ++i ) {
				sb.append( Integer.toHexString( ( array[ i ] & 0xFF ) | 0x100 ).substring( 1, 3 ) );
			}
			return sb.toString();
		} catch ( java.security.NoSuchAlgorithmException e ) {
			throw new BoxRuntimeException( "Error compiling source", e );
		}
	}

	/**
	 * Transforms the filename into the class name
	 *
	 * @param file File object to grab the class name for.
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
	 * @param file File object to grab the package name for.
	 *
	 * @return returns the class name according the name conventions Test.ext - Test$ext
	 */
	public static String getPackageName( File file ) {
		String packg = file.toString().replace( File.separatorChar + file.getName(), "" );
		if ( packg.startsWith( "/" ) ) {
			packg = packg.substring( 1 );
		}
		// trim trailing period
		if ( packg.endsWith( "." ) ) {
			packg = packg.substring( 0, packg.length() - 1 );
		}
		// trim trailing \ or /
		if ( packg.endsWith( "\\" ) || packg.endsWith( "/" ) ) {
			packg = packg.substring( 0, packg.length() - 1 );
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
		// Replace .. with .
		packg	= packg.replaceAll( "\\.\\.", "." );
		// Remove any non alpha-numeric chars.
		packg	= packg.replaceAll( "[^a-zA-Z0-9\\\\.]", "" );
		return packg.toLowerCase();

	}

}
