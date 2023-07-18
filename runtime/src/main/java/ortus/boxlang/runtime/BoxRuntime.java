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
package ortus.boxlang.runtime;

/**
 * Represents the top level runtime container for box lang. Config, global scopes, mappings, threadpools, etc all go here.
 * All threads, requests, invocations, etc share this.
 */
public class BoxRuntime {

	private static BoxRuntime boxRuntimeInstance;

	// Prevent outside instantiation to follow singleton pattern
	protected BoxRuntime() {

	}

	public synchronized static BoxRuntime startup() {
		System.out.println( "Starting up Box Runtime" );
		boxRuntimeInstance = new BoxRuntime();
		return getInstance();
	}

	public static void shutdown() {
		System.out.println( "Shutting down Box Runtime" );
		boxRuntimeInstance = null;
	}

	public static BoxRuntime getInstance() {
		return boxRuntimeInstance;
	}

}
