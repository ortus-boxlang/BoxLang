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
package ortus.boxlang.runtime;

import ortus.boxlang.runtime.scopes.*;

/**
 * Represents an execution context. May be subclassed for more specific contexts such as servlet.
 * Each thread/request has a new execution context and may share the same BoxRuntime instance.
 */
public class ExecutionContext {

	// Should the execution context store an array of scopes so new ones can be registered?
	private IScope[] scopes;

	// Or perhaps separtley store searchable and non-searchable scopes?
	private IScope[] searchableScopes;
	private IScope[] adHocScopes;

	// Or is there really a set number and they should be stored separately and maybe we don't need the lookup order at all as the context knows the
	// order?
	private IScope variablesScope;
	private IScope thisScope;

	// Also, should variables, this, local, arguments live here, or in the associated page or component they belong to, which in turn, gets associated
	// here?
	// Should the non-web context have even server, session, or application, or would a pure boxlang context only know about local, arguments, variables,
	// and this?
	// Decisions, decisions...

}
