
component output="false" {
	function run() {
		include "/src/test/java/ortus/boxlang/runtime/components/system/testCFOutputSaveContent.cfm";
		return isDefined("result") ? result : "";
	}
}