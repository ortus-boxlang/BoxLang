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

/**
 * Transform a SwitchStatement Node the equivalent Java Parser AST nodes
 */
public class BoxSwitchTransformer extends AbstractTransformer {

	Logger logger = LoggerFactory.getLogger( BoxParenthesisTransformer.class );

	/**
	 * Transform a collection for statement
	 *
	 * @param node    a BoxForIn instance
	 * @param context transformation context
	 *
	 * @return a Java Parser Block statement with an iterator and a while loop
	 *
	 * @throws IllegalStateException
	 */
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
				addIndex( javaIfStmt, c );
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
		logger.info( node.getSourceText() + " -> " + javaSwitch );
		addIndex( javaSwitch, node );
		return javaSwitch;
	}
}
