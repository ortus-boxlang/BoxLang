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

import ortus.boxlang.debugger.request.ContinueRequest;

/**
 * Models a SetBreakpoint response in the DAP
 */
public class ContinueResponse extends AbstractResponse {

	public ContinueResponseBody body;

	public static class ContinueResponseBody {

		public boolean allThreadsContinued;
	}

	public ContinueResponse() {

	}

	/**
	 * Constructor
	 * 
	 * @param request The request to respond to
	 */
	public ContinueResponse( ContinueRequest request, boolean allThreadsResumed ) {
		super( request.getCommand(), request.getSeq(), true );
		this.body						= new ContinueResponseBody();

		this.body.allThreadsContinued	= allThreadsResumed;
	}
}
