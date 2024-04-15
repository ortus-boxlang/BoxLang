package com.ortussolutions.interceptors;

import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.events.BaseInterceptor;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.events.InterceptionPoint;

public class ExampleInterceptor extends BaseInterceptor {

	/**
	 * This method is called by the BoxLang runtime to configure the interceptor
	 * with a Struct of properties
	 *
	 * @param properties The properties to configure the interceptor with (if any)
	 */
	@Override
	public void configure( IStruct properties ) {
		this.properties	= properties;
		this.logger		= LoggerFactory.getLogger( this.getClass() );
	}

	/**
	 * Add your events below with an @interceptionPoint
	 */
	@InterceptionPoint
	public void onApplicationStart( IStruct data ) {
		logger.info( "onApplicationStart" );
	}

}
