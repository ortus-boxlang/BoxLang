component {
	static {
		static.scoped = "brad";
		unscoped = "wood"
		static.foo = 9000; 
	}

	static.foo = 42; 

	static {
		static.again = "luis"
	}

	static function myStaticFunc() {
		return "static" & static.foo;
	}

	function myInstanceFunc() {
		return "instance" & myStaticFunc();
	}

}