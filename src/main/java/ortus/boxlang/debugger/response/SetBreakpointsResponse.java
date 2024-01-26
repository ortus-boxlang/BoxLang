package ortus.boxlang.debugger.response;

import ortus.boxlang.debugger.request.Breakpoint;
import ortus.boxlang.debugger.request.SetBreakpointsRequest;

public class SetBreakpointsResponse extends AbstractResponse {

	public SetBreakpointsResponseBody body;

	public class SetBreakpointsResponseBody {

		public Breakpoint[] breakpoints;
	}

	public SetBreakpointsResponse( SetBreakpointsRequest request ) {
		super( request.getCommand(), request.getSeq(), true );
		this.body				= new SetBreakpointsResponseBody();

		this.body.breakpoints	= request.arguments.breakpoints;
	}
}
