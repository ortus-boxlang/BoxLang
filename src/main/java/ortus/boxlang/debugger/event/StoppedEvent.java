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

import java.util.List;
import java.util.stream.IntStream;

import com.sun.jdi.event.BreakpointEvent;

import ortus.boxlang.debugger.DebugAdapter;

/**
 * Models the Stopped event for the Debug Protocol
 */
public class StoppedEvent extends Event {

	public StoppedBody body;

	public static class StoppedBody {

		@SuppressWarnings( value = { "unused" } )
		public String			reason;
		@SuppressWarnings( value = { "unused" } )
		public int				threadId;

		@SuppressWarnings( value = { "unused" } )
		public List<Integer>	hitBreakpointIds;

		public String			text;

		public String			description;
	}

	public StoppedEvent() {
		super( "stopped" );
	}

	/**
	 * Constructor
	 * 
	 * @param reason  One of 'step' | 'breakpoint' | 'exception' | 'pause' | 'entry' | 'goto'
	 *                | 'function breakpoint' | 'data breakpoint' | 'instruction breakpoint'
	 *                | string;
	 * @param stopped The data to Stopped
	 */
	public StoppedEvent( String reason, int threadId, String text, String description ) {
		super( "stopped" );

		this.body				= new StoppedBody();
		this.body.reason		= reason;
		this.body.threadId		= threadId;
		this.body.text			= text;
		this.body.description	= description;
	}

	/**
	 * Constructor
	 * 
	 * @param reason  One of 'step' | 'breakpoint' | 'exception' | 'pause' | 'entry' | 'goto'
	 *                | 'function breakpoint' | 'data breakpoint' | 'instruction breakpoint'
	 *                | string;
	 * @param stopped The data to Stopped
	 */
	public StoppedEvent( String reason, int threadId ) {
		super( "stopped" );

		this.body			= new StoppedBody();
		this.body.reason	= reason;
		this.body.threadId	= threadId;
	}

	/**
	 * Constructor
	 * 
	 * @param reason  One of 'step' | 'breakpoint' | 'exception' | 'pause' | 'entry' | 'goto'
	 *                | 'function breakpoint' | 'data breakpoint' | 'instruction breakpoint'
	 *                | string;
	 * @param stopped The data to Stopped
	 */
	public StoppedEvent( String reason, int threadId, int[] hitBreakpointIds ) {
		super( "stopped" );

		this.body					= new StoppedBody();
		this.body.reason			= reason;
		this.body.threadId			= threadId;
		this.body.hitBreakpointIds	= IntStream.of( hitBreakpointIds ).boxed().toList();
	}

	public static StoppedEvent breakpoint( int threadId ) {
		return new StoppedEvent( "breakpoint", threadId, new int[] {} );
	}

	public static StoppedEvent breakpoint( BreakpointEvent breakpointEvent, int breakpointId ) {

		return new StoppedEvent( "breakpoint", ( int ) breakpointEvent.thread().uniqueID(), new int[] { breakpointId } );
	}

	@Override
	public void accept( DebugAdapter adapter ) {
		adapter.visit( this );
	}

}
