/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.runtime.runnables;

import static com.google.common.truth.Truth.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class BoxClassSupportTest {

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

	@DisplayName( "asString includes class name and property values" )
	@Test
	void testAsString() {
		ByteArrayOutputStream capture = new ByteArrayOutputStream();
		context.getRequestContext().setOut( new PrintStream( capture ) );
		// @formatter:off
		instance.executeSource(
		    """
		    class Address {
		    	property string city;
		    	function init( city ) {
		    		variables.city = arguments.city;
		    		return this;
		    	}
		    }
		    class Person {
		    	property string name;
		    	property numeric age;
		    	property array tags;
		    	property address;
		    	function init( name, age ) {
		    		variables.name = arguments.name;
		    		variables.age = arguments.age;
		    		variables.tags = [ "a", "b", "c" ];
		    		variables.address = new Address( city="Anytown" );
		    		return this;
		    	}
		    }
		    p = new Person( name="Brad", age=30 );
		    println( p );
		    """, context );
		// @formatter:on
		String output = capture.toString();

		assertThat( output ).contains( "Person" );
		assertThat( output ).contains( "name = Brad" );
		assertThat( output ).contains( "age = 30" );
		assertThat( output ).contains( "tags = [Array]" );
		assertThat( output ).contains( "address = [Address]" );
	}

}
