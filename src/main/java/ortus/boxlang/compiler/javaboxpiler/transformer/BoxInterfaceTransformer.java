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
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;

import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxInterface;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxStatement;
import ortus.boxlang.compiler.ast.BoxStaticInitializer;
import ortus.boxlang.compiler.ast.Source;
import ortus.boxlang.compiler.ast.SourceFile;
import ortus.boxlang.compiler.ast.expression.BoxIntegerLiteral;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;
import ortus.boxlang.compiler.ast.statement.BoxFunctionDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxImport;
import ortus.boxlang.compiler.javaboxpiler.JavaTranspiler;
import ortus.boxlang.compiler.javaboxpiler.transformer.expression.BoxStringLiteralTransformer;
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
		import ortus.boxlang.compiler.parser.BoxSourceType;
		import ortus.boxlang.compiler.ast.statement.BoxMethodDeclarationModifier;
		import ortus.boxlang.runtime.runnables.BoxClassSupport;

		// Java Imports
		import java.nio.file.Path;
		import java.nio.file.Paths;
		import java.time.LocalDateTime;
		import java.util.ArrayList;
		import java.util.Collections;
		import java.util.HashMap;
		import java.util.Iterator;
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
			private static Map<Key, AbstractFunction>	abstractMethods	= new LinkedHashMap<>();
			private static Map<Key, Function>	defaultMethods	= new LinkedHashMap<>();
			private static ${classname} instance;
			private static Key name = ${boxFQN};
			private static List<BoxInterface> _supers = new ArrayList();
			private static StaticScope staticScope;

			private ${classname}() {
			}

			public static void staticInitializer( IBoxContext context ) {
				ClassLocator classLocator = ClassLocator.getInstance();
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

			public static ${classname} getInstance( IBoxContext context ) {
				if ( instance == null ) {
					synchronized( ${classname}.class ) {
						if ( instance == null ) {
							instance = new ${classname}();
							staticScope = new StaticScope(instance);
							InterfaceBoxContext interContext = new InterfaceBoxContext( context, instance );
							// This makes the imports from the interface available
							interContext.pushTemplate( instance );
							instance.resolveSupers( interContext );
							${classname}.staticInitializer( interContext );
							instance.pseudoConstructor( interContext );
							interContext.popTemplate();
						}
					}
				}
				return instance;
			}

			public Map<Key, AbstractFunction> getAbstractMethods() {
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

			public void _addSuper( BoxInterface _super ) {
				this._supers.add( _super );
			}

			public List<BoxInterface> getSupers() {
				return this._supers;
			}

			// Instance method required to get from IClassRunnable
			public static StaticScope getStaticScopeStatic() {
				return staticScope;
			}

			// Static method required to get statically
			public StaticScope getStaticScope() {
				return ${className}.staticScope;
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

		BoxInterface					boxInterface	= ( BoxInterface ) node;
		Source							source			= boxInterface.getPosition().getSource();
		String							packageName		= transpiler.getProperty( "packageName" );
		String							boxFQN			= transpiler.getProperty( "boxFQN" );
		String							classname		= transpiler.getProperty( "classname" );
		String							mappingName		= transpiler.getProperty( "mappingName" );
		String							mappingPath		= transpiler.getProperty( "mappingPath" );
		String							relativePath	= transpiler.getProperty( "relativePath" );
		String							fileName		= source instanceof SourceFile file && file.getFile() != null ? file.getFile().getName() : "unknown";
		String							filePath		= source instanceof SourceFile file && file.getFile() != null ? file.getFile().getAbsolutePath()
		    : "unknown";
		String							sourceType		= transpiler.getProperty( "sourceType" );

		Map<String, String>				values			= Map.ofEntries(
		    Map.entry( "packagename", packageName ),
		    Map.entry( "classname", classname ),
		    Map.entry( "fileName", fileName ),
		    Map.entry( "resolvedFilePath", transpiler.getResolvedFilePath( mappingName, mappingPath, relativePath, filePath ) ),
		    Map.entry( "sourceType", sourceType ),
		    Map.entry( "compiledOnTimestamp", transpiler.getDateTime( LocalDateTime.now() ) ),
		    Map.entry( "compileVersion", "1L" ),
		    Map.entry( "boxFQN", createKey( boxFQN ).toString() )
		);
		String							code			= PlaceholderHelper.resolve( template, values );
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

		MethodDeclaration	staticInitializerMethod	= entryPoint.findCompilationUnit().orElseThrow()
		    .getClassByName( classname ).orElseThrow()
		    .getMethodsByName( "staticInitializer" ).get( 0 );

		FieldDeclaration	imports					= entryPoint
		    .getClassByName( classname ).orElseThrow()
		    .getFieldByName( "imports" ).orElseThrow();

		FieldDeclaration	keys					= entryPoint
		    .getClassByName( classname ).orElseThrow()
		    .getFieldByName( "keys" ).orElseThrow();

		/* Transform the annotations creating the initialization value */
		Expression			annotationStruct		= transformAnnotations( boxInterface.getAllAnnotations() );
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
					// process abstract function (no body)
					pseudoConstructorBody.addStatement( 0,
					    ( ( JavaTranspiler ) transpiler ).createAbstractMethod( bfd, this, classname, "interface" )
					);
				}
			} else if ( statement instanceof BoxImport ) {
				transpiler.transform( statement );
			} else if ( statement instanceof BoxStaticInitializer ) {
				transpiler.transform( statement );
			} else {
				throw new ExpressionException( "Statement type not supported in an interface: " + statement.getClass().getSimpleName(), statement );
			}
		}
		// loop over UDF registrations and add them to the _invoke() method
		( ( JavaTranspiler ) transpiler ).getUDFDeclarations().forEach( it -> {
			pseudoConstructorBody.addStatement( 0, it );
		} );

		// loop over static UDF registrations and add them to the static initializer
		( ( JavaTranspiler ) transpiler ).getStaticUDFDeclarations().forEach( it -> {
			staticInitializerMethod.getBody().get().addStatement( it );
		} );

		// For import statements, we add an argument to the constructor of the static List of imports
		MethodCallExpr imp = ( MethodCallExpr ) imports.getVariable( 0 ).getInitializer().orElseThrow();
		imp.getArguments().addAll( transpiler.getJImports() );

		// Add static initializers to the staticInitializer() method.
		transpiler.getStaticInitializers().forEach( it -> staticInitializerMethod.getBody().get().addStatement( it ) );

		// Add the keys to the static keys array
		ArrayCreationExpr keysImp = ( ArrayCreationExpr ) keys.getVariable( 0 ).getInitializer().orElseThrow();
		for ( Map.Entry<String, BoxExpression> entry : transpiler.getKeys().entrySet() ) {
			MethodCallExpr methodCallExpr = new MethodCallExpr( new NameExpr( "Key" ), "of" );
			if ( entry.getValue() instanceof BoxStringLiteral str ) {
				methodCallExpr.addArgument( BoxStringLiteralTransformer.transform( str.getValue() ) );
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
