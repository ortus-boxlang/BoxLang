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
package ortus.boxlang.compiler;

import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.expression.BoxFQN;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;
import ortus.boxlang.compiler.ast.statement.BoxAnnotation;
import ortus.boxlang.compiler.ast.statement.component.BoxComponent;

/**
 * Builds the hidden {@code _key} annotation that identifies a component's call site at compile time.
 * <p>
 * Components that cache their own body (the cache component's {@code action="cache"},
 * {@code "optimal"} and {@code "content"}) need a key that is stable across JVM restarts, unlike any
 * hash of the {@code ComponentBody} lambda. The compiler is the only place that knows both which file
 * the component is in and where in that file it starts, so it passes both down as {@code _key}.
 * <p>
 * The source identity is still needed even though the request context also contributes one: a request
 * context knows how the top-level request was addressed (a URL, in a web runtime), not which file a
 * given component lives in, and one addressed request may pull in many files.
 */
public class ComponentSourceKey {

	/**
	 * The name of the hidden attribute the compiler injects.
	 */
	public static final String	ATTRIBUTE_NAME	= "_key";

	/**
	 * The name of the output-caching component the key is injected for.
	 */
	public static final String	CACHE_COMPONENT	= "cache";

	/**
	 * The label which precedes the source identity inside the key, matching Adobe ColdFusion's shape.
	 */
	public static final String	SOURCE_ID_LABEL	= "pageid:";

	/**
	 * Private constructor for a static utility class.
	 */
	private ComponentSourceKey() {
	}

	/**
	 * Whether the given component should receive an injected {@code _key}.
	 * <p>
	 * The check is by component name only. The {@code action} attribute may be a runtime expression,
	 * so it cannot be resolved while compiling; injecting the key for every cache component is
	 * harmless because the component only reads it for the body-caching actions.
	 *
	 * @param component the component being compiled
	 *
	 * @return true if a {@code _key} should be injected
	 */
	public static boolean needsSourceKey( BoxComponent component ) {
		return component != null && CACHE_COMPONENT.equalsIgnoreCase( component.getName() );
	}

	/**
	 * Build the {@code _key} annotation for a component's call site.
	 * <p>
	 * The value identifies the file the component lives in plus where it starts, in the same
	 * human-readable shape Adobe ColdFusion uses, e.g.:
	 *
	 * <pre>
	 * pageid:29cfc3f214fb2c32adaf979dc80ab540_line:6_col:1
	 * </pre>
	 *
	 * The source identity is given as a hash of the file path rather than the path itself, so that a
	 * deep or long path cannot bloat the resulting cache key. The component prepends the request
	 * context's own key at runtime, yielding for example
	 * {@code http://localhost:1443/products_pageid:29cfc3f2..._line:6_col:1}.
	 * <p>
	 * Returns null when no source identity is available or the component has no position. Callers must
	 * treat null as "no injection" and let the runtime fall back to its weaker key.
	 *
	 * @param component the component being compiled
	 * @param sourceKey the hashed source identity provided by the boxpiler
	 *
	 * @return the annotation to append, or null when it cannot be built
	 */
	public static BoxAnnotation buildKeyAnnotation( BoxComponent component, String sourceKey ) {
		if ( component == null || sourceKey == null ) {
			return null;
		}
		// AST positions are not guaranteed to exist (see the AST nullability invariant).
		Position position = component.getPosition();
		if ( position == null || position.getStart() == null ) {
			return null;
		}
		String value = SOURCE_ID_LABEL + sourceKey
		    + "_line:" + position.getStart().getLine()
		    + "_col:" + position.getStart().getColumn();
		return new BoxAnnotation(
		    new BoxFQN( ATTRIBUTE_NAME, null, ATTRIBUTE_NAME ),
		    new BoxStringLiteral( value, null, null ),
		    null,
		    null
		);
	}
}
