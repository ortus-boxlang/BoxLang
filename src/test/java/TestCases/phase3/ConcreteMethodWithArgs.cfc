component extends="AbstractMethodWithArgs" {

	function getAggregates( required struct args ) {
		return args;
	}

	function getRawAggregates( struct options ) {
		return options ?: {};
	}

	function addMetadata( required string key, required any value, boolean overwrite = false ) {
		return true;
	}

	function getKeys() {
		return [];
	}

}
