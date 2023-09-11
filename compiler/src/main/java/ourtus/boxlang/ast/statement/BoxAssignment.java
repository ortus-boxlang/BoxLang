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

import ourtus.boxlang.ast.BoxNode;
import ourtus.boxlang.ast.BoxStatement;
import ourtus.boxlang.ast.Position;
import ourtus.boxlang.ast.BoxExpr;

public class BoxAssignment extends BoxStatement {

	private BoxExpr left;
	private BoxExpr right;

	public BoxAssignment(BoxExpr left, BoxExpr right, Position position, String sourceText) {
		super( position, sourceText );
		this.left  = left;
		this.right = right;
		this.left.setParent(this);
		this.right.setParent(this);
	}

	public BoxExpr getLeft() {
		return left;
	}

	public void setLeft( BoxExpr left ) {
		this.left = left;
	}

	public BoxExpr getRight() {
		return right;
	}

	public void setRight( BoxExpr right ) {
		this.right = right;
	}
}
