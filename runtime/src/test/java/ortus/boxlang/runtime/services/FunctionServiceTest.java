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

import ortus.boxlang.runtime.functions.BIF;
import ortus.boxlang.runtime.functions.global.Print;
import ortus.boxlang.runtime.util.ClassDiscovery;

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
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FunctionServiceTest {

	@DisplayName( "It can create the function service" )
	@Test
	void testItCanCreateIt() throws Throwable {
		FunctionService functionService = FunctionService.getInstance();
		assertThat( functionService ).isNotNull();

		String packageName = "ortus.boxlang.runtime.functions.global";

		System.out.println( Arrays.toString( ClassDiscovery.getClassFiles( packageName ) ) );
	}

}
