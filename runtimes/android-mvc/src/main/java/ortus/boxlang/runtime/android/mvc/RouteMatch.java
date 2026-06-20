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
package ortus.boxlang.runtime.android.mvc;

import java.util.Collections;
import java.util.Map;

/**
 * The result of resolving an incoming request to a handler + action.
 * <p>
 * Produced either by an explicit {@link Route} match (with {@code route} populated) or
 * by convention resolution / the default event (with {@code route} {@code null}). The
 * {@code params} map holds any path placeholder values extracted from the URI.
 */
public class RouteMatch {

	/**
	 * The resolved handler name (BoxLang class under {@code handlers/}).
	 */
	private final String				handler;

	/**
	 * The resolved action method name.
	 */
	private final String				action;

	/**
	 * Extracted path parameters (e.g. {@code id} from {@code /items/:id}).
	 */
	private final Map<String, String>	params;

	/**
	 * The explicit route that produced this match, or {@code null} for convention/default.
	 */
	private final Route					route;

	/**
	 * Construct a route match.
	 *
	 * @param handler The handler name
	 * @param action  The action name
	 * @param params  The extracted path parameters (may be {@code null})
	 * @param route   The explicit route, or {@code null} for convention/default resolution
	 */
	public RouteMatch( String handler, String action, Map<String, String> params, Route route ) {
		this.handler	= handler;
		this.action		= action;
		this.params		= params == null ? Collections.emptyMap() : Map.copyOf( params );
		this.route		= route;
	}

	/**
	 * @return The resolved handler name
	 */
	public String getHandler() {
		return this.handler;
	}

	/**
	 * @return The resolved action name
	 */
	public String getAction() {
		return this.action;
	}

	/**
	 * @return The extracted path parameters (never {@code null})
	 */
	public Map<String, String> getParams() {
		return this.params;
	}

	/**
	 * @return The explicit route that matched, or {@code null} if resolved by convention
	 */
	public Route getRoute() {
		return this.route;
	}

	/**
	 * @return {@code true} when this match came from an explicit route table entry
	 */
	public boolean isExplicit() {
		return this.route != null;
	}

	/**
	 * @return The fully-qualified event string, e.g. {@code Items.show}
	 */
	public String getEvent() {
		return this.handler + "." + this.action;
	}

	@Override
	public String toString() {
		return "RouteMatch[" + getEvent() + " params=" + this.params
		    + ( this.route == null ? " (convention)" : "" ) + "]";
	}
}
