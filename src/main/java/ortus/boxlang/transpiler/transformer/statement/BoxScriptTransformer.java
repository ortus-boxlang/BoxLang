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
import ortus.boxlang.transpiler.BoxLangTranspiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;

public class BoxScriptTransformer extends AbstractTransformer {

	// @formatter:off
	private final String template = """
		// Auto package creation according to file path on disk
		package ${packageName};

		import ortus.boxlang.runtime.BoxRuntime;
		import ortus.boxlang.runtime.context.*;

		// BoxLang Auto Imports
		import ortus.boxlang.runtime.BoxRuntime;
		import ortus.boxlang.runtime.context.*;

		// BoxLang Auto Imports
		import ortus.boxlang.runtime.runnables.BoxTemplate;
		import ortus.boxlang.runtime.dynamic.Referencer;
		import ortus.boxlang.runtime.interop.DynamicObject;
		import ortus.boxlang.runtime.loader.ClassLocator;
		import ortus.boxlang.runtime.operators.*;
		import ortus.boxlang.runtime.scopes.Key;
		import ortus.boxlang.runtime.scopes.IScope;
		import ortus.boxlang.runtime.scopes.LocalScope;
		import ortus.boxlang.runtime.scopes.VariablesScope;
		import ortus.boxlang.runtime.dynamic.casters.*;
		import ortus.boxlang.runtime.loader.ImportDefinition;


		// Classes Auto-Imported on all Templates and Classes by BoxLang
		import java.time.LocalDateTime;
		import java.time.Instant;
		import java.lang.System;
		import java.lang.String;
		import java.lang.Character;
		import java.lang.Boolean;
		import java.lang.Double;
		import java.lang.Integer;
		import java.util.*;
		import java.nio.file.*;

		public class ${className} extends BoxTemplate {

			// Auto-Generated Singleton Helpers
			private static ${className} instance;

			private static final List<ImportDefinition>	imports			= List.of();
			private static final Path					path			= Paths.get( "${fileFolderPath}" );
			private static final long					compileVersion	= ${compileVersion};
			private static final LocalDateTime			compiledOn		= ${compiledOnTimestamp};
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
			public void _invoke( IBoxContext context ) {
				// Reference to the variables scope
				IBoxContext			catchContext = null;
				IScope variablesScope = context.getScopeNearby( VariablesScope.name );
				ClassLocator JavaLoader = ClassLocator.getInstance();

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


			public static void main(String[] args) {
				BoxRuntime rt = BoxRuntime.getInstance();

				try {
					rt.executeTemplate( ${className}.getInstance() );
				} catch ( Throwable e ) {
					e.printStackTrace();
					System.exit( 1 );
				}

				// Bye bye! Ciao Bella!
				rt.shutdown();


			}
		}
	""";
	// @formatter:on

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {

		BoxScript	script			= ( BoxScript ) node;
		Source		source			= script.getPosition().getSource();
		String		packageName		= BoxLangTranspiler.getPackageName( source );
		String		className		= BoxLangTranspiler.getClassName( source );
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

		String							finalFilePath		= filePath;
		String							finalLastModified	= lastModified;
		Map<String, String>				values				= Map.ofEntries(
		    Map.entry( "packageName", packageName ),
		    Map.entry( "className", className ),
		    Map.entry( "fileName", fileName ),
		    Map.entry( "fileExtension", fileExt ),
		    Map.entry( "fileFolderPath", finalFilePath.replaceAll( "\\\\", "\\\\\\\\" ) ),
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
