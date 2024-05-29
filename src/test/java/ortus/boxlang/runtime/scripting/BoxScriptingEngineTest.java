package ortus.boxlang.runtime.scripting;

import static com.google.common.truth.Truth.assertThat;

import java.io.PrintStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.script.Bindings;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.apache.commons.io.output.ByteArrayOutputStream;
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
	public void testMethodCall() throws ScriptException, NoSuchMethodException {
		engine.eval( "myStr = { foo : 'bar' }" );
		Invocable	invocable	= ( Invocable ) engine;
		Object		result		= invocable.invokeMethod( engine.get( "myStr" ), "count" );
		assertThat( result ).isEqualTo( 1 );
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
		engine.eval( "function testFunction( name ) { return 'Hello, ' & name & '!' }" );
		Invocable	invocable	= ( Invocable ) engine;
		Object		result		= invocable.invokeFunction( "testFunction", "World" );
		assertThat( result ).isEqualTo( "Hello, World!" );
	}

	@Test
	public void testClosureCall() throws ScriptException, NoSuchMethodException {
		engine.eval( "testFunc = ( name ) => { return 'Hello, ' & name & '!' }" );
		Invocable	invocable	= ( Invocable ) engine;
		Object		result		= invocable.invokeFunction( "testFunc", "World" );
		assertThat( result ).isEqualTo( "Hello, World!" );
	}

	@Test
	public void testLambdaCall() throws ScriptException, NoSuchMethodException {
		engine.eval( "testFunc = ( name ) -> { return 'Hello, ' & name & '!' }" );
		Invocable	invocable	= ( Invocable ) engine;
		Object		result		= invocable.invokeFunction( "testFunc", "World" );
		assertThat( result ).isEqualTo( "Hello, World!" );
	}

	@DisplayName( "Eval a script with no bindings" )
	@Test
	public void testEval() throws ScriptException {
		Object result = engine
		    .eval( "2 + 2" );
		assertThat( result ).isEqualTo( 4 );
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

	@DisplayName( "Output to out buffer" )
	@Test
	public void testOutput() throws ScriptException, UnsupportedEncodingException {
		PrintStream				oldPS			= engine.getBoxContext().getOut();
		// Create a new PrintStream to spy on the output
		ByteArrayOutputStream	baos			= new ByteArrayOutputStream();
		PrintStream				spyPrintStream	= new PrintStream( baos );
		engine.getBoxContext().setOut( spyPrintStream );

		// @formatter:off
		engine.eval( """
			   println( 'Hello, world!' )
			   """ );
		// @formatter:on

		assertThat( baos.toString( StandardCharsets.UTF_8.name() ).trim() ).isEqualTo( "Hello, world!" );

		engine.getBoxContext().setOut( oldPS );
	}

	@DisplayName( "Output Writer" )
	@Test
	public void testOutputWithWriter() throws ScriptException {
		Writer			oldWriter		= engine.getContext().getWriter();
		StringWriter	stringWriter	= new StringWriter();
		engine.getContext().setWriter( stringWriter );

		// @formatter:off
		engine.eval("""
			println('Hello, world!')
			""");
		// @formatter:on

		assertThat( stringWriter.toString().trim() ).isEqualTo( "Hello, world!" );
		engine.getContext().setWriter( oldWriter );
	}

	@DisplayName( "Output buffer using Writer" )
	@Test
	public void testOutputBufferWithWriter() throws ScriptException {
		Writer			oldWriter		= engine.getContext().getWriter();

		StringWriter	stringWriter	= new StringWriter();
		engine.getContext().setWriter( stringWriter );

		// @formatter:off
		engine.eval("""
			echo('Hello, world!')
			""");
		// @formatter:on

		assertThat( stringWriter.toString().trim() ).isEqualTo( "Hello, world!" );

		engine.getContext().setWriter( oldWriter );
	}

	@DisplayName( "create interface" )
	@Test
	public void testInterface() throws ScriptException {

		// @formatter:off
		engine.eval("""
			function run() {
				print('Hello, world!');
			}
			""");
		// @formatter:on
		Invocable	invocable	= ( Invocable ) engine;
		Runnable	runnable	= invocable.getInterface( Runnable.class );
		runnable.run();
	}

	@DisplayName( "create interface from object" )
	@Test
	public void testInterfaceFromObject() throws ScriptException {

		// @formatter:off
		engine.eval("""
			methods = {
				run : function() {
					print('Hello, world!');
				}
			}
			""");
		// @formatter:on
		Invocable	invocable	= ( Invocable ) engine;
		Runnable	runnable	= invocable.getInterface( engine.get( "methods" ), Runnable.class );
		runnable.run();
	}

}
