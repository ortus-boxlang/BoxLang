component {
    property name="foo" default="bar";

	function foo() {

	}

	function baz() {
	}

	function spyFoo() {
		return variables.foo;
	}

	function spyBaz() {
		return variables.baz;
	}

}