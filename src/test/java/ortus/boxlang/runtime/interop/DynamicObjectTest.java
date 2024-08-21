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
import TestCases.interop.PrivateConstructors;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.IType;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxLangException;
import ortus.boxlang.runtime.types.exceptions.NoFieldException;
import ortus.boxlang.runtime.types.exceptions.NoMethodException;

public class DynamicObjectTest {

	private IBoxContext context = new ScriptingRequestBoxContext();

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
	void testItCanCallConstructorsWithOneArgument() {
		DynamicObject target = new DynamicObject( String.class );
		target.invokeConstructor( null, "Hello World" );
		assertThat( target.getTargetClass() ).isEqualTo( String.class );
		assertThat( target.getTargetInstance() ).isEqualTo( "Hello World" );
	}

	@DisplayName( "It can call a constructor with many arguments" )
	@Test
	void testItCanCallConstructorsWithManyArguments() {
		DynamicObject target = new DynamicObject( LinkedHashMap.class );
		target.invokeConstructor( null, 16, 0.75f, true );
		assertThat( target.getTargetClass() ).isEqualTo( LinkedHashMap.class );
	}

	@DisplayName( "It can call a constructor with no arguments" )
	@Test
	void testItCanCallConstructorsWithNoArguments() {
		DynamicObject target = new DynamicObject( String.class );
		target.invokeConstructor( null );
		assertThat( target.getTargetClass() ).isEqualTo( String.class );
		assertThat( target.getTargetInstance() ).isEqualTo( "" );
	}

	@DisplayName( "It can call instance methods with no arguments" )
	@Test
	void testItCanCallMethodsWithNoArguments() {
		DynamicObject myMapInvoker = new DynamicObject( HashMap.class );
		myMapInvoker.invokeConstructor( null );
		assertThat( myMapInvoker.invoke( context, "size" ) ).isEqualTo( 0 );
		assertThat( ( Boolean ) myMapInvoker.invoke( context, "isEmpty" ) ).isTrue();
	}

	@DisplayName( "It can call instance methods with many arguments" )
	@Test
	void testItCanCallMethodsWithManyArguments() {
		DynamicObject myMapInvoker = new DynamicObject( HashMap.class );
		myMapInvoker.invokeConstructor( null );
		myMapInvoker.invoke( context, "put", "name", "luis" );
		assertThat( myMapInvoker.invoke( context, "size" ) ).isEqualTo( 1 );
		assertThat( myMapInvoker.invoke( context, "get", "name" ) ).isEqualTo( "luis" );
	}

	@DisplayName( "It can call static methods on classes" )
	@Test
	void testItCanCallStaticMethods() {
		DynamicObject	myInvoker	= DynamicObject.of( Duration.class );
		Duration		results		= null;

		// Use int to long promotion
		results = ( Duration ) myInvoker.invoke( context, "ofSeconds", new Object[] { 120 } );
		assertThat( results.toString() ).isEqualTo( "PT2M" );

		// Normal Long
		results = ( Duration ) myInvoker.invoke( context, "ofSeconds", new Object[] { 200L } );
		assertThat( results.toString() ).isEqualTo( "PT3M20S" );
	}

	@DisplayName( "It can call methods on interfaces" )
	@Test
	@SuppressWarnings( "unchecked" )
	void testItCanCallMethodsOnInterfaces() {
		DynamicObject	myInvoker	= DynamicObject.of( List.class );
		List<Object>	results		= ( List<Object> ) myInvoker.invoke( context, "of", new Object[] { "Hello" } );
		assertThat( results.toString() ).isEqualTo( "[Hello]" );
		assertThat( results ).isNotEmpty();
	}

	@DisplayName( "It can create a class with private constructors" )
	@Test
	void testItCanCreateWithPrivateConstructors() {
		DynamicObject myInvoker = DynamicObject.of( PrivateConstructors.class );
		assertThat( myInvoker ).isNotNull();
		// Now call it via normal `invoke()`
		myInvoker.invoke( context, "getInstance" );
	}

	@DisplayName( "It can get public fields" )
	@Test
	void testItCanGetPublicFields() {
		DynamicObject myInvoker = DynamicObject.of( InvokeDynamicFields.class );
		myInvoker.invokeConstructor( null );
		assertThat( myInvoker.getField( "name" ).get() ).isEqualTo( "luis" );
	}

	@DisplayName( "It can get public fields with any case-sensitivity" )
	@Test
	void testItCanGetPublicFieldsInAnyCase() {
		DynamicObject myInvoker = DynamicObject.of( InvokeDynamicFields.class );
		myInvoker.invokeConstructor( null );
		assertThat( myInvoker.getField( "NaMe" ).get() ).isEqualTo( "luis" );
	}

	@DisplayName( "It can get non-existent field with a default value" )
	@Test
	void testItCanGetPublicFieldsWithADefaultValue() {
		DynamicObject myInvoker = DynamicObject.of( InvokeDynamicFields.class );
		myInvoker.invokeConstructor( null );
		assertThat( myInvoker.getField( "InvalidFieldBaby", "sorry" ).get() ).isEqualTo( "sorry" );
	}

	@DisplayName( "It can get static public fields" )
	@Test
	void testItCanGetStaticPublicFields() {
		DynamicObject myInvoker = DynamicObject.of( InvokeDynamicFields.class );
		assertThat( ( String ) myInvoker.getField( "HELLO" ).get() ).isEqualTo( "Hello World" );
		assertThat( ( Integer ) myInvoker.getField( "MY_PRIMITIVE" ).get() ).isEqualTo( 42 );
	}

	@DisplayName( "It can throw an exception when getting an invalid field" )
	@Test
	void testItCanThrowExceptionForInvalidFields() {
		NoFieldException exception = assertThrows( NoFieldException.class, () -> {
			DynamicObject myInvoker = DynamicObject.of( InvokeDynamicFields.class );
			myInvoker.invokeConstructor( null );
			myInvoker.getField( "InvalidField" );
		} );
		assertThat( exception.getMessage() ).contains( "InvalidField" );
	}

	@DisplayName( "It can get set values on public fields" )
	@Test
	void testItCanSetPublicFields() {
		DynamicObject myInvoker = DynamicObject.of( InvokeDynamicFields.class );
		myInvoker.invokeConstructor( null );

		myInvoker.setField( "name", "Hola Tests" );

		assertThat( myInvoker.getField( "name" ).get() ).isEqualTo( "Hola Tests" );
	}

	@DisplayName( "It can get all the fields of a class" )
	@Test
	void testItCanGetAllFields() {
		DynamicObject	myInvoker	= DynamicObject.of( InvokeDynamicFields.class );
		Field[]			fields		= myInvoker.getFields();
		assertThat( fields ).isNotEmpty();
		assertThat( fields.length ).isEqualTo( 3 );
	}

	@DisplayName( "It can get all the field names of a class" )
	@Test
	void testItCanGetAllFieldNames() {
		DynamicObject	myInvoker	= DynamicObject.of( InvokeDynamicFields.class );
		List<String>	names		= myInvoker.getFieldNames();
		assertThat( names ).isNotEmpty();
		assertThat( names.size() ).isEqualTo( 3 );
		assertThat( names ).containsExactly( new Object[] { "name", "HELLO", "MY_PRIMITIVE" } );
	}

	@DisplayName( "It can get all the field names of a class with no case sensitivity" )
	@Test
	void testItCanGetAllFieldNamesNoCase() {
		DynamicObject	myInvoker	= DynamicObject.of( InvokeDynamicFields.class );
		List<String>	names		= myInvoker.getFieldNamesNoCase();
		assertThat( names ).isNotEmpty();
		assertThat( names.size() ).isEqualTo( 3 );
		assertThat( names ).containsExactly( new Object[] { "NAME", "HELLO", "MY_PRIMITIVE" } );
	}

	@DisplayName( "It can verify if a field with a specific name exists" )
	@Test
	void testItCanCheckForFields() {
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
	void testItCanGetAllMethodNames() {
		DynamicObject	myInvoker	= DynamicObject.of( InvokeDynamicFields.class );
		List<String>	names		= myInvoker.getMethodNames( true );
		assertThat( names ).isNotEmpty();
		assertThat( names.size() ).isEqualTo( 16 );
		assertThat( names ).containsAtLeast(
		    "getName", "setName", "hasName", "hello", "getNow", "equals", "hashCode"
		);
	}

	@DisplayName( "It can get all the callable method names of a class with no case" )
	@Test
	void testItCanGetAllMethodNamesNoCase() {
		DynamicObject	myInvoker	= DynamicObject.of( InvokeDynamicFields.class );
		List<String>	names		= myInvoker.getMethodNamesNoCase( true );
		assertThat( names ).isNotEmpty();
		assertThat( names.size() ).isEqualTo( 16 );
		assertThat( names ).containsAtLeast(
		    "GETNAME", "SETNAME", "HASNAME", "HELLO", "GETNOW", "EQUALS", "HASHCODE"
		);
	}

	@DisplayName( "It can get check if a class has specific method names" )
	@Test
	void testItCanCheckIfItHasMethodNames() {
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
	void testItCanFindMatchingMethod() throws NoMethodException {
		DynamicObject	myInvoker	= DynamicObject.of( InvokeDynamicFields.class );
		Method			method		= null;

		// True Check
		method = myInvoker.findMatchingMethod( context, "GetNAME", new Class[] {}, new Object[] {} );
		assertThat( method.getName() ).isEqualTo( "getName" );
		method = myInvoker.findMatchingMethod( context, "getNoW", new Class[] {}, new Object[] {} );
		assertThat( method.getName() ).isEqualTo( "getNow" );
		method = myInvoker.findMatchingMethod( context, "setName", new Class[] { String.class }, new Object[] { "hola" } );
		assertThat( method.getName() ).isEqualTo( "setName" );
		method = myInvoker.findMatchingMethod( context, "HELLO", new Class[] {}, new Object[] {} );
		assertThat( method.getName() ).isEqualTo( "hello" );
		method = myInvoker.findMatchingMethod( context, "HELLO", new Class[] { String.class }, new Object[] { "hola" } );
		assertThat( method.getName() ).isEqualTo( "hello" );
		method = myInvoker.findMatchingMethod( context, "HELLO", new Class[] { String.class, int.class }, new Object[] { "hola", 1 } );
		assertThat( method.getName() ).isEqualTo( "hello" );

		// False Check
		method = myInvoker.findMatchingMethod( context, "getName", new Class[] { String.class }, new Object[] { "hola" } );
		assertThat( method ).isNull();
		method = myInvoker.findMatchingMethod( context, "BogusName", new Class[] { String.class }, new Object[] { "hola" } );
		assertThat( method ).isNull();
	}

	@DisplayName( "It use native maps" )
	@Test
	@SuppressWarnings( "unchecked" )
	void testItCanUseNativeMaps() {
		Map<Object, Object> map = new HashMap<Object, Object>();
		map.put( "key", "value" );
		DynamicObject myInvoker = DynamicObject.of( map );
		assertThat(
		    myInvoker.dereference(
		        context,
		        Key.of( "key" ),
		        false
		    )
		).isEqualTo( "value" );

		myInvoker.assign(
		    context,
		    Key.of( "key2" ),
		    "value2"
		);

		assertThat(
		    myInvoker.dereference(
		        context,
		        Key.of( "key2" ),
		        false
		    )
		).isEqualTo( "value2" );

		assertThat(
		    ( ( Map<Object, Object> ) myInvoker.getTargetInstance() ).get( "key2" )
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
		DynamicObject myInvoker = DynamicObject.of( map );

		assertThat(
		    myInvoker.dereference(
		        context,
		        Key.of( Struct.EMPTY ),
		        false
		    )
		).isEqualTo( "empty" );

		assertThat(
		    myInvoker.dereference(
		        context,
		        Key.of( str ),
		        false
		    )
		).isEqualTo( "bradwood" );

		myInvoker.assign(
		    context,
		    Key.of( str2 ),
		    "luismajano"
		);

		assertThat(
		    myInvoker.dereference(
		        context,
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
		DynamicObject myInvoker = DynamicObject.of( new String[] { "Brad", "Wood" } );
		assertThat(
		    myInvoker.dereference(
		        context,
		        // Use IntKey
		        Key.of( 1 ),
		        false
		    )
		).isEqualTo( "Brad" );

		assertThat(
		    myInvoker.dereference(
		        context,
		        // Use Key
		        Key.of( "1" ),
		        false
		    )
		).isEqualTo( "Brad" );

		myInvoker.assign(
		    context,
		    Key.of( 2 ),
		    "Ortus Solutions"
		);

		assertThat(
		    myInvoker.dereference(
		        context,
		        Key.of( 2 ),
		        false
		    )
		).isEqualTo( "Ortus Solutions" );

		assertThat(
		    myInvoker.dereference(
		        context,
		        Key.of( "length" ),
		        false
		    )
		).isEqualTo( 2 );

		assertThrows( BoxLangException.class, () -> {
			myInvoker.dereference(
			    context,
			    Key.of( "test" ),
			    false
			);
		} );

		assertThrows( BoxLangException.class, () -> {
			myInvoker.dereference(
			    context,
			    Key.of( 0 ),
			    false
			);
		} );

		assertThrows( BoxLangException.class, () -> {
			myInvoker.dereference(
			    context,
			    Key.of( 1.5 ),
			    false
			);
		} );

		assertThrows( BoxLangException.class, () -> {
			myInvoker.dereference(
			    context,
			    Key.of( 50 ),
			    false
			);
		} );

		assertThat( myInvoker.dereference( context, Key.of( "test" ), true ) ).isEqualTo( null );
		assertThat( myInvoker.dereference( context, Key.of( 0 ), true ) ).isEqualTo( null );
		assertThat( myInvoker.dereference( context, Key.of( 1.5 ), true ) ).isEqualTo( null );
		assertThat( myInvoker.dereference( context, Key.of( 50 ), true ) ).isEqualTo( null );
	}

	@DisplayName( "It use Lists" )
	@Test
	void testItCanUseLists() {
		ArrayList<Object> list = new ArrayList<Object>();
		list.add( "Brad" );
		list.add( "Wood" );
		DynamicObject myInvoker = DynamicObject.of( list );
		assertThat(
		    myInvoker.dereference(
		        context,
		        // Use IntKey
		        Key.of( 1 ),
		        false
		    )
		).isEqualTo( "Brad" );

		assertThat(
		    myInvoker.dereference(
		        context,
		        // Use Key
		        Key.of( "1" ),
		        false
		    )
		).isEqualTo( "Brad" );

		myInvoker.assign(
		    context,
		    Key.of( 2 ),
		    "Ortus Solutions"
		);

		assertThat(
		    myInvoker.dereference(
		        context,
		        Key.of( 2 ),
		        false
		    )
		).isEqualTo( "Ortus Solutions" );

		assertThrows( BoxLangException.class, () -> {
			myInvoker.dereference(
			    context,
			    Key.of( "test" ),
			    false
			);
		} );

		assertThrows( BoxLangException.class, () -> {
			myInvoker.dereference(
			    context,
			    Key.of( 0 ),
			    false
			);
		} );

		assertThrows( BoxLangException.class, () -> {
			myInvoker.dereference(
			    context,
			    Key.of( 1.5 ),
			    false
			);
		} );

		assertThrows( BoxLangException.class, () -> {
			myInvoker.dereference(
			    context,
			    Key.of( 50 ),
			    false
			);
		} );

		assertThat( myInvoker.dereference( context, Key.of( "test" ), true ) ).isEqualTo( null );
		assertThat( myInvoker.dereference( context, Key.of( 0 ), true ) ).isEqualTo( null );
		assertThat( myInvoker.dereference( context, Key.of( 1.5 ), true ) ).isEqualTo( null );
		assertThat( myInvoker.dereference( context, Key.of( 50 ), true ) ).isEqualTo( null );
	}

	@DisplayName( "It can init class" )
	@Test
	void testItCanInitClass() {
		DynamicObject	stringClass	= DynamicObject.of( String.class );
		String			myString	= ( String ) stringClass.dereferenceAndInvoke( context, Key.init, new Object[] { "my string" }, false );
		String			yourString	= ( String ) stringClass.dereferenceAndInvoke( context, Key.init, new Object[] { "your string" }, false );
		assertThat( myString ).isEqualTo( "my string" );
		assertThat( yourString ).isEqualTo( "your string" );

	}

	@DisplayName( "It can init instance" )
	@Test
	void testItCanInitInstance() {
		DynamicObject	stringClass	= DynamicObject.of( new String( "some existing string" ) );
		String			myString	= ( String ) stringClass.dereferenceAndInvoke( context, Key.init, new Object[] { "my string" }, false );
		String			yourString	= ( String ) stringClass.dereferenceAndInvoke( context, Key.init, new Object[] { "your string" }, false );
		assertThat( myString ).isEqualTo( "my string" );
		assertThat( yourString ).isEqualTo( "your string" );

	}

	@DisplayName( "Test hashcode and equals of classes" )
	@Test
	void testHashCodeAndEquals() {
		DynamicObject	stringClass		= DynamicObject.of( String.class );
		DynamicObject	stringClass2	= DynamicObject.of( String.class );

		assertThat( stringClass.hashCode() ).isEqualTo( stringClass2.hashCode() );
		assertThat( stringClass.equals( stringClass2 ) ).isTrue();
	}

	@DisplayName( "Test hashcode and equals of instances" )
	@Test
	void testHashCodeAndEqualsOfInstances() {
		DynamicObject	stringClass		= DynamicObject.of( new String( "some existing string" ) );
		DynamicObject	stringClass2	= DynamicObject.of( new String( "some existing string" ) );

		assertThat( stringClass.hashCode() ).isEqualTo( stringClass2.hashCode() );
		assertThat( stringClass.equals( stringClass2 ) ).isTrue();
	}

}
