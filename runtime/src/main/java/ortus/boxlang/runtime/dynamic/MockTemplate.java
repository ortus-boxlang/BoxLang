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
package ortus.boxlang.runtime.dynamic;

import ortus.boxlang.runtime.context.TemplateContext;
import ortus.boxlang.runtime.scopes.Key;

public class MockTemplate implements ITemplate {

	/**
	 * Private constructor
	 */
	private MockTemplate() {
	}

	public static void invoke( TemplateContext context ) {

		// I can store variables in the context
		context.getVariablesScope().put( Key.of( "MockTemplate" ), "Yea baby!!" );

		System.out.println( "MockTemplate invoked, woot woot!" );
	}

}
