package ortus.boxlang.debugger.response;

import ortus.boxlang.debugger.request.IDebugRequest;

public class NoBodyResponse extends AbstractResponse {

	public NoBodyResponse( IDebugRequest request ) {
		super( request.getCommand(), request.getSeq(), true );
	}
}
