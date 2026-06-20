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

/**
 * The outcome of dispatching a request through the front controller: either rendered HTML
 * to load into the WebView, or a relocate (redirect) target to re-dispatch.
 */
public class DispatchResult {

	/**
	 * The rendered HTML, or {@code null} when relocating.
	 */
	private final String	html;

	/**
	 * The relocate target, or {@code null} when rendered.
	 */
	private final String	relocateTarget;

	private DispatchResult( String html, String relocateTarget ) {
		this.html			= html;
		this.relocateTarget	= relocateTarget;
	}

	/**
	 * Build a rendered result.
	 *
	 * @param html The rendered HTML
	 *
	 * @return A rendered {@link DispatchResult}
	 */
	public static DispatchResult rendered( String html ) {
		return new DispatchResult( html, null );
	}

	/**
	 * Build a relocate result.
	 *
	 * @param target The relocate target (URI or named route)
	 *
	 * @return A relocating {@link DispatchResult}
	 */
	public static DispatchResult relocate( String target ) {
		return new DispatchResult( null, target );
	}

	/**
	 * @return {@code true} if this result is a relocate
	 */
	public boolean isRelocate() {
		return this.relocateTarget != null;
	}

	/**
	 * @return The rendered HTML, or {@code null} when relocating
	 */
	public String getHtml() {
		return this.html;
	}

	/**
	 * @return The relocate target, or {@code null} when rendered
	 */
	public String getRelocateTarget() {
		return this.relocateTarget;
	}
}
