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
package ourtus.boxlang.transpiler.transformer.statement;

import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ourtus.boxlang.ast.BoxNode;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.expr.Expression;
import ourtus.boxlang.ast.statement.BoxAssignment;
import ourtus.boxlang.transpiler.BoxLangTranspiler;
import ourtus.boxlang.transpiler.transformer.AbstractTransformer;
import ourtus.boxlang.transpiler.transformer.TransformerContext;

public class BoxAssignmentTransformer extends AbstractTransformer {
	Logger logger = LoggerFactory.getLogger( BoxAssignmentTransformer.class );
	public BoxAssignmentTransformer() { }
	@Override
	public Node transform(BoxNode node, TransformerContext context) throws IllegalStateException {
		logger.info(node.getSourceText());
		Expression left = (Expression) BoxLangTranspiler.transform(((BoxAssignment)node).getLeft(),TransformerContext.LEFT);
		Expression right = (Expression) BoxLangTranspiler.transform(((BoxAssignment)node).getRight(),TransformerContext.RIGHT);

		if(left instanceof MethodCallExpr method) {
			if("put".equalsIgnoreCase(method.getName().asString())) {
				method.getArguments().add(right);
			}
		}
		ExpressionStmt javaExpr =  new ExpressionStmt(left) ;
		addIndex(javaExpr,node);
		return javaExpr;
	}


}
