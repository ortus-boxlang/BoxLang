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
package ortus.boxlang.modules.javatest;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.modules.BoxModule;
import ortus.boxlang.runtime.modules.IModuleConfig;
import ortus.boxlang.runtime.modules.ModuleRecord;

/**
 * Test implementation of IModuleConfig for use in unit tests.
 * Tracks lifecycle calls and demonstrates the {@code @BoxModule} annotation metadata convention.
 */
@BoxModule( name = "renamedJarModule", version = "2.0.0", author = "Ortus Solutions", description = "A pure-Java test module", webURL = "https://www.ortussolutions.com" )
public class JavaTestModuleConfig implements IModuleConfig {

	// --------------------------------------------------------------------------
	// Lifecycle tracking flags (inspected by tests)
	// --------------------------------------------------------------------------
	public static boolean	configureCalled		= false;
	public static boolean	onLoadCalled		= false;
	public static boolean	onUnloadCalled		= false;
	public static String	configureSettingKey	= null;

	/**
	 * Resets all tracking flags; call in test @BeforeEach.
	 */
	public static void reset() {
		configureCalled		= false;
		onLoadCalled		= false;
		onUnloadCalled		= false;
		configureSettingKey	= null;
	}

	@Override
	public void configure( IBoxContext context, ModuleRecord moduleRecord ) {
		configureCalled = true;
		// Demonstrate direct settings mutation
		moduleRecord.settings.put( "javaKey", "javaValue" );
		configureSettingKey = "javaKey";
	}

	@Override
	public void onLoad( IBoxContext context, ModuleRecord moduleRecord ) {
		onLoadCalled = true;
	}

	@Override
	public void onUnload( IBoxContext context, ModuleRecord moduleRecord ) {
		onUnloadCalled = true;
	}

}
