package ortus.boxlang.debugger;

import java.util.Map;

public interface IAdapterProtocolMessage {

	public void setRawMessageData( Map<String, Object> messageData );

	public Map<String, Object> getRawMessageData();

	/**
	 * @return The type of message event | request | request
	 */
	public String getType();

	/**
	 * @return The command that was issued by the debug tool
	 */
	public String getCommand();

	/**
	 * @return The sequence number of this command
	 */
	public int getSeq();

	/**
	 * Implement this to use a DebugAdapter as a visitor
	 * 
	 * @param adapter The visitor that will visit this node
	 */
	public void accept( DebugAdapter adapter );
}
