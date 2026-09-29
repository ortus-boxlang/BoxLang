component accessors=true extends="ProfilerSuper" {

	// COMPLEX property default — evaluated lazily on first instance access.
	property name="threshold" default=complexSeed;

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
}
