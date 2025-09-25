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
package ortus.boxlang.runtime.types.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.IType;
import ortus.boxlang.runtime.types.meta.BoxMeta;

class TypeUtilTest {

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

	@Test
	void testGetObjectName() {

		assertEquals( "src.test.java.TestCases.phase3.MyClass",
		    TypeUtil.getObjectName( instance.executeStatement( "new src.test.java.TestCases.phase3.MyClass()" ) ) );
		assertEquals( "Struct", TypeUtil.getObjectName( instance.executeStatement( "{}" ) ) );
		assertEquals( "Struct<Ordered>", TypeUtil.getObjectName( instance.executeStatement( "[:]" ) ) );
		assertEquals( "XML<Document>", TypeUtil.getObjectName( instance.executeStatement( "XMLParse( '<root />' )" ) ) );
		assertEquals( "null", TypeUtil.getObjectName( null ) );
		assertEquals( "Class<String>", TypeUtil.getObjectName( String.class ) );
		assertEquals( "Class<ortus.boxlang.runtime.types.util.TypeUtilTest$MyClass>", TypeUtil.getObjectName( MyClass.class ) );
		assertEquals( "MyClassType", TypeUtil.getObjectName( new MyClass() ) );
		assertEquals( "Number<Integer>", TypeUtil.getObjectName( 42 ) );
		assertEquals( "Number<Double>", TypeUtil.getObjectName( 3.14 ) );
		assertEquals( "String", TypeUtil.getObjectName( "Hello" ) );
		assertEquals( "String[]", TypeUtil.getObjectName( new String[ 0 ] ) );
		assertEquals( "Number[]", TypeUtil.getObjectName( new Number[ 0 ] ) );
		assertEquals( "Integer[]", TypeUtil.getObjectName( new Integer[ 0 ] ) );
		assertEquals( "ortus.boxlang.runtime.types.util.TypeUtilTest$MyClass[]", TypeUtil.getObjectName( new MyClass[ 0 ] ) );
	}

	static class MyClass implements IType {

		@Override
		public String getBoxTypeName() {
			return "MyClassType";
		}

		@Override
		public String asString() {
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException( "Unimplemented method 'asString'" );
		}

		@Override
		public BoxMeta<?> getBoxMeta() {
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException( "Unimplemented method 'getBoxMeta'" );
		}
	}

}
