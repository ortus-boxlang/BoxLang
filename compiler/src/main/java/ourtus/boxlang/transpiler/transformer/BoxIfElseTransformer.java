package ourtus.boxlang.transpiler.transformer;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import ourtus.boxlang.ast.BoxNode;
import ourtus.boxlang.ast.BoxStatement;
import ourtus.boxlang.ast.statement.BoxIfElse;
import ourtus.boxlang.transpiler.BoxLangTranspiler;

import java.util.HashMap;
import java.util.Map;

public class BoxIfElseTransformer extends AbstractTransformer  {
	@Override
	public Node transform(BoxNode node, TransformerContext context) throws IllegalStateException {
		BoxIfElse ifElse = (BoxIfElse) node;
		Expression condition =  (Expression) BoxLangTranspiler.transform(ifElse.getCondition());
		Map<String, String> values = new HashMap<>() {{
			put("condition", condition.toString());
		}};


		String template = "if( ${condition}) {}";
		IfStmt javaIfStmt = (IfStmt) parseStatement(template,values);
		BlockStmt thenBlock = new BlockStmt();
		BlockStmt elseBlock = new BlockStmt();
		for(BoxStatement statement : ifElse.getThenBody()) {
			thenBlock.getStatements().add((Statement) BoxLangTranspiler.transform(statement));
		}
		for(BoxStatement statement : ifElse.getElseBody()) {
			elseBlock.getStatements().add((Statement) BoxLangTranspiler.transform(statement));
		}

		javaIfStmt.setThenStmt(thenBlock);
		javaIfStmt.setElseStmt(elseBlock);
		return javaIfStmt;

	}
}
