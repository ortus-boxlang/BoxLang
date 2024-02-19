package ortus.boxlang.runtime.runnables.compiler;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
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

	public Integer convertJavaLinetoSourceLine( int javaLine ) {
		for ( SourceMapRecord sourceMapRecord : sourceMapRecords ) {
			if ( sourceMapRecord.javaSourceLine == javaLine ) {
				return sourceMapRecord.originSourceLine;
			}
		}

		return null;
	}

	public int convertSourceLineToJavaLine( int sourceLine ) throws BoxRuntimeException {
		int result = -1;
		for ( SourceMapRecord sourceMapRecord : sourceMapRecords ) {
			// Move our pointer so long as we haven't passed the source line
			if ( sourceMapRecord.originSourceLine <= sourceLine ) {
				result = sourceMapRecord.javaSourceLine;
			}
			// Once we've reached or gone past the source line, we'll take the last thing we found
			if ( sourceMapRecord.originSourceLine >= sourceLine ) {
				break;
			}
		}
		return result;
	}

	public int convertJavaLineToSourceLine( int javaLine ) throws BoxRuntimeException {
		int						result					= -1;
		List<SourceMapRecord>	sortedSourceMapRecords	= Arrays.asList( sourceMapRecords );
		// Sort sortedSourceMapRecords by javaSourceLine asc
		sortedSourceMapRecords.sort( ( a, b ) -> a.javaSourceLine - b.javaSourceLine );
		for ( SourceMapRecord sourceMapRecord : sortedSourceMapRecords ) {
			// Move our pointer so long as we haven't passed the source line
			if ( sourceMapRecord.javaSourceLine <= javaLine ) {
				result = sourceMapRecord.originSourceLine;
			}
			// Once we've reached or gone past the source line, we'll take the last thing we found
			if ( sourceMapRecord.javaSourceLine >= javaLine ) {
				break;
			}
		}
		return result;
	}

	public boolean isTemplate() {
		return isTemplate.matcher( source ).find();
	}

	public String getFileName() {
		return Path.of( source ).getFileName().toString();
	}

	public String getSource() {
		return source;
	}
}
