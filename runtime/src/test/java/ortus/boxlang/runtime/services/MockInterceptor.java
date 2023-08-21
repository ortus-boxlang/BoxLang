package ortus.boxlang.runtime.services;

import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Struct;

public class MockInterceptor {

	public MockInterceptor() {
	}

	public void onRequestStart( Struct data ) {
		System.out.println( "Executed mock interceptor" );

		// Check incoming counter
		Key counterKey = new Key( "counter" );
		// Add to it
		if ( data.containsKey( counterKey ) ) {
			int counter = ( int ) data.get( counterKey ) + 1;
			data.put( counterKey, counter );
		}
	}

}
