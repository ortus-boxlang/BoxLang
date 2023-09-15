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
package ortus.boxlang.ast;

import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.Position;

/**
 * Abstract Node class representing statements
 */
public abstract class BoxStatement extends BoxNode {

	/**
	 * Constructor
	 *
	 * @param position   position of the statement in the source code
	 * @param sourceText source code of the statement
	 */
	public BoxStatement( Position position, String sourceText ) {
		super( position, sourceText );
	}
}
