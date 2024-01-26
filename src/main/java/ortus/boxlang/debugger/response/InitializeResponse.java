package ortus.boxlang.debugger.response;

import ortus.boxlang.debugger.request.IDebugRequest;

public class InitializeResponse extends AbstractResponse {

	IDebugRequest		request;
	public Capabilities	body;

	public InitializeResponse( IDebugRequest request ) {
		super( request.getCommand(), request.getSeq(), true );
		this.request	= request;
		this.body		= new Capabilities();
	}
}
