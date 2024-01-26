package ortus.boxlang.debugger.response;

public class AbstractResponse implements IDebugResponse {

	public String	type	= "response";
	public String	command;
	public int		request_seq;
	public boolean	success	= true;

	public AbstractResponse( String command, int request_seq, boolean success ) {
		this.command		= command;
		this.request_seq	= request_seq;
		this.success		= success;
	}

	public String getType() {
		return this.type;
	}

	@Override
	public String getName() {
		return this.type;
	}

}
