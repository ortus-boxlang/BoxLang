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
package ortus.boxlang.runtime.types.exceptions;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;

/**
 * This is the base exception for all expression or evaluation errors in the BoxLang runtime.
 */
public class ExpressionException extends BoxRuntimeException {

	/**
	 * Internal expression error number.
	 */
	protected String	errNumber	= null;
	protected Position	position	= null;
	protected String	sourceText	= null;

	/**
	 * Constructor
	 *
	 * @param message The message
	 */
	public ExpressionException( String message, BoxNode node ) {
		this( message, null, null, null, node.getPosition(), node.getSourceText() );
	}

	/**
	 * Constructor
	 *
	 * @param message The message
	 */
	public ExpressionException( String message, Position position, String sourceText ) {
		this( message, null, null, null, position, sourceText );
	}

	/**
	 * Constructor
	 *
	 * @param message   The message
	 * @param detail    The detail
	 * @param errNumber The errNumber
	 * @param cause     The cause
	 */
	public ExpressionException( String message, String detail, String errNumber, Throwable cause, Position position, String sourceText ) {
		// TODO: may want to change the way we dispaly position and sourcetext in the future
		super( message + "\n position:" + position + "\n sourceText: " + sourceText, detail, "expression", cause );
		this.errNumber	= errNumber;
		this.position	= position;
		this.sourceText	= sourceText;
	}

	public Position getPosition() {
		return position;
	}

	public IStruct dataAsStruct() {
		IStruct result = super.dataAsStruct();
		result.put( Key.errNumber, errNumber );
		return result;
	}

}
