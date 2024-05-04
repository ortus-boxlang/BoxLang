component {

    function init() {
        setupFrameworkDefaults();
        return this;
    }

    private void function setupFrameworkDefaults() {
      request.calls.append( "running parent setupFrameworkDefaults()" );
    }

}