package ortus.boxlang.runtime.interop;

import ortus.boxlang.runtime.types.IType;

import java.lang.String;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.time.Duration;

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
		ClassInvoker	target	= new ClassInvoker( String.class );
		String			results	= ( String ) target.invokeConstructor( "Hello World" );
		assertThat( results ).isEqualTo( "Hello World" );
		assertThat( results.getClass() ).isEqualTo( String.class );
	}

	@Test
	void testItCanCallConstructorsWithManyArguments() throws Throwable {
		ClassInvoker target = new ClassInvoker( LinkedHashMap.class );
		System.out.println( int.class );
		LinkedHashMap<?, ?> results = ( LinkedHashMap<?, ?> ) target.invokeConstructor( 16, 0.75f, true );
		assertThat( results.getClass() ).isEqualTo( LinkedHashMap.class );
	}

	@Test
	void testItCanCallConstructorsWithNoArguments() throws Throwable {
		ClassInvoker	target	= new ClassInvoker( String.class );
		String			results	= ( String ) target.invokeConstructor();
		assertThat( results ).isEqualTo( "" );
		assertThat( results.getClass() ).isEqualTo( String.class );
	}

	@Test
	void testItCanCallMethodsWithNoArguments() throws Throwable {
		ClassInvoker myMapInvoker = new ClassInvoker( HashMap.class );
		myMapInvoker.invokeConstructor();
		assertThat( myMapInvoker.invoke( "size" ) ).isEqualTo( 0 );
		assertThat( ( Boolean ) myMapInvoker.invoke( "isEmpty" ) ).isTrue();
	}

	@Test
	void testItCanCallMethodsWithManyArguments() throws Throwable {
		ClassInvoker myMapInvoker = new ClassInvoker( HashMap.class );
		myMapInvoker.invokeConstructor();
		myMapInvoker.invoke( "put", "name", "luis" );
		assertThat( myMapInvoker.invoke( "size" ) ).isEqualTo( 1 );
		assertThat( myMapInvoker.invoke( "get", "name" ) ).isEqualTo( "luis" );
	}

	@Test
	void testItCanCallStaticMethods() throws Throwable {
		ClassInvoker	myInvoker	= ClassInvoker.of( Duration.class );
		Duration		results		= ( Duration ) myInvoker.invokeStatic( "ofSeconds", new Object[] { 120 } );
		assertThat( results.toString() ).isEqualTo( "PT2M" );
	}

}
