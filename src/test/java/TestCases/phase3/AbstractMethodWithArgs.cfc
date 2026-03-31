/**
 * Test component with abstract methods that have arguments.
 * This tests that the ASM boxpiler correctly pre-registers argument name keys
 * for abstract methods before building the keys array.
 */
component abstract {
	
	property name="myProp" type="any";
	
	/**
	 * Abstract method with a single required argument
	 */
	function getAggregates( required struct args );
	
	/**
	 * Abstract method with an optional argument
	 */
	function getRawAggregates( struct options );
	
	/**
	 * Abstract method with multiple arguments
	 */
	function addMetadata( required string key, required any value, boolean overwrite=false );
	
	/**
	 * Abstract method with no arguments (for comparison)
	 */
	function getKeys();
	
	/**
	 * Concrete method that can be called
	 */
	function init( required any interest ) {
		variables.interest = arguments.interest;
		return this;
	}
}
