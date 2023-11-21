package ortus.boxlang.transpiler.transformer.statement;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.BoxStatement;
import ortus.boxlang.ast.statement.BoxDo;
import ortus.boxlang.transpiler.JavaTranspiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;
import ortus.boxlang.transpiler.transformer.expression.BoxParenthesisTransformer;

import java.util.HashMap;
import java.util.Map;

public class BoxDoTransformer extends AbstractTransformer {

	Logger logger = LoggerFactory.getLogger( BoxParenthesisTransformer.class );

	public BoxDoTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxDo		boxDo		= ( BoxDo ) node;
		Expression	condition	= ( Expression ) transpiler.transform( boxDo.getCondition(), TransformerContext.RIGHT );

		String		template	= "do  {} while(  ${condition}  );";
		if ( requiresBooleanCaster( boxDo.getCondition() ) ) {
			template = "do {} while( BooleanCaster.cast( ${condition} ) );";
		}
		Map<String, String>	values	= new HashMap<>() {

										{
											put( "condition", condition.toString() );
											put( "contextName", transpiler.peekContextName() );
										}
									};
		DoStmt				javaDo	= ( DoStmt ) parseStatement( template, values );
		BlockStmt			body	= new BlockStmt();
		for ( BoxStatement statement : boxDo.getBody() ) {
			body.getStatements().add( ( Statement ) transpiler.transform( statement ) );
		}
		javaDo.setBody( body );
		return javaDo;
	}
}
