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
package TestCases.applications;

import static com.google.common.truth.Truth.assertThat;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.ConfigOverrideBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.RequestScope;

public class ApplicationLookups {

	static BoxRuntime	instance;
	IBoxContext			context;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@BeforeEach
	public void setupEach() {
	}

	@Test
	public void testAppTemplateInRoot() {
		context = getContext( "src/test/java/TestCases/applications/appTemplate/", "index.bxm" );
		instance.executeTemplate(
		    "index.bxm",
		    context );

		IScope request = context.getScopeNearby( RequestScope.name );
		assertThat( request.get( "applicationbxmran" ) ).isEqualTo( true );
		assertThat( request.get( "indexbxmran" ) ).isEqualTo( true );
	}

	@Test
	public void testAppClassInRoot() {
		context = getContext( "src/test/java/TestCases/applications/appClass", "index.bxm" );
		instance.executeTemplate(
		    "index.bxm",
		    context );

		IScope request = context.getScopeNearby( RequestScope.name );
		assertThat( request.get( "applicationbxran" ) ).isEqualTo( true );
		assertThat( request.get( "onRequestStart" ) ).isEqualTo( true );
		assertThat( request.get( "indexbxmran" ) ).isEqualTo( true );
	}

	@Test
	public void testAppTemplateInEmptySub() {
		context = getContext( "src/test/java/TestCases/applications/appTemplate/", "sub1/index.bxm" );
		instance.executeTemplate(
		    "sub1/index.bxm",
		    context );

		IScope request = context.getScopeNearby( RequestScope.name );
		assertThat( request.get( "applicationbxmran" ) ).isEqualTo( true );
		assertThat( request.get( "indexbxmsub1ran" ) ).isEqualTo( true );
	}

	@Test
	public void testAppClassInAppSub() {
		context = getContext( "src/test/java/TestCases/applications/appClass/", "sub2/index.bxm" );
		instance.executeTemplate(
		    "sub2/index.bxm",
		    context );

		IScope request = context.getScopeNearby( RequestScope.name );
		assertThat( request.get( "applicationbxsub2ran" ) ).isEqualTo( true );
		assertThat( request.get( "onRequestStartsub2" ) ).isEqualTo( true );
		assertThat( request.get( "indexbxmsub2ran" ) ).isEqualTo( true );
	}

	@Test
	public void testAppTemplateInAppSub() {
		context = getContext( "src/test/java/TestCases/applications/appTemplate/", "sub2/index.bxm" );
		instance.executeTemplate(
		    "sub2/index.bxm",
		    context );

		IScope request = context.getScopeNearby( RequestScope.name );
		assertThat( request.get( "applicationbxmsub2ran" ) ).isEqualTo( true );
		assertThat( request.get( "indexbxmsub2ran" ) ).isEqualTo( true );
	}

	@Test
	public void testAppClassInEmptySub() {
		context = getContext( "src/test/java/TestCases/applications/appClass/", "sub2/index.bxm" );
		instance.executeTemplate(
		    "sub2/index.bxm",
		    context );

		IScope request = context.getScopeNearby( RequestScope.name );
		assertThat( request.get( "applicationbxsub2ran" ) ).isEqualTo( true );
		assertThat( request.get( "onRequestStartsub2" ) ).isEqualTo( true );
		assertThat( request.get( "indexbxmsub2ran" ) ).isEqualTo( true );
	}

	@Test
	public void testAppClassInMapping() {
		instance.getConfiguration().mappings.put( "/secret", new java.io.File( "src/test/java/TestCases/applications/external" ).getAbsolutePath() );
		context = getContext( "src/test/java/TestCases/applications/appClass/", "secret/index.bxm" );
		instance.executeTemplate(
		    "secret/index.bxm",
		    context );

		IScope request = context.getScopeNearby( RequestScope.name );
		assertThat( request.get( "externalapplicationbxran" ) ).isEqualTo( true );
		assertThat( request.get( "externalonRequestStart" ) ).isEqualTo( true );
		assertThat( request.get( "externalindexbxmran" ) ).isEqualTo( true );
	}

	@Test
	public void testAppClassInMappingSub() {
		instance.getConfiguration().mappings.put( "/secret", new java.io.File( "src/test/java/TestCases/applications/external" ).getAbsolutePath() );
		context = getContext( "src/test/java/TestCases/applications/appClass/", "secret/sub1/index.bxm" );
		instance.executeTemplate(
		    "secret/sub1/index.bxm",
		    context );

		IScope request = context.getScopeNearby( RequestScope.name );
		assertThat( request.get( "externalapplicationbxran" ) ).isEqualTo( true );
		assertThat( request.get( "externalonRequestStart" ) ).isEqualTo( true );
		assertThat( request.get( "externalindexbxmsub1ran" ) ).isEqualTo( true );
	}

	private IBoxContext getContext( String rootPath, String template ) {
		try {
			return new ScriptingRequestBoxContext( new ConfigOverrideBoxContext( instance.getRuntimeContext(), config -> {
				config.getAsStruct( Key.mappings ).put( "/", new java.io.File( rootPath ).getAbsolutePath() );
				return config;
			} ), new URI( template ) );
		} catch ( URISyntaxException e ) {
			throw new RuntimeException( e );
		}
	}

}
