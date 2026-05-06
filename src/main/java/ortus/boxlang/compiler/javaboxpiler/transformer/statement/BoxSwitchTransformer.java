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
	 * Transform a switch statement
	 *
	 * @param node    a BoxSwitch instance
	 * @param context transformation context
	 *
	 * @return a Java Parser Block statement
	 *
	 * @throws IllegalStateException
	 */
	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxSwitch	boxSwitch	= ( BoxSwitch ) node;
		boolean		isBreaking	= boxSwitch.hasBreakingCases();

		if ( isBreaking ) {
			return transformBreakingSwitch( boxSwitch );
		} else {
			return transformScriptSwitch( boxSwitch );
		}
	}

	/**
	 * Transform a tag-based switch with breaking cases.
	 * Each case automatically stops after execution (no fall-through).
	 * Break statements inside are NOT caught by this switch - they propagate to enclosing loops.
	 */
	private Node transformBreakingSwitch( BoxSwitch boxSwitch ) {
		int					swtichCount		= transpiler.incrementAndGetSwitchCounter();
		Expression			condition		= ( Expression ) transpiler.transform( boxSwitch.getCondition(), TransformerContext.RIGHT );

		Map<String, String>	values			= new HashMap<>() {

												{
													put( "contextName", transpiler.peekContextName() );
													put( "switchValue", condition.toString() );
													put( "switchValueName", "switchValue" + swtichCount );
												}
											};

		BlockStmt			switchHolder	= new BlockStmt();
		switchHolder.addStatement( ( Statement ) parseStatement( "Object ${switchValueName} = ${switchValue};", values ) );

		// Build an if/else-if chain - no do-while, no break target
		IfStmt	firstIf	= null;
		IfStmt	lastIf	= null;

		for ( var c : boxSwitch.getCases() ) {
			if ( c.getCondition() == null ) {
				continue;
			}

			String caseTemplate;
			if ( c.getDelimiter() == null ) {
				caseTemplate = """
				               	if( EqualsEquals.invoke( ${condition}, ${switchValueName} ) ) {
				               }
				               """;
			} else {
				Expression delimiter = ( Expression ) transpiler.transform( c.getDelimiter(), TransformerContext.RIGHT );
				values.put( "delimiter", delimiter.toString() );
				caseTemplate = """
				               if( ${switchValueName} != null && ListUtil.containsNoCase( StringCaster.cast( ${condition} ), StringCaster.cast( ${switchValueName} ), ${delimiter} ) ) {
				               }
				               """;
			}
			Expression switchExpr = ( Expression ) transpiler.transform( c.getCondition(), TransformerContext.RIGHT );
			values.put( "condition", switchExpr.toString() );

			IfStmt		javaIfStmt	= ( IfStmt ) parseStatement( caseTemplate, values );
			BlockStmt	thenBlock	= javaIfStmt.getThenStmt().asBlockStmt();
			if ( c.getBody() != null ) {
				c.getBody().forEach( stmt -> {
					thenBlock.addStatement( ( Statement ) transpiler.transform( stmt ) );
				} );
			}
			addIndex( javaIfStmt, c );

			if ( firstIf == null ) {
				firstIf	= javaIfStmt;
				lastIf	= javaIfStmt;
			} else {
				lastIf.setElseStmt( javaIfStmt );
				lastIf = javaIfStmt;
			}
		}

		// Add default case as the final else
		boolean hasDefault = false;
		for ( var c : boxSwitch.getCases() ) {
			if ( c.getCondition() == null ) {
				if ( hasDefault ) {
					throw new ExpressionException( "Multiple default cases not supported", c.getPosition(), c.getSourceText() );
				}
				hasDefault = true;
				if ( c.getBody() != null ) {
					BlockStmt defaultBlock = new BlockStmt();
					c.getBody().forEach( stmt -> {
						defaultBlock.addStatement( ( Statement ) transpiler.transform( stmt ) );
					} );
					if ( lastIf != null ) {
						lastIf.setElseStmt( defaultBlock );
					} else {
						// Only a default case, no conditional cases
						switchHolder.getStatements().addAll( defaultBlock.getStatements() );
					}
				}
			}
		}

		if ( firstIf != null ) {
			switchHolder.addStatement( firstIf );
		}
		addIndex( switchHolder, boxSwitch );

		return switchHolder;
	}

	/**
	 * Transform a script-based switch with fall-through behavior.
	 * Uses do-while(false) wrapper so break statements exit the switch.
	 */
	private Node transformScriptSwitch( BoxSwitch boxSwitch ) {
		int					swtichCount	= transpiler.incrementAndGetSwitchCounter();
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
					               if( ${caseEnteredName} || ( ${switchValueName} != null && ListUtil.containsNoCase( StringCaster.cast( ${condition} ), StringCaster.cast( ${switchValueName} ), ${delimiter} ) ) ) {
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
		boolean hasDefault = false;
		for ( var c : boxSwitch.getCases() ) {
			if ( c.getCondition() == null ) {
				if ( hasDefault ) {
					throw new ExpressionException( "Multiple default cases not supported", c.getPosition(), c.getSourceText() );
				}
				hasDefault = true;
				if ( c.getBody() != null ) {
					c.getBody().forEach( stmt -> {
						body.addStatement( ( Statement ) transpiler.transform( stmt ) );
					} );
				}
			}
		}
		javaSwitch.setBody( body );
		addIndex( javaSwitch, boxSwitch );

		BlockStmt switchHolder = new BlockStmt();
		switchHolder.addStatement( ( Statement ) parseStatement( "Object ${switchValueName} = ${switchValue};", values ) );
		switchHolder.addStatement( ( Statement ) parseStatement( "boolean ${caseEnteredName} = false;", values ) );
		switchHolder.addStatement( javaSwitch );

		return switchHolder;
	}
}
