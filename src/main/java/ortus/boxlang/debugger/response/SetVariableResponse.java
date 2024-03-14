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

import ortus.boxlang.debugger.request.SetVariableRequest;
import ortus.boxlang.debugger.types.Message;
import ortus.boxlang.debugger.types.Variable;

/**
 * Models a SetBreakpoint response in the DAP
 */
public class SetVariableResponse extends AbstractResponse {

	public SetVariableResponseBody body;

	public static class SetVariableResponseBody {

		public String	result;

		public String	type;

		public int		variablesReference;

		public Message	error;
	}

	public SetVariableResponse() {

	}

	/**
	 * Constructor
	 * 
	 * @param request The request to respond to
	 */
	public SetVariableResponse( SetVariableRequest request, Variable variable ) {
		super( request.getCommand(), request.getSeq(), true );
		this.body						= new SetVariableResponseBody();

		this.body.result				= variable.value;
		this.body.type					= variable.type;
		this.body.variablesReference	= variable.variablesReference;
	}

	/**
	 * Send an error response.
	 * 
	 * @param request The request to respond to
	 */
	public SetVariableResponse( SetVariableRequest request, String message ) {
		super( request.getCommand(), request.getSeq(), true );
		this.body						= new SetVariableResponseBody();

		this.body.variablesReference	= 0;
		this.body.error					= new Message();
		this.body.error.id				= 1;
		this.body.error.format			= message;

		this.success					= false;
	}
}
