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
package ourtus.boxlang.ast.statement;

import ourtus.boxlang.ast.BoxExpr;
import ourtus.boxlang.ast.BoxStatement;
import ourtus.boxlang.ast.Position;

import java.util.Collections;
import java.util.List;

/**
 * AST Node representing a switch statement
 */
public class BoxSwitch extends BoxStatement {

	private final BoxExpr condition;
	private final List<BoxSwitchCase> cases;

	/**
	 * Creates the AST node
	 * @param condition the expression of the switch statement
	 * @param cases list of the cases
	 * @param position position of the statement in the source code
	 * @param sourceText source code that originated the Node
	 */
	public BoxSwitch(BoxExpr condition,List<BoxSwitchCase> cases,Position position, String sourceText) {
		super(position, sourceText);
		this.condition = condition;
		this.condition.setParent(this);
		this.cases = Collections.unmodifiableList(cases);
		this.cases.forEach(arg -> arg.setParent(this));
	}

	public BoxExpr getCondition() {
		return condition;
	}

	public List<BoxSwitchCase> getCases() {
		return cases;
	}
}
