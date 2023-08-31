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
import ourtus.boxlang.ast.BoxNode;
import ourtus.boxlang.ast.BoxScript;
import ourtus.boxlang.ast.BoxStatement;
import ourtus.boxlang.ast.expression.*;
import ourtus.boxlang.ast.statement.BoxAssignment;
import ourtus.boxlang.transpiler.transformer.*;
import ourtus.boxlang.transpiler.transformer.expression.*;

import java.util.HashMap;

public class BoxLangTranspiler {

	private  static HashMap<Class, Transformer> registry  = new HashMap<>() {{
		put(BoxAssignment.class,new BoxAssignmentTransformer());
		put(BoxArrayAccess.class,new BoxArrayAccessTransformer());
		// Expressions
		put(BoxIdentifier.class,new BoxIdentifierTransformer());
		// Literals
		put(BoxStringLiteral.class,new BoxStringLiteralTransformer());
		put(BoxIntegerLiteral.class,new BoxIntegerLiteralTransformer());
		put(BoxBooleanLiteral.class,new BoxBooleanLiteralTransformer());

		put(BoxBinaryOperation.class,new BoxBinaryOperationTransformer());
		put(BoxNegateOperation.class,new BoxNegateOperationTransformer());
		put(BoxFunctionInvocation.class,new BoxFunctionInvocationTransformer());

	}};
	public BoxLangTranspiler() { }
	public static com.github.javaparser.ast.Node transform(BoxNode node) throws IllegalStateException {
		Transformer transformer = registry.get(node.getClass());
		if(transformer != null) {
			return transformer.transform(node);
		}
		throw new IllegalStateException("unsupported: " + node.getClass().getSimpleName());
	}
	public CompilationUnit transpile(BoxNode node) throws IllegalStateException {
		BoxScript source = (BoxScript) node;
		for(BoxStatement statement : source.getStatements()) {
			Node s = transform(statement);
		}
		return null;
	}
}
