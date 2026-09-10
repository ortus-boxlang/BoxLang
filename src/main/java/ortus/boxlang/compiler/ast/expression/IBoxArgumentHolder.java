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
package ortus.boxlang.compiler.ast.expression;

import java.util.List;

/**
 * Interface for AST nodes that hold a list of {@link BoxArgument} entries
 * (function calls, method calls, constructor invocations, etc.).
 * <p>
 * Provides default helper methods for spread and named-argument detection
 * so every implementing node doesn't have to duplicate the logic.
 */
public interface IBoxArgumentHolder {

	/**
	 * Return the argument list owned by this node.
	 */
	List<BoxArgument> getArguments();

	/**
	 * Check whether the non-spread arguments use named syntax.
	 * Spread arguments are skipped because they don't determine
	 * whether the call site is positional or named.
	 *
	 * @return true if the first non-spread argument is named
	 */
	default boolean isNamedArgs() {
		for ( BoxArgument arg : getArguments() ) {
			if ( arg.isSpread() ) {
				continue;
			}
			return arg.isNamed();
		}
		return false;
	}

	/**
	 * Check whether any argument in the list is a spread expression.
	 *
	 * @return true if at least one argument uses the spread operator
	 */
	default boolean hasSpread() {
		for ( BoxArgument arg : getArguments() ) {
			if ( arg.isSpread() ) {
				return true;
			}
		}
		return false;
	}
}
