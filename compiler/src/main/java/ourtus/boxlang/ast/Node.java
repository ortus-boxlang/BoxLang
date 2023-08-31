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
package ourtus.boxlang.ast;

import java.util.ArrayList;
import java.util.List;

public class Node {

	protected Position position;
	private final String sourceText;
	private Node parent;

	private Node originator;
	private final List<Node> children;

	public Node( Position position, String sourceText ) {
		this.position   = position;
		this.sourceText = sourceText;
		this.children = new ArrayList<>();
	}

	public Position getPosition() {
		return position;
	}

	public String getSourceText() {
		return sourceText;
	}

	public void setParent(Node parent) {
		this.parent = parent;
	}

	public Node getParent() {
		return parent;
	}

	public List<Node> getChildren() {
		return children;
	}

	public Node getOriginator() {
		return originator;
	}
}
