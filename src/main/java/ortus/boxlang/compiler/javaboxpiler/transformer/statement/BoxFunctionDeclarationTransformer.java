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
package ortus.boxlang.compiler.javaboxpiler.transformer.statement;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxStatement;
import ortus.boxlang.compiler.ast.statement.BoxAccessModifier;
import ortus.boxlang.compiler.ast.statement.BoxFunctionDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxMethodDeclarationModifier;
import ortus.boxlang.compiler.ast.statement.BoxReturnType;
import ortus.boxlang.compiler.ast.statement.BoxType;
import ortus.boxlang.compiler.javaboxpiler.JavaTranspiler;
import ortus.boxlang.compiler.javaboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.TransformerContext;
import ortus.boxlang.runtime.config.util.PlaceholderHelper;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * Transform a Function Declaration in the equivalent Java Parser AST nodes
 */
public class BoxFunctionDeclarationTransformer extends AbstractTransformer {

	// @formatter:off
	private String registrationTemplate = "${contextName}.registerUDF( ${enclosingClassName}.${className}.getInstance() );";
	private String classTemplate = """
		package ${packageName};

		public class ${classname} extends UDF {
			private static ${classname}				instance;
			private final static Key				name		= ${functionName};
			private final static Argument[]			arguments	= new Argument[] {};
			private final static String				returnType	= "${returnType}";
			private              Access		   		access		= Access.${access};
			private final static List<BoxMethodDeclarationModifier>	modifiers = List.of( ${modifiers} );

			private final static IStruct	annotations;
			private final static IStruct	documentation;

			private static final long					compileVersion	= ${compileVersion};
			private static final LocalDateTime			compiledOn		= ${compiledOnTimestamp};
			private static final Object					ast				= null;

			public Key getName() {
				return name;
			}
			public Argument[] getArguments() {
				return arguments;
			}
			public String getReturnType() {
				return returnType;
			}

			public Access getAccess() {
   				return access;
   			}
			
			@Override
			public List<BoxMethodDeclarationModifier> getModifiers() {
				return modifiers;		
			}
		

			public  long getRunnableCompileVersion() {
				return ${className}.compileVersion;
			}

			public LocalDateTime getRunnableCompiledOn() {
				return ${className}.compiledOn;
			}

			public Object getRunnableAST() {
				return ${className}.ast;
			}

			private ${classname}() {
				super();
			}

			public static synchronized ${classname} getInstance() {
				if ( instance == null ) {
					instance = new ${classname}();
				}
				return instance;
			}

			@Override
			public IStruct getAnnotations() {
				return annotations;
			}

			@Override
			public IStruct getDocumentation() {
				return documentation;
			}

			public List<ImportDefinition> getImports() {
				return imports;
			}

			public ResolvedFilePath getRunnablePath() {
				return ${enclosingClassName}.path;
			}

			/**
			 * The original source type
			 */
			public BoxSourceType getSourceType() {
				return ${enclosingClassName}.sourceType;
			}

			@Override
			public Object _invoke( FunctionBoxContext context ) {
				ClassLocator classLocator = ClassLocator.getInstance();

			}
		}
 	""";
	public BoxFunctionDeclarationTransformer(JavaTranspiler transpiler) {
    	super(transpiler);
    }
	// @formatter:on
	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxFunctionDeclaration	function			= ( BoxFunctionDeclaration ) node;
		String					packageName			= transpiler.getProperty( "packageName" );
		BoxAccessModifier		access				= function.getAccessModifier();
		String					enclosingClassName	= transpiler.getProperty( "classname" );
		String					className			= "Func_" + function.getName();
		BoxReturnType			boxReturnType		= function.getType();
		BoxType					returnType			= BoxType.Any;
		String					fqn					= null;
		if ( boxReturnType != null ) {
			returnType = boxReturnType.getType();
			if ( returnType.equals( BoxType.Fqn ) ) {
				fqn = boxReturnType.getFqn();
			}
		}
		if ( access == null ) {
			access = BoxAccessModifier.Public;
		}

		Map<String, String> values = Map.ofEntries(
		    Map.entry( "packageName", packageName ),
		    Map.entry( "className", className ),
		    Map.entry( "access", access.toString().toUpperCase() ),
		    Map.entry( "modifiers", transformModifiers( function.getModifiers() ) ),
		    Map.entry( "functionName", createKey( function.getName() ).toString() ),
		    Map.entry( "returnType", returnType.equals( BoxType.Fqn ) ? fqn : returnType.name() ),
		    Map.entry( "enclosingClassName", enclosingClassName ),
		    Map.entry( "compiledOnTimestamp", transpiler.getDateTime( LocalDateTime.now() ) ),
		    Map.entry( "compileVersion", "1L" )
		);
		transpiler.pushContextName( "context" );

		String							code	= PlaceholderHelper.resolve( classTemplate, values );
		ParseResult<CompilationUnit>	result;
		try {
			result = javaParser.parse( code );
		} catch ( Exception e ) {
			// Temp debugging to see generated Java code
			throw new BoxRuntimeException( code, e );
		}
		if ( !result.isSuccessful() ) {
			// Temp debugging to see generated Java code
			throw new BoxRuntimeException( result + "\n" + code );
		}

		/* Transform the arguments creating the initialization values */
		ArrayInitializerExpr argInitializer = new ArrayInitializerExpr();
		function.getArgs().forEach( arg -> {
			Expression argument = ( Expression ) transpiler.transform( arg );
			argInitializer.getValues().add( argument );
		} );
		result.getResult().orElseThrow().getType( 0 ).getFieldByName( "arguments" ).orElseThrow().getVariable( 0 ).setInitializer( argInitializer );

		/* Transform the annotations creating the initialization value */
		Expression annotationStruct = transformAnnotations( function.getAnnotations() );
		result.getResult().orElseThrow().getType( 0 ).getFieldByName( "annotations" ).orElseThrow().getVariable( 0 ).setInitializer( annotationStruct );

		/* Transform the documentation creating the initialization value */
		Expression documentationStruct = transformDocumentation( function.getDocumentation() );
		result.getResult().orElseThrow().getType( 0 ).getFieldByName( "documentation" ).orElseThrow().getVariable( 0 )
		    .setInitializer( documentationStruct );

		CompilationUnit		javaClass		= result.getResult().get();
		MethodDeclaration	invokeMethod	= javaClass.findCompilationUnit().orElseThrow()
		    .getClassByName( className ).orElseThrow()
		    .getMethodsByName( "_invoke" ).get( 0 );

		transpiler.pushfunctionBodyCounter();
		int componentCounter = transpiler.getComponentCounter();
		transpiler.setComponentCounter( 0 );
		for ( BoxStatement statement : function.getBody() ) {
			Node javaStmt = transpiler.transform( statement );
			if ( javaStmt instanceof BlockStmt stmt ) {
				stmt.getStatements().forEach( it -> invokeMethod.getBody().get().addStatement( it ) );
			} else {
				invokeMethod.getBody().get().addStatement( ( Statement ) javaStmt );
			}
		}
		transpiler.setComponentCounter( componentCounter );
		transpiler.popfunctionBodyCounter();
		// Ensure we have a return statement
		invokeMethod.getBody().get().addStatement( new ReturnStmt( new NullLiteralExpr() ) );
		transpiler.popContextName();

		( ( JavaTranspiler ) transpiler ).getUDFcallables().put( Key.of( function.getName() ), javaClass );

		// UDF Declaration
		values = Map.ofEntries(
		    Map.entry( "className", className ),
		    Map.entry( "contextName", "context" ),
		    Map.entry( "enclosingClassName", enclosingClassName )
		);
		Statement javaStmt = parseStatement( registrationTemplate, values );
		// logger.trace( node.getSourceText() + " -> " + javaStmt );
		// commenting this out to prevent BoxFunctionDeclarationTransformer nodes in the SourceMaps
		// This caused the debugger's stepping behavior to hit locations it shouldn't be stopping on
		// addIndex( javaStmt, node );
		if ( function.getModifiers().contains( BoxMethodDeclarationModifier.STATIC ) ) {
			( ( JavaTranspiler ) transpiler ).getStaticUDFDeclarations().add( javaStmt );
		} else {
			( ( JavaTranspiler ) transpiler ).getUDFDeclarations().add( javaStmt );
		}

		// The actual declaration is hoisted to the top, so I just need a dummy node to return here
		return new EmptyStmt();
	}

	/**
	 * Build a list of modifiers for this function
	 * 
	 * @param modifiers list of modifiers
	 * 
	 * @return a string with the list of modifiers
	 */
	private String transformModifiers( List<BoxMethodDeclarationModifier> modifiers ) {
		StringBuilder sb = new StringBuilder();
		for ( BoxMethodDeclarationModifier modifier : modifiers ) {
			sb
			    .append( "BoxMethodDeclarationModifier." )
			    .append( modifier.toString().toUpperCase() )
			    .append( ", " );
		}
		if ( sb.length() > 0 ) {
			sb.setLength( sb.length() - 2 );
		}
		return sb.toString();
	}
}
