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

/**
 * Models the Output event for the Debug Protocol
 */
public class OutputEvent extends Event {

	public OutputBody body;

	private class OutputBody {

		@SuppressWarnings( value = { "unused" } )
		public String	category;

		@SuppressWarnings( value = { "unused" } )
		public String	output;
	}

	/**
	 * Constructor
	 * 
	 * @param category One of 'console' | 'important' | 'stdout' | 'stderr' | 'telemetry'
	 * @param output   The data to output
	 */
	public OutputEvent( String category, String output ) {
		super( "output" );

		this.body			= new OutputBody();
		this.body.category	= category;
		this.body.output	= output;
	}

}
