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
package ortus.boxlang.runtime.operators;

import static com.google.common.truth.Truth.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class InstanceOfTest {

	BoxRuntime	instance		= BoxRuntime.getInstance( true );
	IBoxContext	runtimeContext	= instance.getRuntimeContext();
	IBoxContext	context;
	IScope		variables;

	@BeforeEach
	void setUp() {
		context		= new ScriptingRequestBoxContext( runtimeContext );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "It can check java type" )
	@Test
	void testItCanCheckType() {
		assertThat( InstanceOf.invoke( context, "Brad", "java.lang.String" ) ).isTrue();
		// Lucee-only behavior
		assertThat( InstanceOf.invoke( context, "Brad", "String" ) ).isTrue();
		// Lucee-only behavior
		assertThat( InstanceOf.invoke( context, "Brad", "JAVA.LANG.STRING" ) ).isTrue();

		assertThat( InstanceOf.invoke( context, "Brad", "FooBar" ) ).isFalse();
		assertThat( InstanceOf.invoke( context, "Brad", "java.lang.Double" ) ).isFalse();
	}

	@DisplayName( "It can check java interface" )
	@Test
	void testItCanCheckInterface() {
		assertThat( InstanceOf.invoke( context, new HashMap<String, String>(), "java.util.Map" ) ).isTrue();
	}

	@DisplayName( "It can check java superinterface" )
	@Test
	void testItCanCheckSuperInterface() {
		List<String> target = new ArrayList<String>();
		assertThat( InstanceOf.invoke( context, target, "java.util.ArrayList" ) ).isTrue();
		assertThat( InstanceOf.invoke( context, target, "java.util.List" ) ).isTrue();
		assertThat( InstanceOf.invoke( context, target, "java.util.Collection" ) ).isTrue();
	}

	@DisplayName( "It can check java supertype" )
	@Test
	void testItCanCheckSupertype() {
		Map<String, String> target = new ConcurrentHashMap<String, String>();
		assertThat( InstanceOf.invoke( context, target, "java.util.concurrent.ConcurrentHashMap" ) ).isTrue();
		assertThat( InstanceOf.invoke( context, target, "java.util.HashMap" ) ).isFalse();
		assertThat( InstanceOf.invoke( context, target, "java.util.AbstractMap" ) ).isTrue();
		assertThat( InstanceOf.invoke( context, target, "java.lang.Object" ) ).isTrue();
	}

	@DisplayName( "Can check java classes" )
	@Test
	void testCanCheckJavaClasses() {
		// @formatter:off
		instance.executeSource(
			"""
				target = createObject( "java", "java.util.LinkedHashMap" )
				result = isInstanceOf( target, "java.util.LinkedHashMap" )
			""",
			context );
		// @formatter:on
		assertThat( variables.get( "result" ) ).isEqualTo( true );
	}

	@DisplayName( "Can check java class instances" )
	@Test
	void testCanCheckJavaClassInstances() {
		// @formatter:off
		instance.executeSource(
			"""
				target = createObject( "java", "java.util.LinkedHashMap" ).init()
				result = isInstanceOf( target, "java.util.LinkedHashMap" )
			""",
			context );
		// @formatter:on
		assertThat( variables.get( "result" ) ).isEqualTo( true );
	}

}
