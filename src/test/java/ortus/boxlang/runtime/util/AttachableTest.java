package ortus.boxlang.runtime.util;

import static com.google.common.truth.Truth.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

public class AttachableTest {

	private Attachable attachable;

	@BeforeEach
	void setUp() {
		attachable = new Attachable();
	}

	@DisplayName( "Test attachments" )
	@Test
	void testAttachments() {
		attachable.putAttachment( Key.of( "test" ), this );
		// verify it exists
		assert attachable.getAttachment( Key.of( "test" ) ) == this;
		// use has
		assert attachable.hasAttachment( Key.of( "test" ) );
		// verify in list
		assertThat( attachable.getAttachmentKeys() ).asList().contains( Key.of( "test" ) );
		// remove it
		attachable.removeAttachment( Key.of( "test" ) );
		// verify it is gone
		assertThat( attachable.hasAttachment( Key.of( "test" ) ) ).isFalse();
	}

	@DisplayName( "Test attachment types" )
	@Test
	void testAttachmentTypes() {
		attachable.putAttachment( Key.of( "test" ), "String test" );
		String test = attachable.getAttachment( Key.of( "test" ) );
		assertThat( test ).isEqualTo( "String test" );

		// Try a HashMap
		attachable.putAttachment( Key.of( "test" ), new HashMap<>() );
		Map<String, String> map = attachable.getAttachment( Key.of( "test" ) );
		assertThat( map ).isNotNull();

		// Try a IStruct
		attachable.putAttachment( Key.of( "test" ), Struct.of( "test", "hola" ) );
		IStruct struct = attachable.getAttachment( Key.of( "test" ) );
		assertThat( struct ).isNotNull();
	}

	@DisplayName( "Test attachment compute" )
	@Test
	void testAttachmentCompute() {
		attachable.computeAttachmentIfAbsent( Key.of( "test" ), key -> "String test" );
		String test = attachable.getAttachment( Key.of( "test" ) );
		assertThat( test ).isEqualTo( "String test" );
	}
}
