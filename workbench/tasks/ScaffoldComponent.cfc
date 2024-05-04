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
 *
 * Scaffolds a new BIF
 */
component {


	function init(){
		variables.util 		= shell.getUtil();
		variables.root 		= getCWD();

		variables.util
			.addMapping( '/componentsDirectory',	"#variables.root#/src/main/java/ortus/boxlang/runtime/components" )
			.addMapping( '/componentsTestsDirectory', "#variables.root#/src/test/java/ortus/boxlang/runtime/components" );

	}

	function run(
		required string name,
		required string package,
		boolean withTests = true
	) {

		var bifTargetDirectory = expandPath( "/componentsDirectory/#lcase( package )#" );
		if( !directoryExists( bifTargetDirectory ) ){
			directoryCreate( bifTargetDirectory );
		}
		if( arguments.withTests ){
			var testTargetDirectory = expandPath( "/componentsTestsDirectory/#lcase( package )#" );
			if( !directoryExists( testTargetDirectory ) ){
				directoryCreate( testTargetDirectory );
			}
		}

		if( listLen( package, "." ) == 1 ){
			arguments.package = "ortus.boxlang.runtime.components." & package;
		}

		fileWrite( bifTargetDirectory & "/#name#.java", getComponentBody( name, package ) );

		if( arguments.withTests ){
			fileWrite( testTargetDirectory & "/#name#Test.java", getComponentTestBody( name, package ) );
		}

	}

	function getHeader(){
		return fileRead( "#variables.root#/workbench/CodeHeader.txt" );
	}

	function toTitleCase( required string str ){
		return REReplaceNoCase(arguments.str, "\b(\w)(\w{0,})\b", "\U\1\L\2", "all");
	}

	function getComponentBody(
		required string componentName,
		required package
	){

		var body = "
			#getHeader()#
			package #package#;

			import ortus.boxlang.runtime.components.Attribute;
			import ortus.boxlang.runtime.components.BoxComponent;
			import ortus.boxlang.runtime.components.Component;
			import ortus.boxlang.runtime.context.IBoxContext;
			import ortus.boxlang.runtime.dynamic.ExpressionInterpreter;
			import ortus.boxlang.runtime.dynamic.casters.StructCaster;
			import ortus.boxlang.runtime.scopes.Key;
			import ortus.boxlang.runtime.types.IStruct;
			import ortus.boxlang.runtime.validation.Validator;
		";

		body &='
		public class #componentName# extends Component {

			/**
			 * Constructor
			 */
			public #componentName#() {
				super();
				// Uncomment and define declare argument to this Component
				//declaredAttributes = new Attribute[] {
				//	new Attribute( Key.variable, "string", Set.of( Validator.REQUIRED, Validator.NON_EMPTY ) ),
				//	new Attribute( Key.of( "name" ), "string", Set.of( Validator.REQUIRED ) ),
				//	new Attribute( Key.arguments, "any" )
				// };
			}

			/**
			 * Describe what the invocation of your component does
			 *
			 * @param context        The context in which the Component is being invoked
			 * @param attributes     The attributes to the Component
			 * @param body           The body of the Component
			 * @param executionState The execution state of the Component
			 *
			 * @attribute.foo Describe any expected arguments
			 */
			public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {
				// Replace this example component function body with your own implementation;
				// Example, passing through to a registered BIF
				//IStruct response = StructCaster.cast( runtime.getFunctionService().getGlobalFunction( Key.#listLast( ( memberClass ?: "Foo"), "." )# ).invoke( context, attributes, false, Key.#listLast( ( memberClass ?: "Foo"), "." )# ) );

				// Set the result(s) back into the page
				// ExpressionInterpreter.setVariable( context, attributes.getAsString( Key.variable ), response.getAsString( Key.output ) );

				return DEFAULT_RETURN;
			}

		}
		';

		return body;

	}

	function getComponentTestBody(
		required string componentName,
		required package
	){

		return '
		#getHeader()#
		package #package#;

		import static org.junit.jupiter.api.Assertions.assertTrue;
		import static com.google.common.truth.Truth.assertThat;

		import org.junit.jupiter.api.AfterAll;
		import org.junit.jupiter.api.BeforeAll;
		import org.junit.jupiter.api.BeforeEach;
		import org.junit.jupiter.api.DisplayName;
		import org.junit.jupiter.api.Test;

		import ortus.boxlang.compiler.parser.BoxSourceType;
		import ortus.boxlang.runtime.BoxRuntime;
		import ortus.boxlang.runtime.context.IBoxContext;
		import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
		import ortus.boxlang.runtime.scopes.IScope;
		import ortus.boxlang.runtime.scopes.Key;
		import ortus.boxlang.runtime.scopes.VariablesScope;

		public class #componentName#Test {

			static BoxRuntime	instance;
			IBoxContext	context;
			IScope		variables;
			static Key			result	= new Key( "result" );

			@BeforeAll
			public static void setUp() {
				instance	= BoxRuntime.getInstance( true );
			}

			@AfterAll
			public static void teardown() {
			}

			@BeforeEach
			public void setupEach() {
				context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
				variables	= context.getScopeNearby( VariablesScope.name );
			}

			@DisplayName( "It tests the BIF #componentName# with CFML parsing" )
			@Test
			public void testComponentCF() {
				instance.executeSource(
					"""
					<cf#componentName# variable="result" name="foo" arguments="bar" />
					""",
					context, BoxSourceType.CFTEMPLATE );

				assertTrue( variables.get( result ) instanceof String );
				assertTrue( variables.getAsString( result ).length() > 0 );
			}

			@DisplayName( "It tests the BIF #componentName# with BoxLang parsing" )
			@Test
			public void testComponentBX() {
				instance.executeSource(
					"""
					<bx:#componentName# variable="result" name="foo" arguments="bar" />
					""",
					context, BoxSourceType.BOXTEMPLATE );

				assertTrue( variables.get( result ) instanceof String );
				assertTrue( variables.getAsString( result ).length() > 0 );
			}

		}
		';

	}
  }