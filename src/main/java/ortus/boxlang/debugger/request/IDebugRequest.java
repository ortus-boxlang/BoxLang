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

import ortus.boxlang.debugger.DebugAdapter;

/**
 * Interface to help model the debug protocol request type.
 */
public interface IDebugRequest {

	/**
	 * @return The command that was issued by the debug tool
	 */
	public String getCommand();

	/**
	 * @return The sequence number of this command
	 */
	public int getSeq();

	/**
	 * Implement this to use a DebugAdapter as a visitor
	 * 
	 * @param adapter The visitor that will visit this node
	 */
	public void accept( DebugAdapter adapter );
}
