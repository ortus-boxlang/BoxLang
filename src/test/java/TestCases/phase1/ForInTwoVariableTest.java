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
package TestCases.phase1;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class ForInTwoVariableTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "for-in loop with two variables: array with index" )
	@Test
	public void testForInLoopArrayWithIndex() {
		instance.executeSource(
		    """
		    arr = ["a", "b", "c"];
		    result = [];
		    for(item, idx in arr) {
		        result.append(item & idx);
		    }
		    """,
		    context );
		assertThat( variables.getAsArray( result ) )
		    .containsExactly( "a1", "b2", "c3" ).inOrder();
	}

	@DisplayName( "for-in loop with two variables: array with index" )
	@Test
	public void testForInLoopArrayWithIndexParens() {
		instance.executeSource(
		    """
		    arr = ["x", "y", "z"];
		    result = [];
		    for(item, idx in arr) {
		        result.append(idx & ":" & item);
		    }
		    """,
		    context );
		assertThat( variables.getAsArray( result ) )
		    .containsExactly( "1:x", "2:y", "3:z" ).inOrder();
	}

	@DisplayName( "for-in loop with two variables: struct with value" )
	@Test
	public void testForInLoopStructWithValue() {
		instance.executeSource(
		    """
		    myStruct = {a:1, b:2, c:3};
		    result = [];
		    for(k, v in myStruct) {
		        result.append(k & "=" & v);
		    }
		    """,
		    context );
		assertThat( variables.getAsArray( result ) )
		    .containsExactly( "a=1", "b=2", "c=3" );
	}

	@DisplayName( "for-in loop with two variables: struct accessing only value" )
	@Test
	public void testForInLoopStructAccessingOnlyValue() {
		instance.executeSource(
		    """
		    myStruct = {x:10, y:20};
		    result = [];
		    for(k, v in myStruct) {
		        result.append(v);
		    }
		    """,
		    context );
		assertThat( variables.getAsArray( result ) )
		    .containsExactly( 10, 20 );
	}

	@DisplayName( "for-in loop with two variables: query with index" )
	@Test
	public void testForInLoopQueryWithIndex() {
		instance.executeSource(
		    """
		    query = queryNew("name,age", "varchar,integer",
		                    [["Brad", 40], ["Luis", 45]]);
		    result = [];
		    for(row, idx in query) {
		        result.append(idx & ":" & row.name);
		    }
		    """,
		    context );
		assertThat( variables.getAsArray( result ) )
		    .containsExactly( "1:Brad", "2:Luis" ).inOrder();
	}

	@DisplayName( "for-in loop with two variables: with var keyword" )
	@Test
	@Disabled( "Variable scoping with var keyword for two variables needs investigation" )
	public void testForInLoopTwoVariablesWithVar() {
		instance.executeSource(
		    """
		    arr = [10, 20, 30];
		    result = "";
		    for(var item, idx in arr) {
		        result &= item & ",";
		    }
		    // Variables should not exist outside loop
		    result &= isDefined("item") ? "has_item" : "no_item";
		    """,
		    context );
		assertThat( variables.getAsString( result ) ).isEqualTo( "10,20,30,no_item" );
	}

	@DisplayName( "for-in loop with two variables: nested loops" )
	@Test
	public void testForInLoopNestedTwoVariables() {
		instance.executeSource(
		    """
		    outer = ["a", "b"];
		    inner = {x:1, y:2};
		    result = [];
		    for(item, idx in outer) {
		        for(key, val in inner) {
		            result.append(item & idx & key & val);
		        }
		    }
		    """,
		    context );
		assertThat( variables.getAsArray( result ).size() ).isEqualTo( 4 );
		// Should have combinations: a1x1, a1y2, b2x1, b2y2
		assertThat( variables.getAsArray( result ) ).containsExactly( "a1x1", "a1y2", "b2x1", "b2y2" );
	}

	@DisplayName( "for-in loop with two variables: single statement without braces" )
	@Test
	public void testForInLoopTwoVariablesSingleStatement() {
		instance.executeSource(
		    """
		    arr = [1, 2, 3];
		    result = [];
		    for(item, idx in arr)
		        result.append(item + idx);
		    """,
		    context );
		assertThat( variables.getAsArray( result ) )
		    .containsExactly( 2, 4, 6 ).inOrder();
	}

	@DisplayName( "for-in loop: single variable still works (backward compatibility)" )
	@Test
	public void testForInLoopSingleVariableBackwardCompat() {
		instance.executeSource(
		    """
		    arr = [5, 10, 15];
		    result = [];
		    for(item in arr) {
		        result.append(item);
		    }
		    """,
		    context );
		assertThat( variables.getAsArray( result ) )
		    .containsExactly( 5, 10, 15 ).inOrder();
	}

	@DisplayName( "for-in loop with two variables: array index correct sequence" )
	@Test
	public void testForInLoopArrayIndexSequence() {
		instance.executeSource(
		    """
		    arr = [100, 200, 300, 400];
		    indices = [];
		    for(item, idx in arr) {
		        indices.append(idx);
		    }
		    """,
		    context );
		assertThat( variables.getAsArray( Key.of( "indices" ) ) )
		    .containsExactly( 1, 2, 3, 4 ).inOrder();
	}

	@DisplayName( "for-in loop with two variables: struct access both key and value" )
	@Test
	public void testForInLoopStructKeyAndValue() {
		instance.executeSource(
		    """
		    data = {name:"BoxLang", version:"1.10", year:2024};
		    keys = [];
		    vals = [];
		    for(k, v in data) {
		        keys.append(k);
		        vals.append(v);
		    }
		    """,
		    context );
		assertThat( variables.getAsArray( Key.of( "keys" ) ) )
		    .containsExactly( "name", "version", "year" );
		assertThat( variables.getAsArray( Key.of( "vals" ) ) )
		    .containsExactly( "BoxLang", "1.10", 2024 );
	}

	@DisplayName( "for-in loop with two variables: query columns accessible" )
	@Test
	public void testForInLoopQueryColumnsAccessible() {
		instance.executeSource(
		    """
		    qry = queryNew("id,name", "integer,varchar", [[1,"A"],[2,"B"],[3,"C"]]);
		    result = [];
		    for(row, idx in qry) {
		        result.append("Row " & idx & ": " & row.id & "-" & row.name);
		    }
		    """,
		    context );
		assertThat( variables.getAsArray( result ) )
		    .containsExactly( "Row 1: 1-A", "Row 2: 2-B", "Row 3: 3-C" ).inOrder();
	}
}
