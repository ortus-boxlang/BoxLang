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

package ortus.boxlang.runtime.bifs.global.type;

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
import ortus.boxlang.runtime.types.IStruct;

public class GetMetaDataTest {

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

	@DisplayName( "It should return metadata for a DateTime object" )
	@Test
	public void testDateTimeMetadata() {
		instance.executeSource(
		    """
		    import ortus.boxlang.runtime.types.DateTime;
		    dt = new DateTime();
		    result = getMetadata( dt );
		    """,
		    context );
		Object meta = variables.get( result );
		assertThat( meta ).isNotNull();
		
		// It should be a struct
		assertThat( meta ).isInstanceOf( IStruct.class );
		
		IStruct metaStruct = ( IStruct ) meta;
		// It should have at least a 'class' key
		assertThat( metaStruct.containsKey( Key.of( "class" ) ) ).isTrue();
		
		// The class should be DateTime
		assertThat( metaStruct.get( Key.of( "class" ) ).toString() ).contains( "DateTime" );
	}

	@DisplayName( "It should return metadata for a String" )
	@Test
	public void testStringMetadata() {
		instance.executeSource(
		    """
		    str = "BoxLang";
		    result = getMetadata( str );
		    """,
		    context );
		Object meta = variables.get( result );
		assertThat( meta ).isNotNull();
		
		// For non-IType objects, it should return the class
		assertThat( meta ).isInstanceOf( Class.class );
		assertThat( ( ( Class<?> ) meta ).getName() ).isEqualTo( "java.lang.String" );
	}

	@DisplayName( "It should return metadata for an Array" )
	@Test
	public void testArrayMetadata() {
		instance.executeSource(
		    """
		    arr = [ 1, 2, 3 ];
		    result = getMetadata( arr );
		    """,
		    context );
		Object meta = variables.get( result );
		assertThat( meta ).isNotNull();
		
		// Arrays return structured metadata
		assertThat( meta ).isInstanceOf( IStruct.class );
		
		IStruct metaStruct = ( IStruct ) meta;
		// Generic metadata should have at least a 'class' key
		assertThat( metaStruct.containsKey( Key.of( "class" ) ) ).isTrue();
	}
}
