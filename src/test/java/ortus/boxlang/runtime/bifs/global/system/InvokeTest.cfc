component accessors="true"{

	property name="baz" default="";

	function foo() {
		return "bar";
	}

	function runInvokeOnVariablesScope() {
		return invoke( variables, "foo", [] )
	}

}