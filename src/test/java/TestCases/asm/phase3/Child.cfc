component extends="Parent" {

    function init() {
        return super.init();
    }

    private void function setupFrameworkDefaults() {
        request.calls.append( "running child setupFrameworkDefaults()" );
        super.setupFrameworkDefaults();
    }

}