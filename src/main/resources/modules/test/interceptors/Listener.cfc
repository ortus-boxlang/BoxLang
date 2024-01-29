component{

	function configure(){
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
