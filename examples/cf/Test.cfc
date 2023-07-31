component accessors="true" {

	function init( required any spec, required any assertions ){
		variables.spec   = arguments.spec;
		variables.assert = arguments.assertions;

		return this;
	}

	function registerMatcher( required name, required body ){
		// store new custom matcher function according to specs
		this[ arguments.name ] = variables[ arguments.name ] = function(){
			// execute custom matcher
			var results = body( this, arguments );
			// if not passed, then fail the custom matcher, else you can concatenate
			return ( !results ? variables.assert.fail( this.message ) : this );
		};
	}

	function fail( message = "", detail = "" ){
		variables.assert.fail( argumentCollection = arguments );
	}
}