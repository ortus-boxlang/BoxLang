component accessors="true" extends="GrandParent" {

	property name="properties" type="struct";

    function init( struct properties = {} ) {
		variables.properties = arguments.properties;
		super.init();
        setupFrameworkDefaults();
        return this;
    }

    private void function setupFrameworkDefaults() {
      request.calls.append( "running parent setupFrameworkDefaults()" );
    }

	function parentFunction(){
		return "parentFunction";
	}

}
