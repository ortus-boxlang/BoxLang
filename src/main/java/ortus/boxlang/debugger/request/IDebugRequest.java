package ortus.boxlang.debugger.request;

import ortus.boxlang.debugger.DebugAdapter;

public interface IDebugRequest {

	public String getCommand();

	public int getSeq();

	public void accept( DebugAdapter adapter );
}
