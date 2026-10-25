component accessors=true extends="ProfilerSuper" implements="ProfilerInterfaceCF" {

	// SKIPPED: super preset `threshold`, so this runtime default is never evaluated.
	property name="threshold" default="#static.complexSeed#";
	// RUNS: no preset exists, so defaultProperties() evaluates this default (42).
	property name="other" default="#static.complexSeed#";

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
