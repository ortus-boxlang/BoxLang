component accessors="true"{

	property name="foo";

    function init() {
		variables.foo = "bar";
        setupFrameworkDefaults();
        return this;
    }

    private void function setupFrameworkDefaults() {
      request.calls.append( "running parent setupFrameworkDefaults()" );
    }

}