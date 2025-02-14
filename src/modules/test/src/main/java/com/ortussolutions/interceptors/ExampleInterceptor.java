package com.ortussolutions.interceptors;

import ortus.boxlang.runtime.events.BaseInterceptor;
import ortus.boxlang.runtime.events.InterceptionPoint;
import ortus.boxlang.runtime.types.IStruct;

public class ExampleInterceptor extends BaseInterceptor {

	/**
	 * This method is called by the BoxLang runtime to configure the interceptor
	 * with a Struct of properties
	 *
	 * @param properties The properties to configure the interceptor with (if any)
	 */
	@Override
	public void configure( IStruct properties ) {
		this.properties = properties;
	}

	/**
	 * Add your events below with an @interceptionPoint
	 */
	@InterceptionPoint
	public void onApplicationStart( IStruct data ) {
		getLogger().info( "onApplicationStart" );
	}

}
