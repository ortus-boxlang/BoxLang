
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

import static org.junit.jupiter.api.Assertions.assertTrue;
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
import ortus.boxlang.runtime.util.EncryptionUtil;

public class GeneratePBKDFKeyTest {

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

	@DisplayName( "It tests the BIF GeneratePBKDFKey" )
	@Test
	public void testKeyGeneration() {
		instance.executeSource(
		    """
		    result = generatePBKDFKey("PBKDF2WithHmacSHA1", "secret", "salty", 5000, 128);
		    """,
		    context );
		System.out.println( variables.get( result ) );
		assertTrue( variables.get( result ) instanceof String );
		assertEquals( variables.getAsString( result ), "Y0MCpCe3zb0CNJvyXNUWEQ==" );

		instance.executeSource(
		    """
		    result = generatePBKDFKey("PBKDF2WithHmacSHA224", "secret", "salty", 5000, 128);
		    """,
		    context );

		System.out.println( variables.get( result ) );
		assertTrue( variables.get( result ) instanceof String );
		assertEquals( variables.getAsString( result ), "i1y4f7+cLVzDDFhXjn7q1A==" );

		instance.executeSource(
		    """
		    result = generatePBKDFKey("PBKDF2WithHmacSHA256", "secret", "salty", 5000, 128);
		    """,
		    context );

		System.out.println( variables.get( result ) );
		assertTrue( variables.get( result ) instanceof String );
		assertEquals( variables.getAsString( result ), "jtq50MHsmbc7LpoS/rBH7g==" );

		instance.executeSource(
		    """
		    result = generatePBKDFKey("PBKDF2WithHmacSHA384", "secret", "salty", 5000, 128);
		    """,
		    context );

		System.out.println( variables.get( result ) );
		assertTrue( variables.get( result ) instanceof String );
		assertEquals( variables.getAsString( result ), "IWSkTDmAm6UM1uG2MMRowA==" );

		instance.executeSource(
		    """
		    result = generatePBKDFKey("PBKDF2WithHmacSHA512", "secret", "salty", 5000, 128);
		    """,
		    context );

		System.out.println( variables.get( result ) );
		assertTrue( variables.get( result ) instanceof String );
		assertEquals( variables.getAsString( result ), "asB+flulk/oXFTEVHZ+nkQ==" );

	}

	@DisplayName( "It tests the BIF GeneratePBKDFKey with alt algorithms" )
	@Test
	public void testAltAlgorithms() {
		instance.executeSource(
		    """
		    result = generatePBKDFKey("PBKDF2WithSHA224", "secret", "salty", 5000, 128);
		    """,
		    context );

		System.out.println( variables.get( result ) );
		assertTrue( variables.get( result ) instanceof String );
		assertEquals( variables.getAsString( result ), "i1y4f7+cLVzDDFhXjn7q1A==" );

		instance.executeSource(
		    """
		    result = generatePBKDFKey("PBKDF2WithSHA224", "secret", "salty", 5000, 128);
		    """,
		    context );

		System.out.println( variables.get( result ) );
		assertTrue( variables.get( result ) instanceof String );
		assertEquals( variables.getAsString( result ), "i1y4f7+cLVzDDFhXjn7q1A==" );

		instance.executeSource(
		    """
		    result = generatePBKDFKey("PBKDF2WithSHA256", "secret", "salty", 5000, 128);
		    """,
		    context );

		System.out.println( variables.get( result ) );
		assertTrue( variables.get( result ) instanceof String );
		assertEquals( variables.getAsString( result ), "jtq50MHsmbc7LpoS/rBH7g==" );

		instance.executeSource(
		    """
		    result = generatePBKDFKey("PBKDF2WithSHA384", "secret", "salty", 5000, 128);
		    """,
		    context );

		System.out.println( variables.get( result ) );
		assertTrue( variables.get( result ) instanceof String );
		assertEquals( variables.getAsString( result ), "IWSkTDmAm6UM1uG2MMRowA==" );

		instance.executeSource(
		    """
		    result = generatePBKDFKey("PBKDF2WithSHA512", "secret", "salty", 5000, 128);
		    """,
		    context );

		System.out.println( variables.get( result ) );
		assertTrue( variables.get( result ) instanceof String );
		assertEquals( variables.getAsString( result ), "asB+flulk/oXFTEVHZ+nkQ==" );

	}

}
