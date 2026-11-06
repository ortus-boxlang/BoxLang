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

package ortus.boxlang.runtime.components.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.ComponentSourceKey;
import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

/**
 * Verifies the cache key the {@link Cache} component derives for its body-caching actions when no
 * {@code key} attribute is supplied.
 * <p>
 * The key has three parts, in order: the identity of the request being served, supplied at runtime by
 * {@link RequestBoxContext#getRequestCacheKey()}; the hashed identity of the file the component lives
 * in plus where it starts, injected at compile time as {@code _key}; and finally any explicit
 * {@code key} the caller supplied. Both of the first two are needed, because one addressed request can
 * pull in many files, and the same file can be reached from many requests.
 */
public class CacheImplicitKeyTest {

	static BoxRuntime	instance;
	static Path			templatePath;
	static String		requestCacheKey;

	IBoxContext			context;
	IScope				variables;

	@BeforeAll
	public static void setUp() throws IOException {
		instance		= BoxRuntime.getInstance( true );
		// A real file on disk, because the request context derives its identity from the template which
		// initiated the request. The code under test is executed ad-hoc, exactly as an include would be.
		templatePath	= Files.createTempFile( "boxlang-cache-key", ".bx" );
		requestCacheKey	= templatePath.toAbsolutePath().normalize().toString();
	}

	@AfterAll
	public static void tearDown() throws IOException {
		Files.deleteIfExists( templatePath );
	}

	@BeforeEach
	public void setupEach() {
		ScriptingRequestBoxContext scriptingContext = new ScriptingRequestBoxContext( instance.getRuntimeContext(), false );
		scriptingContext.loadApplicationDescriptor( templatePath.toUri() );
		context		= scriptingContext;
		variables	= context.getScopeNearby( VariablesScope.name );
		variables.clear();
		instance.getCacheService().getDefaultCache().clearAll();
	}

	@DisplayName( "The request context derives its cache key from the template which initiated the request" )
	@Test
	public void testRequestCacheKeyIsTheTemplatePath() {
		assertEquals( requestCacheKey, scriptingRequestContext().getRequestCacheKey() );
	}

	@DisplayName( "A request with no template has no cache key to contribute" )
	@Test
	public void testRequestCacheKeyIsNullWithoutATemplate() {
		assertNull( new ScriptingRequestBoxContext( instance.getRuntimeContext() ).getRequestCacheKey() );
	}

	@DisplayName( "Two cache blocks with different bodies do not share a key" )
	@Test
	public void testImplicitKeyDistinguishesBodies() {
		instance.executeSource(
		    """
		    bx:cache action="cache" {
		        writeOutput( "BODY_ONE" );
		    }
		    bx:cache action="cache" {
		        writeOutput( "BODY_TWO" );
		    }
		         """,
		    context, BoxSourceType.BOXSCRIPT );

		// One cache entry per block, rather than both blocks sharing a single entry
		assertEquals( 2, instance.getCacheService().getDefaultCache().getKeys().size() );
	}

	@DisplayName( "The key is the request identity, then the hashed page id, then the position" )
	@Test
	public void testKeyCombinesRequestIdentityWithCallSite() {
		instance.executeSource(
		    """
		    bx:cache action="cache" {
		        writeOutput( "POSITIONED_BODY" );
		    }
		         """,
		    context, BoxSourceType.BOXSCRIPT );

		// e.g. http://localhost:1443/products_pageid:29cfc3f2..._line:6_col:1
		String cacheKey = instance.getCacheService().getDefaultCache().getKeys().get( 0 ).toString();
		assertTrue(
		    cacheKey.startsWith( requestCacheKey + "_" + ComponentSourceKey.SOURCE_ID_LABEL ),
		    "expected the key to start with [" + requestCacheKey + "_" + ComponentSourceKey.SOURCE_ID_LABEL + "] but was [" + cacheKey + "]" );
		assertTrue( cacheKey.contains( "_line:" ), "expected a _line: segment but was [" + cacheKey + "]" );
		// Annotated with the column too, so two components on the same source line stay distinct
		assertTrue( cacheKey.contains( "_col:" ), "expected a _col: segment but was [" + cacheKey + "]" );
	}

	@DisplayName( "The source identity in the key is hashed, so a long path cannot bloat it" )
	@Test
	public void testSourceIdentityIsHashed() {
		instance.executeSource(
		    """
		    bx:cache action="cache" {
		        writeOutput( "HASHED_BODY" );
		    }
		         """,
		    context, BoxSourceType.BOXSCRIPT );

		// The page id is an MD5 of the source identity, never a raw path, so the key has a bounded
		// length regardless of how deep the file sits in the filesystem
		String cacheKey = instance.getCacheService().getDefaultCache().getKeys().get( 0 ).toString();
		assertTrue(
		    cacheKey.matches( ".*_pageid:[0-9a-f]{32}_line:\\d+_col:\\d+" ),
		    "expected a hashed page id but was [" + cacheKey + "]" );
	}

	@DisplayName( "The same cache block reuses its key on subsequent calls" )
	@Test
	public void testKeyIsStableForSameCallSite() {
		String source = """
		                bx:cache action="cache" {
		                    writeOutput( "computed:" & counter );
		                }
		                     """;

		variables.put( Key.of( "counter" ), 1 );
		instance.executeSource( source, context, BoxSourceType.BOXSCRIPT );
		assertEquals( 1, instance.getCacheService().getDefaultCache().getKeys().size() );

		// The same call site must resolve to the same key, so no new entry is created
		variables.put( Key.of( "counter" ), 2 );
		instance.executeSource( source, context, BoxSourceType.BOXSCRIPT );
		assertEquals( 1, instance.getCacheService().getDefaultCache().getKeys().size() );
	}

	@DisplayName( "An explicit key shared by two blocks is still scoped per call site" )
	@Test
	public void testExplicitKeyIsScopedPerCallSite() {
		// Both blocks use the same explicit key, so the call-site identity is what keeps them apart.
		// Without it these would collide on the single "shared" entry.
		instance.executeSource(
		    """
		    bx:cache action="cache" key="shared" {
		        writeOutput( "BODY_ONE" );
		    }
		    bx:cache action="cache" key="shared" {
		        writeOutput( "BODY_TWO" );
		    }
		         """,
		    context, BoxSourceType.BOXSCRIPT );

		assertEquals( 2, instance.getCacheService().getDefaultCache().getKeys().size() );
	}

	@DisplayName( "A dynamic key still combines with the call-site identity at runtime" )
	@Test
	public void testDynamicKeyCombinesWithCallSiteKey() {
		variables.put( Key.of( "dynamicKey" ), "fromVar" );
		instance.executeSource(
		    """
		    bx:cache action="cache" key="#dynamicKey#" {
		        writeOutput( "DYNAMIC_BODY" );
		    }
		         """,
		    context, BoxSourceType.BOXSCRIPT );

		// The runtime value of the dynamic key is what gets appended, not its source text
		String cacheKey = instance.getCacheService().getDefaultCache().getKeys().get( 0 ).toString();
		assertTrue( cacheKey.endsWith( "_fromVar" ), "expected the key to end with [_fromVar] but was [" + cacheKey + "]" );
		assertTrue(
		    cacheKey.startsWith( requestCacheKey + "_" + ComponentSourceKey.SOURCE_ID_LABEL ),
		    "expected the key to start with [" + requestCacheKey + "_" + ComponentSourceKey.SOURCE_ID_LABEL + "] but was [" + cacheKey + "]" );
	}

	@DisplayName( "The CFML cfcache tag gets the same call-site identity" )
	@Test
	public void testCfmlCacheTagGetsCallSiteIdentity() {
		// The cf prefix is stripped by the parser, so the compiler must recognise cfcache as the cache
		// component and inject the same _key it injects for bx:cache
		instance.executeSource(
		    """
		    <cfcache action="cache">
		        <cfoutput>CFML_BODY</cfoutput>
		    </cfcache>
		    <cfcache action="cache">
		        <cfoutput>CFML_BODY_TWO</cfoutput>
		    </cfcache>
		         """,
		    context, BoxSourceType.CFTEMPLATE );

		var keys = instance.getCacheService().getDefaultCache().getKeys();
		assertEquals( 2, keys.size() );
		for ( var key : keys ) {
			assertTrue(
			    key.toString().contains( ComponentSourceKey.SOURCE_ID_LABEL ),
			    "expected the key to contain [" + ComponentSourceKey.SOURCE_ID_LABEL + "] but was [" + key + "]" );
		}
	}

	@DisplayName( "Without a request identity the call-site identity alone still separates blocks" )
	@Test
	public void testCallSiteIdentityAloneSeparatesBlocks() {
		// No template, so the request contributes no identity of its own
		IBoxContext adHocContext = new ScriptingRequestBoxContext( instance.getRuntimeContext() );

		instance.executeSource(
		    """
		    bx:cache action="cache" {
		        writeOutput( "BODY_ONE" );
		    }
		    bx:cache action="cache" {
		        writeOutput( "BODY_TWO" );
		    }
		         """,
		    adHocContext, BoxSourceType.BOXSCRIPT );

		var keys = instance.getCacheService().getDefaultCache().getKeys();
		assertEquals( 2, keys.size() );
		for ( var key : keys ) {
			assertTrue(
			    key.toString().startsWith( ComponentSourceKey.SOURCE_ID_LABEL ),
			    "expected the key to start with [" + ComponentSourceKey.SOURCE_ID_LABEL + "] but was [" + key + "]" );
		}
	}

	/**
	 * The request context created for the current test
	 *
	 * @return The scripting request context used by the tests
	 */
	private RequestBoxContext scriptingRequestContext() {
		return context.getRequestContext();
	}
}
