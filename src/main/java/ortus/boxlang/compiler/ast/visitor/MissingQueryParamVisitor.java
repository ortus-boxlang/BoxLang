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

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxStringInterpolation;
import ortus.boxlang.compiler.ast.statement.component.BoxComponent;

/**
 * I look for varibles output in tag based code which are not encoded
 */
public class MissingQueryParamVisitor extends VoidBoxVisitor {

	/**
	 * The list of issues found
	 */
	List<Issue> issues = new ArrayList<>();

	/**
	 * Constructor
	 */
	public MissingQueryParamVisitor() {
	}

	/**
	 * Get the issues found during the visit
	 * 
	 * @return the issues
	 */
	public List<Issue> getIssues() {
		return issues;
	}

	public void visit( BoxComponent node ) {
		if ( node.getName().equalsIgnoreCase( "query" ) ) {
			node.getBody().forEach( s -> checkNode( s ) );
		}
		super.visit( node );
	}

	private void checkNode( BoxNode node ) {
		if ( node instanceof BoxStringInterpolation ) {
			if ( node.getFirstAncestorOfType( BoxComponent.class, n -> n.getName().equalsIgnoreCase( "queryparam" ) ) == null ) {
				issues.add( new Issue(
				    "Query contains unparameterized variable output on line " + node.getPosition().getStart().getLine() + " -- " + node.toString(), node ) );
			}
		} else if ( ! ( node instanceof BoxComponent bc && bc.getName().equalsIgnoreCase( "queryparam" ) ) ) {
			node.getChildren().forEach( this::checkNode );
		}

	}

	public record Issue( String message, BoxNode expr ) {

	}
}
