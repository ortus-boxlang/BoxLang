
component accessors="true" {
	property name="name";
	property name="age";
	property name="supervisor";

	function setAge( numeric age ) {
		this.age = age;
	}

	function getAge() {
		return this.age;
	}

	function setSupervisor( String supervisor ) {
		variables.supervisor = arguments.supervisor.reverse();
	}

}