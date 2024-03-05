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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.jr.ob.JSON;
import com.fasterxml.jackson.jr.ob.JSON.Feature;
import com.fasterxml.jackson.jr.ob.JSONObjectException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.parser.BoxParser;
import ortus.boxlang.parser.BoxScriptType;
import ortus.boxlang.parser.ParsingResult;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.runnables.IBoxRunnable;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.ExpressionException;
import ortus.boxlang.runtime.types.exceptions.ParseException;
import ortus.boxlang.transpiler.CustomPrettyPrinter;
import ortus.boxlang.transpiler.TranspiledCode;
import ortus.boxlang.transpiler.Transpiler;
import ortus.boxlang.transpiler.transformer.indexer.BoxNodeKey;

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

	/**
	 * In-memory class loader for compiled classes
	 */
	private JavaMemoryManager		manager;

	/**
	 * The Java compiler
	 */
	private JavaCompiler			compiler;

	/**
	 * The class loader for compiled classes
	 */
	private JavaDynamicClassLoader	classLoader;

	/**
	 * The disk class loader
	 */
	private DiskClassLoader			diskClassLoader;

	/**
	 * Keeps track of how many times a class has been compiled
	 */
	private Map<String, Integer>	classCounter	= new HashMap<>();

	/**
	 * Logger
	 */
	private static final Logger		logger			= LoggerFactory.getLogger( JavaBoxpiler.class );

	/**
	 * The directory where the generated classes are stored
	 */
	private Path					classGenerationDirectory;

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Private constructor
	 */
	private JavaBoxpiler() {

		this.compiler					= ToolProvider.getSystemJavaCompiler();
		this.manager					= new JavaMemoryManager( compiler.getStandardFileManager( null, null, null ) );
		this.classGenerationDirectory	= Paths.get( BoxRuntime.getInstance().getConfiguration().compiler.classGenerationDirectory );

		// If we are in debug mode, let's clean out the class generation directory
		if ( BoxRuntime.getInstance().inDebugMode() && Files.exists( this.classGenerationDirectory ) ) {
			try {
				logger.debug( "Running in debugmode, first startup cleaning out class generation directory: " + classGenerationDirectory );
				FileUtils.cleanDirectory( classGenerationDirectory.toFile() );
			} catch ( IOException e ) {
				throw new BoxRuntimeException( "Error cleaning out class generation directory on first run", e );
			}
		}

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

	/**
	 * Compile a single BoxLang statement into a Java class
	 *
	 * @param source The BoxLang source code as a string
	 * @param type   The type of BoxLang source code
	 *
	 * @return The loaded class
	 */
	public Class<IBoxRunnable> compileStatement( String source, BoxScriptType type ) {
		ClassInfo classInfo = ClassInfo.forStatement( source, type );
		if ( !classLoader.hasClass( classInfo.FQN() ) ) {
			if ( diskClassLoader.hasClass( classInfo.FQN() ) ) {
				return getDiskClass( classInfo.FQN() );
			} else {
				ParsingResult result = parseOrFail( source, BoxScriptType.CFSCRIPT );
				compileSource( generateJavaSource( result.getRoot(), classInfo ), classInfo.FQN() );
			}
		}
		return getClass( classInfo.FQN() );

	}

	/**
	 * Compile a BoxLang script into a Java class
	 *
	 * @param source The BoxLang source code as a string
	 * @param type   The type of BoxLang source code
	 *
	 * @return The loaded class
	 */
	public Class<IBoxRunnable> compileScript( String source, BoxScriptType type ) {
		ClassInfo classInfo = ClassInfo.forScript( source, type );

		if ( !classLoader.hasClass( classInfo.FQN() ) ) {
			if ( diskClassLoader.hasClass( classInfo.FQN() ) ) {
				return getDiskClass( classInfo.FQN() );
			} else {
				ParsingResult result = parseOrFail( source, type );
				compileSource( generateJavaSource( result.getRoot(), classInfo ), classInfo.FQN() );
			}
		}
		return getClass( classInfo.FQN() );
	}

	/**
	 * Compile a BoxLang template (file on disk) into a Java class
	 *
	 * @param source      The BoxLang source on disk as a Path
	 * @param packagePath The package path used to resolve this file path
	 *
	 * @return The loaded class
	 */
	public Class<IBoxRunnable> compileTemplate( Path path, String packagePath ) {
		ClassInfo	classInfo		= ClassInfo.forTemplate( path, packagePath );
		long		lastModified	= path.toFile().lastModified();

		if ( !classLoader.hasClass( classInfo.FQN(), lastModified ) ) {
			if ( diskClassLoader.hasClass( classInfo.FQN(), lastModified ) ) {
				return getDiskClass( classInfo.FQN() );
			} else {
				classInfo = classInfo.next();
				classCounter.put( classInfo.originalFQN(), classInfo.compileCount() );
				ParsingResult result = parseOrFail( path.toFile() );
				compileSource( generateJavaSource( result.getRoot(), classInfo ), classInfo.FQN() );
			}
		}
		return getClass( classInfo.FQN() );
	}

	/**
	 * Compile a BoxLang Class from source into a Java class
	 *
	 * @param source The BoxLang source code as a string
	 *
	 * @return The loaded class
	 */
	public Class<IClassRunnable> compileClass( String source, BoxScriptType type ) {
		ClassInfo classInfo = ClassInfo.forClass( source );

		if ( !classLoader.hasClass( classInfo.FQN() ) ) {
			if ( diskClassLoader.hasClass( classInfo.FQN() ) ) {
				return getDiskClassClass( classInfo.FQN() );
			} else {
				ParsingResult result = parseOrFail( source, type );
				compileSource( generateJavaSource( result.getRoot(), classInfo ), classInfo.FQN() );
			}
		}
		return getClassClass( classInfo.FQN() );
	}

	/**
	 * Compile a BoxLang Class from a file into a Java class
	 *
	 * @param source      The BoxLang source code as a Path on disk
	 * @param packagePath The package path representing the mapping used to resolve this class
	 *
	 * @return The loaded class
	 */
	public Class<IClassRunnable> compileClass( Path path, String packagePath ) {
		ClassInfo	classInfo		= ClassInfo.forClass( path, packagePath );
		long		lastModified	= path.toFile().lastModified();

		if ( !classLoader.hasClass( classInfo.FQN(), lastModified ) ) {
			if ( diskClassLoader.hasClass( classInfo.FQN(), lastModified ) ) {
				return getDiskClassClass( classInfo.FQN() );
			} else {
				classInfo = classInfo.next();
				classCounter.put( classInfo.originalFQN(), classInfo.compileCount() );
				ParsingResult result = parseOrFail( path.toFile() );
				compileSource( generateJavaSource( result.getRoot(), classInfo ), classInfo.FQN() );
			}
		}
		return getClassClass( classInfo.FQN() );
	}

	/**
	 * Parse a file on disk into BoxLang AST nodes. This method will NOT throw an exception if the parse fails.
	 *
	 * @param file The file to parse
	 *
	 * @return The parsed AST nodes and any issues if encountered while parsing.
	 */
	public ParsingResult parse( File file ) {
		BoxParser parser = new BoxParser();
		try {
			return parser.parse( file );
		} catch ( IOException e ) {
			throw new BoxRuntimeException( "Error compiling source " + file.toString(), e );
		}
	}

	/**
	 * Parse source text into BoxLang AST nodes. This method will NOT throw an exception if the parse fails.
	 *
	 * @param file The file to parse
	 *
	 * @return The parsed AST nodes and any issues if encountered while parsing.
	 */
	public ParsingResult parse( String source, BoxScriptType type ) {
		BoxParser parser = new BoxParser();
		try {
			return parser.parse( source, type );
		} catch ( IOException e ) {
			throw new BoxRuntimeException( "Error compiling source", e );
		}
	}

	/**
	 * Parse a file on disk into BoxLang AST nodes. This method will throw an exception if the parse fails.
	 *
	 * @param file The file to parse
	 *
	 * @return The parsed AST nodes and any issues if encountered while parsing.
	 */
	public ParsingResult parseOrFail( File file ) {
		return validateParse( parse( file ), file.toString() );
	}

	/**
	 * Parse source text into BoxLang AST nodes. This method will throw an exception if the parse fails.
	 *
	 * @param file The file to parse
	 *
	 * @return The parsed AST nodes and any issues if encountered while parsing.
	 */
	public ParsingResult parseOrFail( String source, BoxScriptType type ) {
		return validateParse( parse( source, type ), "ad-hoc source" );
	}

	/**
	 * Validate a parsing result and throw an exception if the parse failed.
	 *
	 * @param result The parsing result to validate
	 *
	 * @return The parsing result if the parse was successful
	 */
	public ParsingResult validateParse( ParsingResult result, String source ) {
		if ( !result.isCorrect() ) {
			throw new ParseException( result.getIssues(), source );
		}
		return result;
	}

	/**
	 * Generate Java source code from BoxLang AST nodes
	 *
	 * @param node      The BoxLang root AST node
	 * @param classInfo The class info object for this class
	 *
	 * @return The generated Java source code as a string
	 */
	@SuppressWarnings( "unused" )
	public String generateJavaSource( BoxNode node, ClassInfo classInfo ) {
		Transpiler transpiler = Transpiler.getTranspiler();
		transpiler.setProperty( "classname", classInfo.className() );
		transpiler.setProperty( "packageName", classInfo.packageName() );
		transpiler.setProperty( "boxPackageName", classInfo.boxPackageName() );
		transpiler.setProperty( "baseclass", classInfo.baseclass() );
		transpiler.setProperty( "returnType", classInfo.returnType() );
		TranspiledCode javaASTs;
		try {
			javaASTs = transpiler.transpile( node );
		} catch ( ExpressionException e ) {
			// These are fine as-is
			throw e;
		} catch ( Exception e ) {
			// This is for low-level bugs in the actual compilatin that are unexpected and can be hard to debug
			throw new BoxRuntimeException( "Error transpiling BoxLang to Java. " + classInfo.toString(), e );
		}
		ClassOrInterfaceDeclaration outerClass = javaASTs.getEntryPoint().getClassByName( classInfo.className() ).get();

		// Process functions and lamdas
		for ( CompilationUnit callable : javaASTs.getCallables() ) {
			ClassOrInterfaceDeclaration innerClass = callable.findFirst( ClassOrInterfaceDeclaration.class )
			    .get();
			outerClass.addMember( innerClass.setPublic( true ).setStatic( true ) );
		}
		var		prettyPrinter	= new CustomPrettyPrinter();
		String	javaSource		= prettyPrinter.print( javaASTs.getEntryPoint() );

		if ( false )
			throw new BoxRuntimeException( javaSource );

		// Capture the line numbers of each Java AST node from printing the Java source
		diskClassLoader.writeLineNumbers( classInfo.FQN(), generateLineNumberJSON( classInfo, prettyPrinter.getVisitor().getLineNumbers() ) );
		return javaSource;
	}

	public SourceMap getSourceMapFromFQN( String FQN ) {
		// If fqn ends with $Cloure_xxx or $Func_xxx, $Lambda_xxx, then we need to strip that off to get the original FQN
		Matcher m = Pattern.compile( "(.*?)(\\$Closure_.*|\\$Func_.*|\\$Lambda_.*)$" ).matcher( FQN );
		if ( m.find() ) {
			FQN = m.group( 1 );
		}
		return diskClassLoader.readLineNumbers( FQN );
	}

	public boolean doesFilePathMatchFQNWithoutGeneration( Path sourcePath, String FQN ) {
		ClassInfo classInfo = ClassInfo.forTemplate( sourcePath, sourcePath.toString() );

		return classInfo.matchesFQNWithoutCompileCount( FQN );
	}

	/**
	 * Compile Java source code into a Java class
	 *
	 * @param javaSource The Java source code as a string
	 * @param fqn        The fully qualified name of the class
	 */
	@SuppressWarnings( "unused" )
	public void compileSource( String javaSource, String fqn ) {
		// System.out.println( "Compiling " + fqn );

		// This is just for debugging. Remove later.
		diskClassLoader.writeJavaSource( fqn, javaSource );

		DiagnosticCollector<JavaFileObject>	diagnostics		= new DiagnosticCollector<>();
		String								javaRT			= System.getProperty( "java.class.path" );
		List<JavaFileObject>				sourceFiles		= Collections.singletonList( new JavaSourceString( fqn, javaSource ) );
		List<String>						options			= List.of( "-g" );
		JavaCompiler.CompilationTask		task			= compiler.getTask( null, manager, diagnostics, options, null, sourceFiles );
		boolean								compilerResult	= task.call();

		if ( !compilerResult ) {
			String errors = diagnostics.getDiagnostics().stream().map( d -> d.toString() )
			    .collect( Collectors.joining( "\n" ) );
			throw new BoxRuntimeException( errors + "\n" + javaSource );
		}

	}

	/**
	 * Generate JSON for line numbers mapping BoxLang Source to the transpiled java source
	 *
	 * @param lineNumbers List of line numbers
	 *
	 * @return JSON string
	 */
	private String generateLineNumberJSON( ClassInfo classInfo, List<Object[]> lineNumbers ) {
		List	stuff	= lineNumbers.stream().map( e -> {
							Node	jNode		= ( Node ) e[ 0 ];
							int		jLineStart	= ( int ) e[ 1 ];
							int		jLineEnd	= ( int ) e[ 2 ];
							String	jClassName	= ( String ) e[ 3 ];
							// Not every node of Java source code has a corresponding BoxNode
							if ( !jNode.containsData( BoxNodeKey.BOX_NODE_DATA_KEY ) ) {
								return null;
							}
							BoxNode boxNode = jNode.getData( BoxNodeKey.BOX_NODE_DATA_KEY );
							// Some BoxNodes were created on-the-fly by the parser and don't correspond to any specific source line
							if ( boxNode.getPosition() == null ) {
								return null;
							}
							LinkedHashMap<String, Object> map = new LinkedHashMap<>();
							map.put( "javaSourceLineStart", jLineStart );
							map.put( "javaSourceLineEnd", jLineEnd );
							map.put( "originSourceLineStart", boxNode.getPosition().getStart().getLine() );
							map.put( "originSourceLineEnd", boxNode.getPosition().getEnd().getLine() );
							map.put( "javaSourceClassName", jClassName );
							// Really just for debugging. Remove later.
							map.put( "javaSourceNode", jNode.getClass().getSimpleName() );
							map.put( "originSourceNode", boxNode.getClass().getSimpleName() );
							return map;
						} )
		    // filter out nulls
		    .filter( e -> e != null )
		    // sort by origin source line, then by java source line
		    .sorted( ( e1, e2 ) -> {
			    int result2 = ( ( Integer ) e1.get( "originSourceLineStart" ) )
			        .compareTo( ( Integer ) e2.get( "originSourceLineStart" ) );
			    if ( result2 == 0 ) {
				    result2 = ( ( Integer ) e1.get( "javaSourceLineStart" ) )
				        .compareTo( ( Integer ) e2.get( "javaSourceLineStart" ) );
			    }
			    return result2;
		    } )
		    .toList();

		Map		output	= new HashMap<String, Object>();
		output.put( "sourceMapRecords", stuff );
		output.put( "source", classInfo.sourcePath );
		try {
			return JSON.std.with( Feature.PRETTY_PRINT_OUTPUT ).asString( output );
		} catch ( JSONObjectException e ) {
			e.printStackTrace();
			return null;
		} catch ( IOException e ) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Get a class for a class name
	 *
	 * @param fqn The fully qualified name of the class
	 *
	 * @return The loaded class
	 */
	public Class<IBoxRunnable> getClass( String fqn ) {
		try {
			return ( Class<IBoxRunnable> ) classLoader.loadClass( fqn );
		} catch ( ClassNotFoundException e ) {
			throw new BoxRuntimeException( "Error compiling source " + fqn, e );
		}
	}

	/**
	 * Get a class for a class name from disk
	 *
	 * @param fqn The fully qualified name of the class
	 *
	 * @return The loaded class
	 */
	public Class<IBoxRunnable> getDiskClass( String fqn ) {
		try {
			return ( Class<IBoxRunnable> ) diskClassLoader.loadClass( fqn );
		} catch ( ClassNotFoundException e ) {
			throw new BoxRuntimeException( "Error compiling source " + fqn, e );
		}
	}

	/**
	 * Get a Box class for a class name
	 *
	 * @param fqn The fully qualified name of the class
	 *
	 * @return The loaded class
	 */
	public Class<IClassRunnable> getClassClass( String fqn ) {
		try {
			return ( Class<IClassRunnable> ) classLoader.loadClass( fqn );
		} catch ( ClassNotFoundException e ) {
			throw new BoxRuntimeException( "Error compiling source " + fqn, e );
		}
	}

	/**
	 * Get a Box class for a class name from disk
	 *
	 * @param fqn The fully qualified name of the class
	 *
	 * @return The loaded class
	 */
	public Class<IClassRunnable> getDiskClassClass( String fqn ) {
		try {
			return ( Class<IClassRunnable> ) diskClassLoader.loadClass( fqn );
		} catch ( ClassNotFoundException e ) {
			throw new BoxRuntimeException( "Error compiling source " + fqn, e );
		}
	}

	/**
	 * Generate an MD5 hash.
	 * TODO: Move to util class
	 *
	 * @param md5 String to hash
	 *
	 * @return MD5 hash
	 */
	public static String MD5( String md5 ) {
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
	 * @return returns the class name according the name conventions Test.ext -
	 *         Test$ext
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
	 * @return returns the class name according the name conventions Test.ext -
	 *         Test$ext
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
		// TODO: This needs a lot more work. There are tons of disallowed edge cases
		// such as a folder that is a number.
		// We probably need to iterate each path segment and clean or remove as
		// neccessary to make it a valid package name.
		// Also, I'd like cfincluded files to use the relative path as the package name,
		// which will require some refactoring.

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
		packg	= packg.replaceAll( "[^a-zA-Z0-9\\.]", "" );
		return packg.toLowerCase();

	}

	/**
	 * Get line numbers for fqn
	 *
	 * @param fqn The fully qualified name of the class
	 *
	 * @return The line numbers
	 */
	public SourceMap getLineNumbers( String fqn ) {
		return diskClassLoader.readLineNumbers( fqn );
	}

	/**
	 * A Record that represents the information about a class to be compiled
	 */
	public record ClassInfo( String sourcePath, String packageName, String className, int compileCount, String boxPackageName, String baseclass,
	    String returnType ) {

		public static ClassInfo forScript( String source, BoxScriptType type ) {
			return new ClassInfo(
			    null,
			    "generated",
			    "Script_" + MD5( type.toString() + source ),
			    0,
			    "boxgenerated.generated",
			    "BoxScript",
			    "Object"
			);
		}

		public static ClassInfo forStatement( String source, BoxScriptType type ) {
			return new ClassInfo( null, "generated", "Statement_" + MD5( type.toString() + source ), 0, "boxgenerated.generated", "BoxScript", "Object" );
		}

		public static ClassInfo forTemplate( Path path, String packagePath ) {
			File	lcaseFile	= new File( packagePath.toString().toLowerCase() );
			String	packageName	= getPackageName( lcaseFile );
			// if package name has starting dot, remove it
			if ( packageName.startsWith( "." ) ) {
				packageName = packageName.substring( 1 );
			}
			packageName = "boxgenerated.templates" + ( packageName.equals( "" ) ? "" : "." ) + packageName;
			String className = getClassName( lcaseFile );
			return new ClassInfo(
			    path.toString(),
			    packageName,
			    className,
			    JavaBoxpiler.getInstance().getClassCounter().getOrDefault( packageName + "." + className, 0 ),
			    packageName,
			    "BoxTemplate",
			    "void"
			);
		}

		public static ClassInfo forClass( Path path, String packagePath ) {
			String boxPackagePath = "boxgenerated." + packagePath;
			if ( boxPackagePath.endsWith( "." ) ) {
				boxPackagePath = boxPackagePath.substring( 0, boxPackagePath.length() - 1 );
			}
			packagePath = "boxgenerated.boxclass." + packagePath;
			// trim trailing period
			if ( packagePath.endsWith( "." ) ) {
				packagePath = packagePath.substring( 0, packagePath.length() - 1 );
			}
			String className = getClassName( path.toFile() );

			return new ClassInfo(
			    path.toString(),
			    packagePath,
			    className,
			    JavaBoxpiler.getInstance().getClassCounter().getOrDefault( packagePath + "." + className, 0 ),
			    boxPackagePath,
			    null,
			    null
			);
		}

		public static ClassInfo forClass( String source ) {
			return new ClassInfo(
			    null,
			    "generated",
			    "Class_" + MD5( source ),
			    0,
			    "boxgenerated.generated",
			    null,
			    null
			);
		}

		/**
		 * Called when we need to re-compile a class because it's already been compiled
		 *
		 * @return new ClassInfo object with the compile count incremented
		 */
		public ClassInfo next() {
			return new ClassInfo(
			    this.sourcePath,
			    this.packageName,
			    this.className,
			    this.compileCount + 1,
			    this.boxPackageName,
			    this.baseclass,
			    this.returnType );
		}

		public String FQN() {
			return packageName + "." + className();
		}

		public String className() {
			return className + compileCount;
		}

		public String originalClassName() {
			return className;
		}

		public String originalFQN() {
			return packageName + "." + originalClassName();
		}

		public boolean matchesFQNWithoutCompileCount( String FQN ) {
			Pattern	pattern	= Pattern.compile( originalFQN().replace( "$", "\\$" ) + ".+$" );
			Matcher	matcher	= pattern.matcher( FQN );

			return matcher.find();
		}

		public String toString() {
			return "Class Info-- sourcePath: [" + sourcePath + "], packageName: [" + packageName + "], className: [" + className + "]";
		}

	}

	/**
	 * Get the Class Counter map
	 *
	 * @return the classCounter
	 */
	public Map<String, Integer> getClassCounter() {
		return classCounter;
	}

}
