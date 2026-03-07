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

import java.util.Iterator;

import ortus.boxlang.compiler.ast.expression.BoxArrayLiteral;
import ortus.boxlang.compiler.ast.expression.BoxFQN;
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
import ortus.boxlang.compiler.ast.expression.BoxSpreadExpression;
import ortus.boxlang.compiler.ast.expression.BoxStructLiteral;
import ortus.boxlang.compiler.ast.expression.IBoxSimpleLiteral;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.ExpressionException;

/**
 * Abstract class representing Expressions
 */
public abstract class BoxExpression extends BoxNode {

	/**
	 * Utility method to detect if an expression node is a terminal Literal
	 *
	 * @return true if it is false otherwise
	 */
	public boolean isLiteral() {
		return false;
	}

	/**
	 * Returns true if this expression is guaranteed to return a boolean value.
	 *
	 * @return true if the expression always returns a boolean
	 */
	public boolean returnsBoolean() {
		return false;
	}

	/**
	 * Returns true if this expression is guaranteed to return a Number value.
	 *
	 * @return true if the expression always returns a Number
	 */
	public boolean returnsNumber() {
		return false;
	}

	/**
	 * Constructor
	 *
	 * @param position   position of the expression in the source code
	 * @param sourceText source code of the expression
	 */
	protected BoxExpression( Position position, String sourceText ) {
		super( position, sourceText );
	}

	// Utility methods for working with expressions in AST

	/**
	 * Get the value of this BoxExpression as a simple value
	 *
	 * @return The value
	 */
	public Object getAsSimpleValue() {
		return getAsSimpleValue( null, false );
	}

	/**
	 * Get the value of this BoxExpression as a simple value
	 *
	 * @param defaultValue     The default value to return if the expression is null
	 * @param identifierAsText Whether to return identifier as text
	 * 
	 * @return The value
	 */
	public Object getAsSimpleValue( Object defaultValue, boolean identifierAsText ) {
		if ( this instanceof IBoxSimpleLiteral lit ) {
			return lit.getValue();
		}
		if ( this instanceof BoxFQN fqn ) {
			return fqn.getValue();
		}
		if ( identifierAsText && this instanceof BoxIdentifier id ) {
			return id.getName();
		}
		if ( defaultValue != null ) {
			return defaultValue;
		} else {
			throw new ExpressionException( "Unsupported BoxExpr type: " + this.getClass().getSimpleName(), this );
		}
	}

	/**
	 * Get the value of this BoxExpression as a literal value
	 *
	 * @return The value
	 */
	public Object getAsLiteralValue() {
		if ( this instanceof IBoxSimpleLiteral lit ) {
			return lit.getValue();
		}
		if ( this instanceof BoxFQN fqn ) {
			return fqn.getValue();
		}
		if ( this instanceof BoxArrayLiteral arr ) {
			boolean ambiguousSpreadOnly = !arr.getValues().isEmpty()
			    && arr.getValues().stream().allMatch( value -> value instanceof BoxSpreadExpression );
			if ( ambiguousSpreadOnly ) {
				Array	arrayResult		= Array.of();
				IStruct	structResult	= new Struct( IStruct.TYPES.LINKED );
				boolean	hasArraySpread	= false;
				boolean	hasStructSpread	= false;
				for ( BoxExpression value : arr.getValues() ) {
					BoxSpreadExpression	spread		= ( BoxSpreadExpression ) value;
					Object				spreadValue	= spread.getExpression().getAsLiteralValue();
					if ( spreadValue instanceof IStruct spreadStruct ) {
						hasStructSpread = true;
						spreadStruct.forEach( structResult::put );
					} else {
						hasArraySpread = true;
						Array spreadArray = Array.copyOf( spreadValue );
						arrayResult.addAll( spreadArray );
					}
					if ( hasArraySpread && hasStructSpread ) {
						throw new ExpressionException( "Cannot mix array and struct spread values in an ambiguous bracket literal.", this );
					}
				}
				return hasStructSpread ? structResult : arrayResult;
			}

			Array array = Array.of();
			arr.getValues().forEach( value -> {
				if ( value instanceof BoxSpreadExpression spread ) {
					Array spreadArray = Array.copyOf( spread.getExpression().getAsLiteralValue() );
					array.addAll( spreadArray );
				} else {
					array.add( value.getAsLiteralValue() );
				}
			} );
			return array;
		}
		if ( this instanceof BoxStructLiteral str ) {
			IStruct					struct		= Struct.of();
			Iterator<BoxExpression>	iterator	= str.getValues().iterator();
			while ( iterator.hasNext() ) {
				BoxExpression current = iterator.next();
				if ( current instanceof BoxSpreadExpression spread ) {
					Object spreadValue = spread.getExpression().getAsLiteralValue();
					if ( spreadValue instanceof IStruct spreadStruct ) {
						spreadStruct.forEach( struct::put );
					} else {
						Array spreadArray = Array.copyOf( spreadValue );
						for ( int i = 1; i <= spreadArray.size(); i++ ) {
							struct.put( Key.of( i ), spreadArray.getAt( i ) );
						}
					}
				} else {
					if ( iterator.hasNext() ) {
						BoxExpression value = iterator.next();
						struct.put( Key.of( current.getAsSimpleValue( null, true ) ), value.getAsLiteralValue() );
					} else {
						// Handle odd number of values
						throw new IllegalArgumentException( "Invalid number of values in BoxStructLiteral" );
					}
				}
			}
			return struct;
		}
		// return "[Runtime Expression]";
		throw new ExpressionException( "Non-literal value in BoxExpr type: " + this.getClass().getSimpleName(), this );
	}

}
