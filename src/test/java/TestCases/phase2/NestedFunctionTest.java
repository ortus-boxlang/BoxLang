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
package TestCases.phase2;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import ortus.boxlang.runtime.types.Closure;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class NestedFunctionTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "basic nested function declaration and invocation" )
	@Test
	public void testBasicNestedFunction() {
		instance.executeSource(
		    """
		    function outer() {
		        function inner() {
		            return "from inner";
		        }
		        return inner();
		    }
		    result = outer();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "from inner" );
	}

	@DisplayName( "nested function with arguments" )
	@Test
	public void testNestedFunctionWithArguments() {
		instance.executeSource(
		    """
		    function outer() {
		        function addSomething( var1, var2 ) {
		            var1 &= "inside something";
		            return var1;
		        }
		        var xml = "Outside ";
		        return addSomething( xml, 2 );
		    }
		    result = outer();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "Outside inside something" );
	}

	@DisplayName( "nested function can access parent local variables (closure behavior)" )
	@Test
	public void testNestedFunctionClosureAccess() {
		instance.executeSource(
		    """
		    function outer() {
		        var outerVar = "hello from outer";
		        function inner() {
		            return outerVar;
		        }
		        return inner();
		    }
		    result = outer();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "hello from outer" );
	}

	@DisplayName( "nested function sees updated parent variables" )
	@Test
	public void testNestedFunctionSeesUpdatedVars() {
		instance.executeSource(
		    """
		    function outer() {
		        var x = "initial";
		        function inner() {
		            return x;
		        }
		        x = "updated";
		        return inner();
		    }
		    result = outer();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "updated" );
	}

	@DisplayName( "nested function accessible via local scope" )
	@Test
	public void testNestedFunctionInLocalScope() {
		instance.executeSource(
		    """
		    function outer() {
		        function inner() {
		            return "hi";
		        }
		        return local.inner;
		    }
		    result = outer();
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Closure.class );
	}

	@DisplayName( "nested function is not visible outside parent" )
	@Test
	public void testNestedFunctionNotVisibleOutside() {
		assertThrows( BoxRuntimeException.class, () -> {
			instance.executeSource(
			    """
			    function outer() {
			        function inner() {
			            return "hi";
			        }
			    }
			    outer();
			    inner();
			    """,
			    context );
		} );
	}

	@DisplayName( "multiple nested functions" )
	@Test
	public void testMultipleNestedFunctions() {
		instance.executeSource(
		    """
		    function outer() {
		        function add( a, b ) {
		            return a + b;
		        }
		        function multiply( a, b ) {
		            return a * b;
		        }
		        return add( 3, 4 ) & ":" & multiply( 3, 4 );
		    }
		    result = outer();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "7:12" );
	}

	@DisplayName( "doubly nested functions" )
	@Test
	public void testDoublyNestedFunctions() {
		instance.executeSource(
		    """
		    function outer() {
		        var x = "A";
		        function middle() {
		            var y = "B";
		            function inner() {
		                return x & y;
		            }
		            return inner();
		        }
		        return middle();
		    }
		    result = outer();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "AB" );
	}

	@DisplayName( "each call to parent creates independent inner function" )
	@Test
	public void testIndependentPerInvocation() {
		instance.executeSource(
		    """
		    function makeCounter( start ) {
		        var count = start;
		        function increment() {
		            count++;
		            return count;
		        }
		        return increment();
		    }
		    result = makeCounter( 10 );
		    result2 = makeCounter( 20 );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 11 );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( 21 );
	}

	@DisplayName( "Jira example: function inception" )
	@Test
	public void testJiraExample() {
		instance.executeSource(
		    """
		    function test() {
		        var xml = "Outside ";

		        function addSomething( var1, var2 ) {
		            var1 &= "inside something";
		            return var1;
		        }

		        variables.result2 = local.addSomething;
		        variables.result = addSomething( xml, 2 );
		    }
		    test();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "Outside inside something" );
		assertThat( variables.get( Key.of( "result2" ) ) ).isInstanceOf( Closure.class );
	}

	@DisplayName( "nested function inside closure" )
	@Test
	public void testNestedFunctionInsideClosure() {
		instance.executeSource(
		    """
		    myClosure = function() {
		        var prefix = "Hello ";
		        function greet( name ) {
		            return prefix & name;
		        }
		        return greet( "World" );
		    };
		    result = myClosure();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "Hello World" );
	}

	@DisplayName( "nested function inside arrow function" )
	@Test
	public void testNestedFunctionInsideArrow() {
		instance.executeSource(
		    """
		    myArrow = () => {
		        var prefix = "Hi ";
		        function greet( name ) {
		            return prefix & name;
		        }
		        return greet( "There" );
		    };
		    result = myArrow();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "Hi There" );
	}
}
