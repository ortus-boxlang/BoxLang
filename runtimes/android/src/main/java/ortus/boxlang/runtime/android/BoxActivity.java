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
package ortus.boxlang.runtime.android;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.webkit.WebView;

import androidx.appcompat.app.AppCompatActivity;

import ortus.boxlang.runtime.android.mvc.MVCDispatcher;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.types.IStruct;

/**
 * The generic, config-driven BoxLang Activity. Apps declare THIS class directly in their
 * manifest (no per-app subclass needed) — all behavior is customized in {@code Application.bx}
 * via its lifecycle hooks and handlers.
 * <p>
 * Supports both UI tracks:
 * <ul>
 * <li><b>WebView track</b> (default): hosts a {@link WebView}, builds the MVC front
 * controller, and navigates to the entry route ({@code /} → {@code Main.index}).</li>
 * <li><b>Compose track</b>: a subclass/host can call {@link #setBoxContent(Object)} with a
 * BoxLang UI-tree closure rendered natively by the Compose renderer.</li>
 * </ul>
 * Every Android lifecycle callback forwards to the matching optional {@code Application.bx}
 * hook through {@link AndroidLifecycleDispatcher}.
 */
public class BoxActivity extends AppCompatActivity {

	/**
	 * The entry route dispatched on create (root → default event).
	 */
	protected String					entryRoute	= "/";

	private IBoxContext					context;
	private AndroidLifecycleDispatcher	lifecycle;
	private BoxWebViewRenderer			webRenderer;

	@Override
	protected void onCreate( Bundle savedInstanceState ) {
		super.onCreate( savedInstanceState );

		AndroidBoxRuntime android = AndroidBoxRuntime.getInstance();

		// Per-request context, with Application.bx discovered from the app home.
		this.context	= newRequestContext( android );
		this.lifecycle	= new AndroidLifecycleDispatcher( this.context );

		// Standard BoxLang lifecycle: onRequestStart, then the Android onActivityCreate hook.
		fireRequestStart();
		this.lifecycle.invokeHook( "onActivityCreate", savedInstanceState );

		// Default render mode: WebView track.
		WebView webView = new WebView( this );
		setContentView( webView );
		MVCDispatcher dispatcher = android.getDispatcher();
		this.webRenderer = new BoxWebViewRenderer( webView, dispatcher, this.context );
		this.webRenderer.navigate( this.entryRoute, "GET", null );
	}

	/**
	 * Render the Compose track from a BoxLang UI-tree value (closure or node tree).
	 * Delegates to the Kotlin {@code ComposeTreeRenderer}.
	 *
	 * @param uiTree The BoxLang UI tree (or a closure returning one)
	 */
	public void setBoxContent( Object uiTree ) {
		ComposeBridge.render( this, this.context, uiTree );
	}

	/**
	 * Navigate the WebView track to a route.
	 *
	 * @param path   The route path
	 * @param method The HTTP method
	 * @param params The params (may be {@code null})
	 */
	public void navigate( String path, String method, IStruct params ) {
		if ( this.webRenderer != null ) {
			this.webRenderer.navigate( path, method, params );
		}
	}

	private IBoxContext newRequestContext( AndroidBoxRuntime android ) {
		// A request context that loads the app descriptor (Application.bx) from the app home.
		ScriptingRequestBoxContext ctx = new ScriptingRequestBoxContext( android.getRuntime().getRuntimeContext(), true );
		return ctx;
	}

	private void fireRequestStart() {
		RequestBoxContext rc = this.context.getParentOfType( RequestBoxContext.class );
		rc.getApplicationListener().onRequestStart( this.context, new Object[] { this.entryRoute } );
	}

	// ---- Android lifecycle -> Application.bx hooks ----

	@Override
	protected void onStart() {
		super.onStart();
		this.lifecycle.invokeHook( "onActivityStart" );
	}

	@Override
	protected void onResume() {
		super.onResume();
		this.lifecycle.invokeHook( "onActivityResume" );
	}

	@Override
	protected void onPause() {
		this.lifecycle.invokeHook( "onActivityPause" );
		super.onPause();
	}

	@Override
	protected void onStop() {
		this.lifecycle.invokeHook( "onActivityStop" );
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		this.lifecycle.invokeHook( "onActivityDestroy" );
		RequestBoxContext rc = this.context.getParentOfType( RequestBoxContext.class );
		rc.getApplicationListener().onRequestEnd( this.context, new Object[] {} );
		super.onDestroy();
	}

	@Override
	protected void onActivityResult( int requestCode, int resultCode, Intent data ) {
		super.onActivityResult( requestCode, resultCode, data );
		this.lifecycle.invokeHook( "onActivityResult", requestCode, resultCode, data );
	}

	@Override
	public void onRequestPermissionsResult( int requestCode, String[] permissions, int[] grantResults ) {
		super.onRequestPermissionsResult( requestCode, permissions, grantResults );
		this.lifecycle.invokeHook( "onPermissionResult", requestCode, permissions, grantResults );
	}

	@Override
	@SuppressWarnings( "deprecation" )
	public void onBackPressed() {
		// A hook returning boolean false consumes the back press.
		Object handled = this.lifecycle.invokeHook( "onBackPressed" );
		if ( !Boolean.FALSE.equals( handled ) ) {
			super.onBackPressed();
		}
	}

	@Override
	public void onConfigurationChanged( Configuration newConfig ) {
		super.onConfigurationChanged( newConfig );
		this.lifecycle.invokeHook( "onConfigurationChanged", newConfig );
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		this.lifecycle.invokeHook( "onLowMemory" );
	}
}
