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
package ortus.boxlang.compiler.prettyprint;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.compiler.parser.ParsingResult;
import ortus.boxlang.compiler.prettyprint.config.Config;

@DisplayName( "Local Class Formatting Tests" )
public class LocalClassFormattingTest extends PrettyPrintTest {

	@Test
	@DisplayName( "Simple local class is formatted with name and braces" )
	public void testSimpleLocalClass() throws IOException {
		String			source	= """
		                          class Person {
		                              function getName() {
		                                  return "Brad";
		                              }
		                          }
		                          """;
		ParsingResult	result	= parser.parse( source, BoxSourceType.BOXSCRIPT, false );
		assertTrue( result.isCorrect(), "Source should parse correctly" );
		String output = PrettyPrint.prettyPrint( result.getRoot(), new Config() );
		assertTrue( output.contains( "class Person {" ), "Output should contain 'class Person {'. Actual:\n" + output );
		assertTrue( output.contains( "function getName()" ), "Output should contain function declaration. Actual:\n" + output );
	}

	@Test
	@DisplayName( "Local class with properties is formatted correctly" )
	public void testLocalClassWithProperties() throws IOException {
		String			source	= """
		                          class Animal {
		                              property name="species" type="string";
		                              function getSpecies() {
		                                  return variables.species;
		                              }
		                          }
		                          """;
		ParsingResult	result	= parser.parse( source, BoxSourceType.BOXSCRIPT, false );
		assertTrue( result.isCorrect(), "Source should parse correctly" );
		String output = PrettyPrint.prettyPrint( result.getRoot(), new Config() );
		assertTrue( output.contains( "class Animal {" ), "Output should contain 'class Animal {'. Actual:\n" + output );
		assertTrue( output.contains( "property" ), "Output should contain property declaration. Actual:\n" + output );
	}

	@Test
	@DisplayName( "Local class with annotations is formatted correctly" )
	public void testLocalClassWithAnnotations() throws IOException {
		String			source	= """
		                          @singleton
		                          class Config {
		                              function getValue() {
		                                  return 42;
		                              }
		                          }
		                          """;
		ParsingResult	result	= parser.parse( source, BoxSourceType.BOXSCRIPT, false );
		assertTrue( result.isCorrect(), "Source should parse correctly" );
		String output = PrettyPrint.prettyPrint( result.getRoot(), new Config() );
		assertTrue( output.contains( "class Config {" ), "Output should contain 'class Config {'. Actual:\n" + output );
		assertTrue( output.contains( "singleton" ), "Output should contain annotation. Actual:\n" + output );
	}

	@Test
	@DisplayName( "Multiple local classes in same script are formatted" )
	public void testMultipleLocalClasses() throws IOException {
		String			source	= """
		                          class Dog {
		                              function speak() {
		                                  return "woof";
		                              }
		                          }
		                          class Cat {
		                              function speak() {
		                                  return "meow";
		                              }
		                          }
		                          result = new Dog().speak() & " " & new Cat().speak();
		                          """;
		ParsingResult	result	= parser.parse( source, BoxSourceType.BOXSCRIPT, false );
		assertTrue( result.isCorrect(), "Source should parse correctly" );
		String output = PrettyPrint.prettyPrint( result.getRoot(), new Config() );
		assertTrue( output.contains( "class Dog {" ), "Output should contain 'class Dog {'. Actual:\n" + output );
		assertTrue( output.contains( "class Cat {" ), "Output should contain 'class Cat {'. Actual:\n" + output );
	}

	@Test
	@DisplayName( "Local class formatting is idempotent" )
	public void testLocalClassIdempotent() throws IOException {
		String			source	= """
		                          class Greeter {
		                              function greet( name ) {
		                                  return "Hello, " & name;
		                              }
		                          }
		                          result = new Greeter().greet( "World" );
		                          """;
		Config			config	= new Config();
		ParsingResult	result1	= parser.parse( source, BoxSourceType.BOXSCRIPT, false );
		assertTrue( result1.isCorrect(), "Source should parse correctly" );
		String			output1	= PrettyPrint.prettyPrint( result1.getRoot(), config );

		// Parse the formatted output and format again
		ParsingResult	result2	= parser.parse( output1, BoxSourceType.BOXSCRIPT, false );
		assertTrue( result2.isCorrect(), "Formatted output should parse correctly" );
		String output2 = PrettyPrint.prettyPrint( result2.getRoot(), config );

		assertEqualsIgnoringLineEndings( output1, output2 );
	}

	@Test
	@DisplayName( "Local class with abstract modifier is formatted" )
	public void testAbstractLocalClass() throws IOException {
		String			source	= """
		                          abstract class Shape {
		                              function area() {
		                                  return 0;
		                              }
		                          }
		                          """;
		ParsingResult	result	= parser.parse( source, BoxSourceType.BOXSCRIPT, false );
		assertTrue( result.isCorrect(), "Source should parse correctly" );
		String output = PrettyPrint.prettyPrint( result.getRoot(), new Config() );
		assertTrue( output.contains( "class Shape {" ), "Output should contain 'class Shape {'. Actual:\n" + output );
	}
}
