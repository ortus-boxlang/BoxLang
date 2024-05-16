component extends="../../TestCases/phase3/DotExtendsParent" {
	function childUDF() {
		return "childUDF" & super.parentUDF();
	}
}