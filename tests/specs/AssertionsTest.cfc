component
	displayName="TestBox xUnit suite using all kinds of assertions"
	labels     ="lucee,cf"
	extends    ="BaseAssertionsTest"
{

	function beforeTests(){
		super.beforeTests();
		addAssertions( {
			isAwesome : function( required expected ){
				return ( arguments.expected == "Luis Majano" ? true : fail( "not luis majano" ) );
			},
			isNotAwesome : function( required expected ){
				return ( arguments.expected == "Luis Majano" ? fail( "luis majano is always awesome" ) : true );
			}
		} );
	}

	function testFloatingPointNumberAddition() displayname="Floating point number addition"{
		var sum = 196.4 + 196.4 + 180.8 + 196.4 + 196.4 + 180.8 + 609.6;
		// sum.toString() outputs: 1756.8000000000002
		debug( toString( sum ) == 1756.8 );
		$assert.isEqual( sum, 1756.8 );
	}

	function testThrows(){
		$assert.throws( function(){
			var hello = invalidFunction();
		} );
	}

	function testNotThrows(){
		$assert.notThrows( function(){
			var hello = 1;
		} );
	}

	function testThrowsDoesNotIgnoreTypeWhenRegexMatches(){
		var message = "exception_message";

		var target = function(){
			throw( type = "actual_type", message = message );
		};

		var assertionFailed = false;

		try {
			$assert.throws(
				target = target,
				type   = "expected_type",
				regex  = message
			); // regex matches the exception message, but type does not
		} catch ( any e ) {
			assertionFailed = true;
		}

		$assert.isTrue(
			assertionFailed,
			"$assert.throws() was expected to fail because the expected type ('excpected_type') did not match the actual type ('actual_type')"
		);
	}

	function testAwesomeCustomAssertion(){
		$assert.isAwesome( "Luis Majano" );
	}

	function testNegatedAwesomeCustomAssertion(){
		$assert.isNotAwesome( "Lui Majan" );
	}

	function testIsEmptyFunctions(){
		$assert.throws( function(){
			$assert.isEmpty( variables.beforeTests );
			$assert.isEmpty( function(){
			} );
		} );
	}

	function testJavaValues(){
		var urlA = createObject( "java", "java.net.URL" ).init( "http://www.luismajano.com" );
		var urlB = createObject( "java", "java.net.URL" ).init( "http://www.luismajano.com" );
		var urlC = createObject( "java", "java.net.URL" ).init( "http://www.ortussolutions.com" );
		$assert.isEqual( urlA, urlB );
		$assert.isNotEqual( urlA, urlC );
	}

}
