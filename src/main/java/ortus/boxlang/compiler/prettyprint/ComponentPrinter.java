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

import ortus.boxlang.compiler.ast.expression.BoxAssignment;
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
		// Use the component prefix only when the original source used it.
		// "include" can appear both as a bare keyword (`include template="..."`) and as
		// a prefixed component (`bx:include template="..."`), so we check the source
		// text rather than always applying the visitor prefix.
		String	sourceText	= node.getSourceText();
		boolean	hadPrefix	= sourceText != null && sourceText.toLowerCase().startsWith( visitor.componentPrefix.toLowerCase() );
		if ( hadPrefix ) {
			visitor.print( visitor.componentPrefix );
		}
		visitor.print( node.getName() );

		var hasBody = node.getBody() != null && !node.getBody().isEmpty();

		// The shorthand `include "path"` form is parsed with a synthesized `template` attribute
		// whose key source text is the path expression source (not "template"). Detect this case
		// and print the value directly (e.g. `include "path"`) to preserve the original form.
		if ( isIncludeShorthand( node ) ) {
			visitor.print( " " );
			node.getAttributes().get( 0 ).getValue().accept( visitor );
		} else {
			visitor.helperPrinter.printKeyValueAnnotations( node.getAttributes(), hasBody );
		}

		if ( hasBody ) {
			visitor.helperPrinter.printBlock( node, node.getBody() );
		} else {
			visitor.printSemicolon();
		}
	}

	/**
	 * Returns true if the component is the shorthand {@code include "path"} form.
	 * <p>
	 * Three forms of {@code include} exist in script mode:
	 * <ol>
	 * <li>{@code include "path"} — shorthand; value is a plain string/interpolation,
	 * key source text is the path expression (not {@code "template"})</li>
	 * <li>{@code include template="path"} — explicit via {@code visitInclude}; the entire
	 * {@code template="path"} is parsed as a {@code BoxAssignment} expression</li>
	 * <li>{@code bx:include template="path"} — component form via {@code visitComponent};
	 * key source text is {@code "template"}, value is a plain string/interpolation</li>
	 * </ol>
	 * Only form 1 should be printed without the {@code template=} key name.
	 *
	 * @param node the BoxComponent node to test
	 *
	 * @return true if this was written as {@code include "path"} without the key name
	 */
	private boolean isIncludeShorthand( BoxComponent node ) {
		if ( !node.getName().equalsIgnoreCase( "include" ) ) {
			return false;
		}
		if ( node.getAttributes() == null || node.getAttributes().size() != 1 ) {
			return false;
		}
		var attr = node.getAttributes().get( 0 );
		// Form 2: explicit `include template="path"` — value is a BoxAssignment expression.
		if ( attr.getValue() instanceof BoxAssignment ) {
			return false;
		}
		// Form 3: `bx:include template="path"` — key source text is literally "template".
		String keySrc = attr.getKey() != null ? attr.getKey().getSourceText() : null;
		if ( keySrc != null && keySrc.equalsIgnoreCase( "template" ) ) {
			return false;
		}
		// Form 1: shorthand `include "path"` — value is plain string/interpolation,
		// key source text is the path expression source (not "template").
		return true;
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
