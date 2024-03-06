
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

package ortus.boxlang.runtime.bifs.global.encryption;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.Charset;

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
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.EncryptionUtil;

public class HmacTest {

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

	@DisplayName( "It tests the BIF Hmac on a string" )
	@Test
	public void testHmacString() {
		String referenceMac = EncryptionUtil.hmac( "Hmac me baby!", "foo", "HmacMD5", "utf-8" );
		instance.executeSource(
		    """
		    result = Hmac( "Hmac me baby!", "foo" );
		    """,
		    context );
		var result = variables.get( Key.of( "result" ) );
		assertThat( result ).isInstanceOf( String.class );
		assertThat( variables.getAsString( Key.of( "result" ) ).length() ).isEqualTo( referenceMac.length() );
	}

	@DisplayName( "It tests the Hmac string member function" )
	@Test
	public void testHmacMember() {
		String referenceMac = EncryptionUtil.hmac( "Hmac me baby!", "foo", "HmacMD5", "utf-8" );
		instance.executeSource(
		    """
		    result = "Hmac me baby!".hmac( "foo" );
		    """,
		    context );
		var result = variables.get( Key.of( "result" ) );
		assertThat( result ).isInstanceOf( String.class );
		assertThat( variables.getAsString( Key.of( "result" ) ).length() ).isEqualTo( referenceMac.length() );
	}

	@DisplayName( "It tests the BIF Hmac on a struct" )
	@Test
	public void testHmacStruct() {
		Struct referenceStruct = new Struct();
		referenceStruct.put( "action", "Hmac me baby!" );
		String referenceMac = EncryptionUtil.hmac( referenceStruct, "foo", "HmacMD5", "utf-8" );
		instance.executeSource(
		    """
		    result = hmac( { "action" : "Hmac me baby!" }, "foo" );
		    """,
		    context );
		var result = variables.get( Key.of( "result" ) );
		assertThat( result ).isInstanceOf( String.class );
		assertThat( variables.getAsString( Key.of( "result" ) ).length() ).isEqualTo( referenceMac.length() );
	}

	@DisplayName( "It tests the BIF Hmac on an array" )
	@Test
	public void testHmacArray() {
		Array referenceArray = new Array();
		referenceArray.append( "Hmac me baby!" );
		String referenceMac = EncryptionUtil.hmac( referenceArray, "foo", "HmacMD5", "utf-8" );
		instance.executeSource(
		    """
		    result = hmac( [ "Hmac me baby!" ], "foo" );
		    """,
		    context );
		var result = variables.get( Key.of( "result" ) );
		assertThat( result ).isInstanceOf( String.class );
		assertThat( variables.getAsString( Key.of( "result" ) ).length() ).isEqualTo( referenceMac.length() );
	}

	@DisplayName( "It tests the BIF Hmac with encoding" )
	@Test
	public void testHmacWithEncoding() {
		String referenceMac = EncryptionUtil.hmac( "Hmac me baby!".getBytes( Charset.forName( "utf-16" ) ), "foo", "HmacMD5", "utf-8" );
		instance.executeSource(
		    """
		    result = hmac( "Hmac me baby!", "foo", "HmacMD5", "utf-16" );
		    """,
		    context );
		var result = variables.get( Key.of( "result" ) );
		assertThat( result ).isInstanceOf( String.class );
		assertThat( variables.getAsString( Key.of( "result" ) ).length() ).isEqualTo( referenceMac.length() );
	}

	@DisplayName( "It tests the BIF Hmac with an algorithm" )
	@Test
	public void testHmacWithAlgorithm() {
		String referenceMac = EncryptionUtil.hmac( "Hmac me baby!", "foo", "HmacSHA256", "utf-8" );
		instance.executeSource(
		    """
		    result = hmac( "Hmac me baby!", "foo", "HmacSHA256" );
		    """,
		    context );
		var result = variables.get( Key.of( "result" ) );
		assertThat( result ).isInstanceOf( String.class );
		assertThat( variables.getAsString( Key.of( "result" ) ).length() ).isEqualTo( referenceMac.length() );
	}

	@DisplayName( "It tests that an invalid algorithm will throw an error" )
	@Test
	public void testHmacAlgorithmError() {
		assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeSource(
		        """
		        result = hmac( "Hash me baby!", "blah", "not an algorithm" );
		        """,
		        context )
		);

	}

}
