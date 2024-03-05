package ortus.boxlang.debugger;

import java.util.Map;

public class MapAdapterProtocolMessage implements IAdapterProtocolMessage {

	private Map<String, Object> messageData;

	public Object get( String key ) {
		return this.messageData.get( key );
	}

	@Override
	public void setRawMessageData( Map<String, Object> messageData ) {
		this.messageData = messageData;
	}

	@Override
	public Map<String, Object> getRawMessageData() {
		return this.messageData;
	}

	@Override
	public String getType() {
		return ( String ) this.messageData.get( "type" );
	}

	@Override
	public String getCommand() {
		return ( String ) this.messageData.get( "command" );
	}

	@Override
	public int getSeq() {
		return ( int ) this.messageData.get( "seq" );
	}

	@Override
	public void accept( DebugAdapter adapter ) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException( "Unimplemented method 'accept'" );
	}
}
