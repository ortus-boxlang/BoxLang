package ortus.boxlang.tools.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;

import com.sun.source.doctree.BlockTagTree;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;

import jdk.javadoc.doclet.DocletEnvironment;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.bifs.BIFDescriptor;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.dynamic.casters.KeyCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.FunctionService;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.util.BLCollector;
import ortus.boxlang.runtime.util.FileSystemUtil;

public class BIFDocumentationGenerator {

	private static final String	docsBasePath		= "docs/boxlang-language/reference/";
	private static final String	templatesBasePath	= "workbench/templates/";
	private static final String	BIFDocsPath			= docsBasePath + "built-in-functions";
	private static final String	blankBIFTemplate	= StringCaster.cast( FileSystemUtil.read( templatesBasePath + "BIFDocTemplate.md" ) );
	private static final String	bifNavToken			= "(dynamic-bif-nav)";

	@SuppressWarnings( "unchecked" )
	public static IStruct generate( DocletEnvironment docsEnvironment ) throws IOException {

		BoxRuntime		runtime					= BoxRuntime.getInstance();
		FunctionService	functionService			= runtime.getFunctionService();
		String			PackageNavPlaceholder	= "{PackageNav}";

		docsEnvironment.getSpecifiedElements()
		    .stream()
		    .filter( elem -> elem.getKind().equals( ElementKind.CLASS ) && elem.getAnnotationsByType( BoxBIF.class ).length > 0 )
		    .forEach( elem -> {
			    functionService.processBIFRegistration( ( Class ) elem.getClass(), null, null );
		    } );

		Array			newBifs		= new Array( functionService.getGlobalFunctionNames() );

		List<Element>	docElements	= docsEnvironment.getSpecifiedElements()
		    .stream()
		    .filter( elem -> elem.getAnnotationsByType( BoxBIF.class ).length > 0 )
		    .peek( elem -> elem.getSimpleName() )
		    .map( elem -> ( Element ) elem )
		    .collect( Collectors.toList() );

		try {
			Array	bifInfos	= newBifs.stream()
			    .map( fnName -> Struct.of(
			        Key._NAME, fnName,
			        Key.boxBif, ( BIFDescriptor ) functionService.getGlobalFunction( StringCaster.cast( fnName ) )
			    )
			    )
			    .filter( record -> ( ( BIFDescriptor ) record.get( Key.boxBif ) ).isGlobal )
			    .map( record -> generateBIFTemplate( record, docElements, docsEnvironment ) )
			    .collect( BLCollector.toArray() )
			    .stream()
			    .filter( record -> ( ( HashMap<String, String> ) record ).get( "name" ) != null )
			    .collect( BLCollector.toArray() );
			Struct	groupLinks	= new Struct();
			bifInfos.stream()
			    .forEach( bifInfo -> {
				    HashMap<String, String> bifMeta	= ( ( HashMap<String, String> ) bifInfo );
				    Key					groupKey	= Key.of( bifMeta.get( "package" ) );

				    if ( !groupLinks.containsKey( groupKey ) ) {
					    groupLinks.put( groupKey, new Array() );
				    }
				    ArrayCaster.cast( groupLinks.get( groupKey ) ).push( "[" + bifMeta.get( "name" ) + "](" + bifMeta.get( "file" ) + ")" );
			    } );
			// Generate our BIF files
			bifInfos.stream()
			    .map( bifInfo -> ( HashMap<String, String> ) bifInfo )
			    .forEach( bifMeta -> {
				    String packageNav = bifInfos.stream()
				        .map( bifInfo -> ( HashMap<String, String> ) bifInfo )
				        .filter( bifInfo -> bifInfo.get( "package" )
				            .equals( bifMeta.get( "package" ) )
				            && !bifInfo.get( "name" ).equals( bifMeta.get( "name" ) ) )
				        .map( bifInfo -> {
										        return "  * [" + bifInfo.get( "name" ) + "](./" + bifInfo.get( "fileName" ) + ")";
									        } )
				        .collect( Collectors.joining( "\n" ) );
				    String contents	= bifMeta.get( "template" );
				    contents = contents.replace( PackageNavPlaceholder, packageNav );
				    String bifPath = bifMeta.get( "fullPath" );
				    FileSystemUtil.write( bifPath, contents, "utf-8", true );
				    FileSystemUtil.write( bifPath, contents, "utf-8", true );
			    } );

			// Generate our summary navigation
			String inserts = groupLinks.keySet()
			    .stream()
			    .sorted(
			        ( a, b ) -> ortus.boxlang.runtime.operators.Compare.invoke( StringCaster.cast( a.getName() ), StringCaster.cast( b.getName() ), false ) )
			    .map( key -> {
				    String keyLink = "[" + key.getName() + "](" + BIFDocsPath.replace( "docs/", "" ) + "/" + key.getName() + "/README.md)";
				    String group = "    * " + keyLink + "\n";
				    group += ArrayCaster.cast( groupLinks.get( key ) )
				        .stream()
				        .map( bifLink -> {
					        return "      * " + bifLink;
				        } )
				        .collect( Collectors.joining( "\n" ) );
				    return group;
			    } )
			    .collect( Collectors.joining( "\n" ) );
			return Struct.of(
			    Key.token, bifNavToken,
			    Key.inserts, inserts
			);
		} catch ( Exception e ) {
			throw new BoxRuntimeException( "An error occurred while attempting to generate the technical documentation.", e );
		} finally {
			runtime.shutdown();
		}
	}

	public static HashMap<String, String> generateBIFTemplate( IStruct bifRecord, List<Element> docElements, DocletEnvironment docsEnvironment ) {
		BIFDescriptor	bif						= ( BIFDescriptor ) bifRecord.get( Key.boxBif );
		String			name					= bifRecord.getAsString( Key._NAME );
		String[]		packageParts			= bif.BIFClass.getName().split( "\\." );
		String			path					= packageParts[ packageParts.length - 2 ];
		String			fileName				= name + ".md";
		String			relativePath			= path + '/' + fileName;
		String			bifFile					= BIFDocsPath + '/' + relativePath;
		String			BIFNamePlaceholder		= "{BIFName}";
		String			BIFDescPlaceholder		= "{BIFDescription}";
		String			BIFArgsPlaceholder		= "{BIFArgs}";
		String			BIFArgsTablePlaceholder	= "{BIFArgsTable}";

		if ( !FileSystemUtil.exists( bifFile ) ) {
			Key		bifKey				= Key.of( name );
			Element	javadocElement		= docElements.stream()
			    .filter( elem -> bifKey.equals( Key.of( elem.getSimpleName() ) )
			        ||
			        Stream.of( elem.getAnnotationsByType( BoxBIF.class ) )
			            .filter(
			                annotation -> bifKey.equals( Key.of( annotation.alias() ) )
			            ).count() > 0
			    ).findFirst().orElse( null );

			Array	bifArgs				= new Array( bif.getBIF().getDeclaredArguments() );
			Struct	argComments			= new Struct();
			String	description			= null;
			Array	argumentsExclude	= new Array();
			if ( javadocElement != null ) {
				Element invokeElement = javadocElement.getEnclosedElements().stream()
				    .filter( elem -> elem.getKind().equals( ElementKind.METHOD ) && elem.getSimpleName().contentEquals( "_invoke" ) )
				    .findFirst().orElse( null );
				if ( invokeElement != null ) {
					DocCommentTree commentTree = docsEnvironment.getDocTrees().getDocCommentTree( invokeElement );
					if ( commentTree != null ) {
						DocTree specificDescription = commentTree.getBlockTags().stream()
						    .filter( tag -> tag.getKind().equals( DocTree.Kind.UNKNOWN_BLOCK_TAG ) && tag.toString().contains( "@function" )
						        && ( ( BlockTagTree ) tag ).getTagName().equals( "function." + name ) )
						    .findFirst().orElse( null );
						if ( specificDescription != null ) {
							description = ( ( BlockTagTree ) specificDescription ).toString()
							    .replace( '@' + ( ( BlockTagTree ) specificDescription ).getTagName(), "" ).trim();
						} else {
							description = ( commentTree.getFirstSentence().toString() + "\n\n"
							    + commentTree.getBody().toString() ).trim();
						}

						argumentsExclude = ArrayCaster.cast( commentTree.getBlockTags().stream()
						    .filter( tag -> tag.getKind().equals( DocTree.Kind.UNKNOWN_BLOCK_TAG ) && tag.toString().contains( "@component" )
						        && ( ( BlockTagTree ) tag ).getTagName().equals( "component." + name + ".arguments.exclude" ) )
						    .map( tag -> ( ( BlockTagTree ) tag ).toString().replace( '@' + ( ( BlockTagTree ) tag ).getTagName(), "" ).trim() )
						    .findFirst().orElse( "" ).split( "," ) );

						commentTree.getBlockTags().stream()
						    .filter( tag -> tag.getKind().equals( DocTree.Kind.UNKNOWN_BLOCK_TAG ) && tag.toString().contains( "@argument" ) )
						    .forEach( attribute -> {
							    try {
								    String argName = ( ( BlockTagTree ) attribute ).getTagName().split( "\\." )[ 1 ];
								    String argBody = ( ( BlockTagTree ) attribute ).toString().replace( '@' + ( ( BlockTagTree ) attribute ).getTagName(), "" )
								        .trim();
								    argComments.put( Key.of( argName ), argBody );
							    } catch ( Throwable e ) {
								    System.err.println( "Failed to parse argument comment: " + attribute.toString() );
							    }

						    } );

						// System.out.println( "FirstSentence:" );
						// System.out.println( commentTree.getFirstSentence() );
						// System.out.println( "Preamble:" );
						// System.out.println( commentTree.getPreamble() );
						// System.out.println( "Postamble:" );
						// System.out.println( commentTree.getPostamble() );
						// System.out.println( "Block Tags:" );
						// System.out.println( commentTree.getBlockTags() );
						// System.out.println( "Body:" );
						// System.out.println( commentTree.getBody() );
						// System.out.println( "Full Body:" );
						// System.out.println( commentTree.getFullBody() );
					}

				}
			}

			final Array argumentsExcludeFinal = argumentsExclude;

			if ( argumentsExclude.size() > 0 ) {
				bifArgs = bifArgs.stream()
				    .map( argument -> ( Argument ) argument )
				    .filter( argument -> !argumentsExcludeFinal.contains( KeyCaster.cast( argument.name() ).getName() ) )
				    .collect( BLCollector.toArray() );
			}

			if ( description == null ) {
				description = "No description available.";
			}
			String	argsInline	= "";
			String	argsTable	= "This function does not accept any arguments";
			if ( bifArgs.size() > 0 ) {
				argsTable	= "\n| Argument | Type | Required | Description | Default |\n";
				argsTable	+= "|----------|------|----------|-------------|---------|\n";
				argsTable	+= bifArgs.stream()
				    .map( bifInfo -> ( ortus.boxlang.runtime.types.Argument ) bifInfo )
				    .map( bifInfo -> {
								    Key	argKey			= Key.of( bifInfo.name() );
								    String argDescription = argComments.getAsString( argKey );
								    argDescription = ( argDescription != null ? argDescription : "" ).replace( "\n", "<br>" );
								    String defaultValue = bifInfo.hasDefaultValue() ? bifInfo.defaultValue().toString() : "";
								    if ( !defaultValue.isEmpty() ) {
									    defaultValue = "`" + defaultValue + "`";
								    }
								    return "| `" + bifInfo.name() + "` | `" + bifInfo.type() + "` | `" + bifInfo.required() + "` | "
								        + argDescription + " | "
								        + defaultValue + " |";
							    } )
				    .collect( Collectors.joining( "\n" ) );

				argsInline	= bifArgs.stream()
				    .map( bifInfo -> ( ortus.boxlang.runtime.types.Argument ) bifInfo )
				    .map( bifInfo -> ( bifInfo.name() + "=[" + bifInfo.type() + "]" ) )
				    .collect( Collectors.joining( ", " ) );

			}
			String contents = blankBIFTemplate.replace( BIFNamePlaceholder, name )
			    .replace( BIFDescPlaceholder, description )
			    .replace( BIFArgsPlaceholder, argsInline )
			    .replace( BIFArgsTablePlaceholder, argsTable );
			return new HashMap<String, String>() {

				{
					put( "name", name );
					put( "package", path );
					put( "fileName", fileName );
					put( "fullPath", bifFile );
					put( "file", "boxlang-language/reference/built-in-functions/" + relativePath );
					put( "template", contents );
				}
			};
		}

		return new HashMap<String, String>();

	}

}
