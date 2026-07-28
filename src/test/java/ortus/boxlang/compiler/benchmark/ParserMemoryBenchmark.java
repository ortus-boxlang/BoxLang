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
package ortus.boxlang.compiler.benchmark;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.openjdk.jol.info.GraphLayout;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.parser.Parser;
import ortus.boxlang.compiler.parser.ParsingResult;
import ortus.boxlang.runtime.BoxRuntime;

/**
 * Measures memory retained by AST roots independently of parser temporaries.
 */
public class ParserMemoryBenchmark {

	private static final Set<String> SOURCE_EXTENSIONS = Set.of( ".bx", ".bxs", ".bxm", ".cfc", ".cfm", ".cfml" );

	public static void main( String[] args ) throws IOException {
		if ( args.length == 0 ) {
			throw new IllegalArgumentException( "Usage: ParserMemoryBenchmark <corpus> [--materialize-source-text] [--census-strings] [--limit N]" );
		}

		Path	corpus					= Path.of( args[ 0 ] ).toAbsolutePath().normalize();
		boolean	materializeSourceText	= false;
		boolean	censusStrings			= false;
		int		limit					= Integer.MAX_VALUE;
		Path	manifest				= null;
		for ( int i = 1; i < args.length; i++ ) {
			if ( args[ i ].equals( "--materialize-source-text" ) ) {
				materializeSourceText = true;
			} else if ( args[ i ].equals( "--census-strings" ) ) {
				censusStrings = true;
			} else if ( args[ i ].equals( "--limit" ) && i + 1 < args.length ) {
				limit = Integer.parseInt( args[ ++i ] );
			} else if ( args[ i ].equals( "--manifest" ) && i + 1 < args.length ) {
				manifest = Path.of( args[ ++i ] );
			}
		}

		if ( !Files.isDirectory( corpus ) ) {
			throw new IllegalArgumentException( "Corpus directory does not exist: " + corpus );
		}

		List<Path> sources;
		try ( var paths = Files.walk( corpus ) ) {
			sources = paths
			    .filter( Files::isRegularFile )
			    .filter( ParserMemoryBenchmark::isSourceFile )
			    .sorted( Comparator.naturalOrder() )
			    .limit( limit )
			    .toList();
		}

		BoxRuntime.getInstance();
		Parser parser = new Parser();
		warmParser( parser );

		List<BoxNode>	roots			= new ArrayList<>();
		List<Path>		retainedSources	= new ArrayList<>();
		int				failures		= 0;
		long			sourceBytes		= 0;
		long			started			= System.nanoTime();
		for ( Path source : sources ) {
			sourceBytes += Files.size( source );
			try {
				ParsingResult result = parser.parse( source.toFile(), false );
				if ( result.getRoot() == null ) {
					failures++;
					continue;
				}
				roots.add( result.getRoot() );
				retainedSources.add( source );
			} catch ( RuntimeException e ) {
				failures++;
			}
		}
		long parseNanos = System.nanoTime() - started;
		if ( failures > 0 ) {
			throw new IllegalStateException( "Failed to retain ASTs for " + failures + " of " + sources.size() + " source files" );
		}
		if ( manifest != null ) {
			writeManifest( corpus, manifest, retainedSources, roots );
		}

		Set<BoxNode> uniqueNodes = java.util.Collections.newSetFromMap( new IdentityHashMap<>() );
		for ( BoxNode root : roots ) {
			collectNodes( root, uniqueNodes );
		}
		List<String>	materializedSource	= materializeSourceText
		    ? uniqueNodes.stream().map( BoxNode::getSourceText ).filter( java.util.Objects::nonNull ).toList()
		    : List.of();

		List<Object>	graphRoots			= new ArrayList<>( roots );
		graphRoots.addAll( materializedSource );
		GraphLayout layout = GraphLayout.parseInstance( graphRoots.toArray() );
		System.out.printf( Locale.ROOT, "corpus=%s%n", corpus );
		System.out.printf( Locale.ROOT, "files.discovered=%d%n", sources.size() );
		System.out.printf( Locale.ROOT, "files.retained=%d%n", roots.size() );
		System.out.printf( Locale.ROOT, "files.failed=%d%n", failures );
		System.out.printf( Locale.ROOT, "source.bytes=%d%n", sourceBytes );
		System.out.printf( Locale.ROOT, "ast.nodes=%d%n", uniqueNodes.size() );
		System.out.printf( Locale.ROOT, "ast.graph.objects=%d%n", layout.totalCount() );
		System.out.printf( Locale.ROOT, "ast.graph.bytes=%d%n", layout.totalSize() );
		System.out.printf( Locale.ROOT, "ast.bytes.per.node=%.2f%n", uniqueNodes.isEmpty() ? 0D : ( double ) layout.totalSize() / uniqueNodes.size() );
		for ( int childCount = 0; childCount <= 3; childCount++ ) {
			int		threshold	= childCount;
			long	count		= uniqueNodes.stream().filter( node -> Math.min( node.getChildren().size(), 3 ) == threshold ).count();
			System.out.printf( Locale.ROOT, "ast.children.%s=%d%n", childCount == 3 ? "3-plus" : childCount, count );
		}
		System.out.printf( Locale.ROOT, "parse.millis=%.2f%n", parseNanos / 1_000_000D );
		System.out.printf( Locale.ROOT, "source.materialized=%s%n", materializeSourceText );
		if ( censusStrings ) {
			printStringCensus( roots, uniqueNodes );
		}
		System.out.println( layout.toFootprint() );
		BoxRuntime.getInstance().shutdown();
	}

	private static void warmParser( Parser parser ) {
		parser.parseExpression( "variables.answer + 1" );
	}

	private static boolean isSourceFile( Path path ) {
		String name = path.getFileName().toString().toLowerCase( Locale.ROOT );
		return SOURCE_EXTENSIONS.stream().anyMatch( name::endsWith );
	}

	private static void collectNodes( BoxNode node, Set<BoxNode> nodes ) {
		if ( !nodes.add( node ) ) {
			return;
		}
		node.getChildren().forEach( child -> collectNodes( child, nodes ) );
		node.getComments().forEach( comment -> collectNodes( comment, nodes ) );
	}

	private static void printStringCensus( List<BoxNode> roots, Set<BoxNode> nodes ) {
		Map<String, StringStats>	values					= collectStringStats( nodes );
		long						perRootDuplicateObjects	= 0;
		for ( BoxNode root : roots ) {
			Set<BoxNode> rootNodes = Collections.newSetFromMap( new IdentityHashMap<>() );
			collectNodes( root, rootNodes );
			perRootDuplicateObjects += collectStringStats( rootNodes ).values().stream()
			    .mapToLong( stats -> stats.identities.size() - 1L )
			    .sum();
		}

		long	references	= values.values().stream().mapToLong( stats -> stats.references ).sum();
		long	objects		= values.values().stream().mapToLong( stats -> stats.identities.size() ).sum();
		System.out.printf( Locale.ROOT, "semantic.strings.references=%d%n", references );
		System.out.printf( Locale.ROOT, "semantic.strings.distinct.values=%d%n", values.size() );
		System.out.printf( Locale.ROOT, "semantic.strings.distinct.objects=%d%n", objects );
		System.out.printf( Locale.ROOT, "semantic.strings.duplicate.objects=%d%n", objects - values.size() );
		System.out.printf( Locale.ROOT, "semantic.strings.per-root.duplicate.objects=%d%n", perRootDuplicateObjects );
		values.entrySet().stream()
		    .filter( entry -> entry.getValue().identities.size() > 1 )
		    .sorted( Comparator.<Map.Entry<String, StringStats>>comparingInt( entry -> entry.getValue().identities.size() ).reversed() )
		    .limit( 30 )
		    .forEach( entry -> System.out.printf( Locale.ROOT, "semantic.string value=%s references=%d objects=%d fields=%s%n",
		        escapeValue( entry.getKey() ), entry.getValue().references, entry.getValue().identities.size(), entry.getValue().fields ) );
	}

	private static Map<String, StringStats> collectStringStats( Set<BoxNode> nodes ) {
		Map<String, StringStats> values = new HashMap<>();
		for ( BoxNode node : nodes ) {
			for ( Class<?> type = node.getClass(); type != null && BoxNode.class.isAssignableFrom( type ); type = type.getSuperclass() ) {
				String typeName = type.getSimpleName();
				for ( Field field : type.getDeclaredFields() ) {
					if ( field.getType() != String.class || Modifier.isStatic( field.getModifiers() ) || field.getName().equals( "sourceText" )
					    || field.getName().equals( "commentText" ) ) {
						continue;
					}
					try {
						field.setAccessible( true );
						String value = ( String ) field.get( node );
						if ( value != null ) {
							values.computeIfAbsent( value, ignored -> new StringStats() ).add( value, typeName + "." + field.getName() );
						}
					} catch ( IllegalAccessException e ) {
						throw new IllegalStateException( "Unable to inspect AST string field " + field, e );
					}
				}
			}
		}

		return values;
	}

	private static String escapeValue( String value ) {
		String escaped = value.replace( "\\", "\\\\" ).replace( "\r", "\\r" ).replace( "\n", "\\n" ).replace( "\t", "\\t" );
		return escaped.length() <= 80 ? escaped : escaped.substring( 0, 77 ) + "...";
	}

	private static class StringStats {

		private int					references;
		private final Set<String>	identities	= Collections.newSetFromMap( new IdentityHashMap<>() );
		private final Set<String>	fields		= new java.util.TreeSet<>();

		private void add( String value, String field ) {
			this.references++;
			this.identities.add( value );
			this.fields.add( field );
		}
	}

	private static void writeManifest( Path corpus, Path manifest, List<Path> sources, List<BoxNode> roots ) throws IOException {
		List<String> lines = new ArrayList<>();
		for ( int i = 0; i < roots.size(); i++ ) {
			Set<BoxNode> nodes = java.util.Collections.newSetFromMap( new IdentityHashMap<>() );
			collectNodes( roots.get( i ), nodes );
			TreeMap<String, Integer> types = new TreeMap<>();
			nodes.forEach( node -> types.merge( node.getClass().getSimpleName(), 1, Integer::sum ) );
			lines.add( corpus.relativize( sources.get( i ) ) + "\t" + nodes.size() + "\t" + types );
		}
		Path parent = manifest.toAbsolutePath().getParent();
		if ( parent != null ) {
			Files.createDirectories( parent );
		}
		Files.write( manifest, lines );
	}

}
