package ortus.boxlang.transpiler.transformer.expression;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.expression.BoxIdentifier;
import ortus.boxlang.ast.expression.BoxObjectAccess;
import ortus.boxlang.ast.expression.BoxScope;
import ortus.boxlang.transpiler.BoxLangTranspiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;

import java.util.HashMap;
import java.util.Map;

public class BoxObjectAccessTransformer extends AbstractTransformer {

    Logger logger = LoggerFactory.getLogger( BoxObjectAccessTransformer.class );

    @Override
    public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
        BoxObjectAccess objectAccess = ( BoxObjectAccess ) node;
        String          side         = context == TransformerContext.NONE ? "" : "(" + context.toString() + ") ";

        if ( objectAccess.getContext() instanceof BoxScope && objectAccess.getAccess() instanceof BoxObjectAccess ) {
            Expression scope    = ( Expression ) BoxLangTranspiler.transform( objectAccess.getContext(), TransformerContext.LEFT );
            Node       variable = BoxLangTranspiler.transform( objectAccess.getAccess(), TransformerContext.RIGHT );

            if ( variable instanceof ArrayInitializerExpr ) {
                ArrayInitializerExpr vars     = ( ArrayInitializerExpr ) variable;

                Map<String, String>  values   = new HashMap<>() {

                                                  {
                                                      put( "scope", scope.toString() );
                                                      put( "var0", vars.getValues().get( 0 ).toString() );
                                                      put( "var1", vars.getValues().get( 1 ).toString() );
                                                  }
                                              };

                String               template = switch ( context ) {
                                                  case LEFT -> """
                                                               ${scope}.get( Key.of( ${var0} ) ).get(Key.of( ${var1} ))
                                                               """;
                                                  default -> """
                                                             Referencer.get(${scope}.get( Key.of( ${var0} ) ).get(Key.of( ${var1} )),false)
                                                             """;
                                              };
                Node                 javaExpr = parseExpression( template, values );
                logger.info( side + node.getSourceText() + " -> " + javaExpr );
                addIndex( javaExpr, node );
                return javaExpr;

            } else {
                Map<String, String> values   = new HashMap<>() {

                                                 {
                                                     put( "scope", scope.toString() );
                                                     put( "variable", variable.toString() );
                                                 }
                                             };

                String              template = switch ( context ) {
                                                 case LEFT -> """
                                                              ${scope}.put(Key.of("${variable}"))
                                                              """;
                                                 default -> """
                                                            ${scope}.get( Key.of( "${variable}" ) )
                                                            """;

                                             };
                Node                javaExpr = parseExpression( template, values );
                logger.info( side + node.getSourceText() + " -> " + javaExpr );
                addIndex( javaExpr, node );
                return javaExpr;
            }

        } else if ( objectAccess.getContext() instanceof BoxIdentifier && objectAccess.getAccess() instanceof BoxIdentifier ) {
            Expression           scope    = ( Expression ) BoxLangTranspiler.transform( objectAccess.getContext(), TransformerContext.LEFT );
            Expression           variable = ( Expression ) BoxLangTranspiler.transform( objectAccess.getAccess(), TransformerContext.RIGHT );
            Map<String, String>  values   = new HashMap<>() {

                                              {
                                                  put( "scope", scope.toString() );
                                                  put( "variable", variable.toString() );
                                              }
                                          };
            ArrayInitializerExpr expr     = new ArrayInitializerExpr(
                NodeList.nodeList(
                    new StringLiteralExpr( scope.toString() ),
                    new StringLiteralExpr( variable.toString() )

                )
            );
            return expr;
        } else if ( objectAccess.getContext() instanceof BoxScope && objectAccess.getAccess() instanceof BoxIdentifier ) {
            Expression          scope    = ( Expression ) BoxLangTranspiler.transform( objectAccess.getContext(), TransformerContext.LEFT );
            Expression          variable = ( Expression ) BoxLangTranspiler.transform( objectAccess.getAccess(), TransformerContext.RIGHT );
            Map<String, String> values   = new HashMap<>() {

                                             {
                                                 put( "scope", scope.toString() );
                                                 put( "variable", variable.toString() );
                                             }
                                         };
            String              template = switch ( context ) {
                                             case LEFT -> """
                                                          ${scope}.put(Key.of("${variable}"))
                                                          """;
                                             default -> """
                                                        ${scope}.get( Key.of( "${variable}" ) )
                                                        """;
                                         };
            Node                javaExpr = parseExpression( template, values );
            logger.info( side + node.getSourceText() + " -> " + javaExpr );
            addIndex( javaExpr, node );
            return javaExpr;
        }

        throw new IllegalStateException( "" );
    }
}
