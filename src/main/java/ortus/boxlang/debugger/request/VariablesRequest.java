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
 * Models the request to launch a specific file
 */
public class VariablesRequest extends AbstractRequest {

	public VariablesRequestArguments arguments;

	public static class VariablesRequestArguments {

		/**
		 * The variable for which to retrieve its children. The `variablesReference`
		 * must have been obtained in the current suspended state. See 'Lifetime of
		 * Object References' in the Overview section for details.
		 */
		public int		variablesReference;

		/**
		 * Filter to limit the child variables to either named or indexed. If omitted,
		 * both types are fetched.
		 * Values: 'indexed', 'named'
		 */
		public String	filter;

		/**
		 * The index of the first variable to return; if omitted children start at 0.
		 * The attribute is only honored by a debug adapter if the corresponding
		 * capability `supportsVariablePaging` is true.
		 */
		public int		start;

		/**
		 * The number of variables to return. If count is missing or 0, all variables
		 * are returned.
		 * The attribute is only honored by a debug adapter if the corresponding
		 * capability `supportsVariablePaging` is true.
		 */
		public int		count;

		/**
		 * Specifies details on how to format the Variable values.
		 * The attribute is only honored by a debug adapter if the corresponding
		 * capability `supportsValueFormattingOptions` is true.
		 */
		public String	format;
	}

	@Override
	public void accept( DebugAdapter adapter ) {
		adapter.visit( this );
	}

}
