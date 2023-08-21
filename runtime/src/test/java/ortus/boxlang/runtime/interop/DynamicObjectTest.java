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

import ortus.boxlang.runtime.types.IType;

import java.lang.String;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.time.Duration;

import TestCases.interop.InvokeDynamicFields;
import TestCases.interop.PrivateConstructors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DynamicObjectTest {

	@DisplayName( "It can create class invokers of instances" )
	@Test
	void testItCanBeCreatedWithAnInstance() {
		DynamicObject target = DynamicObject.of( this );
		assertThat( target.getTargetClass() ).isEqualTo( this.getClass() );
		assertThat( target.getTargetInstance() ).isEqualTo( this );
		assertThat( target.isInterface() ).isFalse();
	}

	@DisplayName( "It can unwrap statically" )
	@Test
	void testItCanUnwrapStatically() {
		DynamicObject target = DynamicObject.of( this );
		assertThat( DynamicObject.unWrap( target ) ).isEqualTo( this );
		DynamicObject target2 = DynamicObject.of( this.getClass() );
		assertThat( DynamicObject.unWrap( target2 ) ).isEqualTo( this.getClass() );
	}

	@DisplayName( "It can unwrap itself" )
	@Test
	void testItCanUnwrapItself() {
		DynamicObject target = DynamicObject.of( this );
		assertThat( target.unWrap() ).isEqualTo( this );
		DynamicObject target2 = DynamicObject.of( this.getClass() );
		assertThat( target2.unWrap() ).isEqualTo( this.getClass() );
	}

	@DisplayName( "It can create class invokers of classes" )
	@Test
	void testItCanBeCreatedWithAClass() {
		DynamicObject target = DynamicObject.of( String.class );
		assertThat( target.getTargetClass() ).isEqualTo( String.class );
		assertThat( target.isInterface() ).isFalse();
	}

	@DisplayName( "It can create class invokers of interfaces" )
	@Test
	void testItCanBeCreatedWithAnInterface() {
		DynamicObject target = new DynamicObject( IType.class );
		assertThat( target.isInterface() ).isTrue();
	}

	@DisplayName( "It can call a constructor with one argument" )
	@Test
	void testItCanCallConstructorsWithOneArgument() throws Throwable {
		DynamicObject target = new DynamicObject( String.class );
		target.invokeConstructor( "Hello World" );
		assertThat( target.getTargetClass() ).isEqualTo( String.class );
		assertThat( target.getTargetInstance() ).isEqualTo( "Hello World" );
	}

	@DisplayName( "It can call a constructor with many arguments" )
	@Test
	void testItCanCallConstructorsWithManyArguments() throws Throwable {
		DynamicObject target = new DynamicObject( LinkedHashMap.class );
		System.out.println( int.class );
		target.invokeConstructor( 16, 0.75f, true );
		assertThat( target.getTargetClass() ).isEqualTo( LinkedHashMap.class );
	}

	@DisplayName( "It can call a constructor with no arguments" )
	@Test
	void testItCanCallConstructorsWithNoArguments() throws Throwable {
		DynamicObject target = new DynamicObject( String.class );
		target.invokeConstructor();
		assertThat( target.getTargetClass() ).isEqualTo( String.class );
		assertThat( target.getTargetInstance() ).isEqualTo( "" );
	}

	@DisplayName( "It can call instance methods with no arguments" )
	@Test
	void testItCanCallMethodsWithNoArguments() throws Throwable {
		DynamicObject myMapInvoker = new DynamicObject( HashMap.class );
		myMapInvoker.invokeConstructor();
		assertThat( myMapInvoker.invoke( "size" ).get() ).isEqualTo( 0 );
		assertThat( ( Boolean ) myMapInvoker.invoke( "isEmpty" ).get() ).isTrue();
	}

	@DisplayName( "It can call instance methods with many arguments" )
	@Test
	void testItCanCallMethodsWithManyArguments() throws Throwable {
		DynamicObject myMapInvoker = new DynamicObject( HashMap.class );
		myMapInvoker.invokeConstructor();
		myMapInvoker.invoke( "put", "name", "luis" );
		assertThat( myMapInvoker.invoke( "size" ).get() ).isEqualTo( 1 );
		assertThat( myMapInvoker.invoke( "get", "name" ).get() ).isEqualTo( "luis" );
	}

	@DisplayName( "It can call static methods on classes" )
	@Test
	void testItCanCallStaticMethods() throws Throwable {
		DynamicObject	myInvoker	= DynamicObject.of( Duration.class );
		Duration		results		= null;

		// Use int to long promotion
		results = ( Duration ) myInvoker.invoke( "ofSeconds", new Object[] { 120 } ).get();
		assertThat( results.toString() ).isEqualTo( "PT2M" );

		// Normal Long
		results = ( Duration ) myInvoker.invoke( "ofSeconds", new Object[] { 200L } ).get();
		assertThat( results.toString() ).isEqualTo( "PT3M20S" );
	}

	@DisplayName( "It can call methods on interfaces" )
	@Test
	void testItCanCallMethodsOnInterfaces() throws Throwable {
		DynamicObject	myInvoker	= DynamicObject.of( List.class );
		List			results		= ( List ) myInvoker.invoke( "of", new Object[] { "Hello" } ).get();
		assertThat( results.toString() ).isEqualTo( "[Hello]" );
		assertThat( results ).isNotEmpty();
	}

	@DisplayName( "It can create a class with private constructors" )
	@Test
	void testItCanCreateWithPrivateConstructors() throws Throwable {
		DynamicObject myInvoker = DynamicObject.of( PrivateConstructors.class );
		assertThat( myInvoker ).isNotNull();
		// Now call it via normal `invoke()`
		myInvoker.invoke( "getInstance" );
	}

	@DisplayName( "It can get public fields" )
	@Test
	void testItCanGetPublicFields() throws Throwable {
		DynamicObject myInvoker = DynamicObject.of( InvokeDynamicFields.class );
		myInvoker.invokeConstructor();
		assertThat( myInvoker.getField( "name" ).get() ).isEqualTo( "luis" );
	}

	@DisplayName( "It can get public fields with any case-sensitivity" )
	@Test
	void testItCanGetPublicFieldsInAnyCase() throws Throwable {
		DynamicObject myInvoker = DynamicObject.of( InvokeDynamicFields.class );
		myInvoker.invokeConstructor();
		assertThat( myInvoker.getField( "NaMe" ).get() ).isEqualTo( "luis" );
	}

	@DisplayName( "It can get non-existent field with a default value" )
	@Test
	void testItCanGetPublicFieldsWithADefaultValue() throws Throwable {
		DynamicObject myInvoker = DynamicObject.of( InvokeDynamicFields.class );
		myInvoker.invokeConstructor();
		assertThat( myInvoker.getField( "InvalidFieldBaby", "sorry" ).get() ).isEqualTo( "sorry" );
	}

	@DisplayName( "It can get static public fields" )
	@Test
	void testItCanGetStaticPublicFields() throws Throwable {
		DynamicObject myInvoker = DynamicObject.of( InvokeDynamicFields.class );
		assertThat( ( String ) myInvoker.getField( "HELLO" ).get() ).isEqualTo( "Hello World" );
		assertThat( ( Integer ) myInvoker.getField( "MY_PRIMITIVE" ).get() ).isEqualTo( 42 );
	}

	@DisplayName( "It can throw an exception when getting an invalid field" )
	@Test
	void testItCanThrowExceptionForInvalidFields() {
		NoSuchFieldException exception = assertThrows( NoSuchFieldException.class, () -> {
			DynamicObject myInvoker = DynamicObject.of( InvokeDynamicFields.class );
			myInvoker.invokeConstructor();
			myInvoker.getField( "InvalidField" );
		} );
		assertThat( exception.getMessage() ).contains( "InvalidField" );
	}

	@DisplayName( "It can get set values on public fields" )
	@Test
	void testItCanSetPublicFields() throws Throwable {
		DynamicObject myInvoker = DynamicObject.of( InvokeDynamicFields.class );
		myInvoker.invokeConstructor();

		myInvoker.setField( "name", "Hola Tests" );

		assertThat( myInvoker.getField( "name" ).get() ).isEqualTo( "Hola Tests" );
	}

	@DisplayName( "It can get all the fields of a class" )
	@Test
	void testItCanGetAllFields() throws Throwable {
		DynamicObject	myInvoker	= DynamicObject.of( InvokeDynamicFields.class );
		Field[]			fields		= myInvoker.getFields();
		assertThat( fields ).isNotEmpty();
		assertThat( fields.length ).isEqualTo( 3 );
	}

	@DisplayName( "It can get all the field names of a class" )
	@Test
	void testItCanGetAllFieldNames() throws Throwable {
		DynamicObject	myInvoker	= DynamicObject.of( InvokeDynamicFields.class );
		List<String>	names		= myInvoker.getFieldNames();
		assertThat( names ).isNotEmpty();
		assertThat( names.size() ).isEqualTo( 3 );
		assertThat( names ).containsExactly( new Object[] { "name", "HELLO", "MY_PRIMITIVE" } );
	}

	@DisplayName( "It can get all the field names of a class with no case sensitivity" )
	@Test
	void testItCanGetAllFieldNamesNoCase() throws Throwable {
		DynamicObject	myInvoker	= DynamicObject.of( InvokeDynamicFields.class );
		List<String>	names		= myInvoker.getFieldNamesNoCase();
		assertThat( names ).isNotEmpty();
		assertThat( names.size() ).isEqualTo( 3 );
		assertThat( names ).containsExactly( new Object[] { "NAME", "HELLO", "MY_PRIMITIVE" } );
	}

	@DisplayName( "It can verify if a field with a specific name exists" )
	@Test
	void testItCanCheckForFields() throws Throwable {
		DynamicObject myInvoker = DynamicObject.of( InvokeDynamicFields.class );

		assertThat(
		        myInvoker.hasField( "name" )
		).isTrue();

		assertThat(
		        myInvoker.hasField( "NaMe" )
		).isFalse();
		assertThat(
		        myInvoker.hasFieldNoCase( "NaMe" )
		).isTrue();

		assertThat(
		        myInvoker.hasField( "bogus" )
		).isFalse();

	}

	@DisplayName( "It can get all the callable method names of a class" )
	@Test
	void testItCanGetAllMethodNames() throws Throwable {
		DynamicObject	myInvoker	= DynamicObject.of( InvokeDynamicFields.class );
		List<String>	names		= myInvoker.getMethodNames();
		assertThat( names ).isNotEmpty();
		assertThat( names.size() ).isEqualTo( 16 );
		assertThat( names ).containsAtLeast(
		        "getName", "setName", "hasName", "hello", "getNow", "equals", "hashCode"
		);
	}

	@DisplayName( "It can get all the callable method names of a class with no case" )
	@Test
	void testItCanGetAllMethodNamesNoCase() throws Throwable {
		DynamicObject	myInvoker	= DynamicObject.of( InvokeDynamicFields.class );
		List<String>	names		= myInvoker.getMethodNamesNoCase();
		assertThat( names ).isNotEmpty();
		assertThat( names.size() ).isEqualTo( 16 );
		assertThat( names ).containsAtLeast(
		        "GETNAME", "SETNAME", "HASNAME", "HELLO", "GETNOW", "EQUALS", "HASHCODE"
		);
	}

	@DisplayName( "It can get check if a class has specific method names" )
	@Test
	void testItCanCheckIfItHasMethodNames() throws Throwable {
		DynamicObject myInvoker = DynamicObject.of( InvokeDynamicFields.class );

		assertThat(
		        myInvoker.hasMethod( "getName" )
		).isTrue();
		assertThat(
		        myInvoker.hasMethod( "GETnAme" )
		).isFalse();

		assertThat(
		        myInvoker.hasMethodNoCase( "getNamE" )
		).isTrue();
		assertThat(
		        myInvoker.hasMethodNoCase( "bogus" )
		).isFalse();
	}

	@DisplayName( "It can find methods by case-insensitive name and types" )
	@Test
	void testItCanFindMatchingMethod() throws NoSuchMethodException {
		DynamicObject	myInvoker	= DynamicObject.of( InvokeDynamicFields.class );
		Method			method		= null;

		// True Check
		method = myInvoker.findMatchingMethod( "GetNAME", new Class[] {} );
		assertThat( method.getName() ).isEqualTo( "getName" );
		method = myInvoker.findMatchingMethod( "getNoW", new Class[] {} );
		assertThat( method.getName() ).isEqualTo( "getNow" );
		method = myInvoker.findMatchingMethod( "setName", new Class[] { String.class } );
		assertThat( method.getName() ).isEqualTo( "setName" );
		method = myInvoker.findMatchingMethod( "HELLO", new Class[] {} );
		assertThat( method.getName() ).isEqualTo( "hello" );
		method = myInvoker.findMatchingMethod( "HELLO", new Class[] { String.class } );
		assertThat( method.getName() ).isEqualTo( "hello" );
		method = myInvoker.findMatchingMethod( "HELLO", new Class[] { String.class, int.class } );
		assertThat( method.getName() ).isEqualTo( "hello" );

		// False Check
		assertThrows( NoSuchMethodException.class, () -> {
			myInvoker.findMatchingMethod( "getName", new Class[] { String.class } );
		} );
		assertThrows( NoSuchMethodException.class, () -> {
			myInvoker.findMatchingMethod( "BogusName", new Class[] { String.class } );
		} );
		assertThrows( NoSuchMethodException.class, () -> {
			myInvoker.findMatchingMethod( "setName", new Class[] { Integer.class } );
		} );

	}

}
