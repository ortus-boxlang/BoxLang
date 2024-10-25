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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.UnknownType;

import ortus.boxlang.compiler.ast.BoxClass;
import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxStatement;
import ortus.boxlang.compiler.ast.Source;
import ortus.boxlang.compiler.ast.SourceFile;
import ortus.boxlang.compiler.ast.expression.BoxBooleanLiteral;
import ortus.boxlang.compiler.ast.expression.BoxFQN;
import ortus.boxlang.compiler.ast.expression.BoxIntegerLiteral;
import ortus.boxlang.compiler.ast.expression.BoxNull;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;
import ortus.boxlang.compiler.ast.statement.BoxAnnotation;
import ortus.boxlang.compiler.ast.statement.BoxArgumentDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxFunctionDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxImport;
import ortus.boxlang.compiler.ast.statement.BoxProperty;
import ortus.boxlang.compiler.ast.statement.BoxReturnType;
import ortus.boxlang.compiler.ast.statement.BoxType;
import ortus.boxlang.compiler.javaboxpiler.JavaTranspiler;
import ortus.boxlang.runtime.config.util.PlaceholderHelper;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.javaproxy.InterfaceProxyService;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.ExpressionException;
import ortus.boxlang.runtime.types.util.BLCollector;
import ortus.boxlang.runtime.types.util.ListUtil;

public class BoxClassTransformer extends AbstractTransformer {

	// @formatter:off
	private static final String CLASS_TEMPLATE = """
		package ${packageName};

		// BoxLang Auto Imports
		import ortus.boxlang.compiler.ast.statement.BoxMethodDeclarationModifier;
		import ortus.boxlang.compiler.parser.BoxSourceType;
		import ortus.boxlang.runtime.BoxRuntime;
		import ortus.boxlang.runtime.components.Component;
		import ortus.boxlang.runtime.context.*;
		import ortus.boxlang.runtime.context.ClassBoxContext;
		import ortus.boxlang.runtime.context.FunctionBoxContext;
		import ortus.boxlang.runtime.dynamic.casters.*;
		import ortus.boxlang.runtime.dynamic.ExpressionInterpreter;
		import ortus.boxlang.runtime.dynamic.IReferenceable;
		import ortus.boxlang.runtime.dynamic.Referencer;
		import ortus.boxlang.runtime.interop.DynamicObject;
		import ortus.boxlang.runtime.loader.ClassLocator;
		import ortus.boxlang.runtime.loader.ImportDefinition;
		import ortus.boxlang.runtime.operators.*;
		import ortus.boxlang.runtime.runnables.BoxClassSupport;
		import ortus.boxlang.runtime.runnables.BoxInterface;
		import ortus.boxlang.runtime.runnables.BoxScript;
		import ortus.boxlang.runtime.runnables.BoxTemplate;
		import ortus.boxlang.runtime.runnables.IClassRunnable;
		import ortus.boxlang.runtime.scopes.*;
		import ortus.boxlang.runtime.types.*;
		import ortus.boxlang.runtime.types.exceptions.*;
		import ortus.boxlang.runtime.types.meta.BoxMeta;
		import ortus.boxlang.runtime.types.meta.ClassMeta;
		import ortus.boxlang.runtime.types.Property;
		import ortus.boxlang.runtime.types.util.*;
		import ortus.boxlang.runtime.util.*;
		import ortus.boxlang.runtime.util.conversion.ObjectMarshaller;

		// Java Imports
		import java.io.*;
		import java.lang.invoke.MethodHandle;
		import java.lang.invoke.MethodHandles;
		import java.lang.invoke.MethodType;
		import java.lang.reflect.Field;
		import java.lang.reflect.Method;
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
		import java.util.Set;
		import java.util.Optional;


		public class ${className} ${extendsTemplate} implements ${interfaceList} {

			// Public static fields
			public static final Key[] keys = new Key[] {};

			// Private Static fields
			private static final long serialVersionUID = ${compileVersion};
			private static final List<ImportDefinition>	imports	= List.of();
			private static final ResolvedFilePath path = ${resolvedFilePath};
			private static final BoxSourceType sourceType = BoxSourceType.${sourceType};
			private static final long compileVersion = ${compileVersion};
			private static final LocalDateTime compiledOn = ${compiledOnTimestamp};
			private static final Object	ast	= null;
			private static final IStruct annotations;
			private static final IStruct documentation;
			private static final Map<Key,Property>	properties;
			private static final Map<Key,Property>	getterLookup=null;
			private static final Map<Key,Property>	setterLookup=null;
			private static Map<Key, AbstractFunction>	abstractMethods	= new LinkedHashMap<>();
			private static Set<Key> compileTimeMethodNames = ${compileTimeMethodNames};
			private static final boolean isJavaExtends=${isJavaExtends};
			private static StaticScope staticScope = new StaticScope();
			// This is public so the ClassLocator can check it easily
			public static boolean staticInitialized = false;

			// Private instance fields
			private VariablesScope variablesScope = new ClassVariablesScope(this);
			private ThisScope thisScope = new ThisScope();
			private Key name = ${boxFQN};
			private IClassRunnable _super = null;
			private IClassRunnable child = null;
			private Boolean canOutput = null;
			private Boolean canInvokeImplicitAccessor = null;
			private List<BoxInterface> interfaces = new ArrayList<>();

			// Public instance fields
			public BoxMeta		$bx;

			public ${className}() {
			}

			public static void staticInitializer( IBoxContext context ) {
				ClassLocator classLocator = ClassLocator.getInstance();
			}

			public Map<Key,Property> getGetterLookup() {
				return getterLookup;
			}
			public Map<Key,Property> getSetterLookup() {
				return setterLookup;
			}

			public Map<Key, AbstractFunction> getAbstractMethods() {
				return this.abstractMethods;
			}

			public Map<Key, AbstractFunction> getAllAbstractMethods() {
				// get from parent and override
				Map<Key, AbstractFunction> allAbstractMethods = new LinkedHashMap<>();
				if ( this._super != null ) {
					allAbstractMethods.putAll( this._super.getAllAbstractMethods() );
				}
				allAbstractMethods.putAll( this.abstractMethods );
				return allAbstractMethods;
			}

			public Set<Key> getCompileTimeMethodNames() {
				return compileTimeMethodNames;
			}

			public BoxMeta _getbx() {
				return this.$bx;
			}

			public void _setbx( BoxMeta bx ) {
				this.$bx = bx;
			}

			public void pseudoConstructor( IBoxContext context ) {
				BoxClassSupport.pseudoConstructor( this, context );
			}

			public void _pseudoConstructor( IBoxContext context ) {
				ClassLocator classLocator = ClassLocator.getInstance();
			}

			// ITemplateRunnable implementation methods

			public long getRunnableCompileVersion() {
				return ${className}.compileVersion;
			}

			public LocalDateTime getRunnableCompiledOn() {
				return ${className}.compiledOn;
			}

			public Object getRunnableAST() {
				return ${className}.ast;
			}

			public ResolvedFilePath getRunnablePath() {
				return ${className}.path;
			}

			public BoxSourceType getSourceType() {
				return sourceType;
			}

			public List<ImportDefinition> getImports() {
				return imports;
			}

			public VariablesScope getVariablesScope() {
				return variablesScope;
			}

			public ThisScope getThisScope() {
				return thisScope;
			}

			// Instance method required to get from IClassRunnable
			public static StaticScope getStaticScopeStatic() {
				return staticScope;
			}

			// Static method required to get statically
			public StaticScope getStaticScope() {
				return ${className}.staticScope;
			}

			public IStruct getAnnotations() {
				return annotations;
			}

			public static IStruct getAnnotationsStatic() {
				return ${className}.annotations;
			}

			public IStruct getDocumentation() {
				return documentation;
			}

			public Key getName() {
				return this.name;
			}

			public Map<Key,Property> getProperties() {
				return this.properties;
			}

			public BoxMeta getBoxMeta() {
				return BoxClassSupport.getBoxMeta( this );
			}

			public String asString() {
				return BoxClassSupport.asString( this );
			}

			public Boolean canOutput() {
				return BoxClassSupport.canOutput( this );
			}

			public Boolean getCanOutput() {
				return this.canOutput;
			}

			public void setCanOutput( Boolean canOutput ) {
				this.canOutput = canOutput;
			}

			public Boolean canInvokeImplicitAccessor( IBoxContext context ) {
				return BoxClassSupport.canInvokeImplicitAccessor( this, context );
			}

			public Boolean getCanInvokeImplicitAccessor() {
				return this.canInvokeImplicitAccessor;
			}

			public void setCanInvokeImplicitAccessor( Boolean canInvokeImplicitAccessor ) {
				this.canInvokeImplicitAccessor = canInvokeImplicitAccessor;
			}

			public IClassRunnable getSuper() {
				return this._super;
			}

			public void setSuper( IClassRunnable _super ) {
				BoxClassSupport.setSuper( this, _super );
			}

			public void _setSuper( IClassRunnable _super ) {
				this._super = _super;
			}

			public IClassRunnable getChild() {
				return this.child;
			}

			public void setChild( IClassRunnable child ) {
				this.child = child;
			}

			public IClassRunnable getBottomClass() {
				return BoxClassSupport.getBottomClass( this );
			}

			/**
			 * --------------------------------------------------------------------------
			 * Serialize Methods
			 * --------------------------------------------------------------------------
			 * We only use the serialize, since we wrap the state into a
			 * BoxClassState class.
			 */

			private Object writeReplace() throws ObjectStreamException {
				return ObjectMarshaller.serializeClass( this );
			}

			/**
			 * --------------------------------------------------------------------------
			 * IReferenceable Interface Methods
			 * --------------------------------------------------------------------------
			 */

			public Object assign( IBoxContext context, Key key, Object value ) {
				return BoxClassSupport.assign( this, context, key, value );
			}

			public Object dereference( IBoxContext context, Key key, Boolean safe ) {
				return BoxClassSupport.dereference( this, context, key, safe );
			}

			public Object dereferenceAndInvoke( IBoxContext context, Key name, Object[] positionalArguments, Boolean safe ) {
				return BoxClassSupport.dereferenceAndInvoke( this, context, name, positionalArguments, safe );
			}

			public Object dereferenceAndInvoke( IBoxContext context, Key name, Map<Key, Object> namedArguments, Boolean safe ) {
					return BoxClassSupport.dereferenceAndInvoke( this, context, name, namedArguments, safe );
			}

			public IStruct getMetaData() {
				return BoxClassSupport.getMetaData( this );
			}

			public void registerInterface( BoxInterface _interface ) {
				BoxClassSupport.registerInterface( this, _interface );
			}

			public List<BoxInterface> getInterfaces() {
				return this.interfaces;
			}

			public boolean isJavaExtends() {
				return isJavaExtends;
			}

			/**
			 * This code MUST be inside the class to allow for the lookupPrivate method to work
			 * This proxy is called from the dynamic interop service when calling a super method
			 * while using java extends, and it will return the method handle for the corresponding
			 * method in the super class.
			 */
			public MethodHandle lookupPrivateMethod( Method method ) {
				try {
					return MethodHandles.lookup().findSpecial(
						method.getDeclaringClass(),
						method.getName(),
						MethodType.methodType(method.getReturnType(), method.getParameterTypes()),
						this.getClass()
					);
				} catch (NoSuchMethodException | IllegalAccessException e) {
					throw new BoxRuntimeException( "Error getting Java super class method " + method.getName(), e );
				}
			}

			/**
			 * Same as above
			 */
			public MethodHandle lookupPrivateField( Field field ) {
				try {
					return MethodHandles.lookup().unreflectGetter( field );
				} catch ( IllegalAccessException e) {
					throw new BoxRuntimeException( "Error getting Java super class field " + field.getName(), e );
				}
			}

			${interfaceMethods}

			${extendsMethods}

		}
	""";
	// @formatter:on

	/**
	 * The marker used to indicate that a method should be overridden in the Java class
	 */
	private static final String	EXTENDS_ANNOTATION_MARKER	= "overrideJava";

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
		BoxClass		boxClass			= ( BoxClass ) node;
		Source			source				= boxClass.getPosition().getSource();
		String			packageName			= transpiler.getProperty( "packageName" );
		String			boxFQN				= transpiler.getProperty( "boxFQN" );
		String			className			= transpiler.getProperty( "classname" );
		String			mappingName			= transpiler.getProperty( "mappingName" );
		String			mappingPath			= transpiler.getProperty( "mappingPath" );
		String			relativePath		= transpiler.getProperty( "relativePath" );
		String			interfaceMethods	= "";
		String			extendsTemplate		= "";
		String			extendsMethods		= "";
		String			isJavaExtends		= "false";
		// The list of automatically implemented interfaces
		List<String>	interfaces			= new ArrayList<>();
		interfaces.add( "IClassRunnable" );
		interfaces.add( "IReferenceable" );
		interfaces.add( "IType" );
		interfaces.add( "Serializable" );

		/**
		 * --------------------------------------------------------------------------
		 * Process Interface Annotations
		 * --------------------------------------------------------------------------
		 */
		BoxExpression implementsValue = boxClass.getAnnotations().stream()
		    .filter( it -> it.getKey().getValue().equalsIgnoreCase( "implements" ) )
		    .findFirst()
		    .map( it -> it.getValue() )
		    .orElse( null );
		if ( implementsValue instanceof BoxStringLiteral str ) {
			String	implementsStringList		= str.getValue();
			// Collect and trim all strings starting with "java:"
			Array	implementsArray				= ListUtil.asList( implementsStringList, "," ).stream()
			    .map( String::valueOf )
			    .map( String::trim )
			    .filter( it -> it.toLowerCase().startsWith( "java:" ) )
			    .map( it -> it.substring( 5 ) )
			    .collect( BLCollector.toArray() );
			var		interfaceProxyDefinition	= InterfaceProxyService.generateDefinition( new ScriptingRequestBoxContext(), implementsArray );
			// TODO: Remove methods that already have a @overrideJava UDF definition to avoid duplicates
			interfaces.addAll( interfaceProxyDefinition.interfaces() );
			interfaceMethods = ProxyTransformer.generateInterfaceMethods( interfaceProxyDefinition.methods(), "this" );
		}

		/**
		 * --------------------------------------------------------------------------
		 * Process Extends Annotations
		 * --------------------------------------------------------------------------
		 */
		BoxExpression extendsValue = boxClass.getAnnotations().stream()
		    .filter( it -> it.getKey().getValue().equalsIgnoreCase( "extends" ) )
		    .findFirst()
		    .map( it -> it.getValue() )
		    .orElse( null );
		if ( extendsValue instanceof BoxStringLiteral str ) {
			String extendsStringValue = str.getValue().trim();
			if ( extendsStringValue.toLowerCase().startsWith( "java:" ) ) {
				extendsStringValue	= extendsStringValue.substring( 5 );
				extendsTemplate		= "extends " + extendsStringValue;
				isJavaExtends		= "true";
				// search for UDFs that need a proxy created
				extendsMethods		= boxClass.getDescendantsOfType( BoxFunctionDeclaration.class )
				    .stream()
				    .filter( it -> it.getAnnotations().stream().anyMatch( anno -> anno.getKey().getValue().equalsIgnoreCase( EXTENDS_ANNOTATION_MARKER ) ) )
				    .map( this::createJavaMethodStub )
				    .collect( java.util.stream.Collectors.joining( "\n" ) );
			}
		}

		/**
		 * --------------------------------------------------------------------------
		 * Prep the class template properties
		 * --------------------------------------------------------------------------
		 */
		String							fileName	= source instanceof SourceFile file && file.getFile() != null ? file.getFile().getName() : "unknown";
		String							filePath	= source instanceof SourceFile file && file.getFile() != null ? file.getFile().getAbsolutePath()
		    : "unknown";
		String							sourceType	= transpiler.getProperty( "sourceType" );

		// This map replaces the string template
		Map<String, String>				values		= Map.ofEntries(
		    Map.entry( "packagename", packageName ),
		    Map.entry( "className", className ),
		    Map.entry( "fileName", fileName ),
		    Map.entry( "interfaceMethods", interfaceMethods ),
		    Map.entry( "interfaceList", interfaces.stream().collect( java.util.stream.Collectors.joining( ", " ) ) ),
		    Map.entry( "extendsTemplate", extendsTemplate ),
		    Map.entry( "extendsMethods", extendsMethods ),
		    Map.entry( "isJavaExtends", isJavaExtends ),
		    Map.entry( "sourceType", sourceType ),
		    Map.entry( "resolvedFilePath", transpiler.getResolvedFilePath( mappingName, mappingPath, relativePath, filePath ) ),
		    Map.entry( "compiledOnTimestamp", transpiler.getDateTime( LocalDateTime.now() ) ),
		    Map.entry( "compileVersion", "1L" ),
		    Map.entry( "boxFQN", createKey( boxFQN ).toString() ),
		    Map.entry( "compileTimeMethodNames", generateCompileTimeMethodNames( boxClass ) )
		);
		String							code		= PlaceholderHelper.resolve( CLASS_TEMPLATE, values );
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

		MethodDeclaration	staticInitializerMethod	= entryPoint.findCompilationUnit().orElseThrow()
		    .getClassByName( className ).orElseThrow()
		    .getMethodsByName( "staticInitializer" ).get( 0 );

		FieldDeclaration	imports					= entryPoint.findCompilationUnit().orElseThrow()
		    .getClassByName( className ).orElseThrow()
		    .getFieldByName( "imports" ).orElseThrow();

		FieldDeclaration	keys					= entryPoint.findCompilationUnit().orElseThrow()
		    .getClassByName( className ).orElseThrow()
		    .getFieldByName( "keys" ).orElseThrow();

		Expression			annotationStruct		= transformAnnotations( boxClass.getAnnotations() );
		result.getResult().orElseThrow().getType( 0 ).getFieldByName( "annotations" ).orElseThrow().getVariable( 0 ).setInitializer( annotationStruct );

		/* Transform the documentation creating the initialization value */
		Expression documentationStruct = transformDocumentation( boxClass.getDocumentation() );
		result.getResult().orElseThrow().getType( 0 ).getFieldByName( "documentation" ).orElseThrow().getVariable( 0 )
		    .setInitializer( documentationStruct );

		List<Expression> propertyStructs = transformProperties( boxClass.getProperties(), sourceType );
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
			// We'll pick these up later after any imports in the body have been found.
			transpiler.transform( statement );
		}
		// Add body
		for ( BoxStatement statement : boxClass.getBody() ) {

			// TODO: build this check into the BFD transformer and track abstract method in the transpiler
			// Same change applies to interfaces
			if ( statement instanceof BoxFunctionDeclaration bfd && bfd.getBody() == null ) {
				// process abstract function (no body)
				pseudoConstructorBody.addStatement( 0,
				    ( ( JavaTranspiler ) transpiler ).createAbstractMethod( bfd, this, className, "class" )
				);
				continue;
			}
			Node javaASTNode = transpiler.transform( statement );

			// These get left behind from UDF declarations
			if ( javaASTNode instanceof EmptyStmt ) {
				continue;
			}

			// Java block get each statement in their block added
			if ( javaASTNode instanceof BlockStmt ) {
				BlockStmt stmt = ( BlockStmt ) javaASTNode;
				stmt.getStatements().forEach( it -> {
					pseudoConstructorBody.addStatement( it );
					// statements.add( it );
				} );
			} else {
				// All other statements are added to the _pseudoConstructor() method
				pseudoConstructorBody.addStatement( ( Statement ) javaASTNode );
				// statements.add( ( Statement ) javaASTNode );
			}
		}
		// Properties need defaulted AFTER the UDFs are added, but BEFORE the rest of the pseudoConstructor code runs
		pseudoConstructorBody.addStatement(
		    0,
		    new MethodCallExpr(
		        new NameExpr( "BoxClassSupport" ),
		        "defaultProperties",
		        NodeList.nodeList( new NameExpr( "this" ), new NameExpr( "context" ) )
		    )
		);

		// loop over UDF registrations and add them to the _pseudoConstructor() method
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

	private String generateCompileTimeMethodNames( BoxClass boxClass ) {
		List<String> methodNames = boxClass.getDescendantsOfType( BoxFunctionDeclaration.class )
		    .stream()
		    .map( BoxFunctionDeclaration::getName )
		    .map( this::createKey )
		    .map( String::valueOf )
		    .collect( java.util.stream.Collectors.toList() );
		return "Set.of(" + methodNames.stream().collect( java.util.stream.Collectors.joining( ", " ) ) + ")";
	}

	/**
	 * Transforms a collection of properties into a Map
	 *
	 * @param properties list of properties
	 *
	 * @return A list of the [ properties, getters, setters]
	 */
	private List<Expression> transformProperties( List<BoxProperty> properties, String sourceType ) {
		List<Expression>	members			= new ArrayList<>();
		List<Expression>	getterLookup	= new ArrayList<>();
		List<Expression>	setterLookup	= new ArrayList<>();

		properties.forEach( prop -> {
			List<BoxAnnotation>	finalAnnotations	= normlizePropertyAnnotations( prop );

			BoxAnnotation		nameAnnotation		= finalAnnotations.stream().filter( it -> it.getKey().getValue().equalsIgnoreCase( "name" ) ).findFirst()
			    .orElseThrow( () -> new ExpressionException( "Property [" + prop.getSourceText() + "] missing name annotation", prop ) );
			BoxAnnotation		typeAnnotation		= finalAnnotations.stream().filter( it -> it.getKey().getValue().equalsIgnoreCase( "type" ) ).findFirst()
			    .orElseThrow( () -> new ExpressionException( "Property [" + prop.getSourceText() + "] missing type annotation", prop ) );
			BoxAnnotation		defaultAnnotation	= finalAnnotations.stream().filter( it -> it.getKey().getValue().equalsIgnoreCase( "default" ) ).findFirst()
			    .orElse( null );

			Expression			documentationStruct	= transformDocumentation( prop.getDocumentation() );
			Expression			annotationStruct	= transformAnnotations( finalAnnotations );

			// Process the default value
			String				defaultValue		= "null";
			String				defaultExpression	= "null";
			if ( defaultAnnotation != null && defaultAnnotation.getValue() != null ) {

				if ( defaultAnnotation.getValue().isLiteral() ) {
					Node defaultValueExpr = transpiler.transform( defaultAnnotation.getValue() );
					defaultValue = defaultValueExpr.toString();
				} else {
					String lambdaContextName = "lambdaContext" + transpiler.incrementAndGetLambdaContextCounter();
					transpiler.pushContextName( lambdaContextName );
					Node initExpr = transpiler.transform( defaultAnnotation.getValue() );
					transpiler.popContextName();

					LambdaExpr lambda = new LambdaExpr();
					lambda.setParameters( new NodeList<>(
					    new Parameter( new UnknownType(), lambdaContextName ) ) );
					BlockStmt body = new BlockStmt();
					body.addStatement( parseStatement( "ClassLocator classLocator = ClassLocator.getInstance();", Map.of() ) );
					body.addStatement( new ReturnStmt( ( Expression ) initExpr ) );
					lambda.setBody( body );
					defaultExpression = lambda.toString();
				}
			}

			// name and type must be simple values
			String	name;
			String	type;
			if ( nameAnnotation != null && nameAnnotation.getValue() instanceof BoxStringLiteral namelit ) {
				name = namelit.getValue().trim();
				if ( name.isEmpty() )
					throw new ExpressionException( "Property [" + prop.getSourceText() + "] name cannot be empty", nameAnnotation );
			} else {
				throw new ExpressionException( "Property [" + prop.getSourceText() + "] name must be a simple value", nameAnnotation );
			}
			if ( typeAnnotation != null && typeAnnotation.getValue() instanceof BoxStringLiteral typelit ) {
				type = typelit.getValue().trim();
				if ( type.isEmpty() )
					throw new ExpressionException( "Property [" + prop.getSourceText() + "] type cannot be empty", typeAnnotation );
			} else {
				throw new ExpressionException( "Property [" + prop.getSourceText() + "] type must be a simple value", typeAnnotation );
			}

			// Getter and setter keys
			Expression						jNameKey	= createKey( name );
			Expression						jGetNameKey	= createKey( "get" + name );
			Expression						jSetNameKey	= createKey( "set" + name );
			LinkedHashMap<String, String>	values		= new LinkedHashMap<>();
			values.put( "type", type );
			values.put( "name", jNameKey.toString() );
			values.put( "defaultValue", defaultValue );
			values.put( "defaultExpression", defaultExpression );
			values.put( "annotations", annotationStruct.toString() );
			values.put( "documentation", documentationStruct.toString() );
			values.put( "sourceType", sourceType );
			String		template	= """
			                          				new Property( ${name}, "${type}", ${defaultValue}, ${defaultExpression}, ${annotations} ,${documentation}, BoxSourceType.${sourceType} )
			                          """;
			Expression	javaExpr	= parseExpression( template, values );
			// logger.trace( "{} -> {}", prop.getSourceText(), javaExpr );

			members.add( jNameKey );
			members.add( javaExpr );

			// Check if getter key annotation is defined in finalAnnotations and false. I don't love this as annotations can technically be any literal
			boolean getter = !finalAnnotations.stream()
			    .anyMatch( it -> it.getKey().getValue().equalsIgnoreCase( "getter" ) && !BooleanCaster.cast( getBoxExprAsString( it.getValue() ) ) );
			if ( getter ) {
				getterLookup.add( jGetNameKey );
				getterLookup.add( parseExpression( "properties.get( ${name} )", values ) );
			}
			// Check if setter key annotation is defined in finalAnnotations and false. I don't love this as annotations can technically be any literal
			boolean setter = !finalAnnotations.stream()
			    .anyMatch( it -> it.getKey().getValue().equalsIgnoreCase( "setter" ) && !BooleanCaster.cast( getBoxExprAsString( it.getValue() ) ) );
			if ( setter ) {
				setterLookup.add( jSetNameKey );
				setterLookup.add( parseExpression( "properties.get( ${name} )", values ) );
			}
		} );

		if ( members.isEmpty() ) {
			Expression	emptyMap	= parseExpression( "MapHelper.LinkedHashMapOfProperties()", new HashMap<>() );
			Expression	emptyMap2	= parseExpression( "MapHelper.HashMapOfProperties()", new HashMap<>() );
			return List.of( emptyMap, emptyMap2, emptyMap2 );
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

	public static List<BoxAnnotation> normlizePropertyAnnotations( BoxProperty prop ) {

		/**
		 * normalize annotations to allow for
		 * property String userName;
		 * This means all inline and pre annotations are treated as post annotations
		 */
		List<BoxAnnotation>	finalAnnotations	= new ArrayList<>();
		// Start wiith all inline annotatinos
		List<BoxAnnotation>	annotations			= prop.getPostAnnotations();
		// Add in any pre annotations that have a value, which allows type, name, or default to be set before
		annotations.addAll( prop.getAnnotations().stream().filter( it -> it.getValue() != null ).toList() );

		// Find the position of the name, type, and default annotations
		int					namePosition			= annotations.stream()
		    .filter( it -> it.getKey().getValue().equalsIgnoreCase( "name" ) && it.getValue() != null )
		    .findFirst()
		    .map( annotations::indexOf ).orElse( -1 );
		int					typePosition			= annotations.stream()
		    .filter( it -> it.getKey().getValue().equalsIgnoreCase( "type" ) && it.getValue() != null )
		    .findFirst()
		    .map( annotations::indexOf ).orElse( -1 );
		int					defaultPosition			= annotations.stream()
		    .filter( it -> it.getKey().getValue().equalsIgnoreCase( "default" ) && it.getValue() != null )
		    .findFirst()
		    .map( annotations::indexOf ).orElse( -1 );

		// Count the number of non-valued keys to determine how to handle them by position later
		int					numberOfNonValuedKeys	= ( int ) annotations.stream()
		    .map( BoxAnnotation::getValue )
		    .filter( Objects::isNull )
		    .count();
		List<BoxAnnotation>	nonValuedKeys			= annotations.stream()
		    .filter( it -> it.getValue() == null )
		    .collect( java.util.stream.Collectors.toList() );

		// Find the name, type, and default annotations
		BoxAnnotation		nameAnnotation			= null;
		BoxAnnotation		typeAnnotation			= null;
		BoxAnnotation		defaultAnnotation		= null;
		if ( namePosition > -1 )
			nameAnnotation = annotations.get( namePosition );
		if ( typePosition > -1 )
			typeAnnotation = annotations.get( typePosition );
		if ( defaultPosition > -1 )
			defaultAnnotation = annotations.get( defaultPosition );

		/**
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
				throw new ExpressionException( "Property [" + prop.getSourceText() + "] has no name", prop );
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
		// Now that name, type, and default are finalized, add in any remaining non-valued keys
		finalAnnotations.addAll( prop.getAnnotations().stream().filter( it -> it.getValue() == null ).toList() );

		return finalAnnotations;
	}

	/**
	 * Janky workaround to extract value from a literal expression.
	 *
	 * @param expr the expression to extract the value from
	 *
	 * @return the value as a string
	 */
	private String getBoxExprAsString( BoxExpression expr ) {
		if ( expr == null ) {
			return "";
		}
		if ( expr instanceof BoxStringLiteral str ) {
			return str.getValue();
		}
		if ( expr instanceof BoxBooleanLiteral bool ) {
			return bool.getValue() ? "true" : "false";
		} else {
			throw new ExpressionException( "Unsupported BoxExpr type: " + expr.getClass().getSimpleName(), expr );
		}
	}

	/**
	 * Create a Java method stub for a BoxFunctionDeclaration
	 *
	 * @param func the BoxFunctionDeclaration to create a stub for
	 *
	 * @return the Java method stub as a string
	 */
	private String createJavaMethodStub( BoxFunctionDeclaration func ) {
		StringBuilder sb = new StringBuilder();
		sb.append( "public " );

		BoxReturnType	boxReturnType	= func.getType();
		BoxType			returnType		= BoxType.Any;
		String			fqn				= null;
		if ( boxReturnType != null ) {
			returnType = boxReturnType.getType();
			if ( returnType.equals( BoxType.Fqn ) ) {
				fqn = boxReturnType.getFqn();
			}
		}
		String returnValue = returnType.equals( BoxType.Fqn ) ? fqn : returnType.getSymbol();
		sb.append( returnValue );
		sb.append( " " );
		sb.append( func.getName() );
		sb.append( "(" );
		java.util.TimerTask				f;
		List<BoxArgumentDeclaration>	parameters	= func.getArgs();
		for ( int i = 0; i < parameters.size(); i++ ) {
			BoxArgumentDeclaration parameter = parameters.get( i );
			sb.append( parameter.getType() );
			sb.append( " " );
			sb.append( parameter.getName() );
			if ( i < parameters.size() - 1 ) {
				sb.append( ", " );
			}
		}

		sb.append( ") {\n" );

		// collect method args into an array of Objects
		sb.append( "    Object[] ___args = new Object[] {" );
		for ( int i = 0; i < parameters.size(); i++ ) {
			BoxArgumentDeclaration parameter = parameters.get( i );
			sb.append( parameter.getName() );
			if ( i < parameters.size() - 1 ) {
				sb.append( ", " );
			}
		}
		sb.append( "};\n" );
		sb.append( "    IBoxContext context = RequestBoxContext.getCurrent();\n" );
		sb.append( "    if( context == null ) {\n" );
		sb.append( "      context = new ScriptingRequestBoxContext( BoxRuntime.getInstance().getRuntimeContext() );\n" );
		sb.append( "    }\n" );
		sb.append( "    Object result = this.dereferenceAndInvoke( context, Key.of( \"" );
		sb.append( func.getName() );
		sb.append( "\" ), ___args, false );\n" );

		// return only if the method is not void
		if ( !returnValue.equals( "void" ) ) {
			sb.append( "    return (" );
			sb.append( returnValue );
			sb.append( ") result;\n" );
		}
		sb.append( "}\n" );
		return sb.toString();
	}

}
