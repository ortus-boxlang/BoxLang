package ortus.boxlang.runtime.scripting;

import static com.google.common.truth.Truth.assertThat;

import java.util.Map;

import javax.script.Bindings;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;

public class BoxScriptingEngineTest {

	static BoxScriptingEngine engine;

	@BeforeAll
	public static void setUp() {
		engine = new BoxScriptingEngine( new BoxScriptingFactory() );
	}

	@DisplayName( "Can build a new engine" )
	@Test
	public void testEngine() {
		assertThat( engine ).isNotNull();
	}

	@DisplayName( "Can create bindings" )
	@Test
	public void testBindings() {
		Bindings bindings = engine.createBindings();
		assertThat( bindings ).isInstanceOf( SimpleBindings.class );
		assertThat( bindings.size() ).isEqualTo( 0 );
	}

	@DisplayName( "Can create bindings from a map" )
	@Test
	public void testBindingsFromMap() {
		Bindings bindings = engine.creatBindings( Map.of( "name", "World" ) );
		assertThat( bindings ).isInstanceOf( SimpleBindings.class );
		assertThat( bindings.size() ).isEqualTo( 1 );
		assertThat( bindings.get( "name" ) ).isEqualTo( "World" );
	}

	@Test
	public void testFunctionCallWithNoArguments() throws ScriptException, NoSuchMethodException {
		engine.eval( "function testFunction() { return 'Hello, World!' }" );
		Invocable	invocable	= ( Invocable ) engine;
		Object		result		= invocable.invokeFunction( "testFunction" );
		assertThat( result ).isEqualTo( "Hello, World!" );
	}

	@Test
	public void testFunctionCallWithArguments() throws ScriptException, NoSuchMethodException {
		engine.eval( "function testFunction( name ) { return 'Hello, ' & arguments.1 & '!' }" );
		Invocable	invocable	= ( Invocable ) engine;
		Object		result		= invocable.invokeFunction( "testFunction", "World" );
		assertThat( result ).isEqualTo( "Hello, World!" );
	}

	@DisplayName( "Eval a script with no bindings" )
	@Test
	public void testEval() throws ScriptException {
		Object result = engine
		    .eval( "println( 'Hello, World!' )" );
		assertThat( result ).isNull();
	}

	@DisplayName( "Eval a script with bindings" )
	@Test
	public void testEvalWithBindings() throws ScriptException {
		Bindings bindings = engine.createBindings();
		bindings.put( "name", "World" );
		bindings.put( "age", 1 );

		// @formatter:off
		Object result = engine
		    .eval( """
		           println( 'Hello, ' & name & '!' )
				   newAge = age + 1
		           totalAge = newAge + 1
				   request.nameTest = name
				   server.nameTest = name
		           """, bindings );
		// @formatter:on

		var		modifiedBindings	= engine.getBindings();
		// The result is the last expression evaluated
		assertThat( result ).isEqualTo( "World" );
		assertThat( modifiedBindings.get( "newAge" ) ).isEqualTo( 2 );
		assertThat( engine.getRequestBindings().get( "nameTest" ) ).isEqualTo( "World" );
		assertThat( engine.getServerBindings().get( "nameTest" ) ).isEqualTo( "World" );
	}

	@DisplayName( "Compile a script" )
	@Test
	public void testCompile() throws ScriptException {
		// @formatter:off
		CompiledScript script = engine
		    .compile( """
				import ortus.boxlang.runtime.scopes.Key;

				name = [ 'John', 'Doe',  Key.of( 'test' ) ]

				name.reverse()
		    """ );
		// @formatter:on

		// Execute it
		Object			results	= script.eval();

		assertThat( ( Array ) results ).containsExactly( Key.of( "test" ), "Doe", "John" );
	}

}
