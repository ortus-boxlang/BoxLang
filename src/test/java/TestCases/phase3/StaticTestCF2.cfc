component {
	static {
		static.scoped = "brad";
		unscoped = "wood"
		static foo = 9000; 
		final brad = "wood"
	}

	static.foo = 42; 
	variables.staticBrad = static.brad;
	this.thisStaticBrad = variables.staticBrad;

	static {
		static.again = "luis"
	}

	static function myStaticFunc() {
		return "static" & static.foo;
	}

	function myInstanceFunc() {
		return "instance" & myStaticFunc();
	}

	function getStaticBrad() {
		return staticBrad;
	}

	static function getInstance() {
		return new src.test.java.TestCases.phase3.StaticTestCF2();
	}

}