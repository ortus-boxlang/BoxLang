component {

	static {
		counter = 1
	}

	static function setDyn( name, value ) {
		static[ name ] = value;
		return static[ name ];
	}

	static function increment( k ) {
		static[ k ] += 1;
		return static[ k ];
	}

	static function destructurePair( data ) {
		static [ first, second ] = data;
		return static.first + static.second;
	}

}
