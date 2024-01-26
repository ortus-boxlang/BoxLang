package ortus.boxlang.debugger.request;

import ortus.boxlang.debugger.DebugAdapter;

public class LaunchRequest extends AbstractRequest {

	public LaunchRequestArguments arguments;

	public static class LaunchRequestArguments {

		public String program;
	}

	@Override
	public void accept( DebugAdapter adapter ) {
		adapter.visit( this );
	}

}
