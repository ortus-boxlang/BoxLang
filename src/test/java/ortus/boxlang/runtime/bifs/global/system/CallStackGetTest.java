package ortus.boxlang.runtime.bifs.global.system;

import static com.google.common.truth.Truth.assertThat;

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
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;

public class CallStackGetTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@BeforeEach
	void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "It has the correct keys when calling callStackGet" )
	@Test
	void testCallStackGetKeys() {
		// @formatter:off
		instance.executeSource(
		    """
			cs = new src.test.java.TestCases.components.CallStack()
			result = cs.run();
		    """,
		context );
		Array stack = variables.getAsArray( result );
		assertThat( stack.size() ).isEqualTo( 2 );
		IStruct frame = (IStruct) stack.get(0);
		assertThat( frame.containsKey( "Function" ) ).isEqualTo( true );
		assertThat( frame.get( "Function" ) ).isEqualTo( "nested" );
	}

}
