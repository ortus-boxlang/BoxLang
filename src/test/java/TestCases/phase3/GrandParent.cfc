component accessors="true"{

	property name="grandpa";

    function init() {
		variables.grandpa = "me";
		return this;
    }

	function grandParentFunction(){
		return "grandParentFunction";
	}

}
