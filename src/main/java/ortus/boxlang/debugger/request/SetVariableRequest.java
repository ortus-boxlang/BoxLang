package ortus.boxlang.debugger.request;

import ortus.boxlang.debugger.DebugAdapter;

public class SetVariableRequest extends AbstractRequest {

	public SetVariableRequestArguments arguments;

	public static class SetVariableRequestArguments {

		public int		variablesReference;
		public String	name;
		public String	value;
	}

	@Override
	public void accept( DebugAdapter adapter ) {
		adapter.visit( this );
	}

}