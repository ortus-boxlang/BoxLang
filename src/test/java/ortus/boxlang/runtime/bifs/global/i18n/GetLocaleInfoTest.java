
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

package ortus.boxlang.runtime.bifs.global.i18n;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingBoxContext;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.immutable.ImmutableStruct;

public class GetLocaleInfoTest {

	static BoxRuntime	instance;
	static IBoxContext	context;
	static IScope		variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance	= BoxRuntime.getInstance( true );
		context		= new ScriptingBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@AfterAll
	public static void teardown() {
		instance.shutdown();
	}

	@BeforeEach
	public void setupEach() {
		variables.clear();
	}

	@DisplayName( "It tests the BIF GetLocaleInfo with no args" )
	@Test
	public void testGetLocaleInfoNoArgs() {
		context.setConfigItem( Key.locale, new Locale( "en", "US" ) );
		instance.executeSource(
		    """
		    result = getLocaleInfo();
		    """,
		    context );
		var result = variables.get( Key.of( "result" ) );
		assertThat( result ).isInstanceOf( ImmutableStruct.class );
		IStruct infoStruct = StructCaster.cast( result );
		assertTrue( infoStruct.containsKey( "country" ) );
		assertThat( infoStruct.get( "country" ) ).isInstanceOf( String.class );
		assertTrue( infoStruct.containsKey( "display" ) );
		assertThat( infoStruct.get( "display" ) ).isInstanceOf( ImmutableStruct.class );
		assertTrue( infoStruct.containsKey( "iso" ) );
		assertThat( infoStruct.get( "iso" ) ).isInstanceOf( ImmutableStruct.class );
		assertTrue( infoStruct.containsKey( "language" ) );
		assertTrue( infoStruct.containsKey( "name" ) );
		assertTrue( infoStruct.containsKey( "variant" ) );
		assertEquals( infoStruct.getAsString( Key.country ), "US" );
		assertEquals( infoStruct.getAsString( Key.language ), "eng" );
		assertEquals( infoStruct.getAsString( Key.of( "name" ) ), "English (United States)" );
		assertEquals( infoStruct.getAsString( Key.variant ), "" );
	}

	@DisplayName( "It tests the BIF GetLocaleInfo" )
	@Test
	public void testGetLocaleInfo() {
		instance.executeSource(
		    """
		    result = getLocaleInfo( "en-US", "English" );
		    """,
		    context );
		var result = variables.get( Key.of( "result" ) );
		assertThat( result ).isInstanceOf( ImmutableStruct.class );
		IStruct infoStruct = StructCaster.cast( result );
		assertTrue( infoStruct.containsKey( "country" ) );
		assertThat( infoStruct.get( "country" ) ).isInstanceOf( String.class );
		assertTrue( infoStruct.containsKey( "display" ) );
		assertThat( infoStruct.get( "display" ) ).isInstanceOf( ImmutableStruct.class );
		assertTrue( infoStruct.containsKey( "iso" ) );
		assertThat( infoStruct.get( "iso" ) ).isInstanceOf( ImmutableStruct.class );
		assertTrue( infoStruct.containsKey( "language" ) );
		assertTrue( infoStruct.containsKey( "name" ) );
		assertTrue( infoStruct.containsKey( "variant" ) );
		assertEquals( infoStruct.getAsString( Key.country ), "US" );
		assertEquals( infoStruct.getAsString( Key.language ), "eng" );
		assertEquals( infoStruct.getAsString( Key.of( "name" ) ), "English (United States)" );
		assertEquals( infoStruct.getAsString( Key.variant ), "" );
	}

	@DisplayName( "It tests the BIF GetLocaleInfo with German Display Language" )
	@Test
	public void testGetLocaleInfoGerman() {
		instance.executeSource(
		    """
		    result = getLocaleInfo( "en-US", "German" );
		    """,
		    context );
		var result = variables.get( Key.of( "result" ) );
		assertThat( result ).isInstanceOf( ImmutableStruct.class );
		IStruct infoStruct = StructCaster.cast( result );
		assertTrue( infoStruct.containsKey( "country" ) );
		assertThat( infoStruct.get( "country" ) ).isInstanceOf( String.class );
		assertTrue( infoStruct.containsKey( "display" ) );
		assertThat( infoStruct.get( "display" ) ).isInstanceOf( ImmutableStruct.class );
		assertTrue( infoStruct.containsKey( "iso" ) );
		assertThat( infoStruct.get( "iso" ) ).isInstanceOf( ImmutableStruct.class );
		assertTrue( infoStruct.containsKey( "language" ) );
		assertTrue( infoStruct.containsKey( "name" ) );
		assertTrue( infoStruct.containsKey( "variant" ) );
		assertEquals( infoStruct.getAsString( Key.country ), "US" );
		assertEquals( infoStruct.getAsString( Key.language ), "eng" );
		assertEquals( infoStruct.getAsString( Key.of( "name" ) ), "Englisch (Vereinigte Staaten)" );
		assertEquals( infoStruct.getAsString( Key.variant ), "" );
		assertEquals( infoStruct.getAsStruct( Key.display ).getAsString( Key.country ), "Vereinigte Staaten" );
		assertEquals( infoStruct.getAsStruct( Key.display ).getAsString( Key.language ), "Englisch" );
	}

	@DisplayName( "It tests the BIF GetLocaleInfo with Chinese Display Language" )
	@Test
	public void testGetLocaleInfoChinese() {
		instance.executeSource(
		    """
		    result = getLocaleInfo( "en-US", "Chinese" );
		    """,
		    context );
		var result = variables.get( Key.of( "result" ) );
		assertThat( result ).isInstanceOf( ImmutableStruct.class );
		IStruct infoStruct = StructCaster.cast( result );
		assertTrue( infoStruct.containsKey( "country" ) );
		assertThat( infoStruct.get( "country" ) ).isInstanceOf( String.class );
		assertTrue( infoStruct.containsKey( "display" ) );
		assertThat( infoStruct.get( "display" ) ).isInstanceOf( ImmutableStruct.class );
		assertTrue( infoStruct.containsKey( "iso" ) );
		assertThat( infoStruct.get( "iso" ) ).isInstanceOf( ImmutableStruct.class );
		assertTrue( infoStruct.containsKey( "language" ) );
		assertTrue( infoStruct.containsKey( "name" ) );
		assertTrue( infoStruct.containsKey( "variant" ) );
		assertEquals( infoStruct.getAsString( Key.country ), "US" );
		assertEquals( infoStruct.getAsString( Key.language ), "eng" );
		assertEquals( infoStruct.getAsString( Key.of( "name" ) ), "英语 (美国)" );
		assertEquals( infoStruct.getAsString( Key.variant ), "" );
		assertEquals( infoStruct.getAsStruct( Key.display ).getAsString( Key.country ), "美国" );
		assertEquals( infoStruct.getAsStruct( Key.display ).getAsString( Key.language ), "英语" );
	}

}
