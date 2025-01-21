package ortus.boxlang.compiler.parser;

import java.io.PrintStream;
import java.nio.file.Paths;

import org.antlr.v4.runtime.atn.DecisionInfo;
import org.antlr.v4.runtime.atn.DecisionState;

import ortus.boxlang.compiler.ast.Source;
import ortus.boxlang.compiler.ast.SourceFile;

public class ParserProfilingResults {

	public Source						source;
	public org.antlr.v4.runtime.Parser	parser;

	public ParserProfilingResults( Source source, org.antlr.v4.runtime.Parser parser ) {
		this.source	= source;
		this.parser	= parser;
	}

	public void writeCSV( String outDir ) {
		String name = source instanceof SourceFile ? ( ( SourceFile ) source ).getFile().getName().replaceFirst( "\\.\\w+$", "" ) : "unknown";
		try ( PrintStream out = new PrintStream( Paths.get( outDir, name + ".csv" ).toFile() ) ) {
			out.println( "rule,invocations,time,total_k,max_k,ambiguities,errors" );

			for ( DecisionInfo decisionInfo : parser.getParseInfo().getDecisionInfo() ) {
				DecisionState	ds		= parser.getATN().getDecisionState( decisionInfo.decision );
				String			rule	= parser.getRuleNames()[ ds.ruleIndex ];

				out.printf(
				    "%s,%d,%d,%d,%d,%d%n",
				    rule,
				    decisionInfo.invocations,
				    0,
				    // decisionInfo.timeInPrediction / 1_000_000,
				    decisionInfo.SLL_TotalLook,
				    decisionInfo.SLL_MaxLook,
				    decisionInfo.ambiguities.size(),
				    decisionInfo.errors.size()
				);
			}

			out.close();
		} catch ( Exception e ) {
			e.printStackTrace();
		}

	}
}
