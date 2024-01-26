package ortus.boxlang.debugger.request;

import java.util.Map;

import ortus.boxlang.debugger.DebugAdapter;

public class LaunchRequest extends AbstractRequest {

	public String program;

	public LaunchRequest( Map<String, Object> requestData ) {
		super( requestData );

		this.program = ( String ) ( ( Map ) requestData.get( "arguments" ) ).get( "program" );
	}

	@Override
	public void accept( DebugAdapter adapter ) {
		adapter.visit( this );
	}

}
