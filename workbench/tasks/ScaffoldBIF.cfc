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
			.addMapping( '/bifsDirectory',	"#variables.root#/src/main/java/ortus/boxlang/runtime/bifs/global" )
			.addMapping( '/bifTestsDirectory', "#variables.root#/src/test/java/ortus/boxlang/runtime/bifs/global" );

	}

	function run(
		required string name,
		required string package,
		string memberClass,
		any aliases = [],
		boolean withTests = true
	) {

		var bifTargetDirectory = expandPath( "/bifsDirectory/#lcase( package )#" );
		if( !directoryExists( bifTargetDirectory ) ){
			directoryCreate( bifTargetDirectory );
		}
		if( arguments.withTests ){
			var testTargetDirectory = expandPath( "/bifTestsDirectory/#lcase( package )#" );
			if( !directoryExists( testTargetDirectory ) ){
				directoryCreate( testTargetDirectory );
			}
		}

		if( listLen( package, "." ) == 1 ){
			arguments.package = "ortus.boxlang.runtime.bifs.global." & package;
		}

		if( isSimpleValue( arguments.aliases ) ){
			arguments.aliases = listToArray( arguments.aliases );
		}

		fileWrite( bifTargetDirectory & "/#name#.java", getBIFBody( name, package, memberClass ?: nullValue(), aliases ) );

		if( arguments.withTests ){
			fileWrite( testTargetDirectory & "/#name#Test.java", getBIFTestBody( name, package ) );
		}

		command( "!gradle spotlessApply" ).run( echo=true );

	}

	function getHeader(){
		return fileRead( "#variables.root#/workbench/CodeHeader.txt" );
	}

	function toTitleCase( required string str ){
		return REReplaceNoCase(arguments.str, "\b(\w)(\w{0,})\b", "\U\1\L\2", "all");
	}

	function getBIFBody(
		required string bifName,
		required package,
		string memberClass,
		array aliases = []
	){

		var body = "
			#getHeader()#
			package #package#;

			import ortus.boxlang.runtime.bifs.BIF;
			import ortus.boxlang.runtime.bifs.BoxBIF;
			import ortus.boxlang.runtime.bifs.BoxMember;
			import ortus.boxlang.runtime.context.IBoxContext;
			import ortus.boxlang.runtime.scopes.ArgumentsScope;
			import ortus.boxlang.runtime.scopes.Key;
			import ortus.boxlang.runtime.types.Argument;
			import ortus.boxlang.runtime.types.BoxLangType;
		";

		if( !isNull( memberClass ) ){
			body &= "import #memberClass#;
			";
		}

		body &="@BoxBIF
		";

		arguments.aliases.each( ( alias ) => {
			body &='@BoxBIF( alias = "#alias#")
			';
		});

		if( !isNull( memberClass ) ){
			body &='@BoxMember( type = BoxLangType.#ucase( listLast( memberClass, "." ) )# )
			';
		}

		body &='
		public class #bifName# extends BIF {

			/**
			 * Constructor
			 */
			public #bifName#() {
				super();
				// Uncomment and define declare argument to this BIF
				// declaredArguments = new Argument[] {
				// 	new Argument( true, "numeric", Key.number1 ),
				// 	new Argument( true, "numeric", Key.number2 )
				// };
			}

			/**
			 * Describe what the invocation of your bif function does
			 *
			 * @param context   The context in which the BIF is being invoked.
			 * @param arguments Argument scope for the BIF.
			 *
			 * @argument.foo Describe any expected arguments
			 */
			public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
				// Replace this example function body with your own implementation;
				//#listLast( ( memberClass ?: "Foo"), "." )# actualObj = arguments.get( Key.foo );
				//return actualObj.foo( arguments.get( Key.bar ) );
				return null;
			}

		}
		';

		return body;

	}

	function getBIFTestBody(
		required string bifName,
		required package
	){

		return '
		#getHeader()#
		package #package#;

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

		public class #bifName#Test {

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

			@DisplayName( "It tests the BIF #bifName#" )
			@Test
			public void testBif() {
				// Remove use the following examples to create a test for your member function
				// Full source execution:
				// instance.executeSource(
				// 	"""
				// 	myObj="foo";
				// 	result = #bifName#(arr);
				// 	""",
				// 	context );
				// assertThat( variables.get( result ) ).isEqualTo( "foo" );

				// Statement execution only and return the result:
				// assertThat( ( Boolean ) instance.executeStatement( "#bifName#( '' +  "foo" +'' )" ) ).isTrue();

			}

			@DisplayName( "It tests the member function for #bifName#" )
			@Test
			public void testMemberFunction() {
				// Remove use the following examples to create a test for your member function
				// Full source execution:
				// instance.executeSource(
				// 	"""
				// 	myObj="foo";
				// 	result = myObj.#bifName#();
				// 	""",
				// 	context );
				// assertThat( variables.get( result ) ).isEqualTo( "foo" );

				// Statement execution only and return the result:
				// assertThat( ( Boolean ) instance.executeStatement( "  '' +  "foo" +''.#bifName#()" ) ).isTrue();
			}

		}
		';

	}
  }