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
}
