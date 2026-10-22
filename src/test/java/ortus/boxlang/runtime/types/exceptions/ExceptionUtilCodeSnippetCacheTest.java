/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.runtime.types.exceptions;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.cache.providers.ICacheProvider;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.Key;

public class ExceptionUtilCodeSnippetCacheTest {

	private static final Key	CODE_SNIPPETS_CACHE	= Key.of( "bxCodeSnippets" );

	static BoxRuntime			instance;
	IBoxContext					context;

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@BeforeEach
	public void setupEach() {
		context = new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		ExceptionUtil.clearCodeSnippetCache();
	}

	@DisplayName( "Only the lines around the requested line number are rendered" )
	@Test
	public void testOnlyTheRequestedLinesAreRendered( @TempDir Path tempDir ) throws IOException {
		Path file = tempDir.resolve( "Snippet.bx" );
		Files.writeString( file, "line 1\nline 2\nline 3\nline 4\nline 5\nline 6\nline 7\nline 8\nline 9\n" );

		ExceptionUtil.CodeSnippet snippet = ExceptionUtil.renderSnippet( file.toFile(), 5 );

		assertThat( snippet.plain ).isEqualTo( "3: line 3\n4: line 4\n5: line 5\n6: line 6\n7: line 7\n" );
		assertThat( snippet.html ).contains( "<b>5: line 5</b>" );
		assertThat( snippet.html ).doesNotContain( "line 2" );
	}

	@DisplayName( "Lines are html escaped and the requested line is highlighted" )
	@Test
	public void testSnippetEscapingAndHighlight( @TempDir Path tempDir ) throws IOException {
		Path file = tempDir.resolve( "Escaped.bx" );
		Files.writeString( file, "<b>bold</b> & \"quoted\"\n" );

		ExceptionUtil.CodeSnippet snippet = ExceptionUtil.renderSnippet( file.toFile(), 1 );

		assertThat( snippet.plain ).isEqualTo( "1: <b>bold</b> & \"quoted\"\n" );
		assertThat( snippet.html ).isEqualTo( "<b>1: &lt;b&gt;bold&lt;/b&gt; &amp; &quot;quoted&quot;</b><br>" );
	}

	@DisplayName( "A line number past the end of the file renders nothing" )
	@Test
	public void testSnippetPastEndOfFile( @TempDir Path tempDir ) throws IOException {
		Path file = tempDir.resolve( "Short.bx" );
		Files.writeString( file, "only line\n" );

		ExceptionUtil.CodeSnippet snippet = ExceptionUtil.renderSnippet( file.toFile(), 50 );

		assertThat( snippet.plain ).isEmpty();
		assertThat( snippet.html ).isEmpty();
	}

	@DisplayName( "Precompiled sources and missing files render an empty snippet" )
	@Test
	public void testUnreadableSources( @TempDir Path tempDir ) throws IOException {
		Path precompiled = tempDir.resolve( "Compiled.bx" );
		Files.write( precompiled, new byte[] { ( byte ) 0xCA, ( byte ) 0xFE, ( byte ) 0xBA, ( byte ) 0xBE, 0, 0, 0, 52 } );

		ExceptionUtil.CodeSnippet fromByteCode = ExceptionUtil.renderSnippet( precompiled.toFile(), 1 );
		assertThat( fromByteCode.plain ).isEqualTo( "Precompiled source not available." );
		assertThat( fromByteCode.html ).isEqualTo( "Precompiled source not available." );

		ExceptionUtil.CodeSnippet fromNowhere = ExceptionUtil.renderSnippet( tempDir.resolve( "nope.bx" ).toFile(), 1 );
		assertThat( fromNowhere.plain ).isEmpty();
		assertThat( fromNowhere.html ).isEmpty();
	}

	@DisplayName( "A rendered snippet is cached and reused" )
	@Test
	public void testSnippetsAreCached( @TempDir Path tempDir ) throws IOException {
		Path						file	= writeSource( tempDir, "Cached.bx", "cache this line\n" );
		ICacheProvider				cache	= instance.getCacheService().getCache( CODE_SNIPPETS_CACHE );

		ExceptionUtil.CodeSnippet	first	= ExceptionUtil.getSurroundingLinesOfCode( file.toString(), 1 );

		assertThat( first.plain ).isEqualTo( "1: cache this line\n" );
		assertThat( cache.getKeys().size() ).isEqualTo( 1 );

		// The file is rewritten but not "changed" as far as the cache is concerned, so the cached snippet wins
		Files.writeString( file, "this is not what we should see\n" );
		Files.setLastModifiedTime( file, FileTime.fromMillis( first.lastModified ) );

		assertThat( ExceptionUtil.getSurroundingLinesOfCode( file.toString(), 1 ).plain ).isEqualTo( "1: cache this line\n" );
		assertThat( cache.getKeys().size() ).isEqualTo( 1 );
	}

	@DisplayName( "Changing the file on disk tosses the cached entry" )
	@Test
	public void testChangedFilesInvalidateTheEntry( @TempDir Path tempDir ) throws IOException {
		Path						file	= writeSource( tempDir, "Changing.bx", "original line\n" );

		ExceptionUtil.CodeSnippet	first	= ExceptionUtil.getSurroundingLinesOfCode( file.toString(), 1 );
		assertThat( first.plain ).isEqualTo( "1: original line\n" );

		// A newer timestamp is what an edit looks like to the cache
		Files.writeString( file, "replacement line\n" );
		Files.setLastModifiedTime( file, FileTime.fromMillis( System.currentTimeMillis() - 10_000 ) );

		ExceptionUtil.CodeSnippet second = ExceptionUtil.getSurroundingLinesOfCode( file.toString(), 1 );
		assertThat( second.plain ).isEqualTo( "1: replacement line\n" );
	}

	@DisplayName( "A file that is older than the cached entry reuses the cached snippet" )
	@Test
	public void testOlderFilesReuseTheCachedSnippet( @TempDir Path tempDir ) throws IOException {
		Path						file	= writeSource( tempDir, "Older.bx", "deployed first\n" );

		ExceptionUtil.CodeSnippet	first	= ExceptionUtil.getSurroundingLinesOfCode( file.toString(), 1 );
		assertThat( first.plain ).isEqualTo( "1: deployed first\n" );

		// Same file on another server, deployed before the copy the cached entry came from
		Files.writeString( file, "a different deployment\n" );
		Files.setLastModifiedTime( file, FileTime.fromMillis( first.lastModified - 60_000 ) );

		assertThat( ExceptionUtil.getSurroundingLinesOfCode( file.toString(), 1 ).plain ).isEqualTo( "1: deployed first\n" );
	}

	@DisplayName( "Changing a file discards every snippet cached for it" )
	@Test
	public void testChangedFileDiscardsAllOfItsSnippets( @TempDir Path tempDir ) throws IOException {
		Path			file	= writeSource( tempDir, "Purge.bx", "line one\nline two\nline three\n" );
		ICacheProvider	cache	= instance.getCacheService().getCache( CODE_SNIPPETS_CACHE );

		ExceptionUtil.getSurroundingLinesOfCode( file.toString(), 1 );
		ExceptionUtil.getSurroundingLinesOfCode( file.toString(), 3 );
		assertThat( cache.getKeys().size() ).isEqualTo( 2 );

		// A newer file invalidates every line cached for it, not just the line that was asked for
		Files.writeString( file, "line one\nline two\nline three changed\n" );
		Files.setLastModifiedTime( file, FileTime.fromMillis( System.currentTimeMillis() - 10_000 ) );

		assertThat( ExceptionUtil.getSurroundingLinesOfCode( file.toString(), 1 ).plain ).contains( "line one" );
		assertThat( cache.getKeys().size() ).isEqualTo( 1 );
	}

	@DisplayName( "Deleting the file tosses the cached entry" )
	@Test
	public void testDeletedFilesInvalidateTheEntry( @TempDir Path tempDir ) throws IOException {
		Path						file	= writeSource( tempDir, "Deleted.bx", "going away\n" );

		ExceptionUtil.CodeSnippet	first	= ExceptionUtil.getSurroundingLinesOfCode( file.toString(), 1 );
		assertThat( first.plain ).isEqualTo( "1: going away\n" );

		Files.delete( file );

		assertThat( ExceptionUtil.getSurroundingLinesOfCode( file.toString(), 1 ).plain ).isEmpty();
	}

	@DisplayName( "The cache can be cleared directly and through systemCacheClear" )
	@Test
	public void testClearingTheCache( @TempDir Path tempDir ) throws IOException {
		Path			file	= writeSource( tempDir, "Cleared.bx", "clear me\n" );
		ICacheProvider	cache	= instance.getCacheService().getCache( CODE_SNIPPETS_CACHE );

		ExceptionUtil.getSurroundingLinesOfCode( file.toString(), 1 );
		assertThat( cache.getKeys().size() ).isEqualTo( 1 );

		ExceptionUtil.clearCodeSnippetCache();
		assertThat( cache.getKeys().isEmpty() ).isTrue();

		ExceptionUtil.getSurroundingLinesOfCode( file.toString(), 1 );
		assertThat( cache.getKeys().size() ).isEqualTo( 1 );

		instance.executeStatement( "systemCacheClear( \"codesnippets\" )", context );
		assertThat( cache.getKeys().isEmpty() ).isTrue();
	}

	@DisplayName( "The cache is created on demand when the runtime doesn't have one registered" )
	@Test
	public void testCacheIsCreatedWhenMissing( @TempDir Path tempDir ) throws IOException {
		Path	file	= writeSource( tempDir, "Fallback.bx", "fallback line\n" );
		var		caches	= instance.getCacheService();

		caches.shutdownCache( CODE_SNIPPETS_CACHE );
		assertThat( caches.hasCache( CODE_SNIPPETS_CACHE ) ).isFalse();

		assertThat( ExceptionUtil.getSurroundingLinesOfCode( file.toString(), 1 ).plain ).isEqualTo( "1: fallback line\n" );
		assertThat( caches.hasCache( CODE_SNIPPETS_CACHE ) ).isTrue();
	}

	/**
	 * Write a source file and backdate it so the entry is not invalidated by a same millisecond timestamp
	 */
	private Path writeSource( Path tempDir, String name, String content ) throws IOException {
		Path file = tempDir.resolve( name );
		Files.writeString( file, content );
		Files.setLastModifiedTime( file, FileTime.fromMillis( System.currentTimeMillis() - 60_000 ) );
		return file;
	}
}
