package ortus.boxlang.runtime.runnables.compiler;

import java.nio.file.Path;
import java.util.regex.Pattern;

import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class SourceMap {

	public SourceMapRecord[]	sourceMapRecords;
	public String				source;
	private static Pattern		isTemplate;

	static {
		isTemplate = Pattern.compile( "(.cfs|.cfm|.bx|.bxs)$" );
	}

	public static class SourceMapRecord {

		public int		originSourceLine;
		public int		javaSourceLine;
		public String	originSourceNode;
		public String	javaSourceNode;
	}

	public int convertSourceLineToJavaLine( int sourceLine ) throws BoxRuntimeException {
		for ( SourceMapRecord sourceMapRecord : sourceMapRecords ) {
			if ( sourceMapRecord.originSourceLine == sourceLine ) {
				return sourceMapRecord.javaSourceLine;
			}
		}

		throw ( new BoxRuntimeException( "No matching source line" ) );
	}

	public boolean isTemplate() {
		return isTemplate.matcher( source ).find();
	}

	public String getFileName() {
		return Path.of( source ).getFileName().toString();
	}
}
