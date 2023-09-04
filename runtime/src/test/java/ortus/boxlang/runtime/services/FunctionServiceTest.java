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

package ortus.boxlang.runtime.services;

import org.apache.commons.lang3.ClassUtils;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;

import com.google.common.base.Function;

import ortus.boxlang.runtime.context.TemplateBoxContext;
import ortus.boxlang.runtime.functions.BIF;
import ortus.boxlang.runtime.functions.global.Print;
import ortus.boxlang.runtime.loader.util.ClassDiscovery;

import org.junit.jupiter.api.DisplayName;
import static com.google.common.truth.Truth.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FunctionServiceTest {

	@DisplayName( "It can create the function service" )
	@Test
	void testItCanCreateIt() throws Throwable {
		FunctionService functionService = FunctionService.getInstance();
		assertThat( functionService ).isNotNull();
	}

	@DisplayName( "It can startup and register global functions" )
	@Test
	void testItCanStartup() throws Throwable {
		FunctionService functionService = FunctionService.getInstance();

		assertThat( functionService.getGlobalFunctionCount() ).isGreaterThan( 0 );
		assertThat( functionService.hasGlobalFunction( "print" ) ).isTrue();
	}

	@DisplayName( "It can invoke a global function" )
	@Test
	void testItCanInvokeAGlobalFunction() throws Throwable {
		FunctionService functionService = FunctionService.getInstance();

		assertThat( functionService.hasGlobalFunction( "print" ) ).isTrue();

		Optional<Object> result = functionService.getGlobalFunction( "print" )
		    .invoke(
		        new TemplateBoxContext(), "Hello Unit Test"
		    );

		assertThat( result.isPresent() ).isTrue();
		assertThat( ( Boolean ) result.get() ).isTrue();
	}

}
