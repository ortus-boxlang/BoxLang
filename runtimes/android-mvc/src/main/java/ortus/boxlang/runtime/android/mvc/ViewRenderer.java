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

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;

/**
 * Renders a BoxLang view template and wraps it in a layout, capturing the output as a
 * string of HTML for the WebView track.
 * <p>
 * The flow mirrors ColdBox: the chosen view (under {@code viewsRoot}) is executed against
 * the request context with {@code event}, {@code rc}, and {@code flash} injected into the
 * variables scope and the produced markup captured to a string; that string is then exposed
 * as the {@code renderedView} variable and the chosen layout (under {@code layoutsRoot}) is
 * executed to wrap it. A layout simply outputs {@code #renderedView#} where the view content
 * should appear.
 * <p>
 * This class is Android-free and unit-testable on a plain JVM with a real {@link BoxRuntime}.
 */
public class ViewRenderer {

	/**
	 * Variable name exposed to layouts holding the already-rendered view markup.
	 */
	public static final Key		RENDERED_VIEW	= Key.of( "renderedView" );

	private static final Key	EVENT			= Key.of( "event" );
	private static final Key	RC				= Key.of( "rc" );
	private static final Key	FLASH			= Key.of( "flash" );

	/**
	 * The runtime used to execute templates.
	 */
	private final BoxRuntime	runtime;

	/**
	 * Absolute base directory for views (e.g. {@code <appHome>/views}).
	 */
	private final String		viewsRoot;

	/**
	 * Absolute base directory for layouts (e.g. {@code <appHome>/layouts}).
	 */
	private final String		layoutsRoot;

	/**
	 * The template file extension, including the dot (e.g. {@code .bxm}).
	 */
	private final String		extension;

	/**
	 * Construct a renderer with the default {@code .bxm} extension.
	 *
	 * @param runtime     The BoxLang runtime
	 * @param viewsRoot   Absolute base directory for views
	 * @param layoutsRoot Absolute base directory for layouts
	 */
	public ViewRenderer( BoxRuntime runtime, String viewsRoot, String layoutsRoot ) {
		this( runtime, viewsRoot, layoutsRoot, ".bxm" );
	}

	/**
	 * Construct a renderer.
	 *
	 * @param runtime     The BoxLang runtime
	 * @param viewsRoot   Absolute base directory for views
	 * @param layoutsRoot Absolute base directory for layouts
	 * @param extension   The template file extension (including the dot)
	 */
	public ViewRenderer( BoxRuntime runtime, String viewsRoot, String layoutsRoot, String extension ) {
		this.runtime		= runtime;
		this.viewsRoot		= stripTrailingSlash( viewsRoot );
		this.layoutsRoot	= stripTrailingSlash( layoutsRoot );
		this.extension		= extension;
	}

	/**
	 * Render the event's view wrapped in its layout (unless {@code noLayout}).
	 *
	 * @param context The request context
	 * @param event   The MVC event carrying the view/layout choices and {@code rc}
	 *
	 * @return The fully rendered HTML
	 */
	public String render( IBoxContext context, MVCEvent event ) {
		injectScope( context, event );

		String viewContent = renderView( context, event );
		if ( event.isNoLayout() || event.getLayout() == null ) {
			return viewContent;
		}

		// Expose the rendered view to the layout and render the layout.
		variables( context ).put( RENDERED_VIEW, viewContent );
		return renderTemplate( context, layoutPath( event.getLayout() ) );
	}

	/**
	 * Render just the event's view (no layout).
	 *
	 * @param context The request context
	 * @param event   The MVC event
	 *
	 * @return The rendered view HTML
	 */
	public String renderView( IBoxContext context, MVCEvent event ) {
		injectScope( context, event );
		return renderTemplate( context, viewPath( event.getView() ) );
	}

	/**
	 * Render an arbitrary template by absolute path, capturing its output to a string.
	 *
	 * @param context      The context to render against
	 * @param absolutePath The absolute path of the template
	 *
	 * @return The captured output
	 */
	public String renderTemplate( IBoxContext context, String absolutePath ) {
		StringBuffer buffer = new StringBuffer();
		context.pushBuffer( buffer );
		try {
			this.runtime.executeTemplate( absolutePath, context );
		} finally {
			context.popBuffer();
		}
		return buffer.toString();
	}

	/**
	 * Resolve a view name to its absolute template path.
	 *
	 * @param view The view name relative to {@code viewsRoot} (without extension)
	 *
	 * @return The absolute template path
	 */
	public String viewPath( String view ) {
		return this.viewsRoot + "/" + view + this.extension;
	}

	/**
	 * Resolve a layout name to its absolute template path.
	 *
	 * @param layout The layout name relative to {@code layoutsRoot} (without extension)
	 *
	 * @return The absolute template path
	 */
	public String layoutPath( String layout ) {
		return this.layoutsRoot + "/" + layout + this.extension;
	}

	private void injectScope( IBoxContext context, MVCEvent event ) {
		IScope variables = variables( context );
		variables.put( EVENT, event );
		variables.put( RC, event.getCollection() );
		variables.put( FLASH, event.getFlash() );
	}

	private IScope variables( IBoxContext context ) {
		return context.getScopeNearby( Key.variables );
	}

	private static String stripTrailingSlash( String value ) {
		if ( value != null && value.length() > 1 && value.endsWith( "/" ) ) {
			return value.substring( 0, value.length() - 1 );
		}
		return value;
	}
}
