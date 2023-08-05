package ortus.boxlang.runtime.interop;

import ortus.boxlang.runtime.types.IType;

import java.lang.String;
import java.util.LinkedHashMap;

import org.junit.jupiter.api.Test;
import static com.google.common.truth.Truth.assertThat;

public class ClassInvokerTest {

	@Test
	void testItCanBeCreatedWithAnInstance() {
		ClassInvoker target = new ClassInvoker( this );
		assertThat( target.getTargetClass() ).isEqualTo( this.getClass() );
		assertThat( target.getTargetInstance() ).isEqualTo( this );
		assertThat( target.isInterface() ).isFalse();
	}

	@Test
	void testItCanBeCreatedWithAClass() {
		ClassInvoker target = new ClassInvoker( String.class );
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
	@SuppressWarnings( "unchecked" )
	void testItCanCallConstructorsWithManyArguments() throws Throwable {
		ClassInvoker target = new ClassInvoker( LinkedHashMap.class );
		System.out.println( int.class );
		LinkedHashMap results = ( LinkedHashMap ) target.invokeConstructor( 16, 0.75f, true );
		assertThat( results.getClass() ).isEqualTo( LinkedHashMap.class );
	}

	@Test
	void testItCanCallConstructorsWithNoArguments() throws Throwable {
		ClassInvoker	target	= new ClassInvoker( String.class );
		String			results	= ( String ) target.invokeConstructor();
		assertThat( results ).isEqualTo( "" );
		assertThat( results.getClass() ).isEqualTo( String.class );
	}

}
