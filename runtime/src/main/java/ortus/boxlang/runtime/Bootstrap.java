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
 * BoxLang bootstrapper. Loads up the engine
 */
public class Bootstrap {

	public String getGreeting() {
		return "Hello World!";
	}

	public String getGreeting( String name ) {
		return "Hello " + name + "!";
	}

	public static void main( String[] args ) {
		BoxRuntime boxRuntime = BoxRuntime.startup();

		ExecutionContext context = new ExecutionContext();
		// Here is where we presumably boostrap a page or class that we are executing in our new context.
		// JIT if neccessary

		System.out.println( new Bootstrap().getGreeting() );

		boxRuntime.shutdown();
	}
}
