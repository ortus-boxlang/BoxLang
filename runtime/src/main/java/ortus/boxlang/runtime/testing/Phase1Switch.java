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
package ortus.boxlang.runtime.testing;

import java.util.List;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
// BoxLang Auto Imports
import ortus.boxlang.runtime.dynamic.BaseTemplate;
import ortus.boxlang.runtime.dynamic.Referencer;
import ortus.boxlang.runtime.loader.ClassLocator;
import ortus.boxlang.runtime.loader.ImportDefinition;
import ortus.boxlang.runtime.operators.EqualsEquals;
import ortus.boxlang.runtime.operators.Plus;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;

public class Phase1Switch extends BaseTemplate {

	private static Phase1Switch					instance;

	private final static List<ImportDefinition>	imports	= List.of();

	private Phase1Switch() {
		this.path = "runtime\\src\\main\\java\\ortus\\boxlang\\runtime\\testing\\Phase1Switch.java";
	}

	public static synchronized Phase1Switch getInstance() {
		if ( instance == null ) {
			instance = new Phase1Switch();
		}
		return instance;
	}

	/*
	 * <pre>
	 * <cfscript>
	 * variables.foo = "bar";
	 * variables.systemOut = (create java:java.lang.System).out;
	 *
	 * switch( "12" ) {
	 * case "brad":
	 * variables.systemOut.println("case 1");
	 * break;
	 * case 42: {
	 * variables.systemOut.println("case 2");
	 * break;
	 * }
	 * case 5+7:
	 * variables.systemOut.println("case 3");
	 * case variables.foo:
	 * variables.systemOut.println("case 4");
	 * break;
	 * default:
	 * variables.systemOut.println("default case");
	 * }
	 * </cfscript>
	 * </pre>
	 */
	@Override
	public void _invoke( IBoxContext context ) {
		ClassLocator	classLocator	= ClassLocator.getInstance();

		// Reference to the variables scope
		IScope			variablesScope	= context.getScopeNearby( Key.of( "variables" ) );

		variablesScope.assign(
		    Key.of( "foo" ),
		    "bar"
		);

		variablesScope.assign(
		    Key.of( "systemOut" ),
		    Referencer.get(
		        classLocator.load( context, "java:java.lang.System", imports ),
		        Key.of( "out" ),
		        false
		    )
		);
		Object	switchValue	= "12";
		boolean	caseMatched	= false;

		for ( int i = 1; i == 1; i++ ) {
			if ( EqualsEquals.invoke( switchValue, "brad" ) ) {
				caseMatched = true;
				Referencer.getAndInvoke(
				    context,
				    variablesScope.get( Key.of( "systemOut" ) ),
				    Key.of( "println" ),
				    new Object[] { "case 1" },
				    false
				);
				break;
			}
			if ( caseMatched || EqualsEquals.invoke( switchValue, 42 ) ) {
				caseMatched = true;
				Referencer.getAndInvoke(
				    context,
				    variablesScope.get( Key.of( "systemOut" ) ),
				    Key.of( "println" ),
				    new Object[] { "case 2" },
				    false
				);
				break;
			}

			if ( caseMatched || EqualsEquals.invoke( switchValue, Plus.invoke( 5, 7 ) ) ) {
				caseMatched = true;
				Referencer.getAndInvoke(
				    context,
				    variablesScope.get( Key.of( "systemOut" ) ),
				    Key.of( "println" ),
				    new Object[] { "case 3" },
				    false
				);
			}
			if ( caseMatched || EqualsEquals.invoke( switchValue, variablesScope.get( Key.of( "foo" ) ) ) ) {
				caseMatched = true;
				Referencer.getAndInvoke(
				    context,
				    variablesScope.get( Key.of( "systemOut" ) ),
				    Key.of( "println" ),
				    new Object[] { "case 4" },
				    false
				);
				break;
			}

			Referencer.getAndInvoke(
			    context,
			    variablesScope.get( Key.of( "systemOut" ) ),
			    Key.of( "println" ),
			    new Object[] { "default case" },
			    false
			);
		}

	}

	/**
	 * Main method
	 *
	 * @param args
	 */
	public static void main( String[] args ) {
		// This is the main method, it will be invoked when the template is executed
		// You can use this
		// Get a runtime going
		BoxRuntime boxRuntime = BoxRuntime.getInstance( true );

		try {
			boxRuntime.executeTemplate( Phase1Switch.getInstance() );
		} catch ( Throwable e ) {
			e.printStackTrace();
			System.exit( 1 );
		}

		// Bye bye! Ciao Bella!
		boxRuntime.shutdown();
	}
}
