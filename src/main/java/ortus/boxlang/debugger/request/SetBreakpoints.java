package ortus.boxlang.debugger.request;

import ortus.boxlang.debugger.DebugAdapter;

public class SetBreakpoints extends AbstractRequest {

	public String					program;
	public SetBreakpointArguments	arguments;

	public static class SetBreakpointArguments {

		public Source		source;
		public Breakpoint[]	breakpoints;
	}

	@Override
	public void accept( DebugAdapter adapter ) {
		adapter.visit( this );
	}

}
