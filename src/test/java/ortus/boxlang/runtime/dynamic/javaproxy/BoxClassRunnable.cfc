component {

	function run() {
		println( "I ran!" );
		server.runnableProxyFired = true;
	}

	function call(){
		return "I was called!";
	}

	function hashCode(){
		return 42;
	}

}
