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
import ortus.boxlang.compiler.ast.statement.BoxBreak;
import ortus.boxlang.compiler.javaboxpiler.JavaTranspiler;
import ortus.boxlang.compiler.javaboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.TransformerContext;

public class BoxBreakTransformer extends AbstractTransformer {

	public BoxBreakTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxBreak		breakNode		= ( BoxBreak ) node;
		String			template;
		ExitsAllowed	exitsAllowed	= getExitsAllowed( node );
		String			componentLabel	= "null";
		String			breakLabel		= "";
		if ( breakNode.getLabel() != null ) {
			breakLabel		= breakNode.getLabel().toLowerCase();
			componentLabel	= "\"" + breakNode.getLabel().toLowerCase() + "\"";
		}

		if ( exitsAllowed.equals( ExitsAllowed.COMPONENT ) ) {
			template = "if(true) return Component.BodyResult.ofBreak(" + componentLabel + ");";
		} else if ( exitsAllowed.equals( ExitsAllowed.LOOP ) ) {
			String	breakDetectionName	= null;
			Integer	breakCounter		= transpiler.peekForLoopBreakCounter();
			if ( breakCounter != null ) {
				breakDetectionName	= "didBreak" + breakCounter;
				template			= "if(true) { " + breakDetectionName + "=true; break " + breakLabel + "; }";
			} else {
				template = "if(true) break " + breakLabel + ";";
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
}
