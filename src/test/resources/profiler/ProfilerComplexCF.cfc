component accessors=true extends="ProfilerSuper" implements="ProfilerInterfaceCF" {

	// SKIPPED: super already set `threshold` into the variables scope.
	property name="threshold" default=complexSeed;
	// RUNS: no preset exists, so defaultProperties() applies this default.
	property name="other" default=complexSeed;

	static {
		complexSeed = 42;
		staticInitRan = true;
	}

	instanceInit = 0;

	function doubleIt( n ) {
		return n * 2;
	}

	function member( x ) {
		sleep( 100 );
		return x * 3;
	}

	// Implement the interface's abstract method so the CFC can instantiate.
	function abstractOnly() {
		return "abstract implemented";
	}
}
