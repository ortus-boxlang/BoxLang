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
package ortus.boxlang.runtime.dynamic;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.IBoxContext.ScopeSearchResult;
import ortus.boxlang.runtime.dynamic.casters.NumberCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.RequestScope;
import ortus.boxlang.runtime.scopes.ServerScope;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.ExpressionException;
import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;
import ortus.boxlang.runtime.types.exceptions.ScopeNotFoundException;

/**
 * I handle interpreting expressions
 */
public class ExpressionInterpreter {

	// These are the only scopes that will always exist
	private static Set<Key> scopes = Set.of(
	    VariablesScope.name,
	    RequestScope.name,
	    ServerScope.name
	);

	/**
	 * Resolve an expression pointing to a varaible in the format of foo, foo.bar, foo.bar.baz, etc.
	 * Only handles dot access at the moment.
	 *
	 * @param context    The context
	 * @param expression The expression
	 * @param safe       Whether to throw an exception if the variable is not found
	 *
	 * @return The expression found
	 */
	public static Object getVariable( IBoxContext context, String expression, boolean safe ) {
		expression = expression.trim();
		// If expressions is wrapped in "" or '', then unwrap and return directly
		if ( expression.startsWith( "\"" ) && expression.endsWith( "\"" ) ) {
			// replace "" with ""
			return expression.substring( 1, expression.length() - 1 ).replace( "\"\"", "\"" );
		}
		if ( expression.startsWith( "'" ) && expression.endsWith( "'" ) ) {
			// Replace '' with '
			return expression.substring( 1, expression.length() - 1 ).replace( "''", "'" );
		}
		// If expression is a number, return it directly
		if ( expression.matches( "^-?\\d+(\\.\\d+)?$" ) ) {
			return NumberCaster.cast( expression );
		}
		// Check for true/false
		if ( expression.equalsIgnoreCase( "true" ) ) {
			return true;
		}
		if ( expression.equalsIgnoreCase( "false" ) ) {
			return false;
		}

		String[]	parts	= splitParts( context, expression, safe );
		Object		ref		= null;
		Key			refName	= Key.of( parts[ 0 ] );

		// Expression starts with a scope name like request.foo
		if ( scopes.contains( refName ) ) {
			try {
				ref = context.getScopeNearby( refName );
			} catch ( ScopeNotFoundException e ) {
				// If using safe access, return null if the scope is not found
				if ( safe ) {
					return null;
				}
				throw e;
			}
		} else {
			// Unscoped variable like foo.bar. This finds the first part of the expression
			ref = context.scopeFindNearby( refName, ( safe ? context.getDefaultAssignmentScope() : null ) ).value();
			if ( ref == null && !safe ) {
				throw new KeyNotFoundException( "Variable [" + refName + "] not found." );
			}
		}

		// loop over remaining items and use the dereferencer to find them all.
		for ( int i = 1; i < parts.length; i++ ) {
			ref = Referencer.get( context, ref, Key.of( parts[ i ] ), safe );
		}
		return ref;
	}

	/**
	 * Resolve an expression pointing to a varaible in the format of foo, foo.bar, foo.bar.baz, etc.
	 * Only handles dot access at the moment.
	 *
	 * @param context    The context
	 * @param expression The expression
	 * @param value      The value to set
	 *
	 * @return The expression found
	 */
	public static Object setVariable( IBoxContext context, String expression, Object value ) {
		String[]	parts	= splitParts( context, expression, false );
		Object		ref		= null;
		Key			refName	= Key.of( parts[ 0 ] );
		Key[]		keys;

		// Expression starts with a scope name like request.foo
		if ( scopes.contains( refName ) ) {
			if ( parts.length == 1 ) {
				// We don't allow re-assignment of scopes like
				// url = "test"
				throw new BoxRuntimeException( "Cannot assign to a scope: [" + expression + "]" );
			}
			ref		= context.getScopeNearby( refName );
			// create Key[] out of remaining strings in parts
			keys	= new Key[ parts.length - 1 ];
			for ( int i = 1; i < parts.length; i++ ) {
				keys[ i - 1 ] = Key.of( parts[ i ] );
			}
		} else {
			// Unscoped variable like foo.bar. We need to search and find what scope it lives in, if any.
			ScopeSearchResult scopeSearchResult = context.scopeFindNearby( refName, context.getDefaultAssignmentScope() );
			ref = scopeSearchResult.scope();
			if ( scopeSearchResult.isScope() ) {
				// create Key[] out of remaining strings in parts
				keys = new Key[ parts.length - 1 ];
				for ( int i = 1; i < parts.length; i++ ) {
					keys[ i - 1 ] = Key.of( parts[ i ] );
				}
			} else {
				// create Key[] out of all parts
				keys = new Key[ parts.length ];
				for ( int i = 0; i < parts.length; i++ ) {
					keys[ i ] = Key.of( parts[ i ] );
				}
			}
		}

		// Now that we have the root variable, set the remaining keys
		return Referencer.setDeep( context, ref, value, keys );
	}

	/**
	 * Split the expression into parts
	 *
	 * This supports foo.bar.baz
	 * Also support foo["bar"][ baz ][ "#bum#" ]
	 *
	 * @param expression
	 *
	 * @return
	 */
	private static String[] splitParts( IBoxContext context, String expression, boolean safe ) {
		if ( expression.isEmpty() || expression.startsWith( "." ) || expression.endsWith( "." ) || expression.startsWith( "[" ) ) {
			throw new ExpressionException( "Invalid expression", null, expression );
		}

		boolean			ready		= true;
		boolean			quote		= false;
		char			quoteChar	= 0;
		boolean			bracket		= false;
		List<String>	parts		= new ArrayList<>();
		StringBuilder	part		= new StringBuilder();

		for ( int i = 0; i < expression.length(); i++ ) {
			if ( quote ) {
				if ( expression.charAt( i ) == quoteChar ) {
					// If there is a next char and the next char is also quoteChar, then just append and conttinue, incrementing one extra time
					if ( i + 1 < expression.length() && expression.charAt( i + 1 ) == quoteChar ) {
						part.append( quoteChar );
						i++;
						continue;
					}
					quote	= false;
					ready	= false;
					parts.add( part.toString() );
					part.setLength( 0 );
					continue;
				}
				part.append( expression.charAt( i ) );
				continue;
			}
			if ( expression.charAt( i ) == '"' || expression.charAt( i ) == '\'' ) {
				if ( part.length() > 0 || !ready ) {
					throw new ExpressionException( "Invalid expression, [" + expression.charAt( i ) + "] not allowed at position " + ( i + 1 ), null,
					    expression );
				}
				quote		= true;
				quoteChar	= expression.charAt( i );
				continue;
			}
			if ( expression.charAt( i ) == '.' ) {
				if ( part.length() > 0 ) {
					parts.add( part.toString() );
					part.setLength( 0 );
				}
				ready = true;
				continue;
			}
			if ( expression.charAt( i ) == '[' ) {
				if ( bracket ) {
					throw new ExpressionException( "Invalid expression, ([) not allowed at position " + ( i + 1 ), null, expression );
				}
				if ( part.length() > 0 ) {
					parts.add( part.toString() );
					part.setLength( 0 );
				}
				bracket	= true;
				ready	= true;
				continue;
			}
			if ( expression.charAt( i ) == ']' ) {
				if ( !bracket ) {
					throw new ExpressionException( "Invalid expression, (]) not allowed at position " + ( i + 1 ), null, expression );
				}
				if ( part.length() > 0 ) {
					parts.add( part.toString() );
					part.setLength( 0 );
				}
				bracket = false;
				continue;
			}
			// skip whitespace
			if ( Character.isWhitespace( expression.charAt( i ) ) ) {
				continue;
			}
			if ( !ready ) {
				throw new ExpressionException( "Invalid expression, [" + expression.charAt( i ) + "] not allowed at position " + ( i + 1 ), null, expression );
			}
			// check if char is letter or number, or underscore or dollar sign
			if ( Character.isLetterOrDigit( expression.charAt( i ) ) || expression.charAt( i ) == '_' || expression.charAt( i ) == '$' ) {
				// TODO: simple solution doesn't allow nested brackets. Need better recursion for that
				if ( bracket ) {
					// find ending bracket and grab all the stuff in between including the current char
					int end = expression.indexOf( ']', i );
					if ( end == -1 ) {
						throw new ExpressionException( "Invalid expression, unclosed bracket", null, expression );
					}
					// TODO: Assumes keys are strings. Safe unless the code is dealing with a Java Map with a non-string key
					parts.add( StringCaster.cast( getVariable( context, expression.substring( i, end ), safe ) ) );
					i = end - 1;
					continue;
				}
				part.append( expression.charAt( i ) );
				continue;
			}
		}
		if ( quote ) {
			throw new ExpressionException( "Invalid expression, unclosed quote", null, expression );
		}
		if ( bracket ) {
			throw new ExpressionException( "Invalid expression, unclosed bracket", null, expression );
		}

		if ( part.length() > 0 ) {
			parts.add( part.toString() );
		}
		return parts.toArray( new String[ 0 ] );
	}
}
