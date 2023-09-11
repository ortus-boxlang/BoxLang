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
package ourtus.boxlang.transpiler;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import ourtus.boxlang.ast.BoxNode;
import ourtus.boxlang.ast.BoxScript;
import ourtus.boxlang.ast.BoxStatement;
import ourtus.boxlang.ast.expression.*;
import ourtus.boxlang.ast.statement.*;
import ourtus.boxlang.transpiler.transformer.*;
import ourtus.boxlang.transpiler.transformer.expression.*;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ourtus.boxlang.transpiler.transformer.statement.*;

public class BoxLangTranspiler {
	static Logger logger = LoggerFactory.getLogger( BoxLangTranspiler.class );

	private  static HashMap<Class, Transformer> registry  = new HashMap<>() {{

		put(BoxScript.class,new BoxScriptTransformer());
		put(BoxAssignment.class,new BoxAssignmentTransformer());
		put(BoxArrayAccess.class,new BoxArrayAccessTransformer());
		put(BoxExpression.class,new BoxExpressionTransformer());

		// Expressions
		put(BoxIdentifier.class,new BoxIdentifierTransformer());
		put(BoxScope.class,new BoxScopeTransformer());
		// Literals
		put(BoxStringLiteral.class,new BoxStringLiteralTransformer());
		put(BoxIntegerLiteral.class,new BoxIntegerLiteralTransformer());
		put(BoxBooleanLiteral.class,new BoxBooleanLiteralTransformer());
		put(BoxArgument.class,new BoxArgumentTransformer());

		put(BoxParenthesis.class,new BoxParenthesisTransformer());
		put(BoxBinaryOperation.class,new BoxBinaryOperationTransformer());
		put(BoxTernaryOperation.class,new BoxTernaryOperationTransformer());
		put(BoxNegateOperation.class,new BoxNegateOperationTransformer());
		put(BoxComparisonOperation.class,new BoxComparisonOperationTransformer());
		put(BoxObjectAccess.class,new BoxObjectAccessTransformer());

		put(BoxMethodInvocation.class,new BoxMethodInvocationTransformer());
		put(BoxFunctionInvocation.class,new BoxFunctionInvocationTransformer());
		put(BoxLocalDeclaration.class,new BoxLocalDeclarationTransformer());
		put(BoxIfElse.class,new BoxIfElseTransformer());
		put(BoxWhile.class,new BoxWhileTransformer());
		put(BoxSwitch.class,new BoxSwitchTransformer());
		put(BoxBreak.class,new BoxBreakTransformer());
		put(BoxContinue.class,new BoxContinueTransformer());
		put(BoxForIn.class,new BoxForInTransformer());

	}};
	public BoxLangTranspiler() { }
	public static Node transform(BoxNode node) throws IllegalStateException {
		return BoxLangTranspiler.transform(node,TransformerContext.NONE);
	}
	public static Node transform(BoxNode node,TransformerContext context) throws IllegalStateException {
		Transformer transformer = registry.get(node.getClass());
		if(transformer != null) {
			Node javaNode = transformer.transform(node,context);
			//logger.info(transformer.getClass().getSimpleName() + " : " + node.getSourceText() + " -> " + javaNode );
			return javaNode;
		}
		throw new IllegalStateException("unsupported: " + node.getClass().getSimpleName() + " : " + node.getSourceText());
	}
	public CompilationUnit transpile(BoxNode node) throws IllegalStateException {
		BoxScript source = (BoxScript) node;
		CompilationUnit javaClass = (CompilationUnit) transform(source);
		MethodDeclaration invokeMethod = javaClass.findCompilationUnit().orElseThrow()
			.getClassByName("TestClass").orElseThrow()
			.getMethodsByName("invoke").get(0);

		for(BoxStatement statement : source.getStatements()) {
			Node javaStmt = transform(statement);
			if(javaStmt instanceof BlockStmt) {
				BlockStmt stmt = (BlockStmt)javaStmt;
				stmt.getStatements().stream().forEach( it -> invokeMethod.getBody().get().addStatement((Statement) it) );
			} else {
				invokeMethod.getBody().get().addStatement((Statement) javaStmt);
			}
		}
		return javaClass;
	}
}
