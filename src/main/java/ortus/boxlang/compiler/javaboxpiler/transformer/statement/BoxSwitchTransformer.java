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
package ortus.boxlang.compiler.javaboxpiler.transformer.statement;

import java.util.HashMap;
import java.util.Map;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.statement.BoxSwitch;
import ortus.boxlang.compiler.javaboxpiler.JavaTranspiler;
import ortus.boxlang.compiler.javaboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.TransformerContext;
import ortus.boxlang.runtime.types.exceptions.ExpressionException;

/**
 * Transform a SwitchStatement Node the equivalent Java Parser AST nodes
 */
public class BoxSwitchTransformer extends AbstractTransformer {

	public BoxSwitchTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

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
		int					swtichCount	= transpiler.incrementAndGetSwitchCounter();
		BoxSwitch			boxSwitch	= ( BoxSwitch ) node;
		Expression			condition	= ( Expression ) transpiler.transform( boxSwitch.getCondition(), TransformerContext.RIGHT );

		Map<String, String>	values		= new HashMap<>() {

											{
												put( "contextName", transpiler.peekContextName() );
												put( "switchValue", condition.toString() );
												put( "switchValueName", "switchValue" + swtichCount );
												put( "caseEnteredName", "caseEntered" + swtichCount );
											}
										};
		String				template	= """
		                                  do {

		                                  } while(false);
		                                  """;
		BlockStmt			body		= new BlockStmt();
		DoStmt				javaSwitch	= ( DoStmt ) parseStatement( template, values );

		// Create if statements for each case
		boxSwitch.getCases().forEach( c -> {
			if ( c.getCondition() != null ) {
				String caseTemplate;
				if ( c.getDelimiter() == null ) {
					caseTemplate = """
					               	if( ${caseEnteredName} || EqualsEquals.invoke( ${condition}, ${switchValueName} ) ) {
					               		${caseEnteredName} = true;
					               }
					               """;
				} else {
					Expression delimiter = ( Expression ) transpiler.transform( c.getDelimiter(), TransformerContext.RIGHT );
					values.put( "delimiter", delimiter.toString() );
					caseTemplate = """
					               	if( ${caseEnteredName} || ListUtil.containsNoCase( StringCaster.cast( ${condition} ), StringCaster.cast( ${switchValueName} ), ${delimiter} ) ) {
					               		${caseEnteredName} = true;
					               }
					               """;
				}
				Expression switchExpr = ( Expression ) transpiler.transform( c.getCondition(), TransformerContext.RIGHT );

				values.put( "condition", switchExpr.toString() );
				IfStmt		javaIfStmt	= ( IfStmt ) parseStatement( caseTemplate, values );
				BlockStmt	thenBlock	= javaIfStmt.getThenStmt().asBlockStmt();
				c.getBody().forEach( stmt -> {
					thenBlock.addStatement( ( Statement ) transpiler.transform( stmt ) );
				} );
				body.addStatement( javaIfStmt );
				addIndex( javaIfStmt, c );
			}
		} );
		// Add any default cases to the end
		// TODO: Can there be more than one default case?
		boolean hasDefault = false;
		for ( var c : boxSwitch.getCases() ) {
			if ( c.getCondition() == null ) {
				if ( hasDefault ) {
					throw new ExpressionException( "Multiple default cases not supported", c.getPosition(), c.getSourceText() );
				}
				hasDefault = true;
				c.getBody().forEach( stmt -> {
					body.addStatement( ( Statement ) transpiler.transform( stmt ) );
				} );
			}
		}
		javaSwitch.setBody( body );
		addIndex( javaSwitch, node );

		BlockStmt switchHolder = new BlockStmt();
		switchHolder.addStatement( ( Statement ) parseStatement( "Object ${switchValueName} = ${switchValue};", values ) );
		switchHolder.addStatement( ( Statement ) parseStatement( "boolean ${caseEnteredName} = false;", values ) );
		switchHolder.addStatement( javaSwitch );

		// logger.atTrace().log( node.getSourceText() + " -> " + switchHolder );
		return switchHolder;
	}
}
