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
package ortus.boxlang.transpiler.transformer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;

import ortus.boxlang.ast.BoxClass;
import ortus.boxlang.ast.BoxExpr;
import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.BoxStatement;
import ortus.boxlang.ast.Source;
import ortus.boxlang.ast.SourceFile;
import ortus.boxlang.ast.expression.BoxFQN;
import ortus.boxlang.ast.expression.BoxIntegerLiteral;
import ortus.boxlang.ast.expression.BoxNull;
import ortus.boxlang.ast.expression.BoxStringLiteral;
import ortus.boxlang.ast.statement.BoxAnnotation;
import ortus.boxlang.ast.statement.BoxImport;
import ortus.boxlang.ast.statement.BoxProperty;
import ortus.boxlang.runtime.config.util.PlaceholderHelper;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.transpiler.JavaTranspiler;

public class BoxClassTransformer extends AbstractTransformer {

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
		import ortus.boxlang.web.scopes.*;
		import ortus.boxlang.runtime.dynamic.casters.*;
		import ortus.boxlang.runtime.types.exceptions.ExceptionUtil;
		import ortus.boxlang.runtime.types.*;
		import ortus.boxlang.runtime.types.exceptions.*;
		import ortus.boxlang.runtime.runnables.IClassRunnable;
		import ortus.boxlang.runtime.dynamic.IReferenceable;
		import ortus.boxlang.runtime.context.FunctionBoxContext;
		import ortus.boxlang.runtime.context.ClassBoxContext;
		import ortus.boxlang.runtime.types.meta.BoxMeta;
		import ortus.boxlang.runtime.types.meta.ClassMeta;
		import ortus.boxlang.runtime.interop.DynamicObject;
		import ortus.boxlang.runtime.types.Property;
		import ortus.boxlang.runtime.types.MapHelper;
		import ortus.boxlang.runtime.dynamic.ExpressionInterpreter;
		import java.util.Optional;

		import java.nio.file.Path;
		import java.nio.file.Paths;
		import java.time.LocalDateTime;
		import java.util.List;
		import java.util.Iterator;
		import java.util.Map;
		import java.util.HashMap;
		import java.util.LinkedHashMap;
		import java.util.ArrayList;
		import java.util.Collections;
		import java.util.LinkedHashMap;

		public class ${className} implements IClassRunnable, IReferenceable, IType {

			private static final List<ImportDefinition>	imports			= List.of();
			private static final Path					path			= Paths.get( "${fileFolderPath}" );
			private static final long					compileVersion	= ${compileVersion};
			private static final LocalDateTime			compiledOn		= ${compiledOnTimestamp};
			private static final Object					ast				= null;
			public static final Key[]					keys			= new Key[] {};

			/**
			 * Metadata object
			 */
			public BoxMeta						$bx;

			/**
			 * Cached lookup of the output annotation
			 */
			private Boolean			canOutput			= null;

			private final static IStruct	annotations;
			private final static IStruct	documentation;
			// replace Object with record/class to represent a property
			private final static Map<Key,Property>	properties;
			private final static Map<Key,Property>	getterLookup=null;
			private final static Map<Key,Property>	setterLookup=null;

			private VariablesScope variablesScope = new VariablesScope();
			private ThisScope thisScope = new ThisScope();
			private Key name = ${boxClassName};
			private IClassRunnable _super = null;
			private IClassRunnable child = null;

			public ${className}() {
			}

			public Map<Key,Property> getGetterLookup() {
				return getterLookup;
			}
			public Map<Key,Property> getSetterLookup() {
				return setterLookup;
			}

			public void pseudoConstructor( IBoxContext context ) {
				context.pushTemplate( this );
				try {
					// loop over properties and create variables for those with non-null defult values
					for ( var property : properties.values()) {
						if ( property.defaultValue() != null ) {
							variablesScope.assign( context, property.name(), property.defaultValue() );
						}
					}
					// TODO: pre/post interceptor announcements here
					_pseudoConstructor( context );
				} finally {
					context.popTemplate();
				}
			}

			public void _pseudoConstructor( IBoxContext context ) {
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

			/**
			 * Get the variables scope
			 */
			public VariablesScope getVariablesScope() {
				return variablesScope;
			}

			/**
			 * Get the this scope
			 */
			public ThisScope getThisScope() {
				return thisScope;
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

			/**
			 * Get the properties
			 */
			public Map<Key,Property> getProperties() {
				return this.properties;
			}

			public BoxMeta getBoxMeta() {
				if ( this.$bx == null ) {
					this.$bx = new ClassMeta( this );
				}
				return this.$bx;
			}

			/**
			 * Represent as string, or throw exception if not possible
			 *
			 * @return The string representation
			 */
			public String asString() {
				return "Class: " + name.getName();
			}

			/**
			 * A helper to look at the "output" annotation, caching the result
			 *
			 * @return Whether the function can output
			 */
			public boolean canOutput() {
				// Initialize if neccessary
				if ( this.canOutput == null ) {
					this.canOutput = BooleanCaster.cast( getAnnotations().getOrDefault( Key.output, false ) );
				}
				return this.canOutput;
			}

			/**
			 * Get the super class.  Null if there is none
			 */
			public IClassRunnable getSuper() {
				return this._super;
			}

			/**
			 * Set the super class.
			 */
			public void setSuper( IClassRunnable _super ) {
				this._super = _super;
				_super.setChild( this );
				variablesScope.addAll( _super.getVariablesScope().getWrapped() );
				thisScope.addAll( _super.getThisScope().getWrapped() );
				// TODO: merge properties
			}

			/**
			 * Get the child class.  Null if there is none
			 */
			public IClassRunnable getChild() {
				return this.child;
			}

			/**
			 * Set the child class.
			 */
			public void setChild( IClassRunnable child ) {
				this.child = child;
			}

			/**
			 * Get the bottom class in the inheritance chain
			 */
			public IClassRunnable getBottomClass() {
				if( getChild() != null ) {
					return getChild().getBottomClass();
				}
				return this;
			}

			/**
			 * --------------------------------------------------------------------------
			 * IReferenceable Interface Methods
			 * --------------------------------------------------------------------------
			 */

			/**
			 * Assign a value to a key
			 *
			 * @param key   The key to assign
			 * @param value The value to assign
			 */
			public Object assign( IBoxContext context, Key key, Object value ) {
				// TODO: implicit setters
				thisScope.assign( context, key, value );
				return value;
			}

			/**
			 * Dereference this object by a key and return the value, or throw exception
			 *
			 * @param key  The key to dereference
			 * @param safe Whether to throw an exception if the key is not found
			 *
			 * @return The requested object
			 */
			public Object dereference( IBoxContext context, Key key, Boolean safe ) {

				// Special check for $bx
				if ( key.equals( BoxMeta.key ) ) {
					return getBoxMeta();
				}

				// TODO: implicit getters
				return thisScope.dereference( context, key, safe );
			}

			/**
			 * Dereference this object by a key and invoke the result as an invokable (UDF, java method) using positional arguments
			 *
			 * @param name                The key to dereference
			 * @param positionalArguments The positional arguments to pass to the invokable
			 * @param safe                Whether to throw an exception if the key is not found
			 *
			 * @return The requested object
			 */
			public Object dereferenceAndInvoke( IBoxContext context, Key name, Object[] positionalArguments, Boolean safe ) {
				// TODO: component member methods?

				BaseScope scope = thisScope;
				// we are a super class, so we reached here via super.method()
				if( getChild() != null ) {
					scope = variablesScope;
				}

				// Look for function in this
				Object value = scope.get( name );
				if ( value instanceof Function function ) {
					FunctionBoxContext functionContext = Function.generateFunctionContext(
						function,
						// Function contexts' parent is the caller.  The function will "know" about the CFC it's executing in
						// because we've pushed the CFC onto the template stack in the function context.
						context,
						name,
						function.createArgumentsScope( context, positionalArguments )
					);
					
					functionContext.setThisClass( this );
					functionContext.pushTemplate( this );

					try {
						return function.invoke( functionContext );
					} finally{
						functionContext.popTemplate();
					}
				}

				if ( value != null ) {
					throw new BoxRuntimeException(
						"key '" + name.getName() + "' of type  '" + value.getClass().getName() + "'  is not a function " );
				}

				// Check for generated accessors
				Object hasAccessors = getAnnotations().get( Key.accessors );
				if ( hasAccessors != null && BooleanCaster.cast( hasAccessors ) ) {
					Property getterProperty = getterLookup.get( name );
 					if( getterProperty != null ) {
						return variablesScope.dereference( context, getterLookup.get( name ).name(), safe );
					}
					Property setterProperty = setterLookup.get( name );
					if( setterProperty != null ) {
						Key thisName = setterProperty.name();
						if( positionalArguments.length == 0 ) {
							throw new BoxRuntimeException( "Missing argument for setter '" + name.getName() + "'" );
						}
						variablesScope.assign( context, thisName, positionalArguments[0] );
						return this;
					}
				}

				if( thisScope.get( Key.onMissingMethod ) != null ){
					return dereferenceAndInvoke( context, Key.onMissingMethod, new Object[]{ name.getName(), positionalArguments }, safe );
				}

				if( !safe ) {
					throw new BoxRuntimeException( "Method '" + name.getName() + "' not found" );
				}
				return null;
			}

			/**
			 * Dereference this object by a key and invoke the result as an invokable (UDF, java method)
			 *
			 * @param name           The name of the key to dereference, which becomes the method name
			 * @param namedArguments The arguments to pass to the invokable
			 * @param safe           If true, return null if the method is not found, otherwise throw an exception
			 *
			 * @return The requested return value or null
			 */
			public Object dereferenceAndInvoke( IBoxContext context, Key name, Map<Key, Object> namedArguments, Boolean safe ) {

				BaseScope scope = thisScope;
				// we are a super class, so we reached here via super.method()
				if( getChild() != null ) {
					scope = variablesScope;
				}

				Object value = scope.get( name );
				if ( value instanceof Function function ) {
					FunctionBoxContext functionContext = Function.generateFunctionContext(
							function,
							// Function contexts' parent is the caller.  The function will "know" about the CFC it's executing in
							// because we've pushed the CFC onto the template stack in the function context.
							context,
							name,
							function.createArgumentsScope(  context, namedArguments )
						);

					functionContext.setThisClass( this );
					functionContext.pushTemplate( this );
					try {
						return function.invoke( functionContext );
					} finally{
						functionContext.popTemplate();
					}
				}

				if( getSuper() != null && getSuper().getThisScope().get( name ) != null ) {
					return getSuper().dereferenceAndInvoke( context, name, namedArguments, safe );
				}

				if ( value != null ) {
					throw new BoxRuntimeException(
						"key '" + name.getName() + "' of type  '" + value.getClass().getName() + "'  is not a function " );
				}

				// Check for generated accessors
				Object hasAccessors = getAnnotations().get( Key.accessors );
				if ( hasAccessors != null && BooleanCaster.cast( hasAccessors ) ) {
					Property getterProperty = getterLookup.get( name );
 					if( getterProperty != null ) {
						return variablesScope.dereference( context, getterProperty.name(), safe );
					}
					Property setterProperty = setterLookup.get( name );
					if( setterProperty != null ) {
						Key thisName = setterProperty.name();
						if( !namedArguments.containsKey( thisName ) ) {
							throw new BoxRuntimeException( "Missing argument for setter '" + name.getName() + "'" );
						}
						variablesScope.assign( context, thisName, namedArguments.get( thisName ) );
						return this;
					}
				}

				if( thisScope.get( Key.onMissingMethod ) != null ){
					Map<Key, Object> args = new HashMap<Key, Object>();
					args.put( Key.missingMethodName, name.getName() );
					args.put( Key.missingMethodArguments, namedArguments );
					return dereferenceAndInvoke( context, Key.onMissingMethod, args, safe );
				}

				if( !safe ) {
					throw new BoxRuntimeException( "Method '" + name.getName() + "' not found" );
				}
				return null;
			}


			/**
			 * Get the combined metadata for this function and all it's parameters
			 * This follows the format of Lucee and Adobe's "combined" metadata
			 * TODO: Move this to compat module
			 *
			 * @return The metadata as a struct
			 */
			public IStruct getMetaData() {
				IStruct meta = new Struct();
				if ( getDocumentation() != null ) {
					meta.putAll( getDocumentation() );
				}
				if ( getAnnotations() != null ) {
					meta.putAll( getAnnotations() );
				}
				meta.putIfAbsent( "hint", "" );
				meta.putIfAbsent( "output", false );

				// Assemble the metadata
				var functions = new ArrayList<Object>();
				// loop over target's variables scope and add metadata for each function
				for ( var entry : thisScope.keySet() ) {
					var value = thisScope.get( entry );
					if ( value instanceof Function fun ) {
						functions.add( fun.getMetaData() );
					}
				}

				meta.put( "name", getName().getName() );
				meta.put( "accessors", false );
				if( getSuper() != null ) {
					meta.put( "extends", getSuper().getMetaData() );
				} else {
					meta.put( "extends", Struct.EMPTY );
				}
				meta.put( "functions", Array.fromList( functions ) );
				meta.put( "hashCode", hashCode() );
				var properties = new Array();
				// loop over properties list and add struct for each property
				for ( var entry : this.properties.entrySet() ) {
					var property = entry.getValue();
					var propertyStruct = new Struct();
					propertyStruct.put( "name", property.name().getName() );
					propertyStruct.put( "type", property.type() );
					propertyStruct.put( "default", property.defaultValue() );
					if ( property.documentation() != null ) {
						meta.putAll( property.documentation() );
					}
					if ( property.annotations() != null ) {
						meta.putAll( property.annotations() );
					}
					properties.add( propertyStruct );
				}
				meta.put( "properties", properties );
				meta.put( "type", "Component" );
				meta.put( "name", getName().getName() );
				meta.put( "fullname", getName().getName() );
				meta.put( "path", getRunnablePath().toString() );
				meta.put( "persisent", false );
				meta.put( "output", false );

				return meta;
			}

		}
	""";
	// @formatter:on

	/**
	 * Constructor
	 *
	 * @param transpiler parent transpiler
	 */
	public BoxClassTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {

		BoxClass	boxClass		= ( BoxClass ) node;
		Source		source			= boxClass.getPosition().getSource();
		String		packageName		= transpiler.getProperty( "packageName" );
		String		boxPackageName	= transpiler.getProperty( "boxPackageName" );
		String		className		= transpiler.getProperty( "classname" );
		String		fileName		= source instanceof SourceFile file && file.getFile() != null ? file.getFile().getName() : "unknown";
		String		fileExt			= fileName.substring( fileName.lastIndexOf( "." ) + 1 );
		String		filePath		= source instanceof SourceFile file && file.getFile() != null ? file.getFile().getAbsolutePath()
		    : "unknown";
		String		boxClassName	= boxPackageName + "." + fileName.replace( ".bx", "" ).replace( ".cfc", "" );
		// trim leading . if exists
		if ( boxClassName.startsWith( "." ) ) {
			boxClassName = boxClassName.substring( 1 );
		}

		Map<String, String>				values	= Map.ofEntries(
		    Map.entry( "packagename", packageName ),
		    Map.entry( "boxPackageName", boxPackageName ),
		    Map.entry( "className", className ),
		    Map.entry( "fileName", fileName ),
		    Map.entry( "fileExtension", fileExt ),
		    Map.entry( "fileFolderPath", filePath.replaceAll( "\\\\", "\\\\\\\\" ) ),
		    Map.entry( "compiledOnTimestamp", transpiler.getDateTime( LocalDateTime.now() ) ),
		    Map.entry( "compileVersion", "1L" ),
		    Map.entry( "boxClassName", createKey( boxClassName ).toString() )
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
			    "Error parsing class" + packageName + "." + className + ". The message received was:" + result.toString() + "\n" + code );
		}

		CompilationUnit		entryPoint				= result.getResult().get();

		MethodDeclaration	pseudoConstructorMethod	= entryPoint.findCompilationUnit().orElseThrow()
		    .getClassByName( className ).orElseThrow()
		    .getMethodsByName( "_pseudoConstructor" ).get( 0 );

		FieldDeclaration	imports					= entryPoint.findCompilationUnit().orElseThrow()
		    .getClassByName( className ).orElseThrow()
		    .getFieldByName( "imports" ).orElseThrow();

		FieldDeclaration	keys					= entryPoint.findCompilationUnit().orElseThrow()
		    .getClassByName( className ).orElseThrow()
		    .getFieldByName( "keys" ).orElseThrow();

		/* Transform the annotations creating the initialization value */
		Expression			annotationStruct		= transformAnnotations( boxClass.getAnnotations() );
		result.getResult().orElseThrow().getType( 0 ).getFieldByName( "annotations" ).orElseThrow().getVariable( 0 ).setInitializer( annotationStruct );

		/* Transform the documentation creating the initialization value */
		Expression documentationStruct = transformDocumentation( boxClass.getDocumentation() );
		result.getResult().orElseThrow().getType( 0 ).getFieldByName( "documentation" ).orElseThrow().getVariable( 0 )
		    .setInitializer( documentationStruct );

		List<Expression> propertyStructs = transformProperties( boxClass.getProperties() );
		result.getResult().orElseThrow().getType( 0 ).getFieldByName( "properties" ).orElseThrow().getVariable( 0 )
		    .setInitializer( propertyStructs.get( 0 ) );
		result.getResult().orElseThrow().getType( 0 ).getFieldByName( "getterLookup" ).orElseThrow().getVariable( 0 )
		    .setInitializer( propertyStructs.get( 1 ) );
		result.getResult().orElseThrow().getType( 0 ).getFieldByName( "setterLookup" ).orElseThrow().getVariable( 0 )
		    .setInitializer( propertyStructs.get( 2 ) );

		transpiler.pushContextName( "context" );
		var pseudoConstructorBody = pseudoConstructorMethod.getBody().orElseThrow();

		// Add imports
		for ( BoxImport statement : boxClass.getImports() ) {
			Node			javaASTNode	= transpiler.transform( statement );
			// For import statements, we add an argument to the constructor of the static List of imports
			MethodCallExpr	imp			= ( MethodCallExpr ) imports.getVariable( 0 ).getInitializer().orElseThrow();
			imp.getArguments().add( ( MethodCallExpr ) javaASTNode );
		}
		// Add body
		for ( BoxStatement statement : boxClass.getBody() ) {

			Node javaASTNode = transpiler.transform( statement );
			// Java block get each statement in their block added
			if ( javaASTNode instanceof BlockStmt ) {
				BlockStmt stmt = ( BlockStmt ) javaASTNode;
				stmt.getStatements().forEach( it -> {
					pseudoConstructorBody.addStatement( it );
					// statements.add( it );
				} );
			} else if ( statement instanceof BoxImport ) {
				// For import statements, we add an argument to the constructor of the static List of imports
				MethodCallExpr imp = ( MethodCallExpr ) imports.getVariable( 0 ).getInitializer().orElseThrow();
				imp.getArguments().add( ( MethodCallExpr ) javaASTNode );
			} else {
				// All other statements are added to the _invoke() method
				pseudoConstructorBody.addStatement( ( Statement ) javaASTNode );
				// statements.add( ( Statement ) javaASTNode );
			}
			// loop over UDF registrations and add them to the _invoke() method
			( ( JavaTranspiler ) transpiler ).getUDFDeclarations().forEach( it -> {
				pseudoConstructorBody.addStatement( 0, it );
			} );
		}

		// Add the keys to the static keys array
		ArrayCreationExpr keysImp = ( ArrayCreationExpr ) keys.getVariable( 0 ).getInitializer().orElseThrow();
		for ( Map.Entry<String, BoxExpr> entry : transpiler.getKeys().entrySet() ) {
			MethodCallExpr methodCallExpr = new MethodCallExpr( new NameExpr( "Key" ), "of" );
			if ( entry.getValue() instanceof BoxStringLiteral str ) {
				methodCallExpr.addArgument( new StringLiteralExpr( str.getValue() ) );
			} else if ( entry.getValue() instanceof BoxIntegerLiteral id ) {
				methodCallExpr.addArgument( new IntegerLiteralExpr( id.getValue() ) );
			} else {
				throw new IllegalStateException( "Unsupported key type: " + entry.getValue().getClass().getSimpleName() );
			}
			keysImp.getInitializer().get().getValues().add( methodCallExpr );
		}

		transpiler.popContextName();
		String	text			= entryPoint.toString();
		String	numberedText	= IntStream.range( 0, text.split( "\n" ).length )
		    .mapToObj( index -> ( index + 1 ) + " " + text.split( "\n" )[ index ] )
		    .collect( Collectors.joining( "\n" ) );

		// System.out.println( numberedText );

		return entryPoint;
	}

	/**
	 * Transforms a collection of properties into a Map
	 *
	 * @param properties list of properties
	 *
	 * @return an Expression node
	 */
	private List<Expression> transformProperties( List<BoxProperty> properties ) {
		List<Expression>	members			= new ArrayList<Expression>();
		List<Expression>	getterLookup	= new ArrayList<Expression>();
		List<Expression>	setterLookup	= new ArrayList<Expression>();
		properties.forEach( prop -> {
			Expression			documentationStruct		= transformDocumentation( prop.getDocumentation() );
			/*
			 * normalize annotations to allow for
			 * property String userName;
			 */
			List<BoxAnnotation>	finalAnnotations		= new ArrayList<BoxAnnotation>();
			var					annotations				= prop.getAnnotations();
			int					namePosition			= annotations.stream().map( BoxAnnotation::getKey ).map( BoxFQN::getValue ).map( String::toLowerCase )
			    .collect( java.util.stream.Collectors.toList() ).indexOf( "name" );
			int					typePosition			= annotations.stream().map( BoxAnnotation::getKey ).map( BoxFQN::getValue ).map( String::toLowerCase )
			    .collect( java.util.stream.Collectors.toList() ).indexOf( "type" );
			int					defaultPosition			= annotations.stream().map( BoxAnnotation::getKey ).map( BoxFQN::getValue ).map( String::toLowerCase )
			    .collect( java.util.stream.Collectors.toList() ).indexOf( "default" );
			int					numberOfNonValuedKeys	= ( int ) annotations.stream().map( BoxAnnotation::getValue ).filter( it -> it == null ).count();
			List<BoxAnnotation>	nonValuedKeys			= annotations.stream().filter( it -> it.getValue() == null )
			    .collect( java.util.stream.Collectors.toList() );
			BoxAnnotation		nameAnnotation			= null;
			BoxAnnotation		typeAnnotation			= null;
			BoxAnnotation		defaultAnnotation		= null;

			if ( namePosition > -1 )
				nameAnnotation = annotations.get( namePosition );
			if ( typePosition > -1 )
				typeAnnotation = annotations.get( typePosition );
			if ( defaultPosition > -1 )
				defaultAnnotation = annotations.get( defaultPosition );
			/*
			 * If there is no name, if there is more than one nonvalued keys and no type, use the first nonvalued key
			 * as the type and second nonvalued key as the name. Otherwise, if there are more than one non-valued key, use the first as the name.
			 */
			if ( namePosition == -1 ) {
				if ( numberOfNonValuedKeys > 1 && typePosition == -1 ) {
					typeAnnotation	= new BoxAnnotation( new BoxFQN( "type", null, null ),
					    new BoxStringLiteral( nonValuedKeys.get( 0 ).getKey().getValue(), null, null ), null,
					    null );
					nameAnnotation	= new BoxAnnotation( new BoxFQN( "name", null, null ),
					    new BoxStringLiteral( nonValuedKeys.get( 1 ).getKey().getValue(), null, null ), null,
					    null );
					finalAnnotations.add( nameAnnotation );
					finalAnnotations.add( typeAnnotation );
					annotations.remove( nonValuedKeys.get( 0 ) );
					annotations.remove( nonValuedKeys.get( 1 ) );
				} else if ( numberOfNonValuedKeys > 0 ) {
					nameAnnotation = new BoxAnnotation( new BoxFQN( "name", null, null ),
					    new BoxStringLiteral( nonValuedKeys.get( 0 ).getKey().getValue(), null, null ), null,
					    null );
					finalAnnotations.add( nameAnnotation );
					annotations.remove( nonValuedKeys.get( 0 ) );
				} else {
					throw new BoxRuntimeException( "Property [" + prop.getSourceText() + "] has no name" );
				}
			}
			// add type with value of any if not present
			if ( typeAnnotation == null ) {
				typeAnnotation = new BoxAnnotation( new BoxFQN( "type", null, null ), new BoxStringLiteral( "any", null, null ), null,
				    null );
				finalAnnotations.add( typeAnnotation );
			}
			// add default with value of null if not present
			if ( defaultPosition == -1 ) {
				defaultAnnotation = new BoxAnnotation( new BoxFQN( "default", null, null ), new BoxNull( null, null ), null,
				    null );
				finalAnnotations.add( defaultAnnotation );
			}
			// add remaining annotations
			finalAnnotations.addAll( annotations );

			Expression	annotationStruct	= transformAnnotations( finalAnnotations );
			/* Process default value */
			String		init				= "null";
			if ( defaultAnnotation.getValue() != null ) {
				Node initExpr = transpiler.transform( defaultAnnotation.getValue() );
				init = initExpr.toString();
			}
			// name and type must be simple values
			String	name;
			String	type;
			if ( nameAnnotation.getValue() instanceof BoxStringLiteral namelit ) {
				name = namelit.getValue().trim();
				if ( name.isEmpty() )
					throw new BoxRuntimeException( "Property [" + prop.getSourceText() + "] name cannot be empty" );
			} else {
				throw new BoxRuntimeException( "Property [" + prop.getSourceText() + "] name must be a simple value" );
			}
			if ( typeAnnotation.getValue() instanceof BoxStringLiteral typelit ) {
				type = typelit.getValue().trim();
				if ( type.isEmpty() )
					throw new BoxRuntimeException( "Property [" + prop.getSourceText() + "] type cannot be empty" );
			} else {
				throw new BoxRuntimeException( "Property [" + prop.getSourceText() + "] type must be a simple value" );
			}
			Expression			jNameKey	= ( Expression ) createKey( name );
			Expression			jGetNameKey	= ( Expression ) createKey( "get" + name );
			Expression			jSetNameKey	= ( Expression ) createKey( "set" + name );
			Map<String, String>	values		= Map.of(
			    "type", type,
			    "name", jNameKey.toString(),
			    "init", init,
			    "annotations", annotationStruct.toString(),
			    "documentation", documentationStruct.toString()
			);
			String				template	= """
			                                  				new Property( ${name}, "${type}", ${init}, ${annotations} ,${documentation} )
			                                  """;
			Expression			javaExpr	= ( Expression ) parseExpression( template, values );
			logger.debug( "{} -> {}", prop.getSourceText(), javaExpr );

			members.add( jNameKey );
			members.add( javaExpr );

			// Check if getter key annotation is defined in finalAnnotations and false
			boolean getter = !finalAnnotations.stream()
			    .anyMatch( it -> it.getKey().getValue().equalsIgnoreCase( "getter" ) && !BooleanCaster.cast( it.getValue() ) );
			if ( getter ) {
				getterLookup.add( jGetNameKey );
				getterLookup.add( ( Expression ) parseExpression( "properties.get( ${name} )", values ) );
			}
			// Check if setter key annotation is defined in finalAnnotations and false
			boolean setter = !finalAnnotations.stream()
			    .anyMatch( it -> it.getKey().getValue().equalsIgnoreCase( "setter" ) && !BooleanCaster.cast( it.getValue() ) );
			if ( setter ) {
				setterLookup.add( jSetNameKey );
				setterLookup.add( ( Expression ) parseExpression( "properties.get( ${name} )", values ) );
			}
		} );
		if ( members.isEmpty() ) {
			Expression emptyMap = ( Expression ) parseExpression( "Collections.emptyMap()", new HashMap<>() );
			return List.of( emptyMap, emptyMap, emptyMap );
		} else {
			MethodCallExpr	propertiesStruct	= ( MethodCallExpr ) parseExpression( "MapHelper.LinkedHashMapOfProperties()", new HashMap<>() );
			MethodCallExpr	getterStruct		= ( MethodCallExpr ) parseExpression( "MapHelper.HashMapOfProperties()", new HashMap<>() );
			MethodCallExpr	setterStruct		= ( MethodCallExpr ) parseExpression( "MapHelper.HashMapOfProperties()", new HashMap<>() );
			propertiesStruct.getArguments().addAll( members );
			getterStruct.getArguments().addAll( getterLookup );
			setterStruct.getArguments().addAll( setterLookup );
			return List.of( propertiesStruct, getterStruct, setterStruct );
		}
	}

}
