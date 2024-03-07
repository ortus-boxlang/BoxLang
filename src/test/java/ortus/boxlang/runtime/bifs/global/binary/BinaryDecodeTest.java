
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

package ortus.boxlang.runtime.bifs.global.binary;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;
import java.util.HexFormat;

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

public class BinaryDecodeTest {

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

	@DisplayName( "It tests the BIF BinaryDecode" )
	@Test
	public void testBif() {
		String	myString	= "This is a string";
		String	hexString	= HexFormat.of().formatHex( myString.getBytes() );
		variables.put( Key.of( "binaryString" ), hexString );
		instance.executeSource(
		    """
		    result = BinaryDecode( binaryString, "hex" );
		    """,
		    context );
		assertTrue( variables.get( result ) instanceof byte[] );

		String base64String = Base64.getEncoder().encodeToString( myString.getBytes() );
		variables.put( Key.of( "binaryString" ), base64String );
		instance.executeSource(
		    """
		    result = BinaryDecode( binaryString, "base64" );
		    """,
		    context );

		assertTrue( variables.get( result ) instanceof byte[] );

		instance.executeSource(
		    """
		    result = BinaryDecode( binaryString, "base64Url" );
		    """,
		    context );

		assertTrue( variables.get( result ) instanceof byte[] );

		String mimeString = Base64.getMimeEncoder().encodeToString( myString.getBytes() );

		variables.put( Key.of( "binaryString" ), mimeString );
		instance.executeSource(
		    """
		    result = BinaryDecode( binaryString, "uu" );
		    """,
		    context );

		assertTrue( variables.get( result ) instanceof byte[] );

	}

	@DisplayName( "It tests the member function for BinaryDecode" )
	@Test
	public void testMemberFunction() {
		String	myString		= "This is a string";
		String	base64String	= Base64.getEncoder().encodeToString( myString.getBytes() );

		variables.put( Key.of( "binaryString" ), base64String );
		instance.executeSource(
		    """
		    result = BinaryDecode( binaryString, "base64" );
		    """,
		    context );

		assertTrue( variables.get( result ) instanceof byte[] );
	}

}
