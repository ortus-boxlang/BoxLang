
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
import static org.junit.jupiter.api.Assertions.assertEquals;

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

	@DisplayName( "It tests the BIF BinaryEncode a variety of transformations" )
	@Test
	public void testCycles() {
		String testString = "BoxLang is Great!";
		variables.put( Key.of( "message" ), testString );
		instance.executeSource( """
		                        function base64ToHex( String base64Value ){
		                        	var binaryValue = binaryDecode( base64Value, "base64" );
		                        	var hexValue = binaryEncode( binaryValue, "hex" );
		                        	return( lcase( hexValue ) );
		                        }
		                        function base64ToString( String base64Value ){
		                        	var binaryValue = binaryDecode( base64Value, "base64" );
		                        	var stringValue = toString( binaryValue );
		                        	return( stringValue );
		                        }
		                        function hexToBase64( String hexValue ){
		                        	var binaryValue = binaryDecode( hexValue, "hex" );
		                        	var base64Value = binaryEncode( binaryValue, "base64" );
		                        	return( base64Value );
		                        }
		                        function hexToString( String hexValue ){
		                        	var binaryValue = binaryDecode( hexValue, "hex" );
		                        	var stringValue = toString( binaryValue );
		                        	return( stringValue );
		                        }
		                        function stringToBase64( String stringValue ){
		                        	var binaryValue = stringToBinary( stringValue );
		                        	var base64Value = binaryEncode( binaryValue, "base64" );
		                        	return( base64Value );
		                        }
		                        function stringToBinary( String stringValue ){
		                        	var base64Value = toBase64( stringValue );
		                        	var binaryValue = toBinary( base64Value );
		                        	return( binaryValue );
		                        }
		                        function stringToHex( String stringValue ){
		                        	var binaryValue = stringToBinary( stringValue );
		                        	var hexValue = binaryEncode( binaryValue, "hex" );
		                        	return( lcase( hexValue ) );
		                        }
		                        messageAsHex = stringToHex( message );
		                        messageAsBase64 = stringToBase64( message );
		                        messageAsBinary = stringToBinary( message );
		                        hexToMessage = hexToString( messageAsHex );
		                        base64ToMessage = base64ToString( messageAsBase64 );
		                        binaryToMessage = toString( messageAsBinary );
		                        check1 = stringToHex( message );
		                        check1 = hexToBase64( check1 );
		                        check1 = base64ToString( check1 );
		                        check2 = stringToBase64( message );
		                        check2 = base64ToHex( check2 );
		                        check2 = hexToString( check2 );
		                        	""", context );

		assertEquals( "426f784c616e6720697320477265617421", variables.getAsString( Key.of( "messageAsHex" ) ) );
		assertEquals( "Qm94TGFuZyBpcyBHcmVhdCE=", variables.getAsString( Key.of( "messageAsBase64" ) ) );
		assertEquals( 17, ( ( byte[] ) variables.get( Key.of( "messageAsBinary" ) ) ).length );
		assertEquals( testString, variables.getAsString( Key.of( "hexToMessage" ) ) );
		assertEquals( testString, variables.getAsString( Key.of( "base64ToMessage" ) ) );
		assertEquals( testString, variables.getAsString( Key.of( "binaryToMessage" ) ) );
		assertEquals( testString, variables.getAsString( Key.of( "check1" ) ) );
		assertEquals( testString, variables.getAsString( Key.of( "check2" ) ) );
	}

}
