package ortus.boxlang.transpiler.transformer.expression;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;

import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.expression.BoxAccess;
import ortus.boxlang.ast.expression.BoxDotAccess;
import ortus.boxlang.ast.expression.BoxIdentifier;
import ortus.boxlang.ast.expression.BoxScope;
import ortus.boxlang.transpiler.JavaTranspiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;

public class BoxAccessTransformer extends AbstractTransformer {

	Logger logger = LoggerFactory.getLogger( BoxAccessTransformer.class );

	public BoxAccessTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxAccess	objectAccess	= ( BoxAccess ) node;
		Node		accessKey;
		// DotAccess just uses the string directly, array access allows any expression
		if ( objectAccess instanceof BoxDotAccess dotAccess ) {
			accessKey = createKey( ( ( BoxIdentifier ) dotAccess.getAccess() ).getName() );
		} else {
			accessKey = createKey( objectAccess.getAccess() );
		}

		Map<String, String> values = new HashMap<>() {

			{
				put( "contextName", transpiler.peekContextName() );
				put( "safe", objectAccess.isSafe().toString() );
				put( "accessKey", accessKey.toString() );
			}
		};

		// An access expression starting a scope can be optimized
		if ( objectAccess.getContext() instanceof BoxScope ) {
			Expression jContext = ( Expression ) transpiler.transform( objectAccess.getContext(), TransformerContext.NONE );
			values.put( "scopeReference", jContext.toString() );

			String	template	= """
			                      ${scopeReference}.dereference(
			                        ${accessKey},
			                        ${safe}
			                        )
			                                      """;
			Node	javaExpr	= parseExpression( template, values );
			logger.info( node.getSourceText() + " -> " + javaExpr );
			return javaExpr;

		} else {
			// All other non-scoped vars we just lookup
			Expression jContext = ( Expression ) transpiler.transform( objectAccess.getContext(), TransformerContext.NONE );
			// "scope" here isn't a BoxLang proper scope, it's just whatever Java source represents the context of the access expression
			values.put( "scopeReference", jContext.toString() );

			String	template	= """
			                      Referencer.get(
			                      	  ${scopeReference},
			                            ${accessKey},
			                            ${safe}
			                                  )
			                            """;

			Node	javaExpr	= parseExpression( template, values );
			logger.info( node.getSourceText() + " -> " + javaExpr );
			return javaExpr;
		}
	}

}
