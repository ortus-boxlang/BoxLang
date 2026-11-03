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
package TestCases.phase1;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.RequestScope;
import ortus.boxlang.runtime.scopes.VariablesScope;

/**
 * BL-2745: a script with an import statement and a try/catch failed to load with a VerifyError
 * (inconsistent stackmap frames) because the import left a value on the operand stack.
 * The bug only shows when the script ends with an expression statement, so most cases end with {@code done = true;}.
 */
@DisplayName( "BL-2745: script with import and try/catch" )
public class ScriptImportTryCatchTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@BeforeEach
	public void setupEach() {
		this.context	= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		this.variables	= this.context.getScopeNearby( VariablesScope.name );
	}

	@Test
	@DisplayName( "Unused import with try/catch" )
	public void testUnusedImport() {
		instance.executeSource(
		    """
		    import java.lang.Math;
		    try { result = 1; } catch ( any e ) { result = 2; }
		    done = true;
		    """,
		    this.context );
		assertThat( this.variables.get( result ) ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "Unused import with try/catch where the catch runs" )
	public void testUnusedImportCatchRuns() {
		instance.executeSource(
		    """
		    import java.lang.Math;
		    try { throw( "boom" ); } catch ( any e ) { result = e.message; }
		    done = true;
		    """,
		    this.context );
		assertThat( this.variables.get( result ) ).isEqualTo( "boom" );
	}

	@Test
	@DisplayName( "Import used inside the try" )
	public void testImportUsedInTry() {
		instance.executeSource(
		    """
		    import java.lang.StringBuilder;
		    try {
		    	sb = new StringBuilder( "abc" );
		    	result = sb.reverse().toString();
		    } catch ( any e ) {
		    	result = "error";
		    }
		    done = true;
		    """,
		    this.context );
		assertThat( this.variables.get( result ) ).isEqualTo( "cba" );
	}

	@Test
	@DisplayName( "Static call on an imported class inside the try" )
	public void testStaticCallOnImportInTry() {
		instance.executeSource(
		    """
		    import java.lang.Math;
		    try {
		    	result = Math::max( 3, 7 );
		    } catch ( any e ) {
		    	result = -1;
		    }
		    done = true;
		    """,
		    this.context );
		assertThat( this.variables.get( result ) ).isEqualTo( 7 );
	}

	@Test
	@DisplayName( "Multiple imports with a nested try" )
	public void testNestedTry() {
		instance.executeSource(
		    """
		    import java.lang.Math;
		    import java.lang.StringBuilder;
		    try {
		    	try {
		    		throw( "inner" );
		    	} catch ( any e ) {
		    		inner = e.message;
		    	}
		    	result = inner & Math::abs( -5 );
		    } catch ( any e ) {
		    	result = "outer";
		    }
		    done = true;
		    """,
		    this.context );
		assertThat( this.variables.get( result ) ).isEqualTo( "inner5" );
	}

	@Test
	@DisplayName( "Import with try/catch/finally" )
	public void testTryCatchFinally() {
		instance.executeSource(
		    """
		    import java.lang.Math;
		    log = [];
		    try {
		    	log.append( "try" );
		    	throw( "boom" );
		    } catch ( any e ) {
		    	log.append( "catch" );
		    } finally {
		    	log.append( "finally" );
		    }
		    result = log.toList();
		    """,
		    this.context );
		assertThat( this.variables.get( result ) ).isEqualTo( "try,catch,finally" );
	}

	@Test
	@DisplayName( "Import with try/catch as the last statement of the script" )
	public void testTryAsLastStatement() {
		instance.executeSource(
		    """
		    import java.lang.Math;
		    try { result = Math::min( 4, 2 ); } catch ( any e ) { result = -1; }
		    """,
		    this.context );
		assertThat( this.variables.get( result ) ).isEqualTo( 2 );
	}

	@Test
	@DisplayName( "Import after other statements with try/catch" )
	public void testImportNotFirstStatement() {
		instance.executeSource(
		    """
		    x = 1;
		    import java.lang.Math;
		    try { result = x + 1; } catch ( any e ) { result = -1; }
		    println( result );
		    """,
		    this.context );
		assertThat( this.variables.get( result ) ).isEqualTo( 2 );
	}

	@Test
	@DisplayName( "CF script source with import and try/catch" )
	public void testCFScript() {
		instance.executeSource(
		    """
		    import java.lang.Math;
		    try { result = 1; } catch ( any e ) { result = 2; }
		    done = true;
		    """,
		    this.context, BoxSourceType.CFSCRIPT );
		assertThat( this.variables.get( result ) ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( ".bxs file with import and try/catch" )
	public void testBxsFile() {
		instance.executeTemplate( "src/test/java/TestCases/phase1/files/ScriptImportTryCatch.bxs", this.context );
		IScope request = this.context.getScopeNearby( RequestScope.name );
		assertThat( request.get( Key.of( "bl2745" ) ) ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( ".bxm template with import and try/catch" )
	public void testBxmFile() {
		instance.executeTemplate( "src/test/java/TestCases/phase1/files/ImportTryCatch.bxm", this.context );
		IScope request = this.context.getScopeNearby( RequestScope.name );
		assertThat( request.get( Key.of( "bl2745" ) ) ).isEqualTo( 3 );
	}

	@Test
	@DisplayName( ".bx class with import and try/catch in a method" )
	public void testBxClass() {
		instance.executeSource(
		    """
		    result = new src.test.java.TestCases.phase1.files.ImportTryCatchClass().run();
		    """,
		    this.context );
		assertThat( this.variables.get( result ) ).isEqualTo( 5 );
	}

}
