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
package ortus.boxlang.compiler.javaboxpiler;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import com.fasterxml.jackson.jr.ob.JSON;
import com.fasterxml.jackson.jr.ob.JSON.Feature;
import com.fasterxml.jackson.jr.ob.JSONObjectException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import ortus.boxlang.compiler.Boxpiler;
import ortus.boxlang.compiler.ClassInfo;
import ortus.boxlang.compiler.JavaSourceString;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.javaboxpiler.transformer.ProxyTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.indexer.BoxNodeKey;
import ortus.boxlang.compiler.parser.Parser;
import ortus.boxlang.compiler.parser.ParsingResult;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.ExpressionException;
import ortus.boxlang.runtime.util.FileSystemUtil;
import ortus.boxlang.runtime.util.ResolvedFilePath;

/**
 * This class uses the Java compiler to turn a BoxLang script into a Java class
 */
public class JavaBoxpiler extends Boxpiler {

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Singleton instance
	 */
	private static JavaBoxpiler	instance;

	/**
	 * The Java compiler
	 */
	private JavaCompiler		compiler;

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Private constructor
	 */
	private JavaBoxpiler() {
		super();
		this.compiler = ToolProvider.getSystemJavaCompiler();
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

	@Override
	public void printTranspiledCode( ParsingResult result, ClassInfo classInfo, PrintStream target ) {
		target.print( generateJavaSource( result.getRoot(), classInfo ) );
	}

	/**
	 * Generate Java source code from BoxLang AST nodes
	 *
	 * @param node      The BoxLang root AST node
	 * @param classInfo The class info object for this class
	 *
	 * @return The generated Java source code as a string
	 */
	private String generateJavaSource( BoxNode node, ClassInfo classInfo ) {
		Transpiler transpiler = Transpiler.getTranspiler();
		transpiler.setProperty( "classname", classInfo.className() );
		transpiler.setProperty( "packageName", classInfo.packageName().toString() );
		transpiler.setProperty( "boxPackageName", classInfo.boxPackageName().toString() );
		transpiler.setProperty( "baseclass", classInfo.baseclass() );
		transpiler.setProperty( "returnType", classInfo.returnType() );
		transpiler.setProperty( "sourceType", classInfo.sourceType().name() );
		transpiler.setProperty( "mappingName", classInfo.resolvedFilePath() == null ? null : classInfo.resolvedFilePath().mappingName() );
		transpiler.setProperty( "mappingPath", classInfo.resolvedFilePath() == null ? null : classInfo.resolvedFilePath().mappingPath() );
		transpiler.setProperty( "relativePath", classInfo.resolvedFilePath() == null ? null : classInfo.resolvedFilePath().relativePath() );

		TranspiledCode	javaASTs;
		DynamicObject	trans	= frTransService.startTransaction( "Java Transpilation", classInfo.toString() );
		try {
			javaASTs = transpiler.transpile( node );
		} catch ( ExpressionException e ) {
			// These are fine as-is
			throw e;
		} catch ( Exception e ) {
			// This is for low-level bugs in the actual compilatin that are unexpected and can be hard to debug
			throw new BoxRuntimeException( "Error transpiling BoxLang to Java. " + classInfo.toString(), e );
		} finally {
			frTransService.endTransaction( trans );
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
		diskClassUtil.writeLineNumbers( classInfo.FQN(), generateLineNumberJSON( classInfo, prettyPrinter.getVisitor().getLineNumbers() ) );
		return javaSource;
	}

	@Override
	public void compileClassInfo( String FQN ) {
		ClassInfo classInfo = classPool.get( FQN );
		if ( classInfo == null ) {
			throw new BoxRuntimeException( "ClassInfo not found for " + FQN );
		}
		if ( classInfo.resolvedFilePath() != null ) {
			File sourceFile = classInfo.resolvedFilePath().absolutePath().toFile();
			// Check if the source file contains Java bytecode by reading the first few bytes
			if ( diskClassUtil.isJavaBytecode( sourceFile ) ) {
				System.out.println( "Loading bytecode direct from pre-compiled source file for " + FQN );
				classInfo.getClassLoader().defineClasses( FQN, sourceFile );
				return;
			}
			ParsingResult result = parseOrFail( sourceFile );
			compileSource( generateJavaSource( result.getRoot(), classInfo ), classInfo.FQN() );
		} else if ( classInfo.source() != null ) {
			ParsingResult result = parseOrFail( classInfo.source(), classInfo.sourceType(), classInfo.isClass() );
			compileSource( generateJavaSource( result.getRoot(), classInfo ), classInfo.FQN() );
		} else if ( classInfo.interfaceProxyDefinition() != null ) {
			compileSource( generateProxyJavaSource( classInfo ), classInfo.FQN() );
		} else {
			throw new BoxRuntimeException( "Unknown class info type: " + classInfo.toString() );
		}
	}

	/**
	 * Compile Java source code into a Java class
	 *
	 * @param javaSource The Java source code as a string
	 * @param fqn        The fully qualified name of the class
	 */
	@SuppressWarnings( "unused" )
	private void compileSource( String javaSource, String fqn ) {
		DynamicObject trans = frTransService.startTransaction( "Java Compilation", fqn );

		// This is just for debugging. Remove later.
		diskClassUtil.writeJavaSource( fqn, javaSource );
		try {

			DiagnosticCollector<JavaFileObject>	diagnostics	= new DiagnosticCollector<>();

			// Get the standard file manager
			StandardJavaFileManager				fileManager	= compiler.getStandardFileManager( diagnostics, null, null );

			// Set the location where .class files should be written
			fileManager.setLocation( StandardLocation.CLASS_OUTPUT, Arrays.asList( classGenerationDirectory.toFile() ) );

			String	javaRT	= System.getProperty( "java.class.path" );
			String	jarPath	= getClass().getProtectionDomain().getCodeSource().getLocation().getPath();

			// Am I in windows? If so, remove the leading slash from the jar path
			// Example: /C:/Users/username/... -> C:/Users/username/...
			if ( FileSystemUtil.IS_WINDOWS ) {
				jarPath = jarPath.substring( 1 );
			}

			List<JavaFileObject>			sourceFiles		= Collections.singletonList( new JavaSourceString( fqn, javaSource ) );
			List<String>					options			= List.of( "-g", "-cp", jarPath );
			JavaCompiler.CompilationTask	task			= compiler.getTask( null, fileManager, diagnostics, options, null, sourceFiles );
			boolean							compilerResult	= task.call();

			if ( !compilerResult ) {
				String errors = diagnostics.getDiagnostics().stream().map( d -> d.toString() )
				    .collect( Collectors.joining( "\n" ) );
				throw new BoxRuntimeException( errors + "\n" + javaSource );
			}
		} catch ( IOException e ) {
			throw new BoxRuntimeException( "Error compiling source " + fqn, e );
		} finally {
			frTransService.endTransaction( trans );
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
		List<LinkedHashMap<String, Object>>	stuff	= lineNumbers.stream().map( e -> {
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
		    .collect( Collectors.toList() );

		Map<String, Object>					output	= new HashMap<String, Object>();
		output.put( "sourceMapRecords", stuff );
		output.put( "source", classInfo.resolvedFilePath() == null ? null : classInfo.resolvedFilePath().absolutePath().toString() );
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

	private String generateProxyJavaSource( ClassInfo classInfo ) {
		return ProxyTransformer.transform( classInfo );
	}

	/**
	 * Compile a template, returning a list of byte arrays representing the compiled class and its inner classes
	 */
	@Override
	public List<byte[]> compileTemplateBytes( ResolvedFilePath resolvedFilePath ) {
		Path		path		= resolvedFilePath.absolutePath();
		ClassInfo	classInfo	= null;
		// file extension is .bx or .cfc
		if ( path.toString().endsWith( ".bx" ) || path.toString().endsWith( ".cfc" ) ) {
			classInfo = ClassInfo.forClass( resolvedFilePath, Parser.detectFile( path.toFile() ), this );
		} else {
			classInfo = ClassInfo.forTemplate( resolvedFilePath, Parser.detectFile( path.toFile() ), this );
		}
		classPool.putIfAbsent( classInfo.FQN(), classInfo );
		compileClassInfo( classInfo.FQN() );
		return diskClassUtil.readClassBytes( classInfo.FQN() );
	}

}
