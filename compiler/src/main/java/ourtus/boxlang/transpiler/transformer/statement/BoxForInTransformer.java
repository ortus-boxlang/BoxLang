package ourtus.boxlang.transpiler.transformer.statement;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.WhileStmt;
import ourtus.boxlang.ast.BoxNode;
import ourtus.boxlang.ast.statement.BoxForIn;
import ourtus.boxlang.transpiler.BoxLangTranspiler;
import ourtus.boxlang.transpiler.transformer.AbstractTransformer;
import ourtus.boxlang.transpiler.transformer.TransformerContext;

import java.beans.Expression;
import java.util.HashMap;
import java.util.Map;

public class BoxForInTransformer extends AbstractTransformer {

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxForIn			boxFor		= ( BoxForIn ) node;
		Node				variable	= BoxLangTranspiler.transform( boxFor.getVariable() );
		Node				collection	= BoxLangTranspiler.transform( boxFor.getExpression() );

		BlockStmt			stmt		= new BlockStmt();
		Map<String, String>	values		= new HashMap<>() {

											{
												put( "variable", variable.toString() );
												put( "collection", collection.toString() );
											}
										};

		String				template1	= """
		                                  	Iterator ${variable} = CollectionCaster.cast( ${collection} ).iterator();
		                                  """;
		String				template2	= """
		                                  	while( ${variable}.hasNext() ) {
		                                  		${collection}.put( Key.of( "${variable}" ), ${variable}.next() );
		                                  	}
		                                  """;
		WhileStmt			whileStmt	= ( WhileStmt ) parseStatement( template2, values );
		stmt.addStatement( ( Statement ) parseStatement( template1, values ) );
		boxFor.getBody().forEach( it -> {
			whileStmt.getBody().asBlockStmt().addStatement( ( Statement ) BoxLangTranspiler.transform( it ) );
		} );
		stmt.addStatement( whileStmt );

		return stmt;
	}
}
