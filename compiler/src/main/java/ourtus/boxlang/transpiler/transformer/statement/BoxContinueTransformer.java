package ourtus.boxlang.transpiler.transformer.statement;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.ContinueStmt;
import ourtus.boxlang.ast.BoxNode;
import ourtus.boxlang.transpiler.transformer.AbstractTransformer;
import ourtus.boxlang.transpiler.transformer.TransformerContext;

public class BoxContinueTransformer extends AbstractTransformer {
	@Override
	public Node transform(BoxNode node, TransformerContext context) throws IllegalStateException {
		return addIndex(new ContinueStmt(),node);
	}
}
