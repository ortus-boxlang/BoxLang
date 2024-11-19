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
package ortus.boxlang.runtime.loader.resolvers;

import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.modules.ModuleRecord;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.ModuleService;

public abstract class AbstractResolverTest {

	public static BoxRuntime	runtime;
	public static IBoxContext	context;

	@BeforeAll
	public static void beforeAll() {
		runtime = BoxRuntime.getInstance( true );
	}

	@BeforeEach
	public void beforeEach() {
		context = new ScriptingRequestBoxContext( runtime.getRuntimeContext() );
	}

	public void loadTestModule() {
		Key				moduleName		= new Key( "test" );
		String			physicalPath	= Paths.get( "./modules/test" ).toAbsolutePath().toString();
		ModuleRecord	moduleRecord	= new ModuleRecord( physicalPath );
		ModuleService	moduleService	= runtime.getModuleService();

		moduleRecord
		    .loadDescriptor( context )
		    .register( context )
		    .activate( context );

		moduleService.getRegistry().put( moduleName, moduleRecord );
	}

}
