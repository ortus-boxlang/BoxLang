component extends="Parent" {

	// If you un-comment this out, then the ClassTest.superInitTest will pass
    // function init( struct properties = {} ) {
	// 	super.init( argumentCollection = arguments );
    //     return this;
    // }

	function configure(){
		return variables.properties;
	}

    private void function setupFrameworkDefaults() {
        request.calls.append( "running child setupFrameworkDefaults()" );
        super.setupFrameworkDefaults();
    }

	function childFunction(){
		return "childFunction";
	}

}
