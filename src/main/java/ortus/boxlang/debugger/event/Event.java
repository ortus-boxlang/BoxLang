/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.debugger.event;

import java.util.Map;

import ortus.boxlang.debugger.DebugAdapter;
import ortus.boxlang.debugger.IAdapterProtocolMessage;
import ortus.boxlang.debugger.ISendable;

public class Event implements ISendable, IAdapterProtocolMessage {

	public String				type	= "event";
	public String				event;

	private Map<String, Object>	messageData;

	public Event() {

	}

	public Event( String event ) {
		this.event = event;
	}

	public void setRawMessageData( Map<String, Object> messageData ) {
		this.messageData = messageData;
	}

	public Map<String, Object> getRawMessageData() {
		return this.messageData;
	}

	/**
	 * The command for the debugger to execute
	 */
	public String getCommand() {
		return event;
	}

	/**
	 * Returns the sequence number of this request
	 */
	public int getSeq() {
		return -1;
	}

	/**
	 * Gets the type of the debug protocl message. Always "event".
	 */
	@Override
	public String getType() {
		return type;
	}

	/**
	 * Gets the name of the event
	 */
	@Override
	public String getName() {
		return event;
	}

	@Override
	public void accept( DebugAdapter adapter ) {
		adapter.visit( this );
	}

}
