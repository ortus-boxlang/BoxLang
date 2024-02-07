package ortus.boxlang.debugger.request;

import ortus.boxlang.debugger.DebugAdapter;

public class ScopeRequest extends AbstractRequest {

	public ScopeRequestArguments arguments;

	public static class ScopeRequestArguments {

		public int frameId;
	}

	@Override
	public void accept( DebugAdapter adapter ) {
		adapter.visit( this );
	}

}