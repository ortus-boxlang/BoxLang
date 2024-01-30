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
package ortus.boxlang.debugger.response;

/**
 * Abstract base class to aid in modeling response types
 */
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

	/**
	 * The type of the message. In this case it will always be "request".
	 */
	public String getType() {
		return this.type;
	}

	/**
	 * The name of the message. primarily used for logging. In this case it will always be "request".
	 */
	@Override
	public String getName() {
		return this.type;
	}

}
