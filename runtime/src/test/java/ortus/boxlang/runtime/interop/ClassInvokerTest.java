package ortus.boxlang.runtime.interop;

import java.lang.String;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.types.IType;

import static org.junit.jupiter.api.Assertions.*;
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
	}

}
