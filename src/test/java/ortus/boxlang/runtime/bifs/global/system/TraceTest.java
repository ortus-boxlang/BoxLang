
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

package ortus.boxlang.runtime.bifs.global.system;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.util.Tracer;
import ortus.boxlang.runtime.util.Tracer.TracerRecord;

public class TraceTest {

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

	@AfterEach
	public void tearDownEach() {
		Tracer faciilty = context.getAttachment( Key.bxTracers );
		faciilty.reset();
	}

	@DisplayName( "It tests the BIF trace" )
	@Test
	public void testSimpleTrace() {
		// @formatter:off
		instance.executeSource(
		    """
		    trace( 'Hello World' );
		    """,
		    context );
		// @formatter:on

		assertThat( context.hasAttachment( Key.bxTracers ) ).isTrue();
		Tracer tracerFacility = context.getAttachment( Key.bxTracers );
		assertThat( tracerFacility.count() ).isEqualTo( 1 );
	}

	@DisplayName( "It tests the BIF trace with category" )
	@Test
	public void testTraceWithCategory() {
		// @formatter:off
		instance.executeSource(
		    """
		    trace( 'Hello World', 'myCategory' );
		    """,
		    context );
		// @formatter:on

		assertThat( context.hasAttachment( Key.bxTracers ) ).isTrue();

		Tracer			tracerFacility	= context.getAttachment( Key.bxTracers );
		TracerRecord	record			= tracerFacility.getTracers().peek();
		assertThat( tracerFacility.count() ).isEqualTo( 1 );
		System.out.println( record );
		assertThat( record.category() ).isEqualTo( "myCategory" );
	}

	@DisplayName( "parameterized test with 5 types" )
	@ParameterizedTest
	@ValueSource( strings = { "Info", "warn", "Error", "Debug", "Fatal" } )
	public void testTraceWithTypes( String type ) {
		// @formatter:off
		instance.executeSource(
		    """
		    trace( 'Hello World', 'myCategory', '%s' );
		    """.formatted( type ),
		    context );
		// @formatter:on

		assertThat( context.hasAttachment( Key.bxTracers ) ).isTrue();

		Tracer			tracerFacility	= context.getAttachment( Key.bxTracers );
		TracerRecord	record			= tracerFacility.getTracers().peek();
		assertThat( tracerFacility.count() ).isEqualTo( 1 );
		assertThat( record.type() ).isEqualTo( type.toLowerCase() );
	}

}
