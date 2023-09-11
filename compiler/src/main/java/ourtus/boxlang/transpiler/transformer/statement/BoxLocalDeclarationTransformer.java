package ourtus.boxlang.transpiler.transformer.statement;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ourtus.boxlang.ast.BoxNode;
import ourtus.boxlang.ast.expression.BoxIdentifier;
import ourtus.boxlang.ast.statement.BoxLocalDeclaration;
import ourtus.boxlang.transpiler.BoxLangTranspiler;
import ourtus.boxlang.transpiler.transformer.AbstractTransformer;
import ourtus.boxlang.transpiler.transformer.TransformerContext;
import ourtus.boxlang.transpiler.transformer.expression.BoxParenthesisTransformer;

import java.util.HashMap;
import java.util.Map;


public class BoxLocalDeclarationTransformer extends AbstractTransformer {
	Logger logger = LoggerFactory.getLogger( BoxParenthesisTransformer.class );
	@Override
	public Node transform(BoxNode node, TransformerContext context) throws IllegalStateException {
		BoxLocalDeclaration declaration = (BoxLocalDeclaration)node;
		BlockStmt stmt = new BlockStmt();
		if(  declaration.getExpression() != null  ) {
			Expression expr = (Expression) BoxLangTranspiler.transform(declaration.getExpression(),TransformerContext.RIGHT);;
				declaration.getIdentifiers().stream().forEach( it -> {
					BoxIdentifier variable = (BoxIdentifier) it;
					Map<String, String> values = new HashMap<>() {{
						put("variable", variable.getName() );
						put("expr", expr.toString());
					}};

					String template = """
						context.getScopeNearby( Key.of( LocalScope.name ) )
							.put(Key.of( "${variable}" ),
							${expr});
						""";

					Node javaStmt = parseStatement( template, values );
					logger.info(node.getSourceText() + " -> " + javaStmt);
					stmt.getStatements().add((Statement) javaStmt);

				});

		}



		return stmt;
	}
}
