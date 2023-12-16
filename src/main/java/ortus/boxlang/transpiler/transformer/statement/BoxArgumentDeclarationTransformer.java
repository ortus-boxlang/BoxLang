package ortus.boxlang.transpiler.transformer.statement;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;

import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.statement.BoxArgumentDeclaration;
import ortus.boxlang.transpiler.JavaTranspiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;

/**
 * Transform a BoxArgumentDeclarationTransformer Node the equivalent Java Parser AST nodes
 */
public class BoxArgumentDeclarationTransformer extends AbstractTransformer {

	Logger logger = LoggerFactory.getLogger( BoxArgumentDeclarationTransformer.class );

	public BoxArgumentDeclarationTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxArgumentDeclaration	boxArgument	= ( BoxArgumentDeclaration ) node;

		/* Process initialization value */
		String					init		= "null";
		if ( boxArgument.getValue() != null ) {
			Node initExpr = transpiler.transform( boxArgument.getValue() );
			init = initExpr.toString();
		}

		/* Process annotations */
		Expression			annotationStruct	= transformAnnotations( boxArgument.getAnnotations() );
		/* Process documentation */
		Expression			documentationStruct	= transformDocumentation( boxArgument.getDocumentation() );

		Map<String, String>	values				= Map.of(
		    "required", String.valueOf( boxArgument.getRequired() ),
		    "type", boxArgument.getType(),
		    "name", boxArgument.getName(),
		    "init", init,
		    "annotations", annotationStruct.toString(),
		    "documentation", documentationStruct.toString()
		);
		String				template			= """
		                                          				new Argument( ${required}, "${type}" , Key.of("${name}"), ${init}, ${annotations} ,${documentation} )
		                                          """;
		Expression			javaExpr			= ( Expression ) parseExpression( template, values );
		logger.info( "{} -> {}", node.getSourceText(), javaExpr );
		return javaExpr;
	}

}
