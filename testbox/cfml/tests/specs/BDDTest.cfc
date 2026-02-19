/**
 * My first spec file
 */
component extends="testbox.system.BaseSpec" {

	/**
	 * You can prepare variables here that will be available to all specs in this spec file
	 */
	property testbox;
	property foo;
	property salvador;

	/*********************************** LIFE CYCLE Methods ***********************************/

	/**
	 * Executes BEFORE all suites in this spec file
	 */
	function beforeAll(){
		variables.salvador = 1;
	}

	/**
	 * Executes AFTER all suites in this spec file
	 */
	function afterAll(){
		// do cleanup here
	}

	/*********************************** BDD SUITES ***********************************/

	function run(){
		/**
		 * describe() starts a suite group of spec tests. It is the main BDD construct.
		 * You can also use the aliases: story(), feature(), scenario(), given(), when()
		 * to create fluent chains of human-readable expressions.
		 *
		 * Arguments:
		 *
		 * @title    Required: The title of the suite, Usually how you want to name the desired behavior
		 * @body     Required: A closure that will resemble the tests to execute.
		 * @labels   The list or array of labels this suite group belongs to
		 * @asyncAll If you want to parallelize the execution of the defined specs in this suite group.
		 * @skip     A flag that tells TestBox to skip this suite group from testing if true
		 * @focused  A flag that tells TestBox to only run this suite and no other
		 */
		describe( "A spec", () => {
			/**
			 * --------------------------------------------------------------------------
			 * Runs before each spec in THIS suite group or nested groups
			 * --------------------------------------------------------------------------
			 */
			beforeEach( () => {
				testbox = 0;
				testbox++;
			} );

			/**
			 * --------------------------------------------------------------------------
			 * Runs after each spec in THIS suite group or nested groups
			 * --------------------------------------------------------------------------
			 */
			afterEach( () => {
				foo = 0;
			} );

			/**
			 * it() describes a spec to test. Usually the title is prefixed with the suite name to create an expression.
			 * You can also use the aliases: then(), test() to create fluent chains of human-readable expressions.
			 *
			 * Arguments:
			 *
			 * @title   The title of this spec
			 * @body    The closure that represents the test
			 * @labels  The list or array of labels this spec belongs to
			 * @skip    A flag or a closure that tells TestBox to skip this spec test from testing if true. If this is a closure it must return boolean.
			 * @data    A struct of data you would like to bind into the spec so it can be later passed into the executing body function
			 * @focused A flag that tells TestBox to only run this spec and no other
			 */
			it( "can test for equality", () => {
				expect( testbox ).toBe( 1 );
			} );

			it( "can have more than one expectation to test", () => {
				testbox = testbox * 8;
				// type checks
				expect( testbox ).toBeTypeOf( "numeric" );
				// dynamic type methods
				expect( testbox ).toBeNumeric();
				// delta ranges
				expect( testbox ).toBeCloseTo( expected = 10, delta = 2 );
			} );

			it( "can have negative expectations", () => {
				testbox = testbox * 8;
				// type checks
				expect( testbox ).notToBeTypeOf( "usdate" );
				// dynamic type methods
				expect( testbox ).notToBeArray();
				// delta ranges
				expect( testbox ).notToBeCloseTo( expected = 10, delta = 2 );
			} );

			xit( "can have tests that can be skipped easily like this one by prefixing it with x", () => {
				fail( "xit() this should skip" );
			} );

			it(
				title = "can have tests that execute if the right environment exists (Windows Only)",
				body  = () => {
					expect( server.os.name ).toInclude( "Windows" );
				},
				skip = ( !isWindows() )
			);

			it(
				title = "can have tests that execute if the right environment exists (Mac Only)",
				body  = () => {
					expect( server.os.name ).toInclude( "Mac" );
				},
				skip = ( !isMac() )
			);
		} );
	}

}
