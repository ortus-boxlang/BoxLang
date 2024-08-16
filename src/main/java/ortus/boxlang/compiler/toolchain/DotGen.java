package ortus.boxlang.compiler.toolchain;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.Trees;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DotGen {

	private int				nodeCount	= 0;

	private final ParseTree	tree;
	private final Parser	parser;
	private final File		sourceFile;

	public DotGen( ParseTree tree, Parser parser, File file ) {
		this.tree		= tree;
		this.parser		= parser;
		this.sourceFile	= file;
	}

	public String getDot() {
		StringBuilder builder = new StringBuilder();
		builder.append( "digraph ParseTree {\n" );
		traverse( builder, this.tree, this.parser, 0 );
		builder.append( "}" );
		return builder.toString();
	}

	private void traverse( StringBuilder builder, ParseTree parent, Parser parser, int parentIndex ) {
		for ( int i = 0; i < parent.getChildCount(); i++ ) {
			ParseTree	child		= parent.getChild( i );
			String		nodeName	= Trees.getNodeText( child, parser );
			nodeName = nodeName.replace( "\"", "" ); // escape double quotes
			int childIndex = ++nodeCount;
			builder.append( "  n" )
			    .append( parentIndex )
			    .append( " -> n" )
			    .append( childIndex )
			    .append( " [label=\"" )
			    .append( nodeName )
			    .append( "\"];\n" );
			traverse( builder, child, parser, childIndex );
		}
	}

	public void writeDotFor() {
		Path dotFilePath = Paths.get( this.sourceFile.getAbsolutePath()
		    .replace( ".bx", ".dot" ) );
		try {
			if ( !Files.exists( dotFilePath ) || Files.getLastModifiedTime( dotFilePath )
			    .compareTo( Files.getLastModifiedTime( this.sourceFile.toPath() ) ) < 0 ) {
				String dotSpec = getDot();
				Files.write( dotFilePath, dotSpec.getBytes( StandardCharsets.UTF_8 ) );
			}
		} catch ( IOException e ) {
			throw new RuntimeException( e );
		}
	}

	public void writeTreeFor() {
		Path treeFilePath = Paths.get( this.sourceFile.getAbsolutePath()
		    .replace( ".bx", ".txt" ) );
		try {
			if ( !Files.exists( treeFilePath ) || Files.getLastModifiedTime( treeFilePath )
			    .compareTo( Files.getLastModifiedTime( this.sourceFile.toPath() ) ) < 0 ) {
				String	treeSpec			= Trees.toStringTree( this.tree, this.parser );
				String	formattedTreeSpec	= formatTreeSpec( treeSpec );
				Files.write( treeFilePath, formattedTreeSpec.getBytes( StandardCharsets.UTF_8 ) );
			}
		} catch ( IOException e ) {
			throw new RuntimeException( e );
		}
	}

	public void writeSvgFor() {
		Path	svgFilePath	= Paths.get( this.sourceFile.getAbsolutePath()
		    .replace( ".bx", ".svg" ) );
		Path	dotFilePath	= Paths.get( this.sourceFile.getAbsolutePath()
		    .replace( ".bx", ".dot" ) );
		try {
			if ( !Files.exists( svgFilePath ) || Files.getLastModifiedTime( svgFilePath )
			    .compareTo( Files.getLastModifiedTime( dotFilePath ) ) < 0 ) {
				ProcessBuilder	pb			= new ProcessBuilder( "dot", "-Tsvg", "-o", svgFilePath.toAbsolutePath().toString(),
				    dotFilePath.toAbsolutePath().toString() );
				Process			process		= pb.start();
				int				exitCode	= process.waitFor();
				if ( exitCode != 0 ) {
					System.err.println( "Error occurred while generating SVG file for: " + this.sourceFile.getAbsolutePath() );
				}
			}
		} catch ( IOException | InterruptedException e ) {
			throw new RuntimeException( e );
		}
	}

	public String formatTreeSpec( String treeSpec ) {
		StringBuilder	formatted	= new StringBuilder();
		int				indent		= 0;
		for ( char c : treeSpec.toCharArray() ) {
			if ( c == '(' ) {
				formatted.append( "\n" );
				for ( int i = 0; i < indent; i++ ) {
					formatted.append( "  " );
				}
				formatted.append( c );
				indent++;
			} else if ( c == ')' ) {
				indent--;
				formatted.append( "\n" );
				for ( int i = 0; i < indent; i++ ) {
					formatted.append( "  " );
				}
				formatted.append( c );
			} else {
				formatted.append( c );
			}
		}
		return formatted.toString();
	}
}