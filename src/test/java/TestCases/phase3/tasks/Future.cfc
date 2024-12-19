component {

	property name="name";

	Future function init( string item = "nothing" ){
		variables.name = arguments.item;
		return this;
	}

	function allApply( items ){
		return arguments
			.items
			.map( item => new Future( item ) )
	}
}
