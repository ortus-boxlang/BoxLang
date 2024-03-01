@output true
component  {

	this.name                   = "My Application";
	this.sessionManagement      = true;

	function onRequestStart( targetPage ) {
		println( "onRequestStart() called for #targetPage#" );
		return true;
	}
	
	@output true
	function onRequest( targetPage ) {
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
