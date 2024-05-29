package ortus.boxlang.tools.util;

import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;

import org.apache.commons.lang3.StringUtils;

import com.sun.source.doctree.BlockTagTree;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;

import jdk.javadoc.doclet.DocletEnvironment;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.bifs.MemberDescriptor;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.FunctionService;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.IStruct.TYPES;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.FileSystemUtil;

public class TypeDocumentationGenerator {

	private static final String				docsBasePath		= "docs/boxlang-language/reference/";
	private static final String				templatesBasePath	= "workbench/templates/";
	private static final String				TypeDocsPath		= docsBasePath + "types";
	private static final String				blankTypeTemplate	= StringCaster.cast( FileSystemUtil.read( templatesBasePath + "TypeDocTemplate.md" ) );
	private static final String				navToken			= "(dynamic-types-nav)";

	private static final BoxRuntime			runtime				= BoxRuntime.getInstance();
	private static final FunctionService	functionService		= runtime.getFunctionService();

	@SuppressWarnings( { "rawtypes" } )
	public static IStruct generate( DocletEnvironment docsEnvironment ) throws IOException {

		if ( functionService.getGlobalFunctionCount() == 0 ) {
			docsEnvironment.getSpecifiedElements()
			    .stream()
			    .filter( elem -> elem.getKind().equals( ElementKind.CLASS ) && elem.getAnnotationsByType( BoxMember.class ).length > 0 )
			    .forEach( elem -> {
				    functionService.processBIFRegistration( elem.getClass(), null, null );
			    } );
		}

		IStruct typesData = new Struct( TYPES.LINKED );

		docsEnvironment.getSpecifiedElements()
		    .stream()
		    .filter( elem -> elem.getKind().equals( ElementKind.CLASS ) && elem.getAnnotationsByType( BoxMember.class ).length > 0 )
		    .forEach( elem -> {
			    Stream.of( elem.getAnnotationsByType( BoxMember.class ) )
			        .forEach( member -> {
				        Key typeKey	= member.type().getKey();
				        String memberName = member.name();
				        if ( !typesData.containsKey( typeKey ) ) {
					        typesData.put( typeKey, new Struct( TYPES.LINKED ) );
					        // TODO: Pull dynamic description from type class
					        typesData.getAsStruct( typeKey ).put( Key.description, "" );
					        typesData.getAsStruct( typeKey ).put( Key.functions, new Struct( TYPES.LINKED ) );
				        }
				        IStruct functions = typesData.getAsStruct( typeKey ).getAsStruct( Key.functions );

				        if ( memberName == null || memberName.isEmpty() ) {
					        memberName = StringUtils.replaceOnceIgnoreCase( elem.getSimpleName().toString(), member.type().getKey().getName(), "" );
				        }

				        memberName = memberName.substring( 0, 1 ).toLowerCase() + memberName.substring( 1 );

				        functions.put( Key.of( memberName ), getMemberFunctionData( elem, member, docsEnvironment ) );
			        } );
		    } );

		typesData.keySet().stream().forEach( key -> generateTypeTemplate( key, typesData.getAsStruct( key ) ) );

		String inserts = typesData.keySet()
		    .stream()
		    .sorted(
		        ( a, b ) -> ortus.boxlang.runtime.operators.Compare.invoke( StringCaster.cast( a.getName() ), StringCaster.cast( b.getName() ), false ) )
		    .map( key -> {
			    String group = "    * [" + key.getName() + "](boxlang-language/reference/types/" + key.getName().substring( 0, 1 ).toLowerCase()
			        + key.getName().substring( 1 ) + ".md )";
			    return group;
		    } )
		    .collect( Collectors.joining( "\n" ) );
		return Struct.of(
		    Key.token, navToken,
		    Key.inserts, inserts
		);

	}

	private static IStruct getMemberFunctionData( Element parent, BoxMember memberElement, DocletEnvironment docsEnvironment ) {
		IStruct		memberData	= Struct.of( Key.arguments, new Struct( TYPES.LINKED ) );
		BoxLangType	memberType	= memberElement.type();
		String		memberName	= memberElement.name();
		String		objectArg	= memberElement.objectArgument();

		if ( memberName == null || memberName.isEmpty() ) {
			memberName = StringUtils.replaceOnceIgnoreCase( parent.getSimpleName().toString(), memberType.getKey().getName(), "" );
		}

		try {
			MemberDescriptor descriptor = functionService.getMemberMethod( Key.of( memberName ), memberType );

			if ( descriptor == null ) {
				throw new BoxRuntimeException( "Unable to find member method " + memberName + " for type " + memberType.getKey().getName() );
			}

			Array argsArray = new Array( descriptor.BIFDescriptor.getBIF().getDeclaredArguments() );

			argsArray.intStream().filter(
			    idx -> objectArg == null ? idx == 0 : ( ( Argument ) argsArray.get( idx ) ).name().equals( Key.of( objectArg ) )
			).mapToObj( idx -> ( Argument ) argsArray.get( idx ) )
			    .forEach( arg -> {
				    IStruct argData = Struct.of(
				        Key.type, arg.type(),
				        Key.required, arg.required(),
				        Key.defaultValue, arg.defaultValue(),
				        Key.validators, arg.validators()
				    );
				    memberData.getAsStruct( Key.arguments ).put( Key.of( arg.name() ), argData );
			    } );

			Element invokeElement = parent.getEnclosedElements().stream()
			    .filter( elem -> elem.getKind().equals( ElementKind.METHOD ) && elem.getSimpleName().contentEquals( "_invoke" ) )
			    .findFirst().orElse( null );
			if ( invokeElement != null ) {
				DocCommentTree	commentTree	= docsEnvironment.getDocTrees().getDocCommentTree( invokeElement );
				String			description	= null;
				if ( commentTree != null ) {
					String	memberNameFinal		= memberName;
					DocTree	specificDescription	= commentTree.getBlockTags().stream()
					    .filter( tag -> tag.getKind().equals( DocTree.Kind.UNKNOWN_BLOCK_TAG ) && tag.toString().contains( "@function" )
					        && ( ( BlockTagTree ) tag ).getTagName().equals( "function." + memberNameFinal )
					        || ( ( BlockTagTree ) tag ).getTagName().equals( "function." + memberType + memberNameFinal ) )
					    .findFirst().orElse( null );
					if ( specificDescription != null ) {
						description = ( ( BlockTagTree ) specificDescription ).toString()
						    .replace( '@' + ( ( BlockTagTree ) specificDescription ).getTagName(), "" ).trim();
					} else {
						description = ( commentTree.getFirstSentence().toString() + "\n\n"
						    + commentTree.getPreamble().toString() + commentTree.getBody().toString().trim() ).trim();
					}
					memberData.put( Key.description, description );
					commentTree.getBlockTags().stream()
					    .filter( tag -> tag.getKind().equals( DocTree.Kind.UNKNOWN_BLOCK_TAG ) && tag.toString().contains( "@argument" ) )
					    .forEach( attribute -> {
						    try {
							    String argName = ( ( BlockTagTree ) attribute ).getTagName().split( "\\." )[ 1 ];
							    String argBody = ( ( BlockTagTree ) attribute ).toString().replace( '@' + ( ( BlockTagTree ) attribute ).getTagName(), "" )
							        .trim();
							    IStruct argData = memberData.getAsStruct( Key.arguments ).getAsStruct( Key.of( argName ) );
							    if ( argData != null ) {
								    argData.put( Key.description, argBody );
							    }

						    } catch ( Throwable e ) {
							    System.err.println( "Failed to parse argument comment: " + attribute.toString() );
						    }

					    } );
				}
			}
		} catch ( BoxRuntimeException e ) {
			System.err.println( "Failed to get member method " + memberName + " for type " + memberType.getKey().getName() );
		}

		return memberData;

	}

	private static void generateTypeTemplate( Key typeKey, IStruct typeData ) {
		String	typeDocs		= blankTypeTemplate;
		String	typeDescription	= typeData.getAsString( Key.description );
		String	typeMethods		= "";
		typeMethods	+= typeData.getAsStruct( Key.functions ).keySet().stream().reduce( "", ( content, memberKey ) -> {
						IStruct	memberData			= typeData.getAsStruct( Key.functions ).getAsStruct( memberKey );
						String	memberDescription	= memberData.getAsString( Key.description );
						IStruct	memberArgs			= memberData.getAsStruct( Key.arguments );
						String	memberArgsContent	= memberArgs.keySet().stream().reduce( "", ( argsContent, argKey ) -> {
																				IStruct argData = memberArgs.getAsStruct( argKey );
																				String argDescription = argData.getAsString( Key.description );
																				return argsContent + " * " + argKey.getName() + " ("
																				    + argData.getAsString( Key.type ) + "): " + argDescription
																				    + "\n";
																			},
						    ( a, b ) -> a + b );
						return content + "* `" + memberKey.getName() + "`: " + memberDescription + "\n";
						// TODO: Add member args content handling and exclusions
					},
		    ( a, b ) -> a + b );

		typeDocs	= typeDocs.replace( "{TypeName}", typeKey.getName() );
		typeDocs	= typeDocs.replace( "{TypeDescription}", typeDescription == null ? "" : typeDescription );
		typeDocs	= typeDocs.replace( "{TypeMethods}", typeMethods );
		FileSystemUtil.write( TypeDocsPath + "/" + typeKey.getName() + ".md", typeDocs, "utf-8", true );
	}
}
