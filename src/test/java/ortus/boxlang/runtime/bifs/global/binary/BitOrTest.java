package ortus.boxlang.runtime.bifs.global.binary;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class BitOrTest {

	static BoxRuntime	instance;
	static IBoxContext	context;
	static IScope		variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance	= BoxRuntime.getInstance( true );
		context		= new ScriptingBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@AfterAll
	public static void teardown() {
		instance.shutdown();
	}

	@BeforeEach
	public void setupEach() {
		variables.clear();
	}

	@DisplayName( "Bitwise OR operation with positive integers" )
	@Test
	public void testBitwiseOrWithPositiveIntegers() {
		instance.executeSource( "result = bitOr(5, 3);", context );
		assertThat( variables.get( result ) ).isEqualTo( 7 );
	}

	@DisplayName( "Bitwise OR operation with negative integers" )
	@Test
	public void testBitwiseOrWithNegativeIntegers() {
		instance.executeSource( "result = bitOr(-5, -3);", context );
		assertThat( variables.get( result ) ).isEqualTo( -1 );
	}

	@DisplayName( "Bitwise OR operation with zero" )
	@Test
	public void testBitwiseOrWithZero() {
		instance.executeSource( "result = bitOr(0, 10);", context );
		assertThat( variables.get( result ) ).isEqualTo( 10 );
	}

	@DisplayName( "Bitwise OR operation with large integers" )
	@Test
	public void testBitwiseOrWithLargeIntegers() {
		instance.executeSource( "result = bitOr(123456789, 987654321);", context );
		assertThat( variables.get( result ) ).isEqualTo( 1071639989 );
	}
}
