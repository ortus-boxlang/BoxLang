package ortus.boxlang.runtime.bifs.global.binary;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class BitShlnTest {

	static BoxRuntime	instance;
	static IBoxContext	context;
	static IScope		variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance	= BoxRuntime.getInstance( true );
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
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

	@DisplayName( "Bitwise shift-left operation with positive integers" )
	@Test
	public void testBitwiseShlnWithPositiveIntegers() {
		instance.executeSource( "result = bitShln(5, 1);", context );
		assertThat( variables.get( result ) ).isEqualTo( 10 );
	}

	@DisplayName( "Bitwise shift-left operation with negative integers" )
	@Test
	public void testBitwiseShlnWithNegativeIntegers() {
		instance.executeSource( "result = bitShln(-5, 2);", context );
		assertThat( variables.get( result ) ).isEqualTo( -20 );
	}

	@DisplayName( "Bitwise shift-left operation with zero" )
	@Test
	public void testBitwiseShlnWithZero() {
		instance.executeSource( "result = bitShln(0, 5);", context );
		assertThat( variables.get( result ) ).isEqualTo( 0 );
	}

	@DisplayName( "Bitwise shift-left operation with large integers" )
	@Test
	public void testBitwiseShlnWithLargeIntegers() {
		instance.executeSource( "result = bitShln(123456789, 2);", context );
		assertThat( variables.get( result ) ).isEqualTo( 493827156 );
	}
}
