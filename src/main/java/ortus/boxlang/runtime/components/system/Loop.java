
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

/**
 * The Loop component provides various iteration mechanisms for BoxLang applications.
 * This component is the BoxLang equivalent of ColdFusion's cfloop tag and supports
 * multiple loop types including query loops, array loops, list loops, collection loops,
 * conditional loops, numeric range loops, file iteration, and grouped query loops.
 *
 * @author BoxLang Development Team
 *
 * @see LoopUtil for the underlying loop processing logic
 */
@BoxComponent( requiresBody = true )
public class Loop extends Component {

	/**
	 * --------------------------------------------------------------------------
	 * Constructor(s)
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Constructs a new Loop component with all supported attributes configured.
	 *
	 * <p>
	 * The Loop component supports multiple, mutually exclusive loop types.
	 * Different combinations of attributes determine which loop type is executed:
	 *
	 * <ul>
	 * <li><strong>Query Loop:</strong> Requires <code>query</code> attribute</li>
	 * <li><strong>Grouped Query Loop:</strong> Requires <code>query</code> and <code>group</code> attributes</li>
	 * <li><strong>Array Loop:</strong> Requires <code>array</code> attribute</li>
	 * <li><strong>List Loop:</strong> Requires <code>list</code> attribute</li>
	 * <li><strong>Collection Loop:</strong> Requires <code>collection</code> and <code>item</code> attributes</li>
	 * <li><strong>Numeric Range Loop:</strong> Requires <code>index</code>, <code>from</code>, and <code>to</code> attributes</li>
	 * <li><strong>Conditional Loop:</strong> Requires <code>condition</code> attribute</li>
	 * <li><strong>Times Loop:</strong> Requires <code>times</code> attribute</li>
	 * <li><strong>File Loop:</strong> Requires <code>file</code> and <code>index</code> attributes</li>
	 * </ul>
	 */
	public Loop() {
		super();
		declaredAttributes = new Attribute[] {
		    // Array loop attributes
		    new Attribute( Key.array, "array" ),

		    // Variable name attributes (used across multiple loop types)
		    new Attribute( Key.item, "string", Set.of( Validator.NON_EMPTY ) ),
		    new Attribute( Key.index, "string", Set.of( Validator.NON_EMPTY ) ),

		    // Numeric range loop attributes
		    new Attribute( Key.to, "double", Set.of( Validator.requires( Key.index ) ) ),
		    new Attribute( Key.from, "double" ),
		    new Attribute( Key.step, "number", 1 ),

		    // File loop attributes
		    new Attribute( Key.file, "string", Set.of( Validator.requires( Key.index ) ) ),

		    // List loop attributes
		    new Attribute( Key.list, "string" ),
		    new Attribute( Key.delimiters, "string" ),

		    // Collection loop attributes
		    new Attribute( Key.collection, "collection", Set.of( Validator.requires( Key.item ) ) ),

		    // Conditional loop attributes
		    new Attribute( Key.condition, "function" ),

		    // Query loop attributes
		    new Attribute( Key.query, "any" ),
		    new Attribute( Key.group, "string", Set.of( Validator.NON_EMPTY ) ),
		    new Attribute( Key.groupCaseSensitive, "boolean", false ),
		    new Attribute( Key.startRow, "integer", Set.of( Validator.min( 1 ) ) ),
		    new Attribute( Key.endRow, "integer", Set.of( Validator.min( 1 ) ) ),

		    // Loop control attributes
		    new Attribute( Key.label, "string", Set.of( Validator.NON_EMPTY ) ),

		    // Times loop attributes
		    new Attribute( Key.times, "integer", Set.of( Validator.min( 0 ) ) )

			/**
			 * Future attributes to be implemented:
			 * - characters: Number of characters to read per iteration for file loops
			 */
		};
	}

	/**
	 * Executes the appropriate loop type based on the provided attributes.
	 *
	 * <p>
	 * This method serves as the main entry point for all loop operations, analyzing
	 * the provided attributes to determine which specific loop type to execute and
	 * delegating to the appropriate helper method.
	 * </p>
	 *
	 * <h2>Supported Loop Types</h2>
	 * <ul>
	 * <li><strong>Query Loop:</strong> Iterate over query recordsets with optional row range constraints</li>
	 * <li><strong>Grouped Query Loop:</strong> Process query data with grouping on specified columns</li>
	 * <li><strong>Array Loop:</strong> Iterate over array elements with optional index tracking</li>
	 * <li><strong>List Loop:</strong> Process delimited lists with customizable delimiters</li>
	 * <li><strong>Collection Loop:</strong> Iterate over Java collections and structures</li>
	 * <li><strong>Numeric Range Loop:</strong> Loop through numeric ranges with configurable step values</li>
	 * <li><strong>Conditional Loop:</strong> Continue looping while a condition remains true</li>
	 * <li><strong>Times Loop:</strong> Execute a specific number of iterations</li>
	 * <li><strong>File Loop:</strong> Read text files line by line</li>
	 * </ul>
	 *
	 * <h2>Loop Control</h2>
	 * <p>
	 * All loop types support standard loop control statements:
	 * </p>
	 * <ul>
	 * <li><code>break</code> - Exit the loop immediately</li>
	 * <li><code>continue</code> - Skip to the next iteration</li>
	 * <li><code>return</code> - Exit from the containing function/component</li>
	 * </ul>
	 *
	 * <p>
	 * Loop control statements can be labeled to target specific nested loops:
	 * </p>
	 *
	 * <pre>
	 * &lt;bx:loop label="outer" array="#outerArray#" item="outerItem"&gt;
	 *   &lt;bx:loop array="#innerArray#" item="innerItem"&gt;
	 *     &lt;bx:if condition="#someCondition#"&gt;
	 *       &lt;bx:break label="outer" /&gt; // Breaks out of outer loop
	 *     &lt;/bx:if&gt;
	 *   &lt;/bx:loop&gt;
	 * &lt;/bx:loop&gt;
	 * </pre>
	 *
	 * <h2>Usage Examples</h2>
	 *
	 * <h3>Query Loop</h3>
	 *
	 * <pre>
	 * // Simple query loop
	 * &lt;bx:loop query="#myQuery#"&gt;
	 *   &lt;bx:output&gt;#myQuery.name#: #myQuery.email#&lt;/bx:output&gt;
	 * &lt;/bx:loop&gt;
	 *
	 * // Query loop with row range
	 * &lt;bx:loop query="#myQuery#" startRow="5" endRow="15"&gt;
	 *   &lt;bx:output&gt;Row #currentRow#: #myQuery.name#&lt;/bx:output&gt;
	 * &lt;/bx:loop&gt;
	 * </pre>
	 *
	 * <h3>Grouped Query Loop</h3>
	 *
	 * <pre>
	 * // Group by department
	 * &lt;bx:loop query="#employeeQuery#" group="department"&gt;
	 *   &lt;h3&gt;Department: #department#&lt;/h3&gt;
	 *   &lt;bx:loop&gt; // Inner loop for employees in this department
	 *     &lt;p&gt;#name# - #position#&lt;/p&gt;
	 *   &lt;/bx:loop&gt;
	 * &lt;/bx:loop&gt;
	 *
	 * // Nested grouping (department &gt; manager &gt; employee)
	 * &lt;bx:loop query="#employeeQuery#" group="department"&gt;
	 *   &lt;h2&gt;Department: #department#&lt;/h2&gt;
	 *   &lt;bx:loop group="manager"&gt;
	 *     &lt;h3&gt;Manager: #manager#&lt;/h3&gt;
	 *     &lt;bx:loop&gt; // Individual employees
	 *       &lt;p&gt;#name# - #position#&lt;/p&gt;
	 *     &lt;/bx:loop&gt;
	 *   &lt;/bx:loop&gt;
	 * &lt;/bx:loop&gt;
	 * </pre>
	 *
	 * <h3>Array Loop</h3>
	 *
	 * <pre>
	 * // Loop with item only
	 * &lt;bx:loop array="#myArray#" item="currentItem"&gt;
	 *   &lt;bx:output&gt;#currentItem#&lt;/bx:output&gt;
	 * &lt;/bx:loop&gt;
	 *
	 * // Loop with both item and index
	 * &lt;bx:loop array="#myArray#" item="currentItem" index="currentIndex"&gt;
	 *   &lt;bx:output&gt;Item #currentIndex#: #currentItem#&lt;/bx:output&gt;
	 * &lt;/bx:loop&gt;
	 * </pre>
	 *
	 * <h3>List Loop</h3>
	 *
	 * <pre>
	 * // Default comma delimiter
	 * &lt;bx:loop list="apple,banana,cherry" item="fruit"&gt;
	 *   &lt;bx:output&gt;#fruit#&lt;/bx:output&gt;
	 * &lt;/bx:loop&gt;
	 *
	 * // Custom delimiter
	 * &lt;bx:loop list="apple|banana|cherry" item="fruit" delimiters="|"&gt;
	 *   &lt;bx:output&gt;#fruit#&lt;/bx:output&gt;
	 * &lt;/bx:loop&gt;
	 * </pre>
	 *
	 * <h3>Numeric Range Loop</h3>
	 *
	 * <pre>
	 * // Basic range
	 * &lt;bx:loop from="1" to="10" index="i"&gt;
	 *   &lt;bx:output&gt;#i#&lt;/bx:output&gt;
	 * &lt;/bx:loop&gt;
	 *
	 * // With step
	 * &lt;bx:loop from="0" to="100" step="10" index="i"&gt;
	 *   &lt;bx:output&gt;#i#&lt;/bx:output&gt; // Outputs: 0, 10, 20, 30...
	 * &lt;/bx:loop&gt;
	 *
	 * // Backward iteration
	 * &lt;bx:loop from="10" to="1" step="-1" index="i"&gt;
	 *   &lt;bx:output&gt;#i#&lt;/bx:output&gt; // Outputs: 10, 9, 8, 7...
	 * &lt;/bx:loop&gt;
	 * </pre>
	 *
	 * <h3>Collection Loop</h3>
	 *
	 * <pre>
	 * // Structure iteration
	 * &lt;bx:loop collection="#myStruct#" item="key"&gt;
	 *   &lt;bx:output&gt;#key#: #myStruct[key]#&lt;/bx:output&gt;
	 * &lt;/bx:loop&gt;
	 * </pre>
	 *
	 * <h3>Conditional Loop</h3>
	 *
	 * <pre>
	 * &lt;bx:loop condition="#hasMoreData()#"&gt;
	 *   &lt;bx:set processNextBatch() /&gt;
	 * &lt;/bx:loop&gt;
	 * </pre>
	 *
	 * <h3>Times Loop</h3>
	 *
	 * <pre>
	 * &lt;bx:loop times="5" index="i"&gt;
	 *   &lt;bx:output&gt;Iteration #i#&lt;/bx:output&gt;
	 * &lt;/bx:loop&gt;
	 * </pre>
	 *
	 * <h3>File Loop</h3>
	 *
	 * <pre>
	 * &lt;bx:loop file="/path/to/file.txt" index="line"&gt;
	 *   &lt;bx:output&gt;#line#&lt;/bx:output&gt;
	 * &lt;/bx:loop&gt;
	 * </pre>
	 *
	 * @param context        The context in which the Component is being invoked
	 * @param attributes     The attributes to the Component
	 * @param body           The body of the Component
	 * @param executionState The execution state of the Component
	 *
	 * @return A BodyResult indicating how the loop terminated (normal completion, break, continue, or return)
	 *
	 * @throws BoxRuntimeException if required attributes are missing or invalid for the detected loop type
	 *
	 * @attribute.array An Array object to iterate over. When specified, the loop will process each element
	 *                  in the array. Can be used with <code>item</code> and/or <code>index</code> attributes.
	 *                  <br>
	 *                  <strong>Example:</strong> <code>&lt;bx:loop array="#myArray#" item="element"&gt;</code>
	 *
	 * @attribute.item Variable name to hold the current item value during iteration. Behavior varies by loop type:
	 *                 <ul>
	 *                 <li><strong>Array loops:</strong> Contains the current array element</li>
	 *                 <li><strong>List loops:</strong> Contains the current list item</li>
	 *                 <li><strong>Collection loops:</strong> Contains the current collection key</li>
	 *                 <li><strong>Times loops:</strong> If no index specified, contains the current iteration number</li>
	 *                 </ul>
	 *                 <br>
	 *                 <strong>Example:</strong> <code>&lt;bx:loop array="#colors#" item="color"&gt;</code>
	 *
	 * @attribute.index Variable name to hold the current index/position during iteration. Behavior varies by loop type:
	 *                  <ul>
	 *                  <li><strong>Array loops:</strong> Contains the 1-based array index</li>
	 *                  <li><strong>Numeric range loops:</strong> Contains the current numeric value (required)</li>
	 *                  <li><strong>File loops:</strong> Contains the current line content (required)</li>
	 *                  <li><strong>Times loops:</strong> Contains the current iteration number</li>
	 *                  </ul>
	 *                  <br>
	 *                  <strong>Example:</strong> <code>&lt;bx:loop from="1" to="10" index="i"&gt;</code>
	 *
	 * @attribute.to End value for numeric range loops. The loop continues while the index value
	 *               has not exceeded this value (considering step direction). Required when using
	 *               numeric range loops with <code>from</code> and <code>index</code>.
	 *               <br>
	 *               <strong>Example:</strong> <code>&lt;bx:loop from="1" to="100" step="5" index="i"&gt;</code>
	 *
	 * @attribute.from Starting value for numeric range loops. Defaults to 0 if not specified.
	 *                 Used in conjunction with <code>to</code> and <code>index</code> for numeric iteration.
	 *                 <br>
	 *                 <strong>Example:</strong> <code>&lt;bx:loop from="10" to="1" step="-1" index="i"&gt;</code>
	 *
	 * @attribute.step Increment/decrement value for numeric range loops. Defaults to 1.
	 *                 Positive values increment the index, negative values decrement.
	 *                 Zero values are ignored to prevent infinite loops.
	 *                 <br>
	 *                 <strong>Example:</strong> <code>&lt;bx:loop from="0" to="20" step="2" index="even"&gt;</code>
	 *
	 * @attribute.file Absolute path to a text file to read line by line. Each iteration provides
	 *                 one line of the file content in the <code>index</code> variable. The file
	 *                 is automatically closed when the loop completes. Requires <code>index</code> attribute.
	 *                 <br>
	 *                 <strong>Example:</strong> <code>&lt;bx:loop file="/path/to/data.txt" index="line"&gt;</code>
	 *
	 * @attribute.list A delimited string to process item by item. Each item becomes available
	 *                 through the <code>item</code> or <code>index</code> variable. Use with
	 *                 <code>delimiters</code> to specify custom separators.
	 *                 <br>
	 *                 <strong>Example:</strong> <code>&lt;bx:loop list="red,green,blue" item="color"&gt;</code>
	 *
	 * @attribute.delimiters Characters that separate items in the <code>list</code> attribute.
	 *                       Defaults to comma (",") if not specified. Multiple delimiter characters
	 *                       can be specified.
	 *                       <br>
	 *                       <strong>Example:</strong> <code>&lt;bx:loop list="a|b|c" delimiters="|" item="letter"&gt;</code>
	 *
	 * @attribute.collection A Java Collection object (including BoxLang Structs) to iterate over.
	 *                       For Structs, each iteration provides a key. Requires <code>item</code> attribute
	 *                       to specify the variable name for the current key.
	 *                       <br>
	 *                       <strong>Example:</strong> <code>&lt;bx:loop collection="#myStruct#" item="key"&gt;</code>
	 *
	 * @attribute.condition A function/closure that returns a boolean value. The loop continues
	 *                      while this condition evaluates to true. The condition is evaluated
	 *                      before each iteration.
	 *                      <br>
	 *                      <strong>Example:</strong> <code>&lt;bx:loop condition="#() => hasMoreData()#"&gt;</code>
	 *
	 * @attribute.query A Query object or variable name containing a query to iterate over.
	 *                  Each iteration makes one row of the query available. Can be combined
	 *                  with <code>group</code> for grouped processing, or <code>startRow</code>/
	 *                  <code>endRow</code> for range constraints.
	 *                  <br>
	 *                  <strong>Example:</strong> <code>&lt;bx:loop query="#employeeQuery#"&gt;</code>
	 *
	 * @attribute.group Comma-separated list of query column names to group by. When specified,
	 *                  the loop processes data in groups, executing the body once per group
	 *                  change rather than once per row. Enables nested looping for hierarchical
	 *                  data processing. Requires <code>query</code> attribute.
	 *                  <br>
	 *                  <strong>Example:</strong> <code>&lt;bx:loop query="#data#" group="department,manager"&gt;</code>
	 *
	 * @attribute.groupCaseSensitive Boolean flag controlling whether group comparisons are case-sensitive.
	 *                               Defaults to false (case-insensitive). Only meaningful when
	 *                               <code>group</code> attribute is specified.
	 *                               <br>
	 *                               <strong>Example:</strong> <code>&lt;bx:loop query="#data#" group="name" groupCaseSensitive="true"&gt;</code>
	 *
	 * @attribute.startRow 1-based starting row number for query loops. Only rows from this
	 *                     position onward will be processed. Must be 1 or greater.
	 *                     <br>
	 *                     <strong>Example:</strong> <code>&lt;bx:loop query="#data#" startRow="10" endRow="20"&gt;</code>
	 *
	 * @attribute.endRow 1-based ending row number for query loops. Processing stops after
	 *                   this row. Must be 1 or greater and typically greater than <code>startRow</code>.
	 *                   <br>
	 *                   <strong>Example:</strong> <code>&lt;bx:loop query="#data#" startRow="1" endRow="50"&gt;</code>
	 *
	 * @attribute.label Optional label for the loop, used with break and continue statements
	 *                  to control nested loop execution. Allows targeting specific loops
	 *                  in complex nested structures.
	 *                  <br>
	 *                  <strong>Example:</strong> <code>&lt;bx:loop label="outerLoop" array="#data#"&gt;...&lt;bx:break label="outerLoop"&gt;</code>
	 *
	 * @attribute.times Number of iterations to perform. Creates a simple counting loop
	 *                  from 1 to the specified number. Can be used with <code>item</code>
	 *                  or <code>index</code> to access the current iteration number.
	 *                  <br>
	 *                  <strong>Example:</strong> <code>&lt;bx:loop times="5" index="i"&gt;</code>
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

	/**
	 * Executes a times-based loop for a specified number of iterations.
	 *
	 * <p>
	 * This method implements the "times" loop functionality, executing the body
	 * a specific number of times with an optional counter variable.
	 * </p>
	 *
	 * @param context        The current execution context
	 * @param times          Number of iterations to perform (must be non-negative)
	 * @param item           Variable name for the current iteration number (optional, falls back to index)
	 * @param index          Variable name for the current iteration number (optional)
	 * @param body           The component body to execute for each iteration
	 * @param executionState The execution state of the component
	 * @param label          Optional label for loop control statements
	 *
	 * @return A BodyResult indicating how the loop terminated
	 */
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

	/**
	 * Executes a conditional loop that continues while a condition remains true.
	 *
	 * <p>
	 * This method implements while-loop functionality, repeatedly executing the body
	 * as long as the provided condition function returns true. The condition is
	 * evaluated before each iteration.
	 * </p>
	 *
	 * <p>
	 * Special handling is provided for closures declared within function contexts
	 * to ensure proper scope resolution for arguments and local variables.
	 * </p>
	 *
	 * @param context        The current execution context
	 * @param condition      A function/closure that returns a boolean value
	 * @param body           The component body to execute for each iteration
	 * @param executionState The execution state of the component
	 * @param label          Optional label for loop control statements
	 *
	 * @return A BodyResult indicating how the loop terminated
	 */
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

	/**
	 * Executes a collection loop, iterating over Java Collection objects.
	 *
	 * <p>
	 * This method processes Java Collection objects (including BoxLang Structs),
	 * making each key/element available through the specified item variable.
	 * </p>
	 *
	 * @param context        The current execution context
	 * @param collection     The Java Collection to iterate over
	 * @param item           Variable name to hold the current collection key/element
	 * @param body           The component body to execute for each iteration
	 * @param executionState The execution state of the component
	 * @param label          Optional label for loop control statements
	 *
	 * @return A BodyResult indicating how the loop terminated
	 */
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

	/**
	 * Executes a file loop, reading a text file line by line.
	 *
	 * <p>
	 * This method reads the specified text file and executes the body once
	 * for each line, making the line content available through the index variable.
	 * The file is automatically handled and closed after processing.
	 * </p>
	 *
	 * <p>
	 * Line endings are normalized to handle different platforms (Windows \\r\\n,
	 * Unix \\n, classic Mac \\r).
	 * </p>
	 *
	 * @param context        The current execution context
	 * @param file           Absolute path to the text file to read
	 * @param index          Variable name to hold the current line content
	 * @param body           The component body to execute for each line
	 * @param executionState The execution state of the component
	 * @param label          Optional label for loop control statements
	 *
	 * @return A BodyResult indicating how the loop terminated
	 *
	 * @throws RuntimeException if the file cannot be read or does not exist
	 */
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

	/**
	 * Executes a numeric range loop with configurable step values.
	 *
	 * <p>
	 * This method implements numeric iteration from a starting value to an ending
	 * value with a configurable step increment/decrement. The loop direction is
	 * automatically determined by the step value sign.
	 * </p>
	 *
	 * <p>
	 * Loop termination logic:
	 * <ul>
	 * <li>Positive step: continues while index ≤ to</li>
	 * <li>Negative step: continues while index ≥ to</li>
	 * <li>Zero step: terminates immediately to prevent infinite loops</li>
	 * </ul>
	 *
	 * @param context        The current execution context
	 * @param from           Starting numeric value for the loop
	 * @param to             Ending numeric value for the loop
	 * @param step           Increment/decrement value (positive for forward, negative for backward)
	 * @param index          Variable name to hold the current numeric value
	 * @param body           The component body to execute for each iteration
	 * @param executionState The execution state of the component
	 * @param label          Optional label for loop control statements
	 *
	 * @return A BodyResult indicating how the loop terminated
	 */
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
	 * Executes an array loop with optional item and index variable assignment.
	 *
	 * <p>
	 * This method iterates through an Array object, making each element and/or
	 * its position available through specified variables. The method handles
	 * flexible variable assignment where either item or index can be used alone,
	 * or both can be used together.
	 * </p>
	 *
	 * <p>
	 * Variable assignment logic:
	 * <ul>
	 * <li>If only index is specified: index gets the array element value</li>
	 * <li>If only item is specified: item gets the array element value</li>
	 * <li>If both are specified: item gets element value, index gets 1-based position</li>
	 * </ul>
	 *
	 * @param context        The current execution context
	 * @param array          The Array object to iterate over
	 * @param item           Variable name to hold the current array element (optional)
	 * @param index          Variable name to hold the current 1-based position or element value (optional)
	 * @param body           The component body to execute for each iteration
	 * @param executionState The execution state of the component
	 * @param label          Optional label for loop control statements
	 *
	 * @return A BodyResult indicating how the loop terminated
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
