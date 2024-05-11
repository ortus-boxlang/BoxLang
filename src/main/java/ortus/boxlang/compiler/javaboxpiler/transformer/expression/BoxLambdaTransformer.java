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
package ortus.boxlang.compiler.javaboxpiler.transformer.expression;

import java.time.LocalDateTime;
import java.util.Map;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxLambda;
import ortus.boxlang.compiler.ast.statement.BoxExpressionStatement;
import ortus.boxlang.compiler.javaboxpiler.JavaTranspiler;
import ortus.boxlang.compiler.javaboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.TransformerContext;
import ortus.boxlang.runtime.config.util.PlaceholderHelper;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * Transform a Lambda in the equivalent Java Class
 */
public class BoxLambdaTransformer extends AbstractTransformer {

	// @formatter:off
	private String template = """
		package ${packageName};

		import ortus.boxlang.runtime.scopes.IScope;
		import ortus.boxlang.runtime.scopes.Key;
		import ortus.boxlang.runtime.context.FunctionBoxContext;
		import ortus.boxlang.runtime.types.exceptions.*;
		import ortus.boxlang.runtime.context.ClassBoxContext;
		import ortus.boxlang.runtime.runnables.IBoxRunnable;
		import ortus.boxlang.runtime.context.IBoxContext;
		import ortus.boxlang.runtime.loader.ImportDefinition;
		import ortus.boxlang.runtime.interop.DynamicObject;
		import ortus.boxlang.runtime.dynamic.ExpressionInterpreter;
		import java.util.Optional;
		import ortus.boxlang.runtime.components.Component;
		import ortus.boxlang.compiler.parser.BoxSourceType;

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
		import ortus.boxlang.runtime.types.*;
		import ortus.boxlang.runtime.types.util.*;
		import java.util.LinkedHashMap;
		import java.nio.file.*;

  		public class ${classname} extends Lambda {
			private static ${classname}				instance;
			private final static Key				name		= Lambda.defaultName;
			private final static Argument[]			arguments	= new Argument[] {};
			private final static String				returnType	= "any";

		    private final static IStruct				annotations			= Struct.EMPTY;
			private final static IStruct				documentation		= Struct.EMPTY;
			
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
   				return Access.PUBLIC;
   			}

			public  long getRunnableCompileVersion() {
				return ${className}.compileVersion;
			}

			public LocalDateTime getRunnableCompiledOn() {
				return null;
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
			public List<ImportDefinition> getImports() {
				return imports;
      		}

			@Override
			public IStruct getAnnotations() {
				return annotations;
			}

			@Override
			public IStruct getDocumentation() {
				return documentation;
			}
			@Override
			public Object _invoke( FunctionBoxContext context ) {
				ClassLocator classLocator = ClassLocator.getInstance();

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

  		}
 	""";
	// @formatter:on
	public BoxLambdaTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	/**
	 * Transform a lambda declaration into a Class
	 *
	 * @param node
	 * @param context
	 *
	 * @return
	 *
	 * @throws IllegalStateException
	 */
	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxLambda			boxLambda			= ( BoxLambda ) node;
		String				packageName			= transpiler.getProperty( "packageName" );
		String				lambdaName			= "Lambda_" + transpiler.incrementAndGetLambdaCounter();
		String				enclosingClassName	= transpiler.getProperty( "classname" );
		String				className			= lambdaName;

		Map<String, String>	values				= Map.ofEntries(
		    Map.entry( "packageName", packageName ),
		    Map.entry( "className", className ),
		    Map.entry( "lambdaName", lambdaName ),
		    Map.entry( "enclosingClassName", enclosingClassName ),
		    Map.entry( "compiledOnTimestamp", transpiler.getDateTime( LocalDateTime.now() ) ),
		    Map.entry( "compileVersion", "1L" )
		);
		transpiler.pushContextName( "context" );
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
			throw new BoxRuntimeException( result + "\n" + code );
		}
		CompilationUnit			javaClass		= result.getResult().get();
		/* Transform the arguments creating the initialization values */
		ArrayInitializerExpr	argInitializer	= new ArrayInitializerExpr();
		boxLambda.getArgs().forEach( arg -> {
			Expression argument = ( Expression ) transpiler.transform( arg );
			argInitializer.getValues().add( argument );
		} );
		javaClass.getType( 0 ).getFieldByName( "arguments" ).orElseThrow().getVariable( 0 ).setInitializer( argInitializer );

		/* Transform the annotations creating the initialization value */
		Expression annotationStruct = transformAnnotations( boxLambda.getAnnotations() );
		result.getResult().orElseThrow().getType( 0 ).getFieldByName( "annotations" ).orElseThrow().getVariable( 0 ).setInitializer( annotationStruct );

		MethodDeclaration	invokeMethod	= javaClass.findCompilationUnit().orElseThrow()
		    .getClassByName( className ).orElseThrow()
		    .getMethodsByName( "_invoke" ).get( 0 );

		BlockStmt			body			= invokeMethod.getBody().get();
		transpiler.pushfunctionBodyCounter();
		int componentCounter = transpiler.getComponentCounter();
		transpiler.setComponentCounter( 0 );

		// If the body statement is an expression, then return it
		if ( boxLambda.getBody() instanceof BoxExpressionStatement boxExpr ) {
			body.addStatement( new ReturnStmt( new EnclosedExpr( ( Expression ) transpiler.transform( boxExpr.getExpression() ) ) ) );
		} else {
			// Otherwise, return null
			body.addStatement( ( Statement ) transpiler.transform( boxLambda.getBody() ) );
			invokeMethod.getBody().get().addStatement( new ReturnStmt( new NullLiteralExpr() ) );
		}

		transpiler.setComponentCounter( componentCounter );
		transpiler.popfunctionBodyCounter();
		transpiler.popContextName();

		( ( JavaTranspiler ) transpiler ).getCallables().add( ( CompilationUnit ) javaClass );
		return parseExpression( "${enclosingClassName}.${lambdaName}.getInstance()", values );
	}
}
