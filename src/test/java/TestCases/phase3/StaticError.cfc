component {
	static {
		zzz = (
			() => {
				var pattern = 42; // uncomment for breakage
				return 42;
			}
		)( );
	}

	println(static.zzz)
}