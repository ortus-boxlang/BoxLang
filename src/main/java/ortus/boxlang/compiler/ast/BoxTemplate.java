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
package ortus.boxlang.compiler.ast;

import java.util.List;

import ortus.boxlang.compiler.ast.visitor.ReplacingBoxVisitor;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;

/**
 * Root node for a templating (program) cfm/bxm
 */
public class BoxTemplate extends BoxScript {

	/**
	 * Creates an AST for a program which is represented by a list of statements
	 *
	 * @param statements list of the statements nodes
	 * @param position   position within the source code
	 * @param sourceText source code
	 *
	 * @see Position
	 * @see BoxStatement
	 */
	public BoxTemplate( List<BoxStatement> statements, Position position, String sourceText ) {
		super( statements, position, sourceText );
	}

	public void accept( VoidBoxVisitor v ) {
		v.visit( this );
	}

	public BoxNode accept( ReplacingBoxVisitor v ) {
		return v.visit( this );
	}

}
