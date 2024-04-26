component output=true {

	this.name                   = "My Application";
	this.sessionManagement      = true;

	function onRequestStart( targetPage ) {
		println( "onRequestStart() called for #targetPage#" );
		return true;
	}
	
	function onRequest( targetPage ) output=true {
		println( "onRequest() called for #targetPage#" );
		include "#targetPage#";
	}

	function onApplicationStart() {
		println( "onApplicationStart() called" );
	}

	function onSessionStart() {
		println( "onSessionStart() called" );
	}
}
