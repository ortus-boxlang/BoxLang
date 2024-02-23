
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

package ortus.boxlang.runtime.bifs.global.list;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.util.ListUtil;

public class ListPrependTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@AfterAll
	public static void teardown() {

	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "Can Prepend a number in a string list" )
	@Test
	public void testPrependNumber() {
		instance.executeSource(
		    """
		        nums = "1,2,3,4,5";
		        result = listPrepend( nums, 6 );
		    """,
		    context );
		Array updated = ListUtil.asList( variables.getAsString( result ), ListUtil.DEFAULT_DELIMITER );
		assertThat( updated.size() ).isEqualTo( 6 );
		assertEquals( updated.getAt( 1 ), "6" );
	}

	@DisplayName( "It can Prepend a string value" )
	@Test
	public void testPrependString() {
		instance.executeSource(
		    """
		        nums = "1,2,3,4,5";
		        result = listPrepend( nums, "6" );
		    """,
		    context );
		Array updated = ListUtil.asList( variables.getAsString( result ), ListUtil.DEFAULT_DELIMITER );
		assertThat( updated.size() ).isEqualTo( 6 );
		assertEquals( updated.getAt( 1 ), "6" );
	}

	@DisplayName( "Can Prepend using the member function" )
	@Test
	public void testPrependMember() {
		instance.executeSource(
		    """
		        nums = "1,2,3,4,5";
		        result = nums.listPrepend( 6 );
		    """,
		    context );
		Array updated = ListUtil.asList( variables.getAsString( result ), ListUtil.DEFAULT_DELIMITER );
		assertThat( updated.size() ).isEqualTo( 6 );
		assertEquals( updated.getAt( 1 ), "6" );
	}

	@DisplayName( "Can Prepend using alternate delimiters and options" )
	@Test
	public void testPrependArguments() {
		instance.executeSource(
		    """
		        nums = "1|2|3|4|5";
		        result = nums.listPrepend( 6, "|" );
		    """,
		    context );
		Array updated = ListUtil.asList( variables.getAsString( result ), "|" );
		assertThat( updated.size() ).isEqualTo( 6 );
		assertEquals( updated.getAt( 1 ), "6" );
		instance.executeSource(
		    """
		        nums = "1|2||3|4|5";
		        result = nums.listPrepend( 6, "|", true );
		    """,
		    context );
		updated = ListUtil.asList( variables.getAsString( result ), "|", true, true );
		assertThat( updated.size() ).isEqualTo( 7 );
		assertEquals( updated.getAt( 1 ), "6" );
	}

}
