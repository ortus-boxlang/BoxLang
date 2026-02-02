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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.xnio.OptionMap;
import org.xnio.Xnio;
import org.xnio.XnioIoThread;
import org.xnio.XnioWorker;

import TestCases.interop.InvokeDynamicFields;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.dynamic.Referencer;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxCastException;
import ortus.boxlang.runtime.types.exceptions.BoxLangException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.NoFieldException;
import ortus.boxlang.runtime.types.exceptions.NoMethodException;
import ortus.boxlang.runtime.types.util.BooleanRef;

public class DynamicInteropServiceTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "It can call a constructor with one argument" )
	@Test
	void testItCanCallConstructorsWithOneArgument() {
		Object target = DynamicInteropService.invokeConstructor( null, String.class, "Hello World" );
		assertThat( target.getClass() ).isEqualTo( String.class );
		assertThat( target ).isEqualTo( "Hello World" );
	}

	@DisplayName( "It can call a constructor with many arguments" )
	@Test
	void testItCanCallConstructorsWithManyArguments() {
		Object target = DynamicInteropService.invokeConstructor( null, LinkedHashMap.class, 16, 0.75f, true );
		assertThat( target.getClass() ).isEqualTo( LinkedHashMap.class );
	}

	@DisplayName( "It can call a constructor with no arguments" )
	@Test
	void testItCanCallConstructorsWithNoArguments() {
		Object target = DynamicInteropService.invokeConstructor( null, String.class );
		assertThat( target.getClass() ).isEqualTo( String.class );
		assertThat( target ).isEqualTo( "" );
	}

	@DisplayName( "It can call instance methods with no arguments" )
	@Test
	void testItCanCallMethodsWithNoArguments() {
		HashMap map = DynamicInteropService.invokeConstructor( null, HashMap.class );
		assertThat( DynamicInteropService.invoke( context, map, "size", false ) ).isEqualTo( 0 );
		assertThat( ( Boolean ) DynamicInteropService.invoke( context, map, "isEmpty", false ) ).isTrue();
	}

	@DisplayName( "It can call instance methods with many arguments" )
	@Test
	void testItCanCallMethodsWithManyArguments() {
		HashMap map = DynamicInteropService.invokeConstructor( null, HashMap.class );
		DynamicInteropService.invoke( context, map, "put", false, "name", "luis" );
		assertThat( DynamicInteropService.invoke( context, map, "size", false ) ).isEqualTo( 1 );
		assertThat( DynamicInteropService.invoke( context, map, "get", false, "name" ) ).isEqualTo( "luis" );
	}

	@DisplayName( "It can call static methods on classes" )
	@Test
	void testItCanCallStaticMethods() {
		Duration results = null;

		// Use int to long promotion
		results = ( Duration ) DynamicInteropService.invoke( context, Duration.class, "ofSeconds", false, new Object[] { 120 } );
		assertThat( results.toString() ).isEqualTo( "PT2M" );

		// Normal Long
		results = ( Duration ) DynamicInteropService.invoke( context, Duration.class, "ofSeconds", false, new Object[] { 200L } );
		assertThat( results.toString() ).isEqualTo( "PT3M20S" );
	}

	@DisplayName( "It can call methods on interfaces" )
	@Test
	@SuppressWarnings( "unchecked" )
	void testItCanCallMethodsOnInterfaces() {
		List<Object> results = ( List<Object> ) DynamicInteropService.invoke( context, List.class, "of", false, new Object[] { "Hello" } );
		assertThat( results.toString() ).isEqualTo( "[Hello]" );
		assertThat( results ).isNotEmpty();
	}

	@DisplayName( "It can get public fields" )
	@Test
	void testItCanGetPublicFields() {
		assertThat( DynamicInteropService.getField( InvokeDynamicFields.class, new InvokeDynamicFields(), "name" ).get() ).isEqualTo( "luis" );
		assertThat( DynamicInteropService.getField( new InvokeDynamicFields(), "name" ).get() ).isEqualTo( "luis" );
	}

	@DisplayName( "It can get public fields with any case-sensitivity" )
	@Test
	void testItCanGetPublicFieldsInAnyCase() {
		InvokeDynamicFields invoker = new InvokeDynamicFields();
		assertThat( DynamicInteropService.getField( invoker, "NaMe" ).get() ).isEqualTo( "luis" );
		assertThat( DynamicInteropService.getField( InvokeDynamicFields.class, invoker, "NaMe" ).get() ).isEqualTo( "luis" );
	}

	@DisplayName( "It can get non-existent field with a default value" )
	@Test
	void testItCanGetPublicFieldsWithADefaultValue() {

		InvokeDynamicFields invoker = new InvokeDynamicFields();
		assertThat( DynamicInteropService.getField( invoker, "InvalidFieldBaby", "sorry" ).get() ).isEqualTo( "sorry" );
		assertThat( DynamicInteropService.getField( InvokeDynamicFields.class, invoker, "InvalidFieldBaby", "sorry" ).get() ).isEqualTo( "sorry" );

	}

	@DisplayName( "It can get static public fields" )
	@Test
	void testItCanGetStaticPublicFields() {
		InvokeDynamicFields invoker = new InvokeDynamicFields();

		assertThat( DynamicInteropService.getField( invoker, "HELLO" ).get() ).isEqualTo( "Hello World" );
		assertThat( DynamicInteropService.getField( invoker, "MY_PRIMITIVE" ).get() ).isEqualTo( 42 );
	}

	@DisplayName( "It can throw an exception when getting an invalid field" )
	@Test
	void testItCanThrowExceptionForInvalidFields() {
		NoFieldException exception = assertThrows( NoFieldException.class, () -> {
			InvokeDynamicFields invoker = new InvokeDynamicFields();
			DynamicInteropService.getField( invoker, "InvalidField" );
		} );
		assertThat( exception.getMessage() ).contains( "InvalidField" );
	}

	@DisplayName( "It can get set values on public fields" )
	@Test
	void testItCanSetPublicFields() {
		InvokeDynamicFields invoker = new InvokeDynamicFields();

		DynamicInteropService.setField( invoker, "name", "Hola Tests" );
		assertThat( DynamicInteropService.getField( invoker, "name" ).get() ).isEqualTo( "Hola Tests" );
	}

	@DisplayName( "It can get all the fields of a class" )
	@Test
	void testItCanGetAllFields() {
		Field[] fields = DynamicInteropService.getFields( InvokeDynamicFields.class );
		assertThat( fields ).isNotEmpty();
		assertThat( fields.length ).isEqualTo( 3 );
	}

	@DisplayName( "It can get all the field names of a class" )
	@Test
	void testItCanGetAllFieldNames() {
		List<String> names = DynamicInteropService.getFieldNames( InvokeDynamicFields.class );
		assertThat( names ).isNotEmpty();
		assertThat( names.size() ).isEqualTo( 3 );
		assertThat( names ).containsExactly( new Object[] { "name", "HELLO", "MY_PRIMITIVE" } );
	}

	@DisplayName( "It can get all the field names of a class with no case sensitivity" )
	@Test
	void testItCanGetAllFieldNamesNoCase() {
		List<String> names = DynamicInteropService.getFieldNamesNoCase( InvokeDynamicFields.class );
		assertThat( names ).isNotEmpty();
		assertThat( names.size() ).isEqualTo( 3 );
		assertThat( names ).containsExactly( new Object[] { "NAME", "HELLO", "MY_PRIMITIVE" } );
	}

	@DisplayName( "It can verify if a field with a specific name exists" )
	@Test
	void testItCanCheckForFields() {
		assertThat(
		    DynamicInteropService.hasField( InvokeDynamicFields.class, "name" )
		).isTrue();

		assertThat(
		    DynamicInteropService.hasField( InvokeDynamicFields.class, "NaMe" )
		).isFalse();
		assertThat(
		    DynamicInteropService.hasFieldNoCase( InvokeDynamicFields.class, "NaMe" )
		).isTrue();

		assertThat(
		    DynamicInteropService.hasField( InvokeDynamicFields.class, "bogus" )
		).isFalse();

	}

	@DisplayName( "It can get public static fields from package-private parent class" )
	@Test
	void testItCanGetStaticFieldsFromPackagePrivateParent() {
		// Test accessing inherited public static fields from package-private parent
		assertThat(
		    DynamicInteropService.getField( TestCases.interop.PublicConcreteChild.class, "PARENT_CONSTANT_STRING" ).get()
		).isEqualTo( "ParentValue" );

		assertThat(
		    DynamicInteropService.getField( TestCases.interop.PublicConcreteChild.class, "PARENT_CONSTANT_INT" ).get()
		).isEqualTo( 42 );

		assertThat(
		    DynamicInteropService.getField( TestCases.interop.PublicConcreteChild.class, "PARENT_CONSTANT_BOOLEAN" ).get()
		).isEqualTo( true );

		// Also test accessing the child's own static field
		assertThat(
		    DynamicInteropService.getField( TestCases.interop.PublicConcreteChild.class, "CHILD_CONSTANT" ).get()
		).isEqualTo( "ChildValue" );
	}

	@DisplayName( "It can get public static fields from package-private parent via BoxLang" )
	@Test
	void testItCanGetStaticFieldsFromPackagePrivateParentViaBoxLang() {
		// @formatter:off
		instance.executeSource(
		    """
		        import TestCases.interop.PublicConcreteChild;

		        result1 = PublicConcreteChild.PARENT_CONSTANT_STRING;
		        result2 = PublicConcreteChild.PARENT_CONSTANT_INT;
		        result3 = PublicConcreteChild.PARENT_CONSTANT_BOOLEAN;
		        result4 = PublicConcreteChild.CHILD_CONSTANT;
		    """, context );
		// @formatter:on

		assertThat( variables.get( Key.of( "result1" ) ) ).isEqualTo( "ParentValue" );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( 42 );
		assertThat( variables.get( Key.of( "result3" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "result4" ) ) ).isEqualTo( "ChildValue" );
	}

	@DisplayName( "It can get all the callable method names of a class" )
	@Test
	void testItCanGetAllMethodNames() {
		List<String> names = DynamicInteropService.getMethodNames( InvokeDynamicFields.class, true );
		assertThat( names ).isNotEmpty();
		assertThat( names.size() ).isEqualTo( 16 );
		assertThat( names ).containsAtLeast(
		    "getName", "setName", "hasName", "hello", "getNow", "equals", "hashCode"
		);
	}

	@DisplayName( "It can get all the callable method names of a class with no case" )
	@Test
	void testItCanGetAllMethodNamesNoCase() {
		List<String> names = DynamicInteropService.getMethodNamesNoCase( InvokeDynamicFields.class, true );
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
		    DynamicInteropService.hasMethod( InvokeDynamicFields.class, "getName" )
		).isTrue();
		assertThat(
		    DynamicInteropService.hasMethod( InvokeDynamicFields.class, "GETnAme" )
		).isFalse();

		assertThat(
		    DynamicInteropService.hasMethodNoCase( InvokeDynamicFields.class, "getNamE" )
		).isTrue();
		assertThat(
		    DynamicInteropService.hasMethodNoCase( InvokeDynamicFields.class, "bogus" )
		).isFalse();
	}

	@DisplayName( "It can find methods by case-insensitive name and types" )
	@Test
	void testItCanFindMatchingMethod() throws NoMethodException {
		Method method = null;

		// True Check
		method = DynamicInteropService.findMatchingMethod( context, InvokeDynamicFields.class, "GetNAME", new Class[] {}, BooleanRef.of( true ),
		    new Object[] {} );
		assertThat( method.getName() ).isEqualTo( "getName" );
		method = DynamicInteropService.findMatchingMethod( context, InvokeDynamicFields.class, "getNoW", new Class[] {}, BooleanRef.of( true ),
		    new Object[] {} );
		assertThat( method.getName() ).isEqualTo( "getNow" );
		method = DynamicInteropService.findMatchingMethod( context, InvokeDynamicFields.class, "setName", new Class[] { String.class }, BooleanRef.of( true ),
		    new Object[] { "hola" } );
		assertThat( method.getName() ).isEqualTo( "setName" );
		method = DynamicInteropService.findMatchingMethod( context, InvokeDynamicFields.class, "HELLO", new Class[] {}, BooleanRef.of( true ),
		    new Object[] {} );
		assertThat( method.getName() ).isEqualTo( "hello" );
		method = DynamicInteropService.findMatchingMethod( context, InvokeDynamicFields.class, "HELLO", new Class[] { String.class }, BooleanRef.of( true ),
		    new Object[] { "hola" } );
		assertThat( method.getName() ).isEqualTo( "hello" );
		method = DynamicInteropService.findMatchingMethod( context, InvokeDynamicFields.class, "HELLO", new Class[] { String.class, int.class },
		    BooleanRef.of( true ),
		    new Object[] { "hola", 1 } );
		assertThat( method.getName() ).isEqualTo( "hello" );

		// False Check
		method = DynamicInteropService.findMatchingMethod( context, InvokeDynamicFields.class, "getName", new Class[] { String.class }, BooleanRef.of( true ),
		    new Object[] { "hola" } );
		assertThat( method ).isNull();
		method = DynamicInteropService.findMatchingMethod( context, InvokeDynamicFields.class, "BogusName", new Class[] { String.class }, BooleanRef.of( true ),
		    new Object[] { "hola" } );
		assertThat( method ).isNull();
	}

	@DisplayName( "It use native maps" )
	@Test
	@SuppressWarnings( "unchecked" )
	void testItCanUseNativeMaps() {
		Map<Object, Object> map = new HashMap<Object, Object>();
		map.put( "key", "value" );
		assertThat(
		    DynamicInteropService.dereference(
		        context,
		        map,
		        Key.of( "key" ),
		        false
		    )
		).isEqualTo( "value" );

		DynamicInteropService.assign(
		    context,
		    map,
		    Key.of( "key2" ),
		    "value2"
		);

		assertThat(
		    DynamicInteropService.dereference(
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
		    DynamicInteropService.dereference(
		        context,
		        map,
		        Key.of( Struct.EMPTY ),
		        false
		    )
		).isEqualTo( "empty" );

		assertThat(
		    DynamicInteropService.dereference(
		        context,
		        map,
		        Key.of( str ),
		        false
		    )
		).isEqualTo( "bradwood" );

		DynamicInteropService.assign(
		    context,
		    map,
		    Key.of( str2 ),
		    "luismajano"
		);

		assertThat(
		    DynamicInteropService.dereference(
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
		    DynamicInteropService.dereference(
		        context,
		        array,
		        // Use IntKey
		        Key.of( 1 ),
		        false
		    )
		).isEqualTo( "Brad" );

		assertThat(
		    DynamicInteropService.dereference(
		        context,
		        array,
		        // Use Key
		        Key.of( "1" ),
		        false
		    )
		).isEqualTo( "Brad" );

		DynamicInteropService.assign(
		    context,
		    array,
		    Key.of( 2 ),
		    "Ortus Solutions"
		);

		assertThat(
		    DynamicInteropService.dereference(
		        context,
		        array,
		        Key.of( 2 ),
		        false
		    )
		).isEqualTo( "Ortus Solutions" );

		assertThat(
		    DynamicInteropService.dereference(
		        context,
		        array,
		        Key.of( "length" ),
		        false
		    )
		).isEqualTo( 2 );

		assertThrows( BoxLangException.class, () -> {
			DynamicInteropService.dereference(
			    context,
			    array,
			    Key.of( "test" ),
			    false
			);
		} );

		assertThrows( BoxLangException.class, () -> {
			DynamicInteropService.dereference(
			    context,
			    array,
			    Key.of( 0 ),
			    false
			);
		} );

		assertThrows( BoxLangException.class, () -> {
			DynamicInteropService.dereference(
			    context,
			    array,
			    Key.of( 1.5 ),
			    false
			);
		} );

		assertThrows( BoxLangException.class, () -> {
			DynamicInteropService.dereference(
			    context,
			    array,
			    Key.of( 50 ),
			    false
			);
		} );

		assertThat( DynamicInteropService.dereference( context, array, Key.of( "test" ), true ) ).isEqualTo( null );
		assertThat( DynamicInteropService.dereference( context, array, Key.of( 0 ), true ) ).isEqualTo( null );
		assertThat( DynamicInteropService.dereference( context, array, Key.of( 1.5 ), true ) ).isEqualTo( null );
		assertThat( DynamicInteropService.dereference( context, array, Key.of( 50 ), true ) ).isEqualTo( null );
	}

	@DisplayName( "It use Lists" )
	@Test
	void testItCanUseLists() {
		ArrayList<Object> list = new ArrayList<Object>();
		list.add( "Brad" );
		list.add( "Wood" );

		assertThat(
		    DynamicInteropService.dereference(
		        context,
		        list,
		        // Use IntKey
		        Key.of( 1 ),
		        false
		    )
		).isEqualTo( "Brad" );

		assertThat(
		    DynamicInteropService.dereference(
		        context,
		        list,
		        // Use Key
		        Key.of( "1" ),
		        false
		    )
		).isEqualTo( "Brad" );

		DynamicInteropService.assign(
		    context,
		    list,
		    Key.of( 2 ),
		    "Ortus Solutions"
		);

		assertThat(
		    DynamicInteropService.dereference(
		        context,
		        list,
		        Key.of( 2 ),
		        false
		    )
		).isEqualTo( "Ortus Solutions" );

		assertThrows( BoxLangException.class, () -> {
			DynamicInteropService.dereference(
			    context,
			    list,
			    Key.of( "test" ),
			    false
			);
		} );

		assertThrows( BoxLangException.class, () -> {
			DynamicInteropService.dereference(
			    context,
			    list,
			    Key.of( 0 ),
			    false
			);
		} );

		assertThrows( BoxLangException.class, () -> {
			DynamicInteropService.dereference(
			    context,
			    list,
			    Key.of( 1.5 ),
			    false
			);
		} );

		assertThrows( BoxLangException.class, () -> {
			DynamicInteropService.dereference(
			    context,
			    list,
			    Key.of( 50 ),
			    false
			);
		} );

		assertThat( DynamicInteropService.dereference( context, list, Key.of( "test" ), true ) ).isEqualTo( null );
		assertThat( DynamicInteropService.dereference( context, list, Key.of( 0 ), true ) ).isEqualTo( null );
		assertThat( DynamicInteropService.dereference( context, list, Key.of( 1.5 ), true ) ).isEqualTo( null );
		assertThat( DynamicInteropService.dereference( context, list, Key.of( 50 ), true ) ).isEqualTo( null );
	}

	@DisplayName( "Invoke Public Method Inherited From Private Class Example" )
	@Test
	// This test will no longer work once we remove Undertow as a dependency. We can remove it then
	void testInvokePublicMethodInheritedFromPrivateClassExample() throws IllegalArgumentException, IOException {
		Xnio			xnio	= Xnio.getInstance();
		XnioWorker		worker	= xnio.createWorker( OptionMap.EMPTY );
		XnioIoThread	thread	= worker.getIoThread();
		assertThat(
		    Referencer.getAndInvoke( context, thread, Key.of( "hashCode" ), new Object[] {}, false )
		).isNotNull();
	}

	@DisplayName( "Invoke Public Method Inherited From Private Class in BoxLang" )
	@Test
	void testInvokePublicMethodInheritedFromPrivateClassExampleInBoxLang() throws IllegalArgumentException, IOException {
		Xnio			xnio	= Xnio.getInstance();
		XnioWorker		worker	= xnio.createWorker( OptionMap.EMPTY );
		XnioIoThread	thread	= worker.getIoThread();
		variables.put( Key.of( "xnioThread" ), thread );

		instance.executeSource(
		    """
		       result = xnioThread.hashCode()
		       println( result)
		    """, context );

		assertThat( variables.get( Key.of( "result" ) ) ).isNotNull();
	}

	@Test
	@DisplayName( "It can call a constructor with a dynamic argument that implements an interface" )
	void testItCanCallConstructorWithDynamicInterface() {
		// @formatter:off
		instance.executeSource(
		        """
		            import java:java.lang.Thread;

		          	jRunnable = createDynamicProxy(
		          		"src.test.java.ortus.boxlang.runtime.dynamic.javaproxy.BoxClassRunnable",
		          		"java.lang.Runnable"
		            );

		        	jThread = new java:Thread( jRunnable );
					jThread.start();
		        """, context );
		// @formatter:on
	}

	@DisplayName( "Invoke Interface Method implemented by Private Class in BoxLang" )
	@Test
	void testInvokeInterfaceMethodImplementedByPrivateClassInBoxLang() {
		@SuppressWarnings( "unused" )
		Map<Object, Object> test = Collections.synchronizedMap( new LinkedHashMap<Object, Object>( 5 ) );

		// @formatter:off
		instance.executeSource(
		        """
		            pool = createObject( "java", "java.util.Collections" ).synchronizedMap(
		        		createObject( "java", "java.util.LinkedHashMap" ).init( 5 )
		        	);
		        	result = pool.containsKey( "test" )
					pool.put( "luis", "majano" )
					testLuis = pool.containsKey( "luis" )
		        """, context );
		// @formatter:on
		assertThat( variables.get( Key.of( "result" ) ) ).isEqualTo( false );
		assertThat(
		    context.getScopeNearby( VariablesScope.name ).get( Key.of( "testLuis" ) )
		).isEqualTo( true );
	}

	@Test
	@DisplayName( "It can get all the callable constructors on a class" )
	void testItCanGetAllConstructors() {
		Set<Executable> constructors = DynamicInteropService.getConstructors( String.class, true );
		assertThat( constructors ).isNotEmpty();
		assertThat( constructors.size() ).isAtLeast( 15 );
	}

	@Test
	@DisplayName( "It can get all the callable constructors as a stream" )
	void testItCanGetAllConstructorsAsStream() {
		Stream<Executable> constructors = DynamicInteropService.getConstructorsAsStream( String.class, true );
		assertThat( constructors ).isNotNull();
	}

	@Test
	@DisplayName( "It can find a matching constructor using argument types and length" )
	void testItCanFindMatchingConstructor() {
		Object[]		args		= new Object[] {};
		// String(String original)
		Constructor<?>	constructor	= DynamicInteropService.findMatchingConstructor( context, String.class, new Class[] { String.class },
		    BooleanRef.of( true ), args );
		assertThat( constructor ).isNotNull();

		// String( StringBuider builder )
		constructor = DynamicInteropService.findMatchingConstructor( context, String.class, new Class[] { StringBuilder.class }, BooleanRef.of( true ), args );
		assertThat( constructor ).isNotNull();

		// String( StringBuffer buffer )
		constructor = DynamicInteropService.findMatchingConstructor( context, String.class, new Class[] { StringBuffer.class }, BooleanRef.of( true ), args );
		assertThat( constructor ).isNotNull();

		// String( byte[] bytes, Charset charset )
		constructor = DynamicInteropService.findMatchingConstructor( context, String.class, new Class[] { byte[].class, String.class }, BooleanRef.of( true ),
		    args );
		assertThat( constructor ).isNotNull();
	}

	@Test
	@DisplayName( "I want to import a Java class and call a static method on the import" )
	void testImportJavaClassAndCallStaticMethod() {
		// @formatter:off
		instance.executeSource(
		        """
		            import java:ortus.boxlang.runtime.scopes.Key;
		        	result = Key.of( "hello" ).getName();
		            println( result );
		        """, context );
		// @formatter:on
		assertThat( variables.get( Key.of( "result" ) ) ).isEqualTo( "hello" );
	}

	@DisplayName( "It can call getClass() via createObject( 'java' ) wihtout failing" )
	@Test
	void testItCanCallGetClassOnClasses() {
		// @formatter:off
		instance.executeSource(
			"""
				cl = createObject("java","java.net.URLClassLoader");
				result = cl.getClass().getName();
				println( result );
			""", context);
		// @formatter:on

		var result = variables.get( Key.result );
		assertThat( result ).isEqualTo( "java.net.URLClassLoader" );
	}

	@DisplayName( "It can coerce number types" )
	@Test
	void testItCanCoerceArguments() {
		// @formatter:off
		instance.executeSource(
			"""
				import java.util.concurrent.TimeUnit;
				function getVal( numeric val ){
					return val++;
				}
				// Coerce a Double to a Long
				result = TimeUnit.DAYS.toSeconds( getVal( 1 ) );
			""", context);
		// @formatter:on

		var result = variables.get( Key.result );
		assertThat( result ).isEqualTo( 86400 );
	}

	@SuppressWarnings( "unchecked" )
	@DisplayName( "It can coerce boxlang lambdas to functional interfaces" )
	@Test
	void testItCanCoerceBoxLangLambdas() {
		// @formatter:off
		instance.executeSource(
			"""
				fruits = [ "apple", "banana", "cherry", "ananas", "elderberry" ];
				result = fruits.stream()
					.filter(  fruit -> fruit.startsWith( "a" ) )
					.toList();

			""", context);
		// @formatter:on

		List<String> result = ( List<String> ) variables.get( Key.result );
		assertThat( result.size() ).isEqualTo( 2 );
	}

	@SuppressWarnings( "unchecked" )
	@DisplayName( "It can coerce boxlang lambdas in parallel to functional interfaces" )
	@Test
	void testItCanCoerceBoxLangLambdasInParallel() {
		// @formatter:off
		instance.executeSource(
			"""
				fruits = [ "apple", "banana", "cherry", "ananas", "elderberry", "apricot", "avocado", "almond", "acorn", "banana", "cherry", "ananas", "elderberry", "apricot", "avocado", "almond", "acorn" ];
				result = fruits
					.parallelStream()
					.filter(  fruit -> fruit.startsWith( "a" ) )
					.toList();
			""", context);
		// @formatter:on

		List<String> result = ( List<String> ) variables.get( Key.result );
		assertThat( result.size() ).isEqualTo( 11 );
	}

	@SuppressWarnings( "unchecked" )
	@DisplayName( "It can coerce boxlang closures to functional interfaces" )
	@Test
	void testItCanCoerceBoxLangClosures() {
		// @formatter:off
		instance.executeSource(
			"""
				fruits = [ "apple", "banana", "cherry", "ananas", "elderberry" ];
				result = fruits.stream()
					.filter(  fruit => fruit.startsWith( "a" ) )
					.toList();

			""", context);
		// @formatter:on

		List<String> result = ( List<String> ) variables.get( Key.result );
		assertThat( result.size() ).isEqualTo( 2 );
	}

	@DisplayName( "It can coerce into any functional interface or sam" )
	@Test
	void testItCanExecuteAnySAM() {
		assertDoesNotThrow( () -> {
			// @formatter:off
			instance.executeSource(
				"""
					import java.util.Arrays;

					cacheService = cacheService()
					cacheService.createDefaultCache( "bddTest" );

					cache = cacheService.getCache( "bddTest" );
					cache.set( "bl-1", "Hello World" );
					cache.set( "bx-2", "Hello World" );
					cache.set( "bl-3", "Hello World" );
					cache.set( "bx-4", "Hello World" );

					cache.clearAll(
						( key ) -> key.getName().startsWith( "bl" )
					);

					println( cache.getKeys() );
				""", context);
			// @formatter:on
		} );

	}

	@DisplayName( "Can work with BigDecimal" )
	@Test
	void testItCanWorkWithBigDecimal() {
		// @formatter:off
		instance.executeSource(
			"""
				import java.math.BigDecimal
				a = BigDecimal.valueOf( 2 )
				result = a.add( 444 )
			""", context);
		// @formatter:on

		var result = variables.get( Key.result );
		assertThat( result ).isInstanceOf( BigDecimal.class );
		assertThat( ( ( BigDecimal ) result ).doubleValue() ).isEqualTo( 446 );
	}

	@DisplayName( "It can execute static varargs method" )
	@Test
	void testItCanExecuteVaragsMethod() {
		// @formatter:off
		instance.executeSource(
			"""
				import java.util.stream.IntStream

				result = IntStream.of( [1,2,3,4] )
			""", context);
		// @formatter:on

		var result = variables.get( Key.result );
		assertThat( result ).isNotNull();
		assertThat( result ).isInstanceOf( IntStream.class );
		assertThat( ( ( IntStream ) result ).toArray() ).isEqualTo( new int[] { 1, 2, 3, 4 } );
	}

	@DisplayName( "It can execute static method with varargs and normal args" )
	@Test
	void testItCanExecuteStaticMethodWithVarargsAndNormalArgs() {
	// @formatter:off
    instance.executeSource(
        """
		import java.lang.String;
            result = String.format("Hello %s, you have %d new messages.", ["Alice", 5])
        """, context);
    // @formatter:on

		var result = variables.get( Key.result );
		assertThat( result ).isNotNull();
		assertThat( result ).isInstanceOf( String.class );
		assertThat( result ).isEqualTo( "Hello Alice, you have 5 new messages." );
	}

	@DisplayName( "It can execute varargs instance method with normal args" )
	@Test
	void testItCanExecuteVarargsInstanceMethodWithNormalArgs() {
		// @formatter:off
		instance.executeSource(
			"""
				import java.util.Locale

				formatter = new java.util.Formatter()
				result = formatter.format( "Hello %s, you are %d years old!",  [ "John",  30 ] ).toString()
			""", context);
		// @formatter:on

		var result = variables.get( Key.result );
		assertThat( result ).isNotNull();
		assertThat( result ).isInstanceOf( String.class );
		assertThat( result ).isEqualTo( "Hello John, you are 30 years old!" );
	}

	@DisplayName( "It can execute varargs instance method" )
	@Test
	void testItCanExecuteVarargsInstanceMethod() {
		// @formatter:off
		instance.executeSource(
			"""
				import ortus.boxlang.runtime.interop.VarArgsExample

				result = new VarArgsExample( [] )
				result.setValues( ["one", "two", "three"] );
			""", context);
		// @formatter:on

		var example = variables.get( Key.result );
		assertThat( example ).isNotNull();
		Object r = DynamicObject.unWrap( variables.get( Key.result ) );
		assertThat( r ).isInstanceOf( VarArgsExample.class );
		assertThat( ( ( VarArgsExample ) r ).getValues() ).isEqualTo( new String[] { "one", "two", "three" } );
	}

	@DisplayName( "It can execute varargs constructor" )
	@Test
	void testItCanExecuteVarargsConstructor() {
		// @formatter:off
		instance.executeSource(
			"""
				import ortus.boxlang.runtime.interop.VarArgsExample

				result = new VarArgsExample( [ "one", "two", "three" ] )
			""", context);
		// @formatter:on

		var example = variables.get( Key.result );
		assertThat( example ).isNotNull();
		Object r = DynamicObject.unWrap( variables.get( Key.result ) );
		assertThat( r ).isInstanceOf( VarArgsExample.class );
		assertThat( ( ( VarArgsExample ) r ).getValues() ).isEqualTo( new String[] { "one", "two", "three" } );
	}

	@DisplayName( "It can execute varargs constructor with normal args" )
	@Test
	void testItCanExecuteVarargsConstructorWithNormalArgs() {
		// @formatter:off
		instance.executeSource(
			"""
				import ortus.boxlang.runtime.interop.VarArgsExample

				result = new VarArgsExample( {}, [ "one", "two", "three" ] )
			""", context);
		// @formatter:on

		var example = variables.get( Key.result );
		assertThat( example ).isNotNull();
		Object r = DynamicObject.unWrap( variables.get( Key.result ) );
		assertThat( r ).isInstanceOf( VarArgsExample.class );
		assertThat( ( ( VarArgsExample ) r ).getValues() ).isEqualTo( new String[] { "one", "two", "three" } );
	}

	@DisplayName( "It can pass BL array to T[] argument" )
	@Test
	void testItCanPassBLArrayToTArray() {
		// @formatter:off
		instance.executeSource(
			"""
				import java.util.Arrays;

				result = Arrays.stream( [ 1,2,3 ] ).toArray()
			""", context);
		// @formatter:on

		assertThat( variables.get( Key.result ) ).isNotNull();
		assertThat( variables.get( Key.result ) ).isInstanceOf( int[].class );
	}

	@SuppressWarnings( "unchecked" )
	@DisplayName( "It can pass BL String array to T[] argument" )
	@Test
	void testItCanPassBLStringArrayToTArray() {
		// @formatter:off
		instance.executeSource(
			"""
				import java.util.Arrays;

				result = Arrays.stream( [ "brad", "luis", "jon" ] )
			""", context);
		// @formatter:on

		assertThat( variables.get( Key.result ) ).isNotNull();
		assertThat( variables.get( Key.result ) ).isInstanceOf( Stream.class );
		assertThat( ( ( Stream<String> ) variables.get( Key.result ) ).toArray() ).isEqualTo( new String[] { "brad", "luis", "jon" } );
	}

	@Test
	void testMatchClostestConstructorArgs() {
		// @formatter:off
		instance.executeSource(
			"""
				import java.util.Date;
				result = new Date( 0 );
			""", context);
		// @formatter:on
		assertThat( variables.get( Key.result ) ).isNotNull();
		Object theResult = DynamicObject.unWrap( variables.get( Key.result ) );
		assertThat( theResult ).isInstanceOf( Date.class );
		assertThat( ( ( Date ) theResult ).getTime() ).isEqualTo( 0 );
		new Date( 0 );
	}

	@Test
	void testMatchClostestMethodArgs() {
		// @formatter:off
		instance.executeSource(
			"""
				import ortus.boxlang.runtime.interop.TestClassOverloadedMethods;
				import java.math.BigDecimal;

				result = TestClassOverloadedMethods.go( 5 );
				result2 = TestClassOverloadedMethods.go( javacast( 'long', 5 ) );
				result3 = TestClassOverloadedMethods.go( "5" );
				result4 = TestClassOverloadedMethods.go( javacast( 'char', '5' ) );
				result5 = TestClassOverloadedMethods.go( BigDecimal.valueOf( 5 ) );
			""", context);
		// @formatter:on
		assertThat( variables.get( Key.result ) ).isEqualTo( "Integer boxed" );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( "long" );
		assertThat( variables.get( Key.of( "result3" ) ) ).isEqualTo( "String" );
		assertThat( variables.get( Key.of( "result4" ) ) ).isEqualTo( "char" );
		assertThat( variables.get( Key.of( "result5" ) ) ).isEqualTo( "BigDecimal" );
	}

	@Test
	void testNativeTypedArray() {
		// @formatter:off
		instance.executeSource(
			"""
				import java:ortus.boxlang.runtime.scopes.Key;
				import java:ortus.boxlang.runtime.interop.TestTypedArraySubtype;
				import ortus.boxlang.runtime.interop.TestTypedArray;

				myTypedArray = new TestTypedArray();
				types = [];
				[ "foo", "bar" ].each( ( key ) => arrayAppend( types, new TestTypedArraySubtype( Key.of( key ) ) ) );
				myTypedArray.test( "foo",["blah"], types );

			""", context);
		// @formatter:on
	}

	@Test
	void testNativeTypedArray2() {
		// @formatter:off
		instance.executeSource(
			"""
			import java:org.apache.commons.lang3.ArrayFill;

			result = ArrayFill.fill( [], "f" castas char )

			result2 = ArrayFill.fill( [ "a", "b", "c" ], "f" castas char )

			""", context);
		// @formatter:on
		assertThat( variables.get( Key.result ) ).isNotNull();
		assertThat( variables.get( Key.result ) ).isInstanceOf( char[].class );
		assertThat( ( char[] ) variables.get( Key.result ) ).isEqualTo( new char[] {} );

		assertThat( variables.get( Key.of( "result2" ) ) ).isNotNull();
		assertThat( variables.get( Key.of( "result2" ) ) ).isInstanceOf( char[].class );
		assertThat( ( char[] ) variables.get( Key.of( "result2" ) ) ).isEqualTo( new char[] { 'f', 'f', 'f' } );
	}

	@Test
	void testEmptyVarArgs() {
		// @formatter:off
		instance.executeSource(
			"""
			import java.lang.String as str;
			result = str.format("Hello, world!", []);
			result2 = str.format("Goodbye, world!");
			result3 = str.format("Hello, %s!", ["brad"]);

			""", context);
		// @formatter:on
		assertThat( variables.get( Key.result ) ).isEqualTo( "Hello, world!" );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( "Goodbye, world!" );
		assertThat( variables.get( Key.of( "result3" ) ) ).isEqualTo( "Hello, brad!" );
	}

	@Test
	void testAmbigiuousEmptyVarArgs() {
		// @formatter:off
		instance.executeSource(
			"""
				import ortus.boxlang.runtime.interop.TestAmbiguousVarargs;
				result = TestAmbiguousVarargs.foo( 5, "brad", [] );
				result2 = TestAmbiguousVarargs.foo( 5, "brad" );

			""", context);
		// @formatter:on
		assertThat( variables.get( Key.result ) ).isEqualTo( "Varargs method" );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( "Non-varargs method" );
	}

	@Test
	void testEmptyJavaNamedArgs() {
		// @formatter:off
		instance.executeSource(
			"""
				result = invoke( createObject( "java", "java.util.HashMap" ), "size" );
			""", context);
		// @formatter:on
		assertThat( variables.get( Key.result ) ).isEqualTo( 0 );
	}

	@DisplayName( "It can correctly coerce a DateTime object to java.util.Date" )
	@Test
	void testDateTimeCoercionToDate() {
		// @formatter:off
		instance.executeSource(
			"""
			UtilTimeZone = createObject("java", "java.util.TimeZone")
			// sun.util.calendar.ZoneInfo
			TimeZone = UtilTimeZone.getTimeZone( "America/Los_Angeles" );
			summerDate = parseDateTime( "2025-07-01T00:00:00-06:00" )
			inDayLightTime = TimeZone.inDaylightTime( summerDate );
			result = inDayLightTime;
			""", context);
		// @formatter:on
		assertThat( variables.get( Key.result ) ).isEqualTo( true );
	}

	@DisplayName( "It can correctly coerce a DateTime object to java.sql.Timestamp" )
	@Test
	void testDateTimeCoercionToTimestamp() {
		// @formatter:off
		instance.executeSource(
			"""
			pastDate = parseDateTime( "2025-07-01T00:00:00-06:00" );
			timestamp = createObject("java", "java.sql.Timestamp");
			pastTimestamp = timestamp.valueOf( pastDate.getWrapped().toLocalDateTime() );
			result = pastTimestamp.before( now() );
			""", context);
		// @formatter:on
		assertThat( variables.get( Key.result ) ).isEqualTo( true );
	}

	@DisplayName( "It can correctly coerce a DateTime object to java.sql.Time and java.time.LocalTime" )
	@Test
	void testDateTimeCoercionToTime() {
		// @formatter:off
		instance.executeSource(
			"""
			import java.sql.Time;
			dateOne = parseDateTime( "1970-01-01T12:00:00Z" );
			dateTwo = parseDateTime( "1970-01-01T23:00:00Z" );
			timeValue = Time.valueOf( dateOne );
			result = timeValue.before( dateTwo );
			""", context);
		// @formatter:on
		assertThat( variables.get( Key.result ) ).isEqualTo( true );
	}

	@Test
	void testParseFormatCoercion() {
		// @formatter:off
		instance.executeSource(
			"""
				formatter = createObject( "java", "java.text.SimpleDateFormat" ).init(
					"yyyy-MM-dd'T'HH:mm:ssXXX"
				);
				result = formatter.format( parseDateTime( "2025-07-01T00:00:00-06:00" ) );
				println( result )
			""", context);
		// @formatter:on
	}

	@Test
	void testCatchProxyReturningNullWhichRequiredPrimitive() {
		// BoxCastException: Proxied method [ applyAsInt() ] returned null, but the interface method signature requires a primitive type [ int ] which cannot be null.
		// @formatter:off
		Throwable t = assertThrows( BoxCastException.class, ()->instance.executeSource(
			"""
			import java.util.stream.IntStream;

			IntStream.range( 1, 3 ).map( ::println ).toArray()
			""", context) );
		// @formatter:on
		assertThat( t.getMessage() ).contains(
		    "Proxied method [applyAsInt()] returned null, but the interface method signature requires a primitive type [int] which cannot be null." );
	}

	@Test
	void testIgnoreProxyReturningNullWhichRequiresVoid() {
		// @formatter:off
		instance.executeSource(
			"""
			import java.util.stream.IntStream;

			// len() returns a value, but we'll just ignore it since forEach() expects an IntConsumer, which has a void return
			IntStream.range( 1, 3 ).forEach( ::len )
			""", context);
		// @formatter:on
	}

	@Test
	void testCastWhenSettingField() {
		// @formatter:off
		instance.executeSource(
			"""
			import java:ortus.boxlang.runtime.context.BaseBoxContext;

			BaseBoxContext.nullIsUndefined = "false";
			""", context);
		// @formatter:on

		// @formatter:off
		Throwable t = assertThrows( BoxRuntimeException.class, ()->instance.executeSource(
			"""
			import java:ortus.boxlang.runtime.context.BaseBoxContext;

			BaseBoxContext.nullIsUndefined = {};
			""", context));
		// @formatter:on
		assertThat( t.getMessage() ).contains( "Struct" );
		assertThat( t.getMessage() ).contains( "boolean" );

		// @formatter:off
		t = assertThrows( BoxRuntimeException.class, ()->instance.executeSource(
			"""
			import java:ortus.boxlang.runtime.context.BaseBoxContext;

			BaseBoxContext.nullIsUndefined = null;
			""", context));
		// @formatter:on
		assertThat( t.getMessage() ).contains( "primitive" );
		assertThat( t.getMessage() ).contains( "boolean" );
	}

	@Test
	void testGetterAsField() {
		// @formatter:off
		instance.executeSource(
			"""
			import java.util.UUID;
				result = UUID.randomUUID().mostSignificantBits;
			""", context);
		// @formatter:on
	}

	@DisplayName( "Passing java class to method expecting instance should auto-instantiate via no-arg constructor" )
	@Test
	// @Disabled
	void testPassingJavaClassAutoInstantiatesNoArgConstructor() {
		// @formatter:off
		instance.executeSource(
			"""

			sdf = createObject( "java", "java.text.SimpleDateFormat" ).init();

			// This java method accepts a NumberFormat which perfect for the example since we can't do a direct class name
			// comparison, but actually have to check assignability
			sdf.setNumberFormat( createObject( "java", "java.text.DecimalFormat" ) );

			result = sdf.getNumberFormat();
			""", context);
		// @formatter:on

		var result = variables.get( Key.result );
		assertThat( result ).isNotNull();
		assertThat( DynamicObject.unWrap( result ) ).isInstanceOf( java.text.DecimalFormat.class );
	}

	@DisplayName( "Passing non-String to method expecting String should auto-call toString()" )
	@Test
	@Disabled( "https://ortussolutions.atlassian.net/browse/BL-2092" )
	void testPassingNonStringAutoCallsToString() {
		// @formatter:off
		instance.executeSource(
			"""
			myCRC = createObject( "java", "java.util.zip.CRC32" ).init();
			println( myCRC )
			println( myCRC.toString() )
			createObject( "java", "java.lang.System" ).setProperty( myCRC, "brad" );
			result = createObject( "java", "java.lang.System" ).getProperty( myCRC );
			""", context);
		// @formatter:on

		var result = variables.get( Key.result );
		assertThat( result ).isEqualTo( "brad" );
	}

}
