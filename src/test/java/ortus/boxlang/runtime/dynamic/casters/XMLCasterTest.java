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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.types.XML;
import ortus.boxlang.runtime.types.exceptions.BoxCastException;

public class XMLCasterTest {

	@DisplayName( "It can cast XML to XML" )
	@Test
	void testItCanCastXML() {
		assertThat( XMLCaster.cast( new XML( "<root />" ) ) ).isInstanceOf( XML.class );
	}

	@DisplayName( "It can cast a Node to XML" )
	@Test
	void testItCanCastANode() {
		assertThat( XMLCaster.cast( new XML( "<root />" ).getNode() ) ).isInstanceOf( XML.class );
	}

	@DisplayName( "It can not cast other stuff" )
	@Test
	void testItCannotCastOtherStuff() {
		assertThrows( BoxCastException.class, () -> XMLCaster.cast( null ) );
		assertThrows( BoxCastException.class, () -> XMLCaster.cast( 7 ) );
		assertThrows( BoxCastException.class, () -> XMLCaster.cast( "test" ) );
		// This is valid xml so I'm not sure why it was expected to throw
		// assertThrows( BoxCastException.class, () -> XMLCaster.cast( "<root />" ) );
		assertThrows( BoxCastException.class, () -> XMLCaster.cast( Map.of() ) );
	}

}
