component {
    static function whatever( function f ) {
        return f();
    }

	function foo( ) {
		return src.test.java.TestCases.phase3.WeirdParse::whatever(
			f = () => {
                //
                // weird parse or something
                //
                return 42;
			}
		)
	}

}