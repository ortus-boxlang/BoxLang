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
package ortus.boxlang.compiler.javaboxpiler.transformer;

import java.time.LocalDateTime;
import java.util.Map;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;

import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxInterface;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxStatement;
import ortus.boxlang.compiler.ast.Source;
import ortus.boxlang.compiler.ast.SourceFile;
import ortus.boxlang.compiler.ast.expression.BoxIntegerLiteral;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;
import ortus.boxlang.compiler.ast.statement.BoxAccessModifier;
import ortus.boxlang.compiler.ast.statement.BoxFunctionDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxImport;
import ortus.boxlang.compiler.ast.statement.BoxReturnType;
import ortus.boxlang.compiler.ast.statement.BoxType;
import ortus.boxlang.compiler.javaboxpiler.JavaTranspiler;
import ortus.boxlang.runtime.config.util.PlaceholderHelper;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.ExpressionException;

public class BoxInterfaceTransformer extends AbstractTransformer {

	// @formatter:off
	private final String template = """
		package ${packageName};


		// BoxLang Auto Imports
		import ortus.boxlang.runtime.BoxRuntime;
		import ortus.boxlang.runtime.runnables.BoxInterface;
		import ortus.boxlang.runtime.components.Component;
		import ortus.boxlang.runtime.context.*;
		import ortus.boxlang.runtime.context.ClassBoxContext;
		import ortus.boxlang.runtime.context.FunctionBoxContext;
		import ortus.boxlang.runtime.dynamic.casters.*;
		import ortus.boxlang.runtime.dynamic.ExpressionInterpreter;
		import ortus.boxlang.runtime.dynamic.IReferenceable;
		import ortus.boxlang.runtime.dynamic.Referencer;
		import ortus.boxlang.runtime.interop.DynamicObject;
		import ortus.boxlang.runtime.interop.DynamicObject;
		import ortus.boxlang.runtime.loader.ClassLocator;
		import ortus.boxlang.runtime.loader.ImportDefinition;
		import ortus.boxlang.runtime.operators.*;
		import ortus.boxlang.runtime.runnables.BoxScript;
		import ortus.boxlang.runtime.runnables.BoxTemplate;
		import ortus.boxlang.runtime.runnables.IClassRunnable;
		import ortus.boxlang.runtime.scopes.*;
		import ortus.boxlang.runtime.scopes.Key;
		import ortus.boxlang.runtime.types.*;
		import ortus.boxlang.runtime.types.util.*;
		import ortus.boxlang.runtime.types.exceptions.*;
		import ortus.boxlang.runtime.types.exceptions.ExceptionUtil;
		import ortus.boxlang.runtime.types.meta.BoxMeta;
		import ortus.boxlang.runtime.types.meta.ClassMeta;
		import ortus.boxlang.runtime.types.Property;
		import ortus.boxlang.runtime.util.*;
		import ortus.boxlang.web.scopes.*;
		import ortus.boxlang.compiler.parser.BoxSourceType;

		// Java Imports
		import java.nio.file.Path;
		import java.nio.file.Paths;
		import java.time.LocalDateTime;
		import java.util.ArrayList;
		import java.util.Collections;
		import java.util.HashMap;
		import java.util.Iterator;
		import java.util.LinkedHashMap;
		import java.util.LinkedHashMap;
		import java.util.List;
		import java.util.Map;
		import java.util.Optional;

		public class ${classname} extends BoxInterface {
			
			private static final List<ImportDefinition>	imports			= List.of();
			private static final ResolvedFilePath					path			= ${resolvedFilePath};
			private static final BoxSourceType			sourceType		= BoxSourceType.${sourceType};
			private static final long					compileVersion	= ${compileVersion};
			private static final LocalDateTime			compiledOn		= ${compiledOnTimestamp};
			private static final Object					ast				= null;
			public static final Key[]					keys			= new Key[] {};


			private final static IStruct	annotations;
			private final static IStruct	documentation;
			private static Map<Key, Function>	abstractMethods	= new HashMap<>();
			private static Map<Key, Function>	defaultMethods	= new HashMap<>();
			private static ${classname} instance;
			private static Key name = ${boxInterfacename};

			static {
				${classname} instance = getInstance();
				InterfaceBoxContext interContext = new InterfaceBoxContext( null, instance );
				instance.pseudoConstructor( interContext );
			}


			private ${classname}() {
			}

			public void pseudoConstructor( IBoxContext context ) {
				context.pushTemplate( this );
				try {					
					_pseudoConstructor( context );
				} finally {
					context.popTemplate();
				}
			}

			public void _pseudoConstructor( IBoxContext context ) {
				ClassLocator classLocator = ClassLocator.getInstance();
			}

			public static ${classname} getInstance() {
				if ( instance == null ) {
					instance = new ${classname}();
				}
				return instance;
			}

			public Map<Key, Function> getAbstractMethods() {
				return this.abstractMethods;
			}
		
			public Map<Key, Function> getDefaultMethods() {
				return this.defaultMethods;
			}

			// ITemplateRunnable implementation methods

			/**
				* The version of the BoxLang runtime
			*/
			public long getRunnableCompileVersion() {
				return ${classname}.compileVersion;
			}

			/**
				* The date the template was compiled
			*/
			public LocalDateTime getRunnableCompiledOn() {
				return ${classname}.compiledOn;
			}

			/**
				* The AST (abstract syntax tree) of the runnable
			*/
			public Object getRunnableAST() {
				return ${classname}.ast;
			}

			/**
				* The path to the template
			*/
			public ResolvedFilePath getRunnablePath() {
				return ${classname}.path;
			}

			/**
			 * The original source type
			 */
			public BoxSourceType getSourceType() {
				return sourceType;
			}

			/**
			 * The imports for this runnable
			 */
			public List<ImportDefinition> getImports() {
				return imports;
			}

			public IStruct getAnnotations() {
				return annotations;
			}

			public IStruct getDocumentation() {
				return documentation;
			}

			/**
			 * Get the name
			 */
			public Key getName() {
				return this.name;
			}

		}
	""";
	// @formatter:on

	/**
	 * Constructor
	 *
	 * @param transpiler parent transpiler
	 */
	public BoxInterfaceTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {

		BoxInterface	boxInterface		= ( BoxInterface ) node;
		Source			source				= boxInterface.getPosition().getSource();
		String			packageName			= transpiler.getProperty( "packageName" );
		String			boxPackageName		= transpiler.getProperty( "boxPackageName" );
		String			classname			= transpiler.getProperty( "classname" );
		String			mappingName			= transpiler.getProperty( "mappingName" );
		String			mappingPath			= transpiler.getProperty( "mappingPath" );
		String			relativePath		= transpiler.getProperty( "relativePath" );
		String			fileName			= source instanceof SourceFile file && file.getFile() != null ? file.getFile().getName() : "unknown";
		String			filePath			= source instanceof SourceFile file && file.getFile() != null ? file.getFile().getAbsolutePath() : "unknown";
		String			boxInterfacename	= boxPackageName + "." + fileName.replace( ".bx", "" ).replace( ".cfc", "" );
		String			sourceType			= transpiler.getProperty( "sourceType" );

		// trim leading . if exists
		if ( boxInterfacename.startsWith( "." ) ) {
			boxInterfacename = boxInterfacename.substring( 1 );
		}

		Map<String, String>				values	= Map.ofEntries(
		    Map.entry( "packagename", packageName ),
		    Map.entry( "boxPackageName", boxPackageName ),
		    Map.entry( "classname", classname ),
		    Map.entry( "fileName", fileName ),
		    Map.entry( "resolvedFilePath", transpiler.getResolvedFilePath( mappingName, mappingPath, relativePath, filePath ) ),
		    Map.entry( "sourceType", sourceType ),
		    Map.entry( "compiledOnTimestamp", transpiler.getDateTime( LocalDateTime.now() ) ),
		    Map.entry( "compileVersion", "1L" ),
		    Map.entry( "boxInterfacename", createKey( boxInterfacename ).toString() )
		);
		String							code	= PlaceholderHelper.resolve( template, values );
		ParseResult<CompilationUnit>	result;

		try {
			result = javaParser.parse( code );
		} catch ( Exception e ) {
			// Temp debugging to see generated Java code
			throw new BoxRuntimeException( code, e );
		}
		if ( !result.isSuccessful() ) {
			// Temp debugging to see generated Java code
			throw new BoxRuntimeException(
			    "Error parsing class" + packageName + "." + classname + ". The message received was:" + result.toString() + "\n" + code );
		}

		CompilationUnit		entryPoint				= result.getResult().get();

		MethodDeclaration	pseudoConstructorMethod	= entryPoint.findCompilationUnit().orElseThrow()
		    .getClassByName( classname ).orElseThrow()
		    .getMethodsByName( "_pseudoConstructor" ).get( 0 );

		FieldDeclaration	imports					= entryPoint
		    .getClassByName( classname ).orElseThrow()
		    .getFieldByName( "imports" ).orElseThrow();

		FieldDeclaration	keys					= entryPoint
		    .getClassByName( classname ).orElseThrow()
		    .getFieldByName( "keys" ).orElseThrow();

		/* Transform the annotations creating the initialization value */
		Expression			annotationStruct		= transformAnnotations( boxInterface.getAnnotations() );
		entryPoint
		    .getClassByName( classname ).orElseThrow()
		    .getFieldByName( "annotations" ).orElseThrow()
		    .getVariable( 0 ).setInitializer( annotationStruct );

		/* Transform the documentation creating the initialization value */
		Expression documentationStruct = transformDocumentation( boxInterface.getDocumentation() );
		entryPoint
		    .getClassByName( classname ).orElseThrow()
		    .getFieldByName( "documentation" ).orElseThrow()
		    .getVariable( 0 ).setInitializer( documentationStruct );

		transpiler.pushContextName( "context" );
		var pseudoConstructorBody = pseudoConstructorMethod.getBody().orElseThrow();

		// Add imports
		for ( BoxImport statement : boxInterface.getImports() ) {
			// We'll pick these up later after any imports in the body have been found.
			transpiler.transform( statement );
		}
		// Add body
		for ( BoxStatement statement : boxInterface.getBody() ) {

			if ( statement instanceof BoxFunctionDeclaration bfd ) {
				if ( bfd.getBody() != null ) {
					transpiler.transform( statement );
				} else {
					BoxAccessModifier	access			= bfd.getAccessModifier();
					BoxReturnType		boxReturnType	= bfd.getType();
					BoxType				returnType		= BoxType.Any;
					String				fqn				= null;
					if ( boxReturnType != null ) {
						returnType = boxReturnType.getType();
						if ( returnType.equals( BoxType.Fqn ) ) {
							fqn = boxReturnType.getFqn();
						}
					}
					String returnTypeString = returnType.equals( BoxType.Fqn ) ? fqn : returnType.name();
					if ( access == null ) {
						access = BoxAccessModifier.Public;
					}
					ArrayInitializerExpr argInitializer = new ArrayInitializerExpr();
					bfd.getArgs().forEach( arg -> {
						Expression argument = ( Expression ) transpiler.transform( arg );
						argInitializer.getValues().add( argument );
					} );
					ArrayCreationExpr argumentsArray = new ArrayCreationExpr()
					    .setElementType( "Argument" )
					    .setInitializer( argInitializer );

					// process interface function (no body)
					pseudoConstructorBody.addStatement( 0,
					    new MethodCallExpr( new NameExpr( "abstractMethods" ), "put" )
					        .addArgument( createKey( bfd.getName() ) )
					        .addArgument(
					            new ObjectCreationExpr()
					                .setType( "AbstractFunction" )
					                .addArgument( createKey( bfd.getName() ) )
					                .addArgument( argumentsArray )
					                .addArgument( new StringLiteralExpr( returnTypeString ) )
					                .addArgument(
					                    new FieldAccessExpr( new FieldAccessExpr( new NameExpr( "Function" ), "Access" ), access.toString().toUpperCase() ) )
					                .addArgument( transformAnnotations( bfd.getAnnotations() ) )
					                .addArgument( transformDocumentation( bfd.getDocumentation() ) )
					        )
					);
				}
			} else if ( statement instanceof BoxImport ) {
				transpiler.transform( statement );
			} else {
				throw new ExpressionException( "Statement type not supported in an interface: " + statement.getClass().getSimpleName(), statement );
			}
		}
		// loop over UDF registrations and add them to the _invoke() method
		( ( JavaTranspiler ) transpiler ).getUDFDeclarations().forEach( it -> {
			pseudoConstructorBody.addStatement( 0, it );
		} );

		// For import statements, we add an argument to the constructor of the static List of imports
		MethodCallExpr imp = ( MethodCallExpr ) imports.getVariable( 0 ).getInitializer().orElseThrow();
		imp.getArguments().addAll( transpiler.getJImports() );

		// Add the keys to the static keys array
		ArrayCreationExpr keysImp = ( ArrayCreationExpr ) keys.getVariable( 0 ).getInitializer().orElseThrow();
		for ( Map.Entry<String, BoxExpression> entry : transpiler.getKeys().entrySet() ) {
			MethodCallExpr methodCallExpr = new MethodCallExpr( new NameExpr( "Key" ), "of" );
			if ( entry.getValue() instanceof BoxStringLiteral str ) {
				methodCallExpr.addArgument( new StringLiteralExpr( str.getValue() ) );
			} else if ( entry.getValue() instanceof BoxIntegerLiteral id ) {
				methodCallExpr.addArgument( new IntegerLiteralExpr( id.getValue() ) );
			} else {
				throw new ExpressionException( "Unsupported key type: " + entry.getValue().getClass().getSimpleName(), entry.getValue() );
			}
			keysImp.getInitializer().get().getValues().add( methodCallExpr );
		}

		transpiler.popContextName();

		return entryPoint;
	}

}
