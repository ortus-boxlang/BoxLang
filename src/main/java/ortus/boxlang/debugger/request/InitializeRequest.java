package ortus.boxlang.debugger.request;

import java.util.Map;

import ortus.boxlang.debugger.DebugAdapter;

public class InitializeRequest extends AbstractRequest {

	public InitializeRequest( Map<String, Object> requestData ) {
		super( requestData );
	}

	@Override
	public void accept( DebugAdapter adapter ) {
		adapter.visit( this );
	}

}
