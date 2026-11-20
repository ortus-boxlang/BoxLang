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

import com.github.javaparser.ast.Node;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.statement.BoxContinue;
import ortus.boxlang.compiler.ast.statement.BoxDo;
import ortus.boxlang.compiler.ast.statement.BoxForIn;
import ortus.boxlang.compiler.ast.statement.BoxForIndex;
import ortus.boxlang.compiler.ast.statement.BoxWhile;
import ortus.boxlang.compiler.javaboxpiler.JavaTranspiler;
import ortus.boxlang.compiler.javaboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.TransformerContext;

public class BoxContinueTransformer extends AbstractTransformer {

	public BoxContinueTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxContinue		continueNode	= ( BoxContinue ) node;
		String			template;
		ExitsAllowed	exitsAllowed	= getExitsAllowed( node );
		String			componentLabel	= "null";
		String			continueLabel	= "";
		if ( continueNode.getLabel() != null ) {
			continueLabel	= continueNode.getLabel().toLowerCase();
			componentLabel	= "\"" + continueNode.getLabel().toLowerCase() + "\"";
		}

		if ( exitsAllowed.equals( ExitsAllowed.COMPONENT ) ) {
			template = "if(true) return Component.BodyResult.ofContinue(" + componentLabel + ");";
		} else if ( exitsAllowed.equals( ExitsAllowed.LOOP ) ) {
			Integer breakCounter = transpiler.peekForLoopBreakCounter();
			if ( breakCounter != null && continuesInnermostForLoop( continueNode ) ) {
				// A for loop keeps its step in the loop's finally guarded by a didBreak flag. A
				// continue supersedes any pending break, so the flag must be cleared or the step
				// would be skipped and the loop would never advance.
				template = "if(true) { didBreak" + breakCounter + "=false; continue " + continueLabel + "; }";
			} else {
				template = "if(true) continue " + continueLabel + ";";
			}
		} else if ( exitsAllowed.equals( ExitsAllowed.FUNCTION ) ) {
			template = "if(true) return null;";
		} else {
			template = "if(true) return;";
		}

		Node javaStmt = parseStatement( template, new HashMap<>() );
		// logger.trace( node.getSourceText() + " -> " + javaStmt );
		addIndex( javaStmt, node );
		return javaStmt;
	}

	/**
	 * Determine if this continue targets the innermost enclosing {@code for} loop (a BoxForIndex).
	 * Only that loop owns the {@code didBreak} counter currently on the transpiler's stack, so only
	 * it may have its flag cleared. A labelled continue naming an outer loop leaves this loop
	 * instead, and its flag must be left untouched.
	 *
	 * @param continueNode the continue statement being transformed
	 *
	 * @return true if the continue transfers control to the innermost enclosing for loop
	 */
	private boolean continuesInnermostForLoop( BoxContinue continueNode ) {
		BoxNode nearestLoop = continueNode.getFirstNodeOfTypes( BoxForIndex.class, BoxForIn.class, BoxWhile.class, BoxDo.class );
		if ( ! ( nearestLoop instanceof BoxForIndex forIndex ) ) {
			return false;
		}
		String label = continueNode.getLabel();
		return label == null || label.equalsIgnoreCase( forIndex.getLabel() );
	}
}
