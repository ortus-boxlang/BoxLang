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
package ortus.boxlang.debugger;

public class Debugee {

	public static void main( String[] args ) {
		System.out.println( args[ 0 ] );
		String jpda = "Java Platform Debugger Architecture";
		System.out.println( "Hi Everyone, Welcome to " + jpda ); // add a break point here
		String	jdi		= "Java Debug Interface"; // add a break point here and also stepping in here
		String	text	= "Today, we'll dive into " + jdi;
		System.out.println( text );
	}
}
