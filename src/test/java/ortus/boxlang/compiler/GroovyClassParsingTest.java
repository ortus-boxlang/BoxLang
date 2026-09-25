/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.compiler;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.ast.BoxClass;
import ortus.boxlang.compiler.ast.BoxScript;
import ortus.boxlang.compiler.ast.statement.BoxFunctionDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxLocalClass;
import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.compiler.parser.Parser;
import ortus.boxlang.compiler.parser.ParsingResult;

/**
 * Phase 2 class-file AST-shape tests for the Groovy parser/transpiler effort.
 * <p>
 * Groovy "class files" (source containing one or more top-level class/interface/trait
 * declarations and nothing else) build a {@code BoxClass} root, mirroring how CFParser handles
 * .cfc files - see testTwoTopLevelClassesOnlyBuildsClassRoot for the 2+-class shape.
 * Unlike {@code GroovyExecutionTest}, these tests check the AST shape rather than running the
 * class through the runtime - {@code executeSource} explicitly rejects class-shaped compiled
 * output, and full class instantiation (ClassLocator wiring, module resolution) is out of
 * scope for Phase 2. This still gives real coverage: it proves class/field/method/constructor
 * declarations parse into the correct BoxLang AST node types and shapes.
 */
public class GroovyClassParsingTest {

	private ParsingResult parseClass( String source ) throws IOException {
		return new Parser().parse( source, BoxSourceType.GROOVYSCRIPT, true );
	}

	@Test
	@DisplayName( "a single top-level class builds a BoxClass root" )
	public void testSimpleClass() throws IOException {
		ParsingResult result = parseClass( """
		                                   class Greeter {
		                                     String prefix = "Hi"
		                                     def greet(String name) {
		                                       return "${prefix}, ${name}!"
		                                     }
		                                   }
		                                   """ );

		assertThat( result.isCorrect() ).isTrue();
		assertThat( result.getRoot() ).isInstanceOf( BoxClass.class );
		BoxClass boxClass = ( BoxClass ) result.getRoot();
		// one field-initializing statement + synthesized getPrefix/setPrefix accessors + one
		// user-declared method (see GroovyVisitor.buildFieldAccessors)
		assertThat( boxClass.getBody() ).hasSize( 4 );
		boolean hasGreetMethod = boxClass.getBody().stream()
		    .anyMatch( stmt -> stmt instanceof BoxFunctionDeclaration fn && fn.getName().equals( "greet" ) );
		assertThat( hasGreetMethod ).isTrue();
		boolean hasGetPrefix = boxClass.getBody().stream()
		    .anyMatch( stmt -> stmt instanceof BoxFunctionDeclaration fn && fn.getName().equals( "getPrefix" ) );
		assertThat( hasGetPrefix ).isTrue();
		boolean hasSetPrefix = boxClass.getBody().stream()
		    .anyMatch( stmt -> stmt instanceof BoxFunctionDeclaration fn && fn.getName().equals( "setPrefix" ) );
		assertThat( hasSetPrefix ).isTrue();
	}

	@Test
	@DisplayName( "a class with an explicit constructor maps it to an 'init' method" )
	public void testConstructorMapsToInit() throws IOException {
		ParsingResult result = parseClass( """
		                                   class Point {
		                                     def x
		                                     def y
		                                     Point(a, b) {
		                                       x = a
		                                       y = b
		                                     }
		                                   }
		                                   """ );

		assertThat( result.isCorrect() ).isTrue();
		BoxClass	boxClass	= ( BoxClass ) result.getRoot();
		boolean		hasInit		= boxClass.getBody().stream()
		    .anyMatch( stmt -> stmt instanceof BoxFunctionDeclaration fn && fn.getName().equals( "init" ) );
		assertThat( hasInit ).isTrue();
	}

	@Test
	@DisplayName( "an interface with a bodyless method builds a BoxClass with a null-body BoxFunctionDeclaration" )
	public void testInterfaceBodylessMethod() throws IOException {
		ParsingResult result = parseClass( """
		                                   interface Shape {
		                                     def area()
		                                   }
		                                   """ );

		assertThat( result.isCorrect() ).isTrue();
		BoxClass				boxClass	= ( BoxClass ) result.getRoot();
		BoxFunctionDeclaration	areaMethod	= ( BoxFunctionDeclaration ) boxClass.getBody().get( 0 );
		assertThat( areaMethod.getName() ).isEqualTo( "area" );
		assertThat( areaMethod.getBody() ).isNull();
	}

	@Test
	@DisplayName( "mixing a top-level class with script statements builds a BoxScript with a BoxLocalClass peer" )
	public void testMixedScriptAndClassBuildsScriptWithLocalClass() throws IOException {
		// A file with a class declaration ALONGSIDE other top-level statements is no longer a
		// "class file" (that shape is reserved for a source that is ENTIRELY a single class
		// declaration - see testSimpleClass) - it's a script, and the class declaration becomes a
		// BoxLocalClass peer statement in it, the same AST shape a named nested class or a hoisted
		// anonymous class already produce.
		ParsingResult result = parseClass( """
		                                   def x = 1
		                                   class Foo {}
		                                   """ );

		assertThat( result.isCorrect() ).isTrue();
		BoxScript boxScript = ( BoxScript ) result.getRoot();
		assertThat( boxScript.getStatements().stream().anyMatch( s -> s instanceof BoxLocalClass localClass
		    && localClass.getName().getName().equals( "Foo" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "two top-level classes and nothing else still build a BoxClass root, not a script" )
	public void testTwoTopLevelClassesOnlyBuildsClassRoot() throws IOException {
		// A file with 2+ top-level classes and NO other statements is still entirely "a class
		// file" (unlike testMixedScriptAndClassBuildsScriptWithLocalClass, there's no script
		// statement here at all) - the textually first class ("Greeter") becomes the BoxClass
		// root itself, and the second ("Farewell") becomes a BoxLocalClass peer in its own body,
		// the exact same shape a class nested inside another class's body already produces.
		// Previously this fell through to the script path instead, silently defining both classes
		// as script-local symbols with no statement left in that (otherwise-empty) script to ever
		// reach either of them.
		ParsingResult result = parseClass( """
		                                   class Greeter {
		                                     def greet() { return "hi" }
		                                   }
		                                   class Farewell {
		                                     def bye() { return "bye" }
		                                   }
		                                   """ );

		assertThat( result.isCorrect() ).isTrue();
		BoxClass	boxClass		= ( BoxClass ) result.getRoot();
		boolean		hasGreetMethod	= boxClass.getBody().stream()
		    .anyMatch( stmt -> stmt instanceof BoxFunctionDeclaration fn && fn.getName().equals( "greet" ) );
		assertThat( hasGreetMethod ).isTrue();
		assertThat( boxClass.getBody().stream().anyMatch( s -> s instanceof BoxLocalClass localClass
		    && localClass.getName().getName().equals( "Farewell" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "'extends'/'implements' are wired into the class's annotations, not silently dropped" )
	public void testExtendsImplementsBuildAnnotations() throws IOException {
		ParsingResult result = parseClass( """
		                                   class Dog extends Animal implements Runnable, Comparable {
		                                     def bark() {}
		                                   }
		                                   """ );

		assertThat( result.isCorrect() ).isTrue();
		BoxClass	boxClass			= ( BoxClass ) result.getRoot();

		// BoxClassTransformer (asmboxpiler) resolves inheritance by looking for annotations
		// literally named "extends"/"implements" with a BoxStringLiteral value - the same
		// convention CF's `component extends="Foo" implements="IBar,IBaz"` attribute uses.
		var			extendsAnnotation	= boxClass.getAnnotations().stream()
		    .filter( a -> a.getKey().getValue().equalsIgnoreCase( "extends" ) )
		    .findFirst();
		assertThat( extendsAnnotation.isPresent() ).isTrue();
		assertThat( ( ( ortus.boxlang.compiler.ast.expression.BoxStringLiteral ) extendsAnnotation.get().getValue() ).getValue() )
		    .isEqualTo( "Animal" );

		var implementsAnnotation = boxClass.getAnnotations().stream()
		    .filter( a -> a.getKey().getValue().equalsIgnoreCase( "implements" ) )
		    .findFirst();
		assertThat( implementsAnnotation.isPresent() ).isTrue();
		assertThat( ( ( ortus.boxlang.compiler.ast.expression.BoxStringLiteral ) implementsAnnotation.get().getValue() ).getValue() )
		    .isEqualTo( "Runnable,Comparable" );
	}

	@Test
	@DisplayName( "'abstract class' is wired into the class's annotations, not silently dropped" )
	public void testAbstractClassBuildsAnnotation() throws IOException {
		ParsingResult result = parseClass( """
		                                   abstract class Shape {
		                                     def area()
		                                   }
		                                   """ );

		assertThat( result.isCorrect() ).isTrue();
		BoxClass	boxClass	= ( BoxClass ) result.getRoot();
		boolean		hasAbstract	= boxClass.getAnnotations().stream()
		    .anyMatch( a -> a.getKey().getValue().equalsIgnoreCase( "abstract" ) );
		assertThat( hasAbstract ).isTrue();
	}

}
