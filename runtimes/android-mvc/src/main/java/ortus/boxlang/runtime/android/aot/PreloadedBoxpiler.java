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
package ortus.boxlang.runtime.android.aot;

import java.io.PrintStream;
import java.util.List;

import ortus.boxlang.compiler.Boxpiler;
import ortus.boxlang.compiler.ClassInfo;
import ortus.boxlang.compiler.parser.ParsingResult;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * The Android boxpiler: <b>resolve-only, never compile or define</b>.
 * <p>
 * On Android there is no runtime code generation and no runtime class definition from
 * bytecode (ART forbids {@code defineClass()} of JVM bytecode). All BoxLang classes are
 * AOT-compiled by {@code BXCompiler}, extracted to {@code .class} by {@link BoxClassExtractor},
 * dexed into the APK by D8/R8, and therefore already loadable by the application class loader.
 * Resolution happens through parent-first delegation (see {@link PreloadedClassLoader}).
 * <p>
 * Like {@code NoOpBoxpiler}, this extends {@link Boxpiler} and inherits the class-pool and
 * lookup machinery; it differs in that {@link #compileClassInfo} never defines classes. If it
 * is ever reached, the requested class was not AOT-compiled/dexed, which is a build error —
 * we fail loudly rather than attempt an illegal runtime define.
 * <p>
 * This boxpiler is registered via {@code ServiceLoader} only in the Android distribution
 * (its provider file ships in the {@code :runtimes:android} module), so JVM builds continue to
 * use the ASM boxpiler.
 */
public class PreloadedBoxpiler extends Boxpiler {

	/**
	 * The unique boxpiler name.
	 */
	public static final Key NAME = Key.of( "preloaded" );

	public PreloadedBoxpiler() {
		super();
	}

	@Override
	public Key getName() {
		return NAME;
	}

	@Override
	public void printTranspiledCode( ParsingResult result, ClassInfo classInfo, PrintStream target ) {
		throw new BoxRuntimeException( "PreloadedBoxpiler does not transpile — classes are AOT-compiled at build time." );
	}

	/**
	 * Never defines classes. Reaching this method means a class was requested that is not
	 * present on the app class loader (i.e. it was not AOT-compiled and dexed into the APK).
	 *
	 * @param classPoolName The class pool name
	 * @param FQN           The fully-qualified class name requested
	 *
	 * @return never returns normally
	 */
	@Override
	public List<byte[]> compileClassInfo( String classPoolName, String FQN ) {
		throw new BoxRuntimeException(
		    "PreloadedBoxpiler cannot compile [" + FQN + "]. On Android, BoxLang classes must be "
		        + "AOT-compiled (BXCompiler), extracted to .class (BoxClassExtractor), and dexed into "
		        + "the APK. There is no runtime compilation or defineClass on ART. Ensure the source "
		        + "was included in the build's compileBoxLangAot step and that its generated FQN is stable." );
	}
}
