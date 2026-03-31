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

import java.io.IOException;
import java.nio.file.Paths;

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

public class GetClassMetadataTest {

	static BoxRuntime	runtime;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		runtime = BoxRuntime.getInstance( true );
	}

	@AfterAll
	public static void teardown() {

	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( runtime.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "It can get the metadata for a bx class" )
	@Test
	public void testClass() {
		runtime.executeSource(
		    """
		    result = getClassMetadata( "src.test.bx.Person" );
		    """,
		    context );

		var metadata = variables.getAsStruct( result );
		assertThat( metadata.containsKey( "name" ) ).isTrue();
		assertThat( metadata.containsKey( "type" ) ).isTrue();
		assertThat( metadata.getAsString( Key._name ) ).isEqualTo( "src.test.bx.Person" );
		assertThat( metadata.getAsString( Key.type ) ).isEqualTo( "Class" );
	}

	@DisplayName( "It can get the metadata for a bx class with correct case on file path" )
	@Test
	public void testClassCaseSensitive() throws IOException {
		runtime.executeSource(
		    """
		    result = getClassMetadata( "SRC.test.BX.pErSoN" );
		    """,
		    context );

		var metadata = variables.getAsStruct( result );
		assertThat( metadata.getAsString( Key.path ) ).isEqualTo( Paths.get( "src/test/bx/Person.bx" ).toAbsolutePath().toRealPath().toString() );
	}

	@DisplayName( "It can get the metadata for a class with constructor args" )
	@Test
	public void testClassWithConstructorArgs() {
		runtime.executeSource(
		    """
		    result = new src.test.java.ortus.boxlang.runtime.bifs.global.system.MetadataConstructorArg();
		    """,
		    context );

		var clazz = variables.getAsClassRunnable( result );
		// Simulate legacy metadata call that compat invokes
		clazz.getMetaData();
	}

	@DisplayName( "It can get the metadata for a bx interface" )
	@Test
	public void testInterface() {
		runtime.executeSource(
		    """
		    result = getClassMetadata( "src.test.bx.INotifiable" );
		    """,
		    context );

		var metadata = variables.getAsStruct( result );
		assertThat( metadata.containsKey( "name" ) ).isTrue();
		assertThat( metadata.getAsString( Key._name ) ).isEqualTo( "src.test.bx.INotifiable" );

		assertThat( metadata.containsKey( "type" ) ).isTrue();
		assertThat( metadata.getAsString( Key.type ) ).isEqualTo( "Interface" );

		assertThat( metadata.containsKey( "annotations" ) ).isTrue();
		var annotations = metadata.getAsStruct( Key.annotations );
		assertThat( annotations.containsKey( "Anno" ) ).isTrue();
		assertThat( annotations.getAsString( Key.of( "Anno" ) ) ).isEqualTo( "value" );

		assertThat( metadata.containsKey( "documentation" ) ).isTrue();
		var documentation = metadata.getAsStruct( Key.documentation );
		assertThat( documentation.getAsString( Key.of( "foo" ) ) ).isEqualTo( "bar" );
	}

	@DisplayName( "It can get the implements for a bx class that has no implements" )
	@Test
	public void testClassNoImplements() {
		runtime.executeSource(
		    """
		    result = getClassMetadata( "src.test.bx.Person" );
		    """,
		    context );

		var metadata = variables.getAsStruct( result );
		assertThat( metadata.containsKey( "implements" ) ).isTrue();
		assertThat( metadata.getAsStruct( Key.of( "implements" ) ) ).isEmpty();
	}

	@DisplayName( "It can get the implements for a bx class that has implements" )
	@Test
	public void testClassWithImplements() {
		runtime.executeSource(
		    """
		    result = getClassMetadata( "src.test.java.TestCases.phase3.InterfaceInheritenceTest" );
		    """,
		    context );

		var metadata = variables.getAsStruct( result );
		assertThat( metadata.getAsString( Key._name ) ).isEqualTo( "src.test.java.TestCases.phase3.InterfaceInheritenceTest" );

		assertThat( metadata.containsKey( "implements" ) ).isTrue();
		var implementsMap = metadata.getAsStruct( Key.of( "implements" ) );
		assertThat( implementsMap ).isNotEmpty();
		assertThat( implementsMap.containsKey( "src.test.java.TestCases.phase3.IChildInterface" ) ).isTrue();
	}

}
