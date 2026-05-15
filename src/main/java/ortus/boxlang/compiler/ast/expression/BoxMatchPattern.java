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

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.Position;

/**
 * Base type for match patterns so pattern AST does not overload plain expressions.
 */
public abstract class BoxMatchPattern extends BoxNode {

	protected BoxMatchPattern( Position position, String sourceText ) {
		super( position, sourceText );
	}

	public final boolean hasBindingTarget() {
		if ( this instanceof BoxMatchBindingPattern bindingPattern ) {
			return bindingPattern.getBinding() != null;
		}
		if ( this instanceof BoxMatchTypePattern typePattern ) {
			return typePattern.getBinding() != null;
		}
		if ( this instanceof BoxMatchObjectPattern objectPattern ) {
			return containsBindingTarget( objectPattern.getPattern() );
		}
		if ( this instanceof BoxMatchArrayPattern arrayPattern ) {
			return containsBindingTarget( arrayPattern.getPattern() );
		}
		if ( this instanceof BoxMatchConstructorPattern constructorPattern ) {
			return containsBindingTarget( constructorPattern.getPatterns() );
		}
		if ( this instanceof BoxMatchOrPattern orPattern ) {
			return containsBindingTarget( orPattern.getPatterns() );
		}
		if ( this instanceof BoxMatchAndPattern andPattern ) {
			return containsBindingTarget( andPattern.getPatterns() );
		}
		if ( this instanceof BoxMatchNotPattern notPattern ) {
			return notPattern.getPattern() != null && notPattern.getPattern().hasBindingTarget();
		}

		return false;
	}

	private static boolean containsBindingTarget( List<BoxMatchPattern> patterns ) {
		for ( BoxMatchPattern pattern : patterns ) {
			if ( pattern != null && pattern.hasBindingTarget() ) {
				return true;
			}
		}

		return false;
	}

	private static boolean containsBindingTarget( BoxObjectDestructuringPattern pattern ) {
		if ( pattern == null ) {
			return false;
		}

		for ( BoxObjectDestructuringBinding binding : pattern.getBindings() ) {
			if ( binding.getTarget() != null || containsBindingTarget( binding.getPattern() ) ) {
				return true;
			}
		}

		return false;
	}

	private static boolean containsBindingTarget( BoxArrayDestructuringPattern pattern ) {
		if ( pattern == null ) {
			return false;
		}

		for ( BoxArrayDestructuringBinding binding : pattern.getBindings() ) {
			if ( binding.getTarget() != null || containsBindingTarget( binding.getPattern() ) ) {
				return true;
			}
		}

		return false;
	}
}