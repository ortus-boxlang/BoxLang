package ortus.boxlang.runtime.util;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.scopes.Key;

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

}
