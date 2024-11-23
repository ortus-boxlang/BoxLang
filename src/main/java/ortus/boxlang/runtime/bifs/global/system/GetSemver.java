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
package ortus.boxlang.runtime.bifs.global.system;

import org.semver4j.Semver;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;

@BoxBIF
public class GetSemver extends BIF {

	/**
	 * Constructor
	 */
	public GetSemver() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( false, "string", Key.version, "" )
		};
	}

	/**
	 * Parses and returns a Semver object version of the passed version string or if you do not pass
	 * in a version, we will return to you a Semver builder {@link Semver#of()} object, so you can programmaticaly build
	 * a version object.
	 * <p>
	 * Semver is a tool that provides useful methods to manipulate versions that follow the "semantic versioning"
	 * specification (see <a href="http://semver.org">semver.org</a> and <a href="https://github.com/semver4j/semver4j">github</a>).
	 * <p>
	 * Some of the methods provided by Semver are:
	 * <ul>
	 * <li>{@link Semver#compare(Semver)}: Compares two versions and returns -1, 0, or 1 if the first version is less than,
	 * equal to, or greater than the second version, respectively.</li>
	 * <li>{@link Semver#satisfies(String)}: Checks if the version satisfies the given range.</li>
	 * <li>{@link Semver#valid()}: Checks if the version is valid.</li>
	 * <li>{@link Semver#incMajor()}: Increments the major version.</li>
	 * <li>{@link Semver#incMinor()}: Increments the minor version.</li>
	 * <li>{@link Semver#incPatch()}: Increments the patch version.</li>
	 * <li>{@link Semver#incPreRelease()}: Increments the pre-release version.</li>
	 * <li>{@link Semver#incBuild()}: Increments the build version.</li>
	 * <li>{@link Semver#isStable()}: Checks if the version is stable.</li>
	 * </ul>
	 * <p>
	 * Here are some examples of how to parse and manipulate versions using Semver:
	 *
	 * <pre>
	 *
	 * var version = GetSemver( "1.2.3-alpha+20151212" );
	 * var version = GetSemver( "1.2.3-alpha" );
	 * var version = GetSemver( "1.2.3" );
	 * var version = GetSemver( "1.2.3+20151212" );
	 * var version = GetSemver( "1.2.3-alpha.1" );
	 * var version = GetSemver( "1.2.3-alpha.beta" );
	 * </pre>
	 *
	 * Here are some examples of comparing versions using Semver:
	 *
	 * <pre>
	 * var version1 = GetSemver( "1.2.3" );
	 * var version2 = GetSemver( "1.2.4" );
	 * var version3 = GetSemver( "1.3.0" );
	 *
	 * version1.compare( version2 ); // -1
	 * version1.compare( version3 ); // -1
	 * version2.compare( version3 ); // -1
	 * </pre>
	 *
	 * <p>
	 * To use the builder you can do something like this:
	 *
	 * <pre>
	 *
	 * var version = GetSemver().withMajor( 1 ).withMinor( 2 ).withPatch( 3 ).withPreRelease( "alpha" ).toSemver();
	 * var versionString = GetSemver().withMajor( 1 ).withMinor( 2 ).withPatch( 3 ).withPreRelease( "alpha" ).toString();
	 * </pre>
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.version The version string to parse.
	 *
	 * @return A Semver object
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String version = arguments.getAsString( Key.version );
		return ( version.isEmpty() ) ? Semver.of() : new Semver( version );
	}
}
