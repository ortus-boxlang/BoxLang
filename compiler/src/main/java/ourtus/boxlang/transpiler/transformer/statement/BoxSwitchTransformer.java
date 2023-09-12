package ourtus.boxlang.transpiler.transformer.statement;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ourtus.boxlang.ast.BoxNode;
import ourtus.boxlang.ast.BoxStatement;
import ourtus.boxlang.ast.expression.BoxStringLiteral;
import ourtus.boxlang.ast.statement.BoxSwitch;
import ourtus.boxlang.ast.statement.BoxWhile;
import ourtus.boxlang.transpiler.BoxLangTranspiler;
import ourtus.boxlang.transpiler.transformer.AbstractTransformer;
import ourtus.boxlang.transpiler.transformer.TransformerContext;
import ourtus.boxlang.transpiler.transformer.expression.BoxParenthesisTransformer;

import java.util.HashMap;
import java.util.Map;

public class BoxSwitchTransformer extends AbstractTransformer {

	Logger logger = LoggerFactory.getLogger( BoxParenthesisTransformer.class );

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxSwitch	boxSwitch	= ( BoxSwitch ) node;
		Expression	condition	= ( Expression ) BoxLangTranspiler.transform( boxSwitch.getCondition(), TransformerContext.RIGHT );

		String		template	= """
		                          do {

		                          } while(false);
		                          """;
		BlockStmt	body		= new BlockStmt();
		DoStmt		javaSwitch	= ( DoStmt ) parseStatement( template, new HashMap<>() );
		boxSwitch.getCases().forEach( c -> {
			if ( c.getCondition() != null ) {
				String caseTemplate = "if(  ${condition}  ) {}";
				if ( requiresBooleanCaster( c.getCondition() ) ) {
					caseTemplate = "if( BooleanCaster.cast( ${condition} ) ) {}";
				}
				Expression			switchExpr	= ( Expression ) BoxLangTranspiler.transform( c.getCondition() );
				Map<String, String>	values		= new HashMap<>() {

													{
														put( "condition", switchExpr.toString() );
													}
												};
				IfStmt				javaIfStmt	= ( IfStmt ) parseStatement( caseTemplate, values );
				BlockStmt			thenBlock	= new BlockStmt();
				c.getBody().forEach( stmt -> {
					thenBlock.addStatement( ( Statement ) BoxLangTranspiler.transform( stmt ) );
				} );
				javaIfStmt.setThenStmt( thenBlock );
				body.addStatement( javaIfStmt );
			}
		} );
		boxSwitch.getCases().forEach( c -> {
			if ( c.getCondition() == null ) {
				c.getBody().forEach( stmt -> {
					body.addStatement( ( Statement ) BoxLangTranspiler.transform( stmt ) );
				} );
			}
		} );
		javaSwitch.setBody( body );
		// if(requiresBooleanCaster(boxSwitch.getCondition())) {
		// template = "while( BooleanCaster.cast( ${condition} ) ) {}";
		// }
		// Map<String, String> values = new HashMap<>() {{
		// put("condition", condition.toString());
		// }};
		// WhileStmt javaWhile = (WhileStmt) parseStatement(template,values);
		// BlockStmt body = new BlockStmt();
		// for(BoxStatement statement : boxSwitch.getBody()) {
		// body.getStatements().add((Statement) BoxLangTranspiler.transform(statement));
		// }
		// javaWhile.setBody(body);
		return javaSwitch;
	}
}
