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
package external;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;

public class TestBoxTest {

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

	@TestFactory
	@Disabled
	Stream<DynamicTest> runDynamicTests() {

		// @formatter:off
		instance.executeSource(
		    """
			application name="testbox runner" mappings={"/testbox":expandPath("/src/test/resources/testbox")};

			result = new testbox.system.TestBox().runRaw(  directory='src.test.java.external.specs' ).getMemento();

			//println( result );

			// JUnit doesn't support nesting, so we need to flatten all specs to the same level and append all ancestor suite names
			// This funcitnn handles an array of suites, which may have come form a bundle or another suite
			function mapSuites( array suiteStats ) {
				// Process each suite
				return suiteStats.reduce( (acc, suite ) =>{
					// Process each spec at the top of the suite
					return acc.append( suite.specStats.map( spec => {
						// Pull out the data Junit needs
						return {
							name : spec.name,
							skip : spec.skip,
							status : spec.status,
							failMessage : spec.failMessage,
							failStacktrace : spec.failStacktrace,
							error : spec.error,
						}
					} )
					// Recurse into any nested suites
					.append( mapSuites( suite.suiteStats), true )
					// Slap the suite name on the front of each spec (which includes nested specs)
					.map( s => {
						s.name = suite.name & ' - ' & s.name;
						return s;
					} ), true );
				}, [] );
			}

			// Flatten the results from all bundles
			testResults = result.bundleStats.reduce(  (acc, bundle) => {
				// If the bundle fell flat and didn't even get to execute the specs, pass along the global error as a failure
				if( !isSimpleValue( bundle.globalException ) ) {
					// If there are global exceptions, we need to add them to the results
					acc.append( {
							name : bundle.path,
							status : 'Error',
							failMessage : bundle.globalException.message,
							error : bundle.globalException,
						} );
					return acc;
				}
				// Flatten the suites in this bundle
				return acc.append( mapSuites( bundle.suiteStats )
					// Slap the bundle name on the front of each spec from all the nested suites
					.map( s => {
						s.name = bundle.path & ' - ' & s.name;
						return s;
					} ), true );
			}, [] );
		    //println( testResults );
		                     """,
		    context );
		// @formatter:on
		Array testResults = variables.getAsArray( Key.of( "testResults" ) );

		return testResults.stream().map( t -> {
			IStruct test = ( IStruct ) t;
			return DynamicTest.dynamicTest(
			    test.getAsString( Key._NAME ),
			    () -> {
				    String status = test.getAsString( Key.of( "status" ) );
				    if ( status.equalsIgnoreCase( "Failed" ) || status.equalsIgnoreCase( "Error" ) ) {
					    // + "\n" + test.getAsString( Key.of( "failStacktrace" ) )
					    fail( test.getAsString( Key.of( "failMessage" ) ),
					        ( Throwable ) test.get( Key.of( "error" ) ) );
				    }
			    }
			);
		} );

	}
}
