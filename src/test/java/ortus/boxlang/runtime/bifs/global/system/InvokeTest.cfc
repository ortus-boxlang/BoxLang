component {

	function foo() {
		return "bar";
	}

	function runInvokeOnVariablesScope() {
		return invoke( variables, "foo", [] )
	}

}