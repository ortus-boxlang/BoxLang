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
package ortus.boxlang.runtime.bifs.global.stringbuilder;

import static com.google.common.truth.Truth.assertThat;

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

public class StringBuilderAppendTest {

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

	@DisplayName( "append() headless function adds text and returns the same instance" )
	@Test
	public void testAppendHeadless() {
		instance.executeSource( """
		                        sb = sb{'Hello'};
		                        stringBuilderAppend( sb, ' World' );
		                        result = sb.toString();
		                        """, context );
		assertThat( variables.get( result ) ).isEqualTo( "Hello World" );
	}

	@DisplayName( "append() member method adds text and returns the same instance" )
	@Test
	public void testAppendMember() {
		instance.executeSource( """
		                        sb = sb{'Hello'};
		                        sb.append( ' World' );
		                        result = sb.toString();
		                        """, context );
		assertThat( variables.get( result ) ).isEqualTo( "Hello World" );
	}

	@DisplayName( "append() returns the same instance (fluent chaining)" )
	@Test
	public void testAppendChaining() {
		instance.executeSource( """
		                        result = sb{"foo"}
		                            .append( 'bar' )
		                            .append( 'baz' )
		                            .toString();
		                        """, context );
		assertThat( variables.get( result ) ).isEqualTo( "foobarbaz" );
	}

}
