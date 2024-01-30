
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
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.ListUtil;

public class ListRemoveDuplicatesTest {

	static BoxRuntime	instance;
	static IBoxContext	context;
	static IScope		variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance	= BoxRuntime.getInstance( true );
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@AfterAll
	public static void teardown() {
		instance.shutdown();
	}

	@BeforeEach
	public void setupEach() {
		variables.clear();
	}

	@DisplayName( "Can deduplicate with the default settings" )
	@Test
	public void testMatchNumber() {
		instance.executeSource(
		    """
		        nums = "1,2,2,4,4";
		        result = listRemoveDuplicates( nums );
		    """,
		    context );
		Array list = ListUtil.asList( StringCaster.cast( variables.get( result ) ), "," );
		assertThat( list.size() ).isEqualTo( 3 );
	}

	@DisplayName( "Can deduplicate with delimiters" )
	@Test
	public void testDelim() {
		instance.executeSource(
		    """
		        nums = "1|2|2|4|4";
		        result = listRemoveDuplicates( nums, "|" );
		    """,
		    context );
		Array list = ListUtil.asList( StringCaster.cast( variables.get( result ) ), "|" );
		assertThat( list.size() ).isEqualTo( 3 );
	}

	@DisplayName( "Can deduplicate case sensitively" )
	@Test
	public void testCaseSenstive() {
		instance.executeSource(
		    """
		        nums = "Brad,BRAD,Luis,Luis,LUIS";
		        result = listRemoveDuplicates( list=nums );
		    """,
		    context );
		Array list = ListUtil.asList( StringCaster.cast( variables.get( result ) ), "," );
		assertThat( list.size() ).isEqualTo( 4 );
	}

	@DisplayName( "Can deduplicate case insensitively" )
	@Test
	public void testCaseInsenstive() {
		instance.executeSource(
		    """
		        nums = "Brad,BRAD,Luis,Luis,LUIS,GrAnT,Grant";
		        result = listRemoveDuplicates( list=nums, ignoreCase=true );
		    """,
		    context );
		Array list = ListUtil.asList( StringCaster.cast( variables.get( result ) ), "," );
		System.out.println( list );
		assertThat( list.size() ).isEqualTo( 3 );
		assertEquals( list.get( 0 ), "Brad" );
		assertEquals( list.get( 1 ), "Luis" );
		assertEquals( list.get( 2 ), "GrAnT" );
	}

	@DisplayName( "Can deduplicate case insensitively" )
	@Test
	public void testMemberFunction() {
		instance.executeSource(
		    """
		        nums = "Brad,BRAD,Luis,Luis,LUIS,GrAnT,Grant";
		        result = nums.listRemoveDuplicates( ignoreCase=true );
		    """,
		    context );
		Array list = ListUtil.asList( StringCaster.cast( variables.get( result ) ), "," );
		System.out.println( list );
		assertThat( list.size() ).isEqualTo( 3 );
		assertEquals( list.get( 0 ), "Brad" );
		assertEquals( list.get( 1 ), "Luis" );
		assertEquals( list.get( 2 ), "GrAnT" );
	}

}
