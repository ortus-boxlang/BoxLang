
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

public class ListQualifyTest {

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

	@DisplayName( "It tests the BIF ListQualify with defaults" )
	@Test
	public void testBif() {
		instance.executeSource(
		    """
		    myList="a,b,c,d,e";
		    result = ListQualify( myList, "@" );
		    """,
		    context );

		assertEquals( variables.getAsString( result ), "@a@,@b@,@c@,@d@,@e@" );
	}

	@DisplayName( "It tests the BIF ListQualify with only char elements selected" )
	@Test
	public void testBifElements() {
		instance.executeSource(
		    """
		    myList="a,b,c,d,2,e";
		    result = ListQualify( myList, "@", ",", "char" );
		    """,
		    context );

		assertEquals( variables.getAsString( result ), "@a@,@b@,@c@,@d@,2,@e@" );
	}

	@DisplayName( "It tests the BIF ListQualify with alternate delimiter" )
	@Test
	public void testBifDelimiter() {
		instance.executeSource(
		    """
		    myList="a|b|c|d|2|e";
		    result = ListQualify( myList, "@", "|", "char" );
		    """,
		    context );

		assertEquals( "@a@|@b@|@c@|@d@|2|@e@", variables.getAsString( result ) );
	}

	@DisplayName( "It tests the BIF ListQualify with empty values retained" )
	@Test
	public void testBifIncludeEmpty() {
		instance.executeSource(
		    """
		    myList="a,b,c,,d,2,e";
		    result = ListQualify( list=myList, qualifier="@", includeEmptyFields=true );
		    """,
		    context );

		assertEquals( "@a@,@b@,@c@,@@,@d@,@2@,@e@", variables.getAsString( result ) );
	}

	@DisplayName( "It tests the member function for List.ListQualify" )
	@Test
	public void testMemberFunction() {
		instance.executeSource(
		    """
		    myList="a,b,c,d,e";
		    result = myList.listQualify( "@" );
		    """,
		    context );

		assertEquals( variables.getAsString( result ), "@a@,@b@,@c@,@d@,@e@" );
	}

}
