
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

package ortus.boxlang.runtime.components.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class ObjectTest {

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

	@DisplayName( "It tests the BIF Object a java object" )
	@Test
	public void testComponentCF() {
		instance.executeSource(
		    """
		    <cfobject name="result" type="java" className="java.lang.String" />
		    """,
		    context, BoxSourceType.CFTEMPLATE );

		assertTrue( variables.get( result ) instanceof DynamicObject );
		assertEquals( ( ( DynamicObject ) variables.get( result ) ).getTargetClass(), String.class );
	}

	@Disabled( "It tests the BIF Object with BoxLang parsing" )
	@Test
	public void testComponentBX() {
		instance.executeSource(
		    """
		    <bx:object name="result" type="java" className="java.lang.String" />
		    """,
		    context, BoxSourceType.BOXTEMPLATE );

		assertTrue( variables.get( result ) instanceof DynamicObject );
		assertEquals( ( ( DynamicObject ) variables.get( result ) ).getTargetClass(), String.class );
	}

	@Disabled( "It tests the BIF Object with BoxLang parsing" )
	@Test
	public void testComponentScript() {
		instance.executeSource(
		    """
		    object name="result" type="java" className="java.lang.String";
		    """,
		    context, BoxSourceType.BOXSCRIPT );

		assertTrue( variables.get( result ) instanceof DynamicObject );
		assertEquals( ( ( DynamicObject ) variables.get( result ) ).getTargetClass(), String.class );
	}

}
