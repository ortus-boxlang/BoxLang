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

import android.annotation.SuppressLint;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import ortus.boxlang.runtime.android.mvc.DispatchResult;
import ortus.boxlang.runtime.android.mvc.MVCDispatcher;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * Renders the WebView track: dispatches virtual routes through the in-process
 * {@link MVCDispatcher} and loads the resulting HTML into an Android {@link WebView}.
 * There is NO web server — form submissions and link navigation are captured in the
 * WebView and routed back into the BoxLang runtime as synthetic requests.
 * <p>
 * Two capture paths:
 * <ul>
 * <li><b>Link nav / GET forms</b> → {@link WebViewClient#shouldOverrideUrlLoading} (fields
 * ride the query string).</li>
 * <li><b>POST forms</b> → a JS bridge ({@code BoxBridge.submit(route, json)}), because
 * Android's {@code WebResourceRequest} does not expose the POST body.</li>
 * </ul>
 */
public class BoxWebViewRenderer {

	/**
	 * JS injected to intercept POST forms and forward them through the bridge.
	 */
	private static final String	FORM_HOOK_JS	= "javascript:(function(){document.addEventListener('submit',function(e){"
	    + "var f=e.target; if((f.method||'get').toLowerCase()==='post'){e.preventDefault();"
	    + "var d={}; new FormData(f).forEach(function(v,k){d[k]=v;});"
	    + "BoxBridge.submit(f.getAttribute('action')||'/', JSON.stringify(d));}},true);})()";

	private final WebView		webView;
	private final MVCDispatcher	dispatcher;
	private final IBoxContext	context;

	/**
	 * @param webView    The Android WebView to render into
	 * @param dispatcher The MVC front-controller dispatcher
	 * @param context    The request context to dispatch against
	 */
	@SuppressLint( "SetJavaScriptEnabled" )
	public BoxWebViewRenderer( WebView webView, MVCDispatcher dispatcher, IBoxContext context ) {
		this.webView	= webView;
		this.dispatcher	= dispatcher;
		this.context	= context;

		this.webView.getSettings().setJavaScriptEnabled( true );
		this.webView.addJavascriptInterface( new Bridge(), "BoxBridge" );
		this.webView.setWebViewClient( new RouterClient() );
	}

	/**
	 * Dispatch a route and load the rendered HTML (or follow a relocate).
	 *
	 * @param path   The virtual route path
	 * @param method The HTTP method
	 * @param params Incoming params (may be {@code null})
	 */
	public void navigate( String path, String method, IStruct params ) {
		DispatchResult result = this.dispatcher.dispatch( this.context, path, method, params );
		if ( result.isRelocate() ) {
			navigate( result.getRelocateTarget(), "GET", null );
			return;
		}
		this.webView.loadDataWithBaseURL( "https://boxlang.local/", result.getHtml(), "text/html", "UTF-8", null );
	}

	/**
	 * Re-inject the form hook after each page load.
	 */
	private void installFormHook() {
		this.webView.evaluateJavascript( FORM_HOOK_JS, null );
	}

	/**
	 * Parse a query string into an {@link IStruct} for GET routing.
	 *
	 * @param uri The URI possibly containing a query string
	 *
	 * @return The parsed params (never {@code null})
	 */
	static IStruct parseQuery( String uri ) {
		IStruct	params	= new Struct();
		int		q		= uri.indexOf( '?' );
		if ( q < 0 || q == uri.length() - 1 ) {
			return params;
		}
		for ( String pair : uri.substring( q + 1 ).split( "&" ) ) {
			int eq = pair.indexOf( '=' );
			if ( eq > 0 ) {
				params.put( Key.of( pair.substring( 0, eq ) ), pair.substring( eq + 1 ) );
			}
		}
		return params;
	}

	static String pathOf( String uri ) {
		int q = uri.indexOf( '?' );
		return q < 0 ? uri : uri.substring( 0, q );
	}

	/**
	 * Intercepts link navigation / GET forms and routes them in-process.
	 */
	private final class RouterClient extends WebViewClient {

		@Override
		public boolean shouldOverrideUrlLoading( WebView view, WebResourceRequest request ) {
			String url = request.getUrl().toString().replaceFirst( "^https?://boxlang\\.local", "" );
			navigate( pathOf( url ), "GET", parseQuery( url ) );
			return true;		// handled in-process
		}

		@Override
		public void onPageFinished( WebView view, String url ) {
			installFormHook();
		}
	}

	/**
	 * The JS-accessible bridge for POST form submissions.
	 */
	private final class Bridge {

		@JavascriptInterface
		public void submit( String route, String json ) {
			IStruct params = JSONToStruct( json );
			// WebView callbacks run on a JS thread; marshal back to the UI thread.
			webView.post( () -> navigate( pathOf( route ), "POST", params ) );
		}

		private IStruct JSONToStruct( String json ) {
			Object parsed = context.invokeFunction( Key.of( "JSONDeserialize" ), new Object[] { json } );
			return parsed instanceof IStruct s ? s : new Struct();
		}
	}
}
