package ourtus.boxlang.transpiler.transformer.statement;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import ourtus.boxlang.ast.BoxNode;
import ourtus.boxlang.ast.statement.BoxExpression;
import ourtus.boxlang.transpiler.BoxLangTranspiler;
import ourtus.boxlang.transpiler.transformer.AbstractTransformer;
import ourtus.boxlang.transpiler.transformer.TransformerContext;

public class BoxExpressionTransformer extends AbstractTransformer {

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxExpression exprStmt = ( BoxExpression ) node;
		Expression expr = ( Expression ) BoxLangTranspiler.transform( exprStmt.getExpression() );
		return new ExpressionStmt( expr );
	}
}
