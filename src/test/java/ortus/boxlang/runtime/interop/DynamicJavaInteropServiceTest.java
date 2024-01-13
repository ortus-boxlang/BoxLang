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
package ortus.boxlang.runtime.interop;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import TestCases.interop.InvokeDynamicFields;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxLangException;
import ortus.boxlang.runtime.types.exceptions.NoFieldException;
import ortus.boxlang.runtime.types.exceptions.NoMethodException;

public class DynamicJavaInteropServiceTest {

	private IBoxContext context = new ScriptingBoxContext();

	@DisplayName( "It can call a constructor with one argument" )
	@Test
	void testItCanCallConstructorsWithOneArgument() {
		Object target = DynamicJavaInteropService.invokeConstructor( null, String.class, "Hello World" );
		assertThat( target.getClass() ).isEqualTo( String.class );
		assertThat( target ).isEqualTo( "Hello World" );
	}

	@DisplayName( "It can call a constructor with many arguments" )
	@Test
	void testItCanCallConstructorsWithManyArguments() {
		Object target = DynamicJavaInteropService.invokeConstructor( null, LinkedHashMap.class, 16, 0.75f, true );
		assertThat( target.getClass() ).isEqualTo( LinkedHashMap.class );
	}

	@DisplayName( "It can call a constructor with no arguments" )
	@Test
	void testItCanCallConstructorsWithNoArguments() {
		Object target = DynamicJavaInteropService.invokeConstructor( null, String.class );
		assertThat( target.getClass() ).isEqualTo( String.class );
		assertThat( target ).isEqualTo( "" );
	}

	@DisplayName( "It can call instance methods with no arguments" )
	@Test
	void testItCanCallMethodsWithNoArguments() {
		HashMap map = DynamicJavaInteropService.invokeConstructor( null, HashMap.class );
		assertThat( DynamicJavaInteropService.invoke( map, "size", false ) ).isEqualTo( 0 );
		assertThat( ( Boolean ) DynamicJavaInteropService.invoke( map, "isEmpty", false ) ).isTrue();
	}

	@DisplayName( "It can call instance methods with many arguments" )
	@Test
	void testItCanCallMethodsWithManyArguments() {
		HashMap map = DynamicJavaInteropService.invokeConstructor( null, HashMap.class );
		DynamicJavaInteropService.invoke( map, "put", false, "name", "luis" );
		assertThat( DynamicJavaInteropService.invoke( map, "size", false ) ).isEqualTo( 1 );
		assertThat( DynamicJavaInteropService.invoke( map, "get", false, "name" ) ).isEqualTo( "luis" );
	}

	@DisplayName( "It can call static methods on classes" )
	@Test
	void testItCanCallStaticMethods() {
		Duration results = null;

		// Use int to long promotion
		results = ( Duration ) DynamicJavaInteropService.invoke( Duration.class, "ofSeconds", false, new Object[] { 120 } );
		assertThat( results.toString() ).isEqualTo( "PT2M" );

		// Normal Long
		results = ( Duration ) DynamicJavaInteropService.invoke( Duration.class, "ofSeconds", false, new Object[] { 200L } );
		assertThat( results.toString() ).isEqualTo( "PT3M20S" );
	}

	@DisplayName( "It can call methods on interfaces" )
	@Test
	@SuppressWarnings( "unchecked" )
	void testItCanCallMethodsOnInterfaces() {
		List<Object> results = ( List<Object> ) DynamicJavaInteropService.invoke( List.class, "of", false, new Object[] { "Hello" } );
		assertThat( results.toString() ).isEqualTo( "[Hello]" );
		assertThat( results ).isNotEmpty();
	}

	@DisplayName( "It can get public fields" )
	@Test
	void testItCanGetPublicFields() {
		assertThat( DynamicJavaInteropService.getField( InvokeDynamicFields.class, new InvokeDynamicFields(), "name" ).get() ).isEqualTo( "luis" );
		assertThat( DynamicJavaInteropService.getField( new InvokeDynamicFields(), "name" ).get() ).isEqualTo( "luis" );
	}

	@DisplayName( "It can get public fields with any case-sensitivity" )
	@Test
	void testItCanGetPublicFieldsInAnyCase() {
		InvokeDynamicFields invoker = new InvokeDynamicFields();
		assertThat( DynamicJavaInteropService.getField( invoker, "NaMe" ).get() ).isEqualTo( "luis" );
		assertThat( DynamicJavaInteropService.getField( InvokeDynamicFields.class, invoker, "NaMe" ).get() ).isEqualTo( "luis" );
	}

	@DisplayName( "It can get non-existent field with a default value" )
	@Test
	void testItCanGetPublicFieldsWithADefaultValue() {

		InvokeDynamicFields invoker = new InvokeDynamicFields();
		assertThat( DynamicJavaInteropService.getField( invoker, "InvalidFieldBaby", "sorry" ).get() ).isEqualTo( "sorry" );
		assertThat( DynamicJavaInteropService.getField( InvokeDynamicFields.class, invoker, "InvalidFieldBaby", "sorry" ).get() ).isEqualTo( "sorry" );

	}

	@DisplayName( "It can get static public fields" )
	@Test
	void testItCanGetStaticPublicFields() {
		InvokeDynamicFields invoker = new InvokeDynamicFields();

		assertThat( DynamicJavaInteropService.getField( invoker, "HELLO" ).get() ).isEqualTo( "Hello World" );
		assertThat( DynamicJavaInteropService.getField( invoker, "MY_PRIMITIVE" ).get() ).isEqualTo( 42 );
	}

	@DisplayName( "It can throw an exception when getting an invalid field" )
	@Test
	void testItCanThrowExceptionForInvalidFields() {
		NoFieldException exception = assertThrows( NoFieldException.class, () -> {
			InvokeDynamicFields invoker = new InvokeDynamicFields();
			DynamicJavaInteropService.getField( invoker, "InvalidField" );
		} );
		assertThat( exception.getMessage() ).contains( "InvalidField" );
	}

	@DisplayName( "It can get set values on public fields" )
	@Test
	void testItCanSetPublicFields() {
		InvokeDynamicFields invoker = new InvokeDynamicFields();

		DynamicJavaInteropService.setField( invoker, "name", "Hola Tests" );
		assertThat( DynamicJavaInteropService.getField( invoker, "name" ).get() ).isEqualTo( "Hola Tests" );
	}

	@DisplayName( "It can get all the fields of a class" )
	@Test
	void testItCanGetAllFields() {
		Field[] fields = DynamicJavaInteropService.getFields( InvokeDynamicFields.class );
		assertThat( fields ).isNotEmpty();
		assertThat( fields.length ).isEqualTo( 3 );
	}

	@DisplayName( "It can get all the field names of a class" )
	@Test
	void testItCanGetAllFieldNames() {
		List<String> names = DynamicJavaInteropService.getFieldNames( InvokeDynamicFields.class );
		assertThat( names ).isNotEmpty();
		assertThat( names.size() ).isEqualTo( 3 );
		assertThat( names ).containsExactly( new Object[] { "name", "HELLO", "MY_PRIMITIVE" } );
	}

	@DisplayName( "It can get all the field names of a class with no case sensitivity" )
	@Test
	void testItCanGetAllFieldNamesNoCase() {
		List<String> names = DynamicJavaInteropService.getFieldNamesNoCase( InvokeDynamicFields.class );
		assertThat( names ).isNotEmpty();
		assertThat( names.size() ).isEqualTo( 3 );
		assertThat( names ).containsExactly( new Object[] { "NAME", "HELLO", "MY_PRIMITIVE" } );
	}

	@DisplayName( "It can verify if a field with a specific name exists" )
	@Test
	void testItCanCheckForFields() {
		assertThat(
		    DynamicJavaInteropService.hasField( InvokeDynamicFields.class, "name" )
		).isTrue();

		assertThat(
		    DynamicJavaInteropService.hasField( InvokeDynamicFields.class, "NaMe" )
		).isFalse();
		assertThat(
		    DynamicJavaInteropService.hasFieldNoCase( InvokeDynamicFields.class, "NaMe" )
		).isTrue();

		assertThat(
		    DynamicJavaInteropService.hasField( InvokeDynamicFields.class, "bogus" )
		).isFalse();

	}

	@DisplayName( "It can get all the callable method names of a class" )
	@Test
	void testItCanGetAllMethodNames() {
		List<String> names = DynamicJavaInteropService.getMethodNames( InvokeDynamicFields.class );
		assertThat( names ).isNotEmpty();
		assertThat( names.size() ).isEqualTo( 16 );
		assertThat( names ).containsAtLeast(
		    "getName", "setName", "hasName", "hello", "getNow", "equals", "hashCode"
		);
	}

	@DisplayName( "It can get all the callable method names of a class with no case" )
	@Test
	void testItCanGetAllMethodNamesNoCase() {
		List<String> names = DynamicJavaInteropService.getMethodNamesNoCase( InvokeDynamicFields.class );
		assertThat( names ).isNotEmpty();
		assertThat( names.size() ).isEqualTo( 16 );
		assertThat( names ).containsAtLeast(
		    "GETNAME", "SETNAME", "HASNAME", "HELLO", "GETNOW", "EQUALS", "HASHCODE"
		);
	}

	@DisplayName( "It can get check if a class has specific method names" )
	@Test
	void testItCanCheckIfItHasMethodNames() {
		assertThat(
		    DynamicJavaInteropService.hasMethod( InvokeDynamicFields.class, "getName" )
		).isTrue();
		assertThat(
		    DynamicJavaInteropService.hasMethod( InvokeDynamicFields.class, "GETnAme" )
		).isFalse();

		assertThat(
		    DynamicJavaInteropService.hasMethodNoCase( InvokeDynamicFields.class, "getNamE" )
		).isTrue();
		assertThat(
		    DynamicJavaInteropService.hasMethodNoCase( InvokeDynamicFields.class, "bogus" )
		).isFalse();
	}

	@DisplayName( "It can find methods by case-insensitive name and types" )
	@Test
	void testItCanFindMatchingMethod() throws NoMethodException {
		Method method = null;

		// True Check
		method = DynamicJavaInteropService.findMatchingMethod( InvokeDynamicFields.class, "GetNAME", new Class[] {} );
		assertThat( method.getName() ).isEqualTo( "getName" );
		method = DynamicJavaInteropService.findMatchingMethod( InvokeDynamicFields.class, "getNoW", new Class[] {} );
		assertThat( method.getName() ).isEqualTo( "getNow" );
		method = DynamicJavaInteropService.findMatchingMethod( InvokeDynamicFields.class, "setName", new Class[] { String.class } );
		assertThat( method.getName() ).isEqualTo( "setName" );
		method = DynamicJavaInteropService.findMatchingMethod( InvokeDynamicFields.class, "HELLO", new Class[] {} );
		assertThat( method.getName() ).isEqualTo( "hello" );
		method = DynamicJavaInteropService.findMatchingMethod( InvokeDynamicFields.class, "HELLO", new Class[] { String.class } );
		assertThat( method.getName() ).isEqualTo( "hello" );
		method = DynamicJavaInteropService.findMatchingMethod( InvokeDynamicFields.class, "HELLO", new Class[] { String.class, int.class } );
		assertThat( method.getName() ).isEqualTo( "hello" );

		// False Check
		assertThrows( NoMethodException.class, () -> {
			DynamicJavaInteropService.findMatchingMethod( InvokeDynamicFields.class, "getName", new Class[] { String.class } );
		} );
		assertThrows( NoMethodException.class, () -> {
			DynamicJavaInteropService.findMatchingMethod( InvokeDynamicFields.class, "BogusName", new Class[] { String.class } );
		} );
		assertThrows( NoMethodException.class, () -> {
			DynamicJavaInteropService.findMatchingMethod( InvokeDynamicFields.class, "setName", new Class[] { Integer.class } );
		} );

	}

	@DisplayName( "It use native maps" )
	@Test
	@SuppressWarnings( "unchecked" )
	void testItCanUseNativeMaps() {
		Map<Object, Object> map = new HashMap<Object, Object>();
		map.put( "key", "value" );
		assertThat(
		    DynamicJavaInteropService.dereference(
		        context,
		        map,
		        Key.of( "key" ),
		        false
		    )
		).isEqualTo( "value" );

		DynamicJavaInteropService.assign(
		    context,
		    map,
		    Key.of( "key2" ),
		    "value2"
		);

		assertThat(
		    DynamicJavaInteropService.dereference(
		        context,
		        map,
		        Key.of( "key2" ),
		        false
		    )
		).isEqualTo( "value2" );

		assertThat(
		    ( map ).get( "key2" )
		).isEqualTo( "value2" );
	}

	@DisplayName( "It use native maps with complex keys" )
	@Test
	void testItCanUseNativeMapsWithComplexKeys() {
		IStruct					str		= Struct.of( "brad", "wood" );
		IStruct					str2	= Struct.of( "luis", "majano" );
		Map<IStruct, Object>	map		= new HashMap<IStruct, Object>();
		map.put( Struct.EMPTY, "empty" );
		map.put( str, "bradwood" );

		assertThat(
		    DynamicJavaInteropService.dereference(
		        context,
		        map,
		        Key.of( Struct.EMPTY ),
		        false
		    )
		).isEqualTo( "empty" );

		assertThat(
		    DynamicJavaInteropService.dereference(
		        context,
		        map,
		        Key.of( str ),
		        false
		    )
		).isEqualTo( "bradwood" );

		DynamicJavaInteropService.assign(
		    context,
		    map,
		    Key.of( str2 ),
		    "luismajano"
		);

		assertThat(
		    DynamicJavaInteropService.dereference(
		        context,
		        map,
		        Key.of( str2 ),
		        false
		    )
		).isEqualTo( "luismajano" );

		assertThat(
		    map.get( str2 )
		).isEqualTo( "luismajano" );

	}

	@DisplayName( "It use native arrays" )
	@Test
	void testItCanUseNativeArrays() {
		String[] array = new String[] { "Brad", "Wood" };
		assertThat(
		    DynamicJavaInteropService.dereference(
		        context,
		        array,
		        // Use IntKey
		        Key.of( 1 ),
		        false
		    )
		).isEqualTo( "Brad" );

		assertThat(
		    DynamicJavaInteropService.dereference(
		        context,
		        array,
		        // Use Key
		        Key.of( "1" ),
		        false
		    )
		).isEqualTo( "Brad" );

		DynamicJavaInteropService.assign(
		    context,
		    array,
		    Key.of( 2 ),
		    "Ortus Solutions"
		);

		assertThat(
		    DynamicJavaInteropService.dereference(
		        context,
		        array,
		        Key.of( 2 ),
		        false
		    )
		).isEqualTo( "Ortus Solutions" );

		assertThat(
		    DynamicJavaInteropService.dereference(
		        context,
		        array,
		        Key.of( "length" ),
		        false
		    )
		).isEqualTo( 2 );

		assertThrows( BoxLangException.class, () -> {
			DynamicJavaInteropService.dereference(
			    context,
			    array,
			    Key.of( "test" ),
			    false
			);
		} );

		assertThrows( BoxLangException.class, () -> {
			DynamicJavaInteropService.dereference(
			    context,
			    array,
			    Key.of( 0 ),
			    false
			);
		} );

		assertThrows( BoxLangException.class, () -> {
			DynamicJavaInteropService.dereference(
			    context,
			    array,
			    Key.of( 1.5 ),
			    false
			);
		} );

		assertThrows( BoxLangException.class, () -> {
			DynamicJavaInteropService.dereference(
			    context,
			    array,
			    Key.of( 50 ),
			    false
			);
		} );

		assertThat( DynamicJavaInteropService.dereference( context, array, Key.of( "test" ), true ) ).isEqualTo( null );
		assertThat( DynamicJavaInteropService.dereference( context, array, Key.of( 0 ), true ) ).isEqualTo( null );
		assertThat( DynamicJavaInteropService.dereference( context, array, Key.of( 1.5 ), true ) ).isEqualTo( null );
		assertThat( DynamicJavaInteropService.dereference( context, array, Key.of( 50 ), true ) ).isEqualTo( null );
	}

	@DisplayName( "It use Lists" )
	@Test
	void testItCanUseLists() {
		ArrayList<Object> list = new ArrayList<Object>();
		list.add( "Brad" );
		list.add( "Wood" );

		assertThat(
		    DynamicJavaInteropService.dereference(
		        context,
		        list,
		        // Use IntKey
		        Key.of( 1 ),
		        false
		    )
		).isEqualTo( "Brad" );

		assertThat(
		    DynamicJavaInteropService.dereference(
		        context,
		        list,
		        // Use Key
		        Key.of( "1" ),
		        false
		    )
		).isEqualTo( "Brad" );

		DynamicJavaInteropService.assign(
		    context,
		    list,
		    Key.of( 2 ),
		    "Ortus Solutions"
		);

		assertThat(
		    DynamicJavaInteropService.dereference(
		        context,
		        list,
		        Key.of( 2 ),
		        false
		    )
		).isEqualTo( "Ortus Solutions" );

		assertThrows( BoxLangException.class, () -> {
			DynamicJavaInteropService.dereference(
			    context,
			    list,
			    Key.of( "test" ),
			    false
			);
		} );

		assertThrows( BoxLangException.class, () -> {
			DynamicJavaInteropService.dereference(
			    context,
			    list,
			    Key.of( 0 ),
			    false
			);
		} );

		assertThrows( BoxLangException.class, () -> {
			DynamicJavaInteropService.dereference(
			    context,
			    list,
			    Key.of( 1.5 ),
			    false
			);
		} );

		assertThrows( BoxLangException.class, () -> {
			DynamicJavaInteropService.dereference(
			    context,
			    list,
			    Key.of( 50 ),
			    false
			);
		} );

		assertThat( DynamicJavaInteropService.dereference( context, list, Key.of( "test" ), true ) ).isEqualTo( null );
		assertThat( DynamicJavaInteropService.dereference( context, list, Key.of( 0 ), true ) ).isEqualTo( null );
		assertThat( DynamicJavaInteropService.dereference( context, list, Key.of( 1.5 ), true ) ).isEqualTo( null );
		assertThat( DynamicJavaInteropService.dereference( context, list, Key.of( 50 ), true ) ).isEqualTo( null );
	}

}
