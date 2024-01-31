component{

	function configure(){
		// Create The Hello Model
		var hello = new bxModules.test.models.Hello();
		printLn( hello.sayHello() );
	}

	function afterModuleActivations( data ){
		printLn( "All modules activated" )
	}

	function onRuntimeShutdown( data ){
		printLn( "Runtime has gone down" )
	}

	function onBxTestModule( data ){
		printLn( "BX Test Module" )
	}
}
