// /**
// * [BoxLang]
// *
// * Copyright [2023] [Ortus Solutions, Corp]
// *
// * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
// * License. You may obtain a copy of the License at
// *
// * http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
// * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
// * governing permissions and limitations under the License.
// */
// package ortus.boxlang.runtime.bifs.global.decision;

// import java.util.Arrays;

// import ortus.boxlang.runtime.bifs.BIF;
// import ortus.boxlang.runtime.bifs.BoxBIF;
// import ortus.boxlang.runtime.context.IBoxContext;
// import ortus.boxlang.runtime.scopes.ArgumentsScope;
// import ortus.boxlang.runtime.scopes.Key;
// import ortus.boxlang.runtime.types.Argument;

// @BoxBIF
// public class IsDefined extends BIF {

// /**
// * Constructor
// */
// public IsDefined() {
// super();
// declaredArguments = new Argument[] {
// new Argument( true, "any", Key.value ),
// };
// }

// /**
// * Determine whether a given variable is defined.
// *
// * @param context The context in which the BIF is being invoked.
// * @param arguments Argument scope for the BIF.
// *
// * @argument.value Variable reference in string form. For example `form.name`, `variables.foo` or `myStruct.bar`.
// */
// public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
// String reference = arguments.getAsString( Key.value );
// if ( reference == null || reference.isEmpty() ) {
// return false;
// }
// if ( reference.contains( "." ) ) {
// String[] parts = reference.split( "\\." );
// return context.getScope( Key.of( parts[ 0 ] ) )
// .containsKey( parts[ 1 ] );
// }
// }
// }