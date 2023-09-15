/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.transpiler.transformer.expression;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.expression.BoxStringLiteral;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;

/**
 * Transform a BoxStringLiteral Node the equivalent Java Parser AST nodes
 */
public class BoxStringLiteralTransformer extends AbstractTransformer {

    Logger logger = LoggerFactory.getLogger( BoxStringLiteralTransformer.class );

    /**
     * Transform BoxStringLiteral argument
     *
     * @param node    a BoxStringLiteral instance
     * @param context transformation context
     *
     * @return generates a Java Parser string Literal
     */
    @Override
    public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
        BoxStringLiteral  literal = ( BoxStringLiteral ) node;
        StringLiteralExpr expr    = new StringLiteralExpr( literal.getValue() );
        String            side    = context == TransformerContext.NONE ? "" : "(" + context.toString() + ") ";
        logger.info( side + node.getSourceText() + " -> " + expr );
        return expr;
    }
}
