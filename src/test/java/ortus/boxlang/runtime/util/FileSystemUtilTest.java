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
package ortus.boxlang.runtime.util;

import static com.google.common.truth.Truth.assertThat;

import java.net.URI;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class FileSystemUtilTest {

	@DisplayName( "createFileUri encodes spaces in Unix absolute paths" )
	@Test
	void testCreateFileUriUnixAbsoluteWithSpaces() {
		URI uri = FileSystemUtil.createFileUri( "/ortus solutions/test.bxs" );
		assertThat( uri.toString() ).doesNotContain( " " );
		assertThat( uri.toString() ).contains( "%20" );
		assertThat( uri.getScheme() ).isEqualTo( "file" );
	}

	@DisplayName( "createFileUri handles Unix absolute paths without spaces" )
	@Test
	void testCreateFileUriUnixAbsoluteWithoutSpaces() {
		URI uri = FileSystemUtil.createFileUri( "/ortus/test.bxs" );
		assertThat( uri.toString() ).isEqualTo( "file:///ortus/test.bxs" );
		assertThat( uri.getScheme() ).isEqualTo( "file" );
	}

	@DisplayName( "createFileUri encodes spaces in relative paths" )
	@Test
	void testCreateFileUriRelativeWithSpaces() {
		URI uri = FileSystemUtil.createFileUri( "ortus solutions/test.bxs" );
		assertThat( uri.toString() ).doesNotContain( " " );
		assertThat( uri.toString() ).contains( "%20" );
		assertThat( uri.isAbsolute() ).isFalse();
	}

	@DisplayName( "createFileUri handles relative paths without spaces" )
	@Test
	void testCreateFileUriRelativeWithoutSpaces() {
		URI uri = FileSystemUtil.createFileUri( "ortus/test.bxs" );
		assertThat( uri.toString() ).isEqualTo( "ortus/test.bxs" );
		assertThat( uri.isAbsolute() ).isFalse();
	}

	@DisplayName( "createFileUri converts backslashes in relative paths" )
	@Test
	void testCreateFileUriRelativeBackslashes() {
		URI uri = FileSystemUtil.createFileUri( "ortus\\test.bxs" );
		assertThat( uri.toString() ).doesNotContain( "\\" );
		assertThat( uri.isAbsolute() ).isFalse();
	}

}
