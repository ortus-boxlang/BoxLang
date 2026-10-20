/**
 * Scratch probe — property default scope visibility (real .bx files, correct syntax).
 */
package ortus.boxlang.compiler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class ScratchProbeTest {

	@BeforeAll
	public static void setup() {
		BoxRuntime.getInstance( true );
	}

	@Test
	void probe() throws IOException {
		StringBuilder	sb		= new StringBuilder();
		BoxRuntime		runtime	= BoxRuntime.getInstance();

		probe( sb, runtime, "V1: no leading comment, property AFTER static, default=#complexSeed#" );
		probe( sb, runtime, "V2: leading comment, property AFTER static, default=#complexSeed#" );
		probe( sb, runtime, "V3: property BEFORE static, default=#complexSeed# (UNSCOPED)" );
		probe( sb, runtime, "V4: instance-body var #myVar# (property after body stmt)" );
		probe( sb, runtime, "V5: property BEFORE static, default=#static.complexSeed# (SCOPED)" );
		probe( sb, runtime, "V6: instance var, property BEFORE body stmt, #myVar#" );
		probe( sb, runtime, "V7: this-scoped #this.seed#" );
		probe( sb, runtime, "V8: EXISTING fixture PropertyDefaultToStaticVar" );

		Files.writeString( Path.of( "build", "scratch-probe.txt" ), sb.toString() );
	}

	private void probe( StringBuilder sb, BoxRuntime runtime, String label ) {
		RunnableLoader.getInstance().getBoxpiler().clearPagePool();
		sb.append( "==== " ).append( label ).append( "\n" );
		try {
			IBoxContext	context	= new ScriptingRequestBoxContext( runtime.getRuntimeContext() );
			IScope		vars	= context.getScopeNearby( VariablesScope.name );
			String		name	= label.substring( 0, 2 );
			String		fqn;
			if ( name.equals( "V8" ) ) {
				fqn = "src.test.java.TestCases.phase3.PropertyDefaultToStaticVar";
			} else {
				fqn = "src.test.resources.scratch." + name;
			}
			runtime.executeSource( "result = new " + fqn + "().get" + ( name.equals( "V8" ) ? "MyProp" : "P" ) + "();\n", context );
			sb.append( "   OK, applied = " ).append( vars.get( Key.of( "result" ) ) ).append( "\n" );
		} catch ( Throwable t ) {
			String m = t.getClass().getSimpleName() + ": " + String.valueOf( t.getMessage() ).replace( "\n", " | " );
			if ( m.length() > 260 ) {
				m = m.substring( 0, 260 );
			}
			sb.append( "   FAIL => " ).append( m ).append( "\n" );
		}
	}
}
