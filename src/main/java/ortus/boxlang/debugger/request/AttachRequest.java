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
package ortus.boxlang.debugger.request;

import java.util.Map;

import ortus.boxlang.debugger.DebugAdapter;

/**
 * An abstract requet. Implement this to model a request that conforms to the debug protocol.
 */
public class AttachRequest extends AbstractRequest {

	public String					command;
	public int						seq;
	private Map<String, Object>		messageData;

	public AttachRequestArguments	arguments;

	public static class AttachRequestArguments {

		public Integer serverPort;
	}

	public void setRawMessageData( Map<String, Object> messageData ) {
		this.messageData = messageData;
	}

	public Map<String, Object> getRawMessageData() {
		return this.messageData;
	}

	public String getType() {
		return "request";
	}

	/**
	 * The command for the debugger to execute
	 */
	public String getCommand() {
		return command;
	}

	/**
	 * Returns the sequence number of this request
	 */
	public int getSeq() {
		return this.seq;
	}

	@Override
	public void accept( DebugAdapter adapter ) {
		adapter.visit( this );
	}
}
