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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.time.Duration;

import TestCases.PrivateConstructors;

import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import static com.google.common.truth.Truth.assertThat;

public class ClassInvokerTest {

	@Test
	void testItCanBeCreatedWithAnInstance() {
		ClassInvoker target = ClassInvoker.of( this );
		assertThat( target.getTargetClass() ).isEqualTo( this.getClass() );
		assertThat( target.getTargetInstance() ).isEqualTo( this );
		assertThat( target.isInterface() ).isFalse();
	}

	@Test
	void testItCanBeCreatedWithAClass() {
		ClassInvoker target = ClassInvoker.of( String.class );
		assertThat( target.getTargetClass() ).isEqualTo( String.class );
		assertThat( target.isInterface() ).isFalse();
	}

	@Test
	void testItCanBeCreatedWithAnInterface() {
		ClassInvoker target = new ClassInvoker( IType.class );
		assertThat( target.isInterface() ).isTrue();
	}

	@Test
	void testItCanCallConstructorsWithOneArgument() throws Throwable {
		ClassInvoker target = new ClassInvoker( String.class );
		target.invokeConstructor( "Hello World" );
		assertThat( target.getTargetClass() ).isEqualTo( String.class );
		assertThat( target.getTargetInstance() ).isEqualTo( "Hello World" );
	}

	@Test
	void testItCanCallConstructorsWithManyArguments() throws Throwable {
		ClassInvoker target = new ClassInvoker( LinkedHashMap.class );
		System.out.println( int.class );
		target.invokeConstructor( 16, 0.75f, true );
		assertThat( target.getTargetClass() ).isEqualTo( LinkedHashMap.class );
	}

	@Test
	void testItCanCallConstructorsWithNoArguments() throws Throwable {
		ClassInvoker target = new ClassInvoker( String.class );
		target.invokeConstructor();
		assertThat( target.getTargetClass() ).isEqualTo( String.class );
		assertThat( target.getTargetInstance() ).isEqualTo( "" );
	}

	@Test
	void testItCanCallMethodsWithNoArguments() throws Throwable {
		ClassInvoker myMapInvoker = new ClassInvoker( HashMap.class );
		myMapInvoker.invokeConstructor();
		assertThat( myMapInvoker.invoke( "size" ).get() ).isEqualTo( 0 );
		assertThat( ( Boolean ) myMapInvoker.invoke( "isEmpty" ).get() ).isTrue();
	}

	@Test
	void testItCanCallMethodsWithManyArguments() throws Throwable {
		ClassInvoker myMapInvoker = new ClassInvoker( HashMap.class );
		myMapInvoker.invokeConstructor();
		myMapInvoker.invoke( "put", "name", "luis" );
		assertThat( myMapInvoker.invoke( "size" ).get() ).isEqualTo( 1 );
		assertThat( myMapInvoker.invoke( "get", "name" ).get() ).isEqualTo( "luis" );
	}

	@Test
	void testItCanCallStaticMethods() throws Throwable {
		ClassInvoker	myInvoker	= ClassInvoker.of( Duration.class );
		Duration		results		= ( Duration ) myInvoker.invokeStatic( "ofSeconds", new Object[] { 120 } ).get();
		assertThat( results.toString() ).isEqualTo( "PT2M" );
	}

	@Test
	void testItCanCallMethodsOnInterfaces() throws Throwable {
		ClassInvoker	myInvoker	= ClassInvoker.of( List.class );
		List			results		= ( List ) myInvoker.invokeStatic( "of", new Object[] { "Hello" } ).get();
		assertThat( results.toString() ).isEqualTo( "[Hello]" );
		assertThat( results ).isNotEmpty();
	}

	@Ignore
	@Test
	void testItCanCreateWithPrivateConstructors() throws Throwable {
		ClassInvoker myInvoker = ClassInvoker.of( PrivateConstructors.class );
		assertThat( myInvoker ).isNotNull();
		myInvoker.invoke( "getInstance" );
	}

}
