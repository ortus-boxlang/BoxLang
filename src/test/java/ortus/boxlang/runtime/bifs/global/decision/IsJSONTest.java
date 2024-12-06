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

package ortus.boxlang.runtime.bifs.global.decision;

import static com.google.common.truth.Truth.assertThat;

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

public class IsJSONTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;

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

	@DisplayName( "It detects JSON values" )
	@Test
	public void testTrueConditions() {
		instance.executeSource(
		    """
		    // both engines return true for these cases
		    anEmptyStruct          = isJSON( '{}' );
		    anEmptyArray           = isJSON( '[]' );
		    anArrayWithMixedValues = isJSON( '["a","b",123]' );
		    aPopulatedStruct       = isJSON( '{ "a" : 123, "b" : 456 }' );
		    anInteger              = isJSON( '123' );
		    aQuotedInteger         = isJSON( '"456"' );
		      """,
		    context );
		assertThat( ( Boolean ) variables.get( Key.of( "anEmptyStruct" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "anEmptyArray" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "anArrayWithMixedValues" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "aPopulatedStruct" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "anInteger" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "aQuotedInteger" ) ) ).isTrue();
	}

	@DisplayName( "It returns false for non-JSON values" )
	@Test
	public void testFalseConditions() {
		instance.executeSource(
		    """
		    // both engines return false for these cases
		    anEmptyString         = isJSON( '' );
		    anUnquotedStringValue = isJSON( 'gibberish' );

		    // ACF23 returns false, Lucee5 returns true
		    aStructWithUnquotedKeys     = isJSON( '[ { a : "" } ]' );
		    anArrayWithTrailingComma    = isJSON( '["a","b",123,]' );
		    anArrayWithAFloat           = isJSON( '[123456789,.11]' );
		    aStructWithSingleQuotedKeys = isJSON( '[ { ''a'' : ''123'' } ]' );
		      """,
		    context );
		assertThat( ( Boolean ) variables.get( Key.of( "anEmptyString" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "anUnquotedStringValue" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "aStructWithUnquotedKeys" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "anArrayWithTrailingComma" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "anArrayWithAFloat" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "aStructWithSingleQuotedKeys" ) ) ).isFalse();
	}

	@DisplayName( "It will return true with valid JSON with escape characters" )
	@Test
	public void testEscapeCharacters() {
		String testJSON = """
		                  [
		                  	{
		                  		"whitelist": "user\\.login,user\\.logout,^main.*",
		                  		"securelist": "^user\\.*, ^admin",
		                  		"match": "event",
		                  		"roles": "admin",
		                  		"permissions": "",
		                  		"redirect": "user.login"
		                  	},
		                  	{
		                  		"whitelist": "",
		                  		"securelist": "^shopping",
		                  		"match": "url",
		                  		"roles": "",
		                  		"permissions": "shop,checkout",
		                  		"redirect": "user.login",
		                  		"useSSL": true
		                  	}
		                  ]
		                  """;
		variables.put( Key.of( "testJSON" ), testJSON );
		instance.executeSource(
		    """
		    assert isJSON( trim( testJSON ) ) == true;
		       """,
		    context );
	}

	// For future reference when building deserializeJSON():
	// // both engines succeed
	// writeDump( deserializeJSON( '{}' ) );
	// writeDump( deserializeJSON( '[]' ) );
	// writeDump( deserializeJSON( '["a","b",123]' ) );
	// writeDump( deserializeJSON( '{ "a" : 123, "b" : 456 }' ) );
	// writeDump( deserializeJSON( '123' ) );
	// writeDump( deserializeJSON( '"456"' ) );

	// // fails in both engines
	// // writeDump( deserializeJSON( 'gibberish' ) );

	// // fails in ACF23, succeeds in Lucee5
	// writeDump( deserializeJSON( '' ) );
	// writeDump( deserializeJSON( '[ { a : "" } ]' ) ); // unquoted struct key
	// writeDump( deserializeJSON( '["a","b",123,]' ) ); // trailing comma
	// writeDump( deserializeJSON( '[123456789,.11]' ) ); // float with int
	// writeDump( deserializeJSON( '[ { ''a'' : ''123'' } ]' ) ); // single quotes instead of double
}
