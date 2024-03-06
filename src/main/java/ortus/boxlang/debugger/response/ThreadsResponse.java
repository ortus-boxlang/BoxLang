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

import java.util.List;

import ortus.boxlang.debugger.request.ThreadsRequest;
import ortus.boxlang.debugger.types.Thread;

/**
 * Models a SetBreakpoint response in the DAP
 */
public class ThreadsResponse extends AbstractResponse {

	public ThreadsResponseBody body;

	public static class ThreadsResponseBody {

		public List<Thread> threads;
	}

	public ThreadsResponse() {

	}

	/**
	 * Constructor
	 * 
	 * @param request The request to respond to
	 */
	public ThreadsResponse( ThreadsRequest request, List<Thread> threads ) {
		super( request.getCommand(), request.getSeq(), true );
		this.body			= new ThreadsResponseBody();

		this.body.threads	= threads;
	}
}
