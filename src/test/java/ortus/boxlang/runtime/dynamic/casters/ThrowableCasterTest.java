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
package ortus.boxlang.runtime.dynamic.casters;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.CustomException;

public class ThrowableCasterTest {

	@Test
	void testItCanCastAThrowable() {
		assertThat( ThrowableCaster.cast( new Exception( "my message" ) ).getMessage() ).isEqualTo( "my message" );
	}

	@Test
	void testItCanCastAStruct() {
		Throwable t = ThrowableCaster.cast( Struct.of(
		    Key.message, "my message",
		    Key.detail, "my detail",
		    Key.type, "my type",
		    Key.errorcode, "my errorcode",
		    Key.extendedinfo, "my extendedinfo",
		    Key.stackTrace, "my stacktrace"
		) );
		assertThat( t.getMessage() ).isEqualTo( "my message" );
		assertThat( t ).isInstanceOf( CustomException.class );
		CustomException ce = ( CustomException ) t;
		assertThat( ce.getDetail() ).isEqualTo( "my detail" );
		assertThat( ce.getType() ).isEqualTo( "my type" );
		assertThat( ce.getErrorCode() ).isEqualTo( "my errorcode" );
		assertThat( ce.getExtendedInfo() ).isEqualTo( "my extendedinfo\nmy stacktrace" );
	}

	@Test
	void testItCanCastAStructWithoutExtendedInfo() {
		Throwable t = ThrowableCaster.cast( Struct.of(
		    Key.message, "my message",
		    Key.detail, "my detail",
		    Key.type, "my type",
		    Key.errorcode, "my errorcode",
		    Key.stackTrace, "my stacktrace"
		) );
		assertThat( t.getMessage() ).isEqualTo( "my message" );
		assertThat( t ).isInstanceOf( CustomException.class );
		CustomException ce = ( CustomException ) t;
		assertThat( ce.getDetail() ).isEqualTo( "my detail" );
		assertThat( ce.getType() ).isEqualTo( "my type" );
		assertThat( ce.getErrorCode() ).isEqualTo( "my errorcode" );
		assertThat( ce.getExtendedInfo() ).isEqualTo( "my stacktrace" );
	}

	@Test
	void testItCanCastAStructNested() {
		Throwable t = ThrowableCaster.cast( Struct.of(
		    Key.message, "my message",
		    Key.detail, "my detail",
		    Key.type, "my type",
		    Key.errorcode, "my errorcode",
		    Key.cause, Struct.of(
		        Key.message, "my cause message",
		        Key.detail, "my cause detail",
		        Key.type, "my cause type",
		        Key.errorcode, "my cause errorcode",
		        Key.extendedinfo, "my cause extendedinfo",
		        Key.stackTrace, "my cause stacktrace"
		    ),
		    Key.extendedinfo, "my extendedinfo",
		    Key.stackTrace, "my stacktrace"

		) );
		assertThat( t.getMessage() ).isEqualTo( "my message" );
		assertThat( t ).isInstanceOf( CustomException.class );
		CustomException ce = ( CustomException ) t;
		assertThat( ce.getDetail() ).isEqualTo( "my detail" );
		assertThat( ce.getType() ).isEqualTo( "my type" );
		assertThat( ce.getErrorCode() ).isEqualTo( "my errorcode" );
		assertThat( ce.getExtendedInfo() ).isEqualTo( "my extendedinfo\nmy stacktrace" );
		assertThat( ce.getCause().getMessage() ).isEqualTo( "my cause message" );
		assertThat( ce.getCause() ).isInstanceOf( CustomException.class );
		CustomException ceCause = ( CustomException ) ce.getCause();
		assertThat( ceCause.getDetail() ).isEqualTo( "my cause detail" );
		assertThat( ceCause.getType() ).isEqualTo( "my cause type" );
		assertThat( ceCause.getErrorCode() ).isEqualTo( "my cause errorcode" );
		assertThat( ceCause.getExtendedInfo() ).isEqualTo( "my cause extendedinfo\nmy cause stacktrace" );

	}

}
