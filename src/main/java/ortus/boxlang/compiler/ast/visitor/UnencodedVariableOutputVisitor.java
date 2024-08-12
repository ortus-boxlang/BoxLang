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
package ortus.boxlang.compiler.ast.visitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.expression.BoxFunctionInvocation;
import ortus.boxlang.compiler.ast.expression.BoxStringInterpolation;
import ortus.boxlang.compiler.ast.statement.BoxBufferOutput;
import ortus.boxlang.runtime.BoxRuntime;

/**
 * I look for varibles output in tag based code which are not encoded
 */
public class UnencodedVariableOutputVisitor extends VoidBoxVisitor {

	/**
	 * The list of issues found
	 */
	List<Issue>	issues		= new ArrayList<>();

	/**
	 * The list of BIFs that encode output
	 * TODO: Check if this is accurate
	 */
	Set<String>	encodeBIFs	= Set.of( "encodeforhtml", "encodeforhtmlattribute", "encodeforjavascript", "encodeforcss", "encodeforurl", "htmleditformat" );

	/**
	 * Constructor
	 */
	public UnencodedVariableOutputVisitor() {
	}

	/**
	 * Get the issues found during the visit
	 * 
	 * @return the issues
	 */
	public List<Issue> getIssues() {
		return issues;
	}

	public void visit( BoxBufferOutput node ) {
		if ( node.getExpression() instanceof BoxStringInterpolation ) {
			checkNode( node.getExpression() );
		}

		super.visit( node );
	}

	private void checkNode( BoxExpression node ) {
		if ( ! ( node instanceof BoxStringInterpolation ) && !node.isLiteral() && !isBIFCall( node ) && isNotEncoded( node ) ) {
			issues.add( new Issue(
			    "Un-encoded " + node.getClass().getSimpleName() + " output on line " + node.getPosition().getStart().getLine() + " -- " + node.toString(),
			    node ) );
		} else {
			node.getChildren().forEach( e -> checkNode( ( BoxExpression ) e ) );
		}
	}

	/**
	 * Is this expression inside of a function call named one of our encoding BIFs above
	 * 
	 * @param expr
	 * 
	 * @return
	 */
	private boolean isBIFCall( BoxExpression node ) {
		if ( node instanceof BoxFunctionInvocation bfi ) {
			return BoxRuntime.getInstance().getFunctionService().hasGlobalFunction( bfi.getName() );
		}
		return false;
	}

	/**
	 * Is this expression inside of a function call named one of our encoding BIFs above
	 * 
	 * @param expr
	 * 
	 * @return
	 */
	private boolean isNotEncoded( BoxExpression expr ) {
		return expr.getFirstAncestorOfType( BoxFunctionInvocation.class, n -> encodeBIFs.contains( n.getName().toLowerCase() ) ) == null;
	}

	public record Issue( String message, BoxExpression expr ) {

	}
}
