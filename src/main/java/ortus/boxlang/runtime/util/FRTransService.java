package ortus.boxlang.runtime.util;

import ortus.boxlang.runtime.interop.DynamicObject;

public class FRTransService {

	private static FRTransService	instance	= null;
	private boolean					FREnabled	= false;
	private DynamicObject			FRAPI;

	private FRTransService() {
		try {
			DynamicObject	frapiClass	= DynamicObject.of( Class.forName( "com.intergral.fusionreactor.api.FRAPI" ) );
			Object			FRAPIObject	= frapiClass.invokeStatic( "getInstance" );
			while ( FRAPIObject == null || ! ( ( Boolean ) DynamicObject.of( FRAPIObject ).invoke( "isInitialized" ) ) ) {
				// System.out.println( "Waiting for FusionReactor to initialize..." );
				Thread.sleep( 200 );
				if ( FRAPIObject == null ) {
					FRAPIObject = frapiClass.invokeStatic( "getInstance" );
				}
			}
			FRAPI		= DynamicObject.of( FRAPIObject );
			FREnabled	= true;
		} catch ( Throwable e ) {
			//  e.printStackTrace();
			FREnabled = false;
		}
	}

	public synchronized static FRTransService getInstance() {
		if ( instance == null ) {
			instance = new FRTransService();
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

		DynamicObject FRTransaction = DynamicObject.of( FRAPI.invoke( "createTrackedTransaction", name ) );
		FRAPI.invoke( "setTransactionApplicationName", "BL" );
		FRTransaction.invoke( "setDescription", description );
		return FRTransaction;
	}

	public void endTransaction( DynamicObject FRTransaction ) {
		if ( !FREnabled ) {
			return;
		}
		FRTransaction.invoke( "close" );
	}

	public void errorTransaction( DynamicObject FRTransaction, Exception javaException ) {
		if ( !FREnabled ) {
			return;
		}
		FRTransaction.invoke( "setTrappedThrowable", javaException );
	}

	public void setCurrentTransactionName( String name ) {
		if ( !FREnabled ) {
			return;
		}
		FRAPI.invoke( "setTransactionName", name );
	}

	public void setCurrentTransactionApplicationName( String name ) {
		if ( !FREnabled ) {
			return;
		}
		FRAPI.invoke( "setTransactionApplicationName", name );
	}
}