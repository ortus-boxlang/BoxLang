
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

import static com.google.common.truth.Truth.assertThat;

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

public class BinaryEncodeTest {

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

	@DisplayName( "It tests the BIF BinaryEncode" )
	@Test
	public void testBif() {
		String	myString	= "This is a string";
		String	hexString	= HexFormat.of().formatHex( myString.getBytes() );
		variables.put( Key.of( "binaryData" ), myString.getBytes() );
		instance.executeSource(
		    """
		    result = BinaryEncode( binaryData, "hex" );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( hexString );

		byte[]	base64Bytes		= Base64.getEncoder().encode( myString.getBytes() );
		String	base64String	= Base64.getEncoder().encodeToString( base64Bytes );

		variables.put( Key.of( "binaryData" ), base64Bytes );
		instance.executeSource(
		    """
		    result = BinaryEncode( binaryData, "base64" );
		    """,
		    context );

		assertThat( variables.getAsString( result ) ).isEqualTo( base64String );

		base64Bytes		= Base64.getUrlEncoder().encode( myString.getBytes() );
		base64String	= Base64.getUrlEncoder().encodeToString( base64Bytes );

		variables.put( Key.of( "binaryData" ), base64Bytes );
		instance.executeSource(
		    """
		    result = BinaryEncode( binaryData, "base64Url" );
		    """,
		    context );

		assertThat( variables.getAsString( result ) ).isEqualTo( base64String );

		byte[]	mimeBytes	= Base64.getMimeEncoder().encode( myString.getBytes() );
		String	mimeString	= Base64.getMimeEncoder().encodeToString( mimeBytes );

		variables.put( Key.of( "binaryData" ), mimeBytes );
		instance.executeSource(
		    """
		    result = BinaryEncode( binaryData, "uu" );
		    """,
		    context );

		assertThat( variables.getAsString( result ) ).isEqualTo( mimeString );

	}

}
