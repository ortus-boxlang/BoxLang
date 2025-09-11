package ortus.boxlang.compiler.prettyprint;

import java.io.IOException;

import org.junit.Test;

public class StructTest extends PrettyPrintTest {

    @Test
    public void test() throws IOException {
        printTestWithConfigFile( "struct", "padding_false" );
    }

    @Test
    public void testStructWithPadding() throws IOException {
        printTestWithConfigFile( "struct", "padding_true" );
    }

    @Test
    public void testStructWithNoEmptyPadding() throws IOException {
        printTestWithConfigFile( "struct", "empty_padding_false" );
    }

    @Test
    public void testStructWithEmptyPadding() throws IOException {
        printTestWithConfigFile( "struct", "empty_padding_true" );
    }

    @Test
    public void testStructQuoteKeys() throws IOException {
        printTestWithConfigFile( "struct", "quote_keys_true" );
    }

    @Test
    public void testStructSeparator() throws IOException {
        printTestWithConfigFile( "struct", "separator_equals" );
    }

    @Test
    public void testStructMultiline() throws IOException {
        printTestWithConfigFile( "struct", "multiline_3" );
    }

    @Test
    public void testStructMultilineDefault() throws IOException {
        printTestWithConfigFile( "struct", "multiline_3_default" );
    }
}
