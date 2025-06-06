
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
package ortus.boxlang.runtime.components.system;

import java.util.Collection;
import java.util.Set;
import java.util.function.Supplier;

import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.BoxComponent;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.components.util.LoopUtil;
import ortus.boxlang.runtime.context.FunctionBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.ExpressionInterpreter;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.Closure;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.util.ListUtil;
import ortus.boxlang.runtime.util.FileSystemUtil;
import ortus.boxlang.runtime.validation.Validator;

@BoxComponent( requiresBody = true )
public class Loop extends Component {

	/**
	 * --------------------------------------------------------------------------
	 * Constructor(s)
	 * --------------------------------------------------------------------------
	 */

	public Loop() {
		super();
		declaredAttributes = new Attribute[] {
		    new Attribute( Key.array, "array" ),
		    new Attribute( Key.item, "string", Set.of( Validator.NON_EMPTY ) ),
		    new Attribute( Key.index, "string", Set.of( Validator.NON_EMPTY ) ),
		    // I think dates are allowed here, so numeric may be too strict
		    new Attribute( Key.to, "double", Set.of( Validator.requires( Key.index ) ) ),
		    new Attribute( Key.from, "double" ),
		    new Attribute( Key.file, "string", Set.of( Validator.requires( Key.index ) ) ),
		    new Attribute( Key.list, "string" ),
		    new Attribute( Key.delimiters, "string" ),
		    new Attribute( Key.collection, "collection", Set.of( Validator.requires( Key.item ) ) ),
		    new Attribute( Key.condition, "function" ),
		    new Attribute( Key.query, "any" ),
		    new Attribute( Key.group, "string", Set.of( Validator.NON_EMPTY ) ),
		    new Attribute( Key.groupCaseSensitive, "boolean", false ),
		    new Attribute( Key.startRow, "integer", Set.of( Validator.min( 1 ) ) ),
		    new Attribute( Key.endRow, "integer", Set.of( Validator.min( 1 ) ) ),
		    new Attribute( Key.label, "string", Set.of( Validator.NON_EMPTY ) ),
		    new Attribute( Key.times, "integer", Set.of( Validator.min( 0 ) ) ),
		    new Attribute( Key.step, "number", 1 )

			/**
			 * array
			 * characters
			 */
		};
	}

	/**
	 * Different items are required based on loop type. Items listed as required may not be depending on your loop type. Loop forms: [query] [condition]
	 * [index + from + to ] [index + list] [collection + item ]
	 *
	 *
	 * @param context        The context in which the Component is being invoked
	 * @param attributes     The attributes to the Component
	 * @param body           The body of the Component
	 * @param executionState The execution state of the Component
	 *
	 */
	public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {
		Array				array				= attributes.getAsArray( Key.array );
		String				item				= attributes.getAsString( Key.item );
		String				index				= attributes.getAsString( Key.index );
		Double				to					= attributes.getAsDouble( Key.to );
		Double				from				= attributes.getAsDouble( Key.from );
		String				file				= attributes.getAsString( Key.file );
		String				list				= attributes.getAsString( Key.list );
		String				delimiters			= attributes.getAsString( Key.delimiters );
		Collection<Object>	collection			= ( Collection<Object> ) attributes.get( Key.collection );
		Function			condition			= attributes.getAsFunction( Key.condition );
		String				group				= attributes.getAsString( Key.group );
		Boolean				groupCaseSensitive	= attributes.getAsBoolean( Key.groupCaseSensitive );
		Integer				startRow			= attributes.getAsInteger( Key.startRow );
		Integer				endRow				= attributes.getAsInteger( Key.endRow );
		Object				queryOrName			= attributes.get( Key.query );
		String				label				= attributes.getAsString( Key.label );
		Integer				times				= attributes.getAsInteger( Key.times );
		Number				step				= attributes.getAsNumber( Key.step );

		if ( times != null ) {
			return _invokeTimes( context, times, item, index, body, executionState, label );
		}
		if ( array != null ) {
			return _invokeArray( context, array, item, index, body, executionState, label );
		}
		if ( to != null && from != null ) {
			return _invokeRange( context, from, to, step, index, body, executionState, label );
		}
		if ( file != null ) {
			return _invokeFile( context, file, index, body, executionState, label );
		}
		if ( list != null ) {
			if ( delimiters == null ) {
				delimiters = ListUtil.DEFAULT_DELIMITER;
			}
			return _invokeArray( context, ListUtil.asList( list, delimiters ), item, index, body, executionState, label );
		}
		if ( collection != null ) {
			return _invokeCollection( context, collection, item, body, executionState, label );
		}
		if ( condition != null ) {
			return _invokeCondition( context, condition, body, executionState, label );
		}
		if ( queryOrName != null ) {
			if ( group == null ) {
				return LoopUtil.processQueryLoop( this, context, body, executionState, queryOrName, startRow, endRow, null, label );
			} else {
				return LoopUtil.processQueryLoopGrouped( this, context, body, executionState, queryOrName, group, groupCaseSensitive, startRow, endRow, null,
				    label, executionState );
			}
		}

		// Check and see if we are a nested loop inside a group loop, ignoring ourselves, of course
		IStruct parentComponent = context.findClosestComponent( Key.loop, 1 );
		if ( parentComponent != null ) {
			IStruct parentAttributes = parentComponent.getAsStruct( Key.attributes );
			if ( parentAttributes.get( Key.group ) != null ) {
				LoopUtil.GroupData groupData = ( LoopUtil.GroupData ) parentComponent.get( Key.groupData );
				return LoopUtil.processQueryLoopGrouped(
				    this,
				    context,
				    body,
				    executionState,
				    groupData.getQuery(),
				    group,
				    groupCaseSensitive,
				    groupData.getCurrentRow() + 1,
				    // Inherit the same end row as our parent loop
				    parentComponent.getAsInteger( Key.endRow ),
				    null,
				    label,
				    parentComponent
				);
			}
		}

		throw new BoxRuntimeException( "CFLoop attributes not implemented yet! " + attributes.asString() );
		// return DEFAULT_RETURN;
	}

	private BodyResult _invokeTimes( IBoxContext context, Integer times, String item, String index, ComponentBody body, IStruct executionState, String label ) {
		// If no item is provided, use the index as the item
		if ( index == null && item != null ) {
			index	= item;
			item	= null;
		}
		// loop from 1 to times
		for ( int i = 1; i <= times; i++ ) {
			// Set the index and item variables
			if ( index != null ) {
				ExpressionInterpreter.setVariable( context, index, i );
			}
			// Run the code inside of the output loop
			BodyResult bodyResult = processBody( context, body );
			// IF there was a return statement inside our body, we early exit now
			if ( bodyResult.isEarlyExit() ) {
				if ( bodyResult.isContinue( label ) ) {
					continue;
				} else if ( bodyResult.isBreak( label ) ) {
					break;
				} else {
					return bodyResult;
				}
			}
		}
		return DEFAULT_RETURN;
	}

	private BodyResult _invokeCondition( IBoxContext context, Function condition, ComponentBody body, IStruct executionState, String label ) {
		// Loop over array, executing body every time
		Supplier<Boolean>	cond				= () -> BooleanCaster.cast( context.invokeFunction( condition ) );
		// If our loop is inside a function, we need to use the original context to execute the condition, otherwise
		// arguments and local scope lookups will be incorrect
		IBoxContext			declaringContext	= ( ( Closure ) condition ).getDeclaringContext();
		if ( declaringContext instanceof FunctionBoxContext fbc ) {
			cond = () -> BooleanCaster.cast( condition._invoke( fbc ) );
		}
		while ( cond.get() ) {
			// Run the code inside of the output loop
			BodyResult bodyResult = processBody( context, body );
			// IF there was a return statement inside our body, we early exit now
			if ( bodyResult.isEarlyExit() ) {
				if ( bodyResult.isContinue( label ) ) {
					continue;
				} else if ( bodyResult.isBreak( label ) ) {
					break;
				} else {
					return bodyResult;
				}
			}
		}
		return DEFAULT_RETURN;
	}

	private BodyResult _invokeCollection( IBoxContext context, Collection<Object> collection, String item, ComponentBody body, IStruct executionState,
	    String label ) {
		// Loop over array, executing body every time
		for ( Object key : collection ) {
			ExpressionInterpreter.setVariable( context, item, key );
			// Run the code inside of the output loop
			BodyResult bodyResult = processBody( context, body );
			// IF there was a return statement inside our body, we early exit now
			if ( bodyResult.isEarlyExit() ) {
				if ( bodyResult.isContinue( label ) ) {
					continue;
				} else if ( bodyResult.isBreak( label ) ) {
					break;
				} else {
					return bodyResult;
				}
			}
		}
		return DEFAULT_RETURN;
	}

	private BodyResult _invokeFile( IBoxContext context, String file, String index, ComponentBody body, IStruct executionState, String label ) {
		String		fileContents	= StringCaster.cast( FileSystemUtil.read( file ) );
		// loop over lines
		String[]	lines			= fileContents.split( "\r?\n" );

		// Loop over array, executing body every time
		for ( int i = 0; i < lines.length; i++ ) {
			String thisLine = lines[ i ];
			// Set the index and item variables
			ExpressionInterpreter.setVariable( context, index, thisLine );
			// Run the code inside of the output loop
			BodyResult bodyResult = processBody( context, body );
			// IF there was a return statement inside our body, we early exit now
			if ( bodyResult.isEarlyExit() ) {
				if ( bodyResult.isContinue( label ) ) {
					continue;
				} else if ( bodyResult.isBreak( label ) ) {
					break;
				} else {
					return bodyResult;
				}
			}
		}
		return DEFAULT_RETURN;
	}

	private BodyResult _invokeRange( IBoxContext context, Double from, Double to, Number step, String index, ComponentBody body, IStruct executionState,
	    String label ) {
		double											toD			= to.doubleValue();
		double											fromD		= from.doubleValue();
		double											stepD		= step.doubleValue();
		// If step is positive, we loop until we're greater than or equal to the "to" value, otherwise we loop until we're less than or equal to the "to" value
		java.util.function.Function<Double, Boolean>	condition	= stepD > 0 ? i -> i <= toD : i -> i >= toD;
		// Prevent infinite loops
		if ( stepD == 0 ) {
			return DEFAULT_RETURN;
		}
		// Loop over array, executing body every time
		for ( double i = fromD; condition.apply( i ); i = i + stepD ) {
			// Set the index and item variables
			ExpressionInterpreter.setVariable( context, index, i );
			// Run the code inside of the output loop
			BodyResult bodyResult = processBody( context, body );
			// IF there was a return statement inside our body, we early exit now
			if ( bodyResult.isEarlyExit() ) {
				if ( bodyResult.isContinue( label ) ) {
					continue;
				} else if ( bodyResult.isBreak( label ) ) {
					break;
				} else {
					return bodyResult;
				}
			}
		}
		return DEFAULT_RETURN;
	}

	/**
	 * Loop over array with optional item and index
	 *
	 * @param context        The context in which the Component is being invoked
	 * @param array          The array to loop over
	 * @param item           The name of the variable to hold the current item
	 * @param index          The name of the variable to hold the current index
	 * @param body           The body of the Component
	 * @param executionState The execution state of the Component
	 *
	 * @return The result of the loop body execution
	 */
	private BodyResult _invokeArray( IBoxContext context, Array array, String item, String index, ComponentBody body, IStruct executionState, String label ) {
		// If no item is provided, use the index as the item
		if ( item == null && index != null ) {
			item	= index;
			index	= null;
		}
		// Loop over array, executing body every time
		for ( int i = 0; i < array.size(); i++ ) {
			// Set the index and item variables
			if ( index != null ) {
				ExpressionInterpreter.setVariable( context, index, i + 1 );
			}
			if ( item != null ) {
				ExpressionInterpreter.setVariable( context, item, array.get( i ) );
			}
			// Run the code inside of the output loop
			BodyResult bodyResult = processBody( context, body );
			// IF there was a return statement inside our body, we early exit now
			if ( bodyResult.isEarlyExit() ) {
				if ( bodyResult.isContinue( label ) ) {
					continue;
				} else if ( bodyResult.isBreak( label ) ) {
					break;
				} else {
					return bodyResult;
				}
			}
		}
		return DEFAULT_RETURN;
	}

}
