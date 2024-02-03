package ortus.boxlang.debugger;

import java.util.Map;

public interface IAdapterProtocolMessage {

	public void setRawMessageData( Map<String, Object> messageData );

	public Map<String, Object> getRawMessageData();
}
