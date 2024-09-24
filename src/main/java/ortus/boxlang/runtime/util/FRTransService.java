/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.runtime.util;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.interop.DynamicObject;

public class FRTransService {

	private static FRTransService	instance	= null;
	private boolean					FREnabled	= false;
	private DynamicObject			FRAPI;

	private FRTransService( Boolean enabled ) {
		if ( !enabled ) {
			FREnabled = false;
			return;
		}
		try {
			DynamicObject	frapiClass	= DynamicObject.of( Class.forName( "com.intergral.fusionreactor.api.FRAPI" ) );
			Object			FRAPIObject	= frapiClass.invokeStatic( BoxRuntime.getInstance().getRuntimeContext(), "getInstance" );
			while ( FRAPIObject == null
			    || ! ( ( Boolean ) DynamicObject.of( FRAPIObject ).invoke( BoxRuntime.getInstance().getRuntimeContext(), "isInitialized" ) ) ) {
				// System.out.println( "Waiting for FusionReactor to initialize..." );
				Thread.sleep( 200 );
				if ( FRAPIObject == null ) {
					FRAPIObject = frapiClass.invokeStatic( BoxRuntime.getInstance().getRuntimeContext(), "getInstance" );
				}
			}
			FRAPI		= DynamicObject.of( FRAPIObject );
			FREnabled	= true;
		} catch ( Throwable e ) {
			// e.printStackTrace();
			FREnabled = false;
		}
	}

	public synchronized static FRTransService getInstance( Boolean enabled ) {
		if ( instance == null ) {
			instance = new FRTransService( enabled );
		}
		return instance;
	}

	public boolean isEnabled() {
		return FREnabled;
	}

	public DynamicObject startTransaction( String name, String description ) {
		if ( !FREnabled ) {
			return null;
		}

		DynamicObject FRTransaction = DynamicObject.of( FRAPI.invoke( BoxRuntime.getInstance().getRuntimeContext(), "createTrackedTransaction", name ) );
		FRAPI.invoke( BoxRuntime.getInstance().getRuntimeContext(), "setTransactionApplicationName", "BL" );
		FRTransaction.invoke( BoxRuntime.getInstance().getRuntimeContext(), "setDescription", description );
		return FRTransaction;
	}

	public void endTransaction( DynamicObject FRTransaction ) {
		if ( !FREnabled ) {
			return;
		}
		FRTransaction.invoke( BoxRuntime.getInstance().getRuntimeContext(), "close" );
	}

	public void errorTransaction( DynamicObject FRTransaction, Exception javaException ) {
		if ( !FREnabled ) {
			return;
		}
		FRTransaction.invoke( BoxRuntime.getInstance().getRuntimeContext(), "setTrappedThrowable", javaException );
	}

	public void setCurrentTransactionName( String name ) {
		if ( !FREnabled ) {
			return;
		}
		FRAPI.invoke( BoxRuntime.getInstance().getRuntimeContext(), "setTransactionName", name );
	}

	public void setCurrentTransactionApplicationName( String name ) {
		if ( !FREnabled ) {
			return;
		}
		FRAPI.invoke( BoxRuntime.getInstance().getRuntimeContext(), "setTransactionApplicationName", name );
	}
}
