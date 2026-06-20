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
package ortus.boxlang.runtime.android.ui;

import java.util.ArrayList;
import java.util.List;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * An immutable-by-convention declarative UI node for the Compose track (Track 1).
 * <p>
 * BoxLang authors a tree of these nodes (via the {@link UI} DSL) — e.g. a {@code Column}
 * containing {@code Text} and {@code Button} nodes. A Kotlin/Compose renderer in the
 * Android module walks the tree and emits genuine Compose widgets, wiring node event
 * handlers (BoxLang closures stored in {@link #getEvents()}) back to the runtime and
 * binding state for recomposition.
 * <p>
 * This node model is intentionally Android-free so it can be constructed and unit-tested on
 * a plain JVM; only the renderer that consumes it depends on Compose.
 */
public class UINode {

	/**
	 * The node type, e.g. {@code Column}, {@code Row}, {@code Text}, {@code Button}.
	 */
	private final String		type;

	/**
	 * Display/layout properties (e.g. {@code text}, {@code padding}, {@code color}).
	 */
	private final IStruct		props;

	/**
	 * Event handlers keyed by event name (e.g. {@code onClick}, {@code onChange}). Values
	 * are BoxLang closures/functions invoked by the renderer.
	 */
	private final IStruct		events;

	/**
	 * Child nodes.
	 */
	private final List<UINode>	children;

	/**
	 * Construct a node of the given type with empty props/events/children.
	 *
	 * @param type The node type
	 */
	public UINode( String type ) {
		this.type		= type;
		this.props		= new Struct();
		this.events		= new Struct();
		this.children	= new ArrayList<>();
	}

	/**
	 * @return The node type
	 */
	public String getType() {
		return this.type;
	}

	/**
	 * @return The display/layout properties
	 */
	public IStruct getProps() {
		return this.props;
	}

	/**
	 * @return The event handler map (name -> closure)
	 */
	public IStruct getEvents() {
		return this.events;
	}

	/**
	 * @return The child nodes
	 */
	public List<UINode> getChildren() {
		return this.children;
	}

	/**
	 * Set a display/layout property.
	 *
	 * @param name  The property name
	 * @param value The property value
	 *
	 * @return This node for chaining
	 */
	public UINode prop( String name, Object value ) {
		this.props.put( Key.of( name ), value );
		return this;
	}

	/**
	 * Get a property value.
	 *
	 * @param name The property name
	 *
	 * @return The value, or {@code null} if absent
	 */
	public Object getProp( String name ) {
		return this.props.get( Key.of( name ) );
	}

	/**
	 * Register an event handler closure.
	 *
	 * @param name    The event name (e.g. {@code onClick})
	 * @param handler The BoxLang closure/function
	 *
	 * @return This node for chaining
	 */
	public UINode on( String name, Object handler ) {
		this.events.put( Key.of( name ), handler );
		return this;
	}

	/**
	 * @param name The event name
	 *
	 * @return The handler closure, or {@code null} if none registered
	 */
	public Object getHandler( String name ) {
		return this.events.get( Key.of( name ) );
	}

	/**
	 * Append a child node.
	 *
	 * @param child The child to add
	 *
	 * @return This node for chaining
	 */
	public UINode child( UINode child ) {
		this.children.add( child );
		return this;
	}

	/**
	 * Append several child nodes.
	 *
	 * @param nodes The children to add
	 *
	 * @return This node for chaining
	 */
	public UINode children( UINode... nodes ) {
		for ( UINode node : nodes ) {
			this.children.add( node );
		}
		return this;
	}

	@Override
	public String toString() {
		return "UINode[" + this.type + " props=" + this.props.size()
		    + " events=" + this.events.size() + " children=" + this.children.size() + "]";
	}
}
