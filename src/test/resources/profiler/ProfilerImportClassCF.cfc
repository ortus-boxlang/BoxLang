import "src.test.resources.profiler.ProfilerStaticOnlyCF";
component implements="src.test.resources.profiler.ProfilerImportInterfaceCF" {

	static {
		importedInitRan = true;
	}

	public struct function getQ(required array ids) {
		return {};
	}
}
