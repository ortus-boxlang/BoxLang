package ortus.boxlang.debugger.request;

import ortus.boxlang.debugger.DebugAdapter;

public class InitializeRequest extends AbstractRequest {

	@Override
	public void accept( DebugAdapter adapter ) {
		adapter.visit( this );
	}

}
