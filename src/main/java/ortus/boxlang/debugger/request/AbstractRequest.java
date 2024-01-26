package ortus.boxlang.debugger.request;

public abstract class AbstractRequest implements IDebugRequest {

	public String	command;
	public int		seq;

	public String getCommand() {
		return command;
	}

	public int getSeq() {
		return this.seq;
	}
}
