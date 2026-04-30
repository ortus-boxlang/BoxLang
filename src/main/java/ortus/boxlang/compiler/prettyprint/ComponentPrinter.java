/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.compiler.prettyprint;

import ortus.boxlang.compiler.ast.expression.BoxClosure;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;
import ortus.boxlang.compiler.ast.statement.BoxReturn;
import ortus.boxlang.compiler.ast.statement.component.BoxComponent;

public class ComponentPrinter {

	private Visitor visitor;

	public ComponentPrinter( Visitor visitor ) {
		this.visitor = visitor;
	}

	public void print( BoxComponent node ) {
		visitor.printPreComments( node );
		if ( visitor.isTemplate() ) {
			printTemplate( node );
		} else {
			printScript( node );
		}
		visitor.printPostComments( node );
	}

	private void printTemplate( BoxComponent node ) {
		var currentDoc = visitor.getCurrentDoc();

		currentDoc
		    .append( "<" + visitor.componentPrefix )
		    .append( node.getName() );

		// Unwrap condition closures: the CF parser wraps cfloop condition expressions
		// in a BoxClosure(BoxReturn(expr)). For pretty printing, we want to output
		// just the original expression text, not the closure wrapper.
		unwrapConditionClosures( node );

		// Check if single_attribute_per_line is enabled for templates
		boolean singleAttributePerLine = visitor.config.getTemplate().getSingleAttributePerLine();
		visitor.helperPrinter.printKeyValueAnnotations( node.getAttributes(), false, singleAttributePerLine );

		if ( node.getBody() != null ) {
			if ( node.getBody().isEmpty() ) {
				// existing, but empty body gives us <bx:componentName />
				// This is important for custom tags that expect to execute twice-- start and end
				currentDoc.append( "/>" );
			} else {
				// existing body with statements gives us <bx:componentName> statements... </bx:componentName>
				currentDoc.append( ">" );
				visitor.helperPrinter.printTemplateBody( node.getBody() );
				currentDoc.append( "</" + visitor.componentPrefix );
				currentDoc.append( node.getName() );
				currentDoc.append( ">" );
			}
		} else {
			// not existing body gives us <bx:componentName> or <bx:componentName /> based on config
			if ( visitor.config.getTemplate().getSelfClosing() ) {
				currentDoc.append( " />" );
			} else {
				currentDoc.append( ">" );
			}
		}
	}

	private void printScript( BoxComponent node ) {
		if ( "bx:".equals( visitor.componentPrefix ) ) {
			visitor.print( visitor.componentPrefix );
		}
		visitor.print( node.getName() );

		var hasBody = node.getBody() != null && !node.getBody().isEmpty();

		visitor.helperPrinter.printKeyValueAnnotations( node.getAttributes(), hasBody );

		if ( hasBody ) {
			visitor.helperPrinter.printBlock( node, node.getBody() );
		} else {
			visitor.printSemicolon();
		}
	}

	/**
	 * Unwrap the condition closure on a loop component. The CF parser wraps
	 * cfloop condition expressions in a BoxClosure(BoxReturn(expr)) for runtime
	 * evaluation. For pretty printing, we replace the closure with a BoxStringLiteral
	 * containing the original expression text so it prints as a plain attribute value.
	 * This ONLY applies to the "condition" attribute of the "loop" component.
	 */
	private void unwrapConditionClosures( BoxComponent node ) {
		if ( !node.getName().equalsIgnoreCase( "loop" ) ) {
			return;
		}
		for ( var attr : node.getAttributes() ) {
			if ( attr.getKey().getSourceText() != null
			    && attr.getKey().getSourceText().equalsIgnoreCase( "condition" )
			    && attr.getValue() instanceof BoxClosure closure
			    && closure.getArgs().isEmpty()
			    && closure.getBody() instanceof BoxReturn retStmt ) {
				// Use the closure's source text (original condition expression) if available,
				// otherwise toString() the return expression
				String conditionText;
				if ( closure.getSourceText() != null ) {
					conditionText = closure.getSourceText();
				} else if ( retStmt.getExpression() != null ) {
					conditionText = retStmt.getExpression().toString();
				} else {
					continue;
				}
				attr.setValue( new BoxStringLiteral( conditionText, null, null ) );
			}
		}
	}
}
