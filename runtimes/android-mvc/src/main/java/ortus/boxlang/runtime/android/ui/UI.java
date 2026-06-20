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

/**
 * A small factory DSL for building {@link UINode} trees (Track 1, Compose).
 * <p>
 * Designed to read naturally from BoxLang via Java interop, e.g.:
 *
 * <pre>
 * ui = createObject( "java", "ortus.boxlang.runtime.android.ui.UI" );
 * tree = ui.column()
 *     .child( ui.text( "Count: " &amp; count ) )
 *     .child( ui.button( "Increment" ).on( "onClick", () =&gt; increment() ) );
 * </pre>
 *
 * The node type names map 1:1 to Compose widgets in the Android renderer.
 */
public final class UI {

	private UI() {
		// static factory
	}

	/**
	 * @return A new vertical container node
	 */
	public static UINode column() {
		return new UINode( "Column" );
	}

	/**
	 * @return A new horizontal container node
	 */
	public static UINode row() {
		return new UINode( "Row" );
	}

	/**
	 * @return A new box/stack container node
	 */
	public static UINode box() {
		return new UINode( "Box" );
	}

	/**
	 * A text node.
	 *
	 * @param value The text to display
	 *
	 * @return The text node
	 */
	public static UINode text( String value ) {
		return new UINode( "Text" ).prop( "text", value );
	}

	/**
	 * A button node.
	 *
	 * @param label The button label
	 *
	 * @return The button node
	 */
	public static UINode button( String label ) {
		return new UINode( "Button" ).prop( "label", label );
	}

	/**
	 * A single-line text input node.
	 *
	 * @param value The current value
	 *
	 * @return The text field node
	 */
	public static UINode textField( String value ) {
		return new UINode( "TextField" ).prop( "value", value );
	}

	/**
	 * A spacer node.
	 *
	 * @param size The size in dp
	 *
	 * @return The spacer node
	 */
	public static UINode spacer( int size ) {
		return new UINode( "Spacer" ).prop( "size", size );
	}
}
