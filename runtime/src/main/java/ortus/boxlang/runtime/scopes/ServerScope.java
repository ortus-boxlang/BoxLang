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
package ortus.boxlang.runtime.scopes;

/**
 * represents boxlang server scope container
 * Note, this doesn't have to be a "web", it can reprsesent any long-running runtime which
 * processes one or more "requests" for execution.
 */
public class ServerScope extends BaseScope {

	/**
	 * --------------------------------------------------------------------------
	 * Public Properties
	 * --------------------------------------------------------------------------
	 */
	public static final Key name = Key.of( "server" );

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	public ServerScope() {
		super( ServerScope.name );
	}

}
