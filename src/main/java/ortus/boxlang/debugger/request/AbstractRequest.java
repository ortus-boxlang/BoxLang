package ortus.boxlang.debugger.request;

import java.util.Map;

public abstract class AbstractRequest implements IDebugRequest {

	public String	command;
	public int		seq;

	public AbstractRequest( String command, int seq ) {
		this.command	= command;
		this.seq		= seq;
	}

	public AbstractRequest( Map<String, Object> requestData ) {
		this( ( String ) requestData.get( "command" ), ( int ) requestData.get( "seq" ) );
	}

	public String getCommand() {
		return command;
	}

	public int getSeq() {
		return this.seq;
	}
}
