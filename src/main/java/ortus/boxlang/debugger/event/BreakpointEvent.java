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

import ortus.boxlang.debugger.DebugAdapter;
import ortus.boxlang.debugger.types.Breakpoint;

/**
 * Models the Breakpoint event for the Debug Protocol
 */
public class BreakpointEvent extends Event {

	public BreakpointBody body;

	public static class BreakpointBody {

		public String		reason;
		public Breakpoint	breakpoint;
	}

	public BreakpointEvent() {
	}

	/**
	 * Constructor
	 *
	 * @param reason     the reason for the breakpoint
	 * @param breakpoint the breakpoint
	 */
	public BreakpointEvent( String reason, Breakpoint breakpoint ) {
		super( "breakpoint" );

		this.body				= new BreakpointBody();
		this.body.reason		= reason;
		this.body.breakpoint	= breakpoint;
	}

	@Override
	public void accept( DebugAdapter adapter ) {
		adapter.visit( this );
	}

}
