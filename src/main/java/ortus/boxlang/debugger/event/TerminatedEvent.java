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

/**
 * Models the Exit event for the Debug Protocol
 */
public class TerminatedEvent extends Event {

	public TerminatedBody body;

	public static class TerminatedBody {

		@SuppressWarnings( value = { "unused" } )
		public boolean restart;
	}

	/**
	 * Constructor
	 */
	public TerminatedEvent() {
		super( "terminated" );

		this.body			= new TerminatedBody();
		this.body.restart	= false;
	}

	@Override
	public void accept( DebugAdapter adapter ) {
		adapter.visit( this );
	}

}
