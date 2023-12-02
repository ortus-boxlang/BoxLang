/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.transpiler.transformer.statement;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;

import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.BoxScript;
import ortus.boxlang.ast.Source;
import ortus.boxlang.ast.SourceFile;
import ortus.boxlang.runtime.config.util.PlaceholderHelper;
import ortus.boxlang.runtime.types.exceptions.ApplicationException;
import ortus.boxlang.transpiler.JavaTranspiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;

public class BoxScriptTransformer extends AbstractTransformer {

	// @formatter:off
	private final String template = """
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
		import ortus.boxlang.runtime.scopes.*;
		import ortus.boxlang.runtime.dynamic.casters.*;
		import ortus.boxlang.runtime.types.exceptions.ExceptionUtil;
		import ortus.boxlang.runtime.types.*;

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
				// Removed for now because this can be a moving target inside of a catch block
				//IScope variablesScope = context.getScopeNearby( Key.of( "variables" ) );
				ClassLocator classLocator = ClassLocator.getInstance();
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

			/**
			 * The imports for this runnable
			 */
			public List<ImportDefinition> getImports() {
				return imports;
			}

		}
	""";
	// @formatter:on

	/**
	 * Constructor
	 *
	 * @param transpiler parent transpiler
	 */
	public BoxScriptTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {

		BoxScript	script			= ( BoxScript ) node;
		Source		source			= script.getPosition().getSource();
		String		packageName		= JavaTranspiler.getPackageName( source );
		String		className		= JavaTranspiler.getClassName( source );
		String		fileName		= source instanceof SourceFile file && file.getFile() != null ? file.getFile().getName() : "unknown";
		String		fileExt			= fileName.substring( fileName.lastIndexOf( "." ) + 1 );
		String		filePath		= source instanceof SourceFile file && file.getFile() != null ? file.getFile().getAbsolutePath() : "unknown";
		// LocalDateTime lastModified = LocalDateTime.now();
		// LocalDateTime lastCompiled = LocalDateTime.now();
		String		lastModified	= getDateTime( LocalDateTime.now() );
		String		compiledOn		= getDateTime( LocalDateTime.now() );

		try {
			if ( source instanceof SourceFile file && file.getFile() != null ) {
				FileTime		fileTime	= Files.getLastModifiedTime( file.getFile().toPath() );
				LocalDateTime	ldt			= LocalDateTime.ofInstant( fileTime.toInstant(), ZoneId.systemDefault() );
				lastModified = getDateTime( ldt );
				File path = file.getFile().getCanonicalFile();
				filePath = path.toString().replace( File.separatorChar + path.getName(), "" );
			}
		} catch ( IOException e ) {
			throw new IllegalStateException();
		}

		//
		className	= transpiler.getProperty( "classname" ) != null ? transpiler.getProperty( "classname" ) : className;
		packageName	= transpiler.getProperty( "packageName" ) != null ? transpiler.getProperty( "packageName" ) : packageName;
		String	baseClass	= transpiler.getProperty( "baseclass" ) != null ? transpiler.getProperty( "baseclass" ) : "BoxScript";
		String	returnType	= baseClass.equals( "BoxScript" ) ? "Object" : "void";
		returnType = transpiler.getProperty( "returnType" ) != null ? transpiler.getProperty( "returnType" ) : returnType;

		String							finalFilePath		= filePath;
		String							finalLastModified	= lastModified;
		Map<String, String>				values				= Map.ofEntries(
		    Map.entry( "packagename", packageName ),
		    Map.entry( "className", className ),
		    Map.entry( "fileName", fileName ),
		    Map.entry( "baseclass", baseClass ),
		    Map.entry( "returnType", returnType ),
		    Map.entry( "fileExtension", fileExt ),
		    Map.entry( "fileFolderPath", "" /* finalFilePath.replaceAll( "\\\\", "\\\\\\\\" ) */ ),
		    Map.entry( "lastModifiedTimestamp", finalLastModified ),
		    Map.entry( "compiledOnTimestamp", compiledOn ),
		    Map.entry( "compileVersion", "1L" )
		);
		String							code				= PlaceholderHelper.resolve( template, values );
		ParseResult<CompilationUnit>	result;

		try {
			result = javaParser.parse( code );
		} catch ( Exception e ) {
			// Temp debugging to see generated Java code
			throw new ApplicationException( code, e );
		}
		if ( !result.isSuccessful() ) {
			// Temp debugging to see generated Java code
			throw new ApplicationException( result.toString() + "\n" + code );
		}

		return result.getResult().get();
	}

	private String getDateTime( LocalDateTime locaTime ) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern( "yyyy-MM-dd'T'hh:mm:ss" );

		return "LocalDateTime.parse(\"" + formatter.format( locaTime ) + "\")";
	}
}
