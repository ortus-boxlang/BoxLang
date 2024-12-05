component extends="Parent" {

	property name="properties";

    function init( properties = {} ) {
		variables.properties = arguments.properties;
        return super.init();
    }

    private void function setupFrameworkDefaults() {
        request.calls.append( "running child setupFrameworkDefaults()" );
        super.setupFrameworkDefaults();
    }

}