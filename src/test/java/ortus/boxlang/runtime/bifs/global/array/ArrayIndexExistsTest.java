package ortus.boxlang.runtime.bifs.global.array;

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

public class ArrayIndexExistsTest {

    static BoxRuntime  instance;
    static IBoxContext context;
    static IScope      variables;
    static Key         result = new Key( "result" );

    @BeforeAll
    public static void setUp() {
        instance  = BoxRuntime.getInstance( true );
        context   = new ScriptingBoxContext( instance.getRuntimeContext() );
        variables = context.getScopeNearby( VariablesScope.name );
    }

    @AfterAll
    public static void teardown() {
        instance.shutdown();
    }

    @BeforeEach
    public void setupEach() {
        variables.clear();
    }

    @DisplayName( "It should return true if the index has a value" )
    @Test
    public void testHasIndex() {
        instance.executeSource(
            """
            arr = [ 1, 2, 3 ];
            result = ArrayIndexExists( arr, 3 );
            """,
            context );
        Boolean res = ( Boolean ) variables.dereference( result, false );
        assertThat( res ).isEqualTo( true );
    }

    @DisplayName( "It should return false if the index is 0" )
    @Test
    public void testIndexIsZero() {
        instance.executeSource(
            """
            arr = [ 1, 2, 3 ];
            result = result = ArrayIndexExists( arr, 0 );
            """,
            context );
        Boolean res = ( Boolean ) variables.dereference( result, false );
        assertThat( res ).isEqualTo( false );
    }

    @DisplayName( "It should return false if the index is negative" )
    @Test
    public void testIndexIsNegative() {
        instance.executeSource(
            """
            arr = [ 1, 2, 3 ];
            result = ArrayIndexExists( arr, -3 );
            """,
            context );
        Boolean res = ( Boolean ) variables.dereference( result, false );
        assertThat( res ).isEqualTo( false );
    }

    @DisplayName( "It should return false if the index is greater than the length of the array" )
    @Test
    public void testIndexIsGreaterThanLength() {
        instance.executeSource(
            """
            arr = [ 1, 2, 3 ];
            result = ArrayIndexExists( arr, 4 );
            """,
            context );
        Boolean res = ( Boolean ) variables.dereference( result, false );
        assertThat( res ).isEqualTo( false );
    }

    @DisplayName( "It should return false if the index is a null value" )
    @Test
    public void testIndexIsNull() {
        instance.executeSource(
            """
            arr = [ 1, 2, null ];
            result = ArrayIndexExists( arr, 3 );
            """,
            context );
        Boolean res = ( Boolean ) variables.dereference( result, false );
        assertThat( res ).isEqualTo( false );
    }

    @DisplayName( "It should be invocable as a member function" )
    @Test
    public void testMemberInvocation() {
        instance.executeSource(
            """
            arr = [ 1, 2, 3 ];
            result = arr.indexExists( 3 );
            """,
            context );
        Boolean res = ( Boolean ) variables.dereference( result, false );
        assertThat( res ).isEqualTo( true );
    }
}
