package ortus.boxlang.tools.util;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

import org.apache.commons.lang3.StringUtils;

import com.sun.source.doctree.BlockTagTree;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;

import jdk.javadoc.doclet.DocletEnvironment;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.bifs.BoxMemberExpose;
import ortus.boxlang.runtime.bifs.MemberDescriptor;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.FunctionService;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.IStruct.TYPES;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.util.BLCollector;
import ortus.boxlang.runtime.types.util.StringUtil;
import ortus.boxlang.runtime.types.util.StructUtil;
import ortus.boxlang.runtime.util.FileSystemUtil;

public class TypeDocumentationGenerator {

	private static final String				docsBasePath		= "docs/boxlang-language/reference/";
	private static final String				templatesBasePath	= "workbench/templates/";
	private static final String				TypeDocsPath		= docsBasePath + "types";
	private static final String				blankTypeTemplate	= StringCaster.cast( FileSystemUtil.read( templatesBasePath + "TypeDocTemplate.md" ) );
	private static final String				navToken			= "(dynamic-types-nav)";

	private static final BoxRuntime			runtime				= BoxRuntime.getInstance();
	private static final FunctionService	functionService		= runtime.getFunctionService();

	public static IStruct generate( DocletEnvironment docsEnvironment ) {

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
		    .filter( elem -> elem.getKind().equals( ElementKind.CLASS )
		        && Stream.of( elem.getAnnotationsByType( BoxMember.class ) )
		            // filter out any member functions which are marked for deprecation or have no other member functions except utility functions
		            .filter( annotation -> !annotation.deprecated() && !annotation.name().equals( "dump" ) && !annotation.name().equals( "toJSON" ) )
		            .toArray().length > 0 )
		    .forEach( elem -> {
			    Stream.of( elem.getAnnotationsByType( BoxMember.class ) )
			        .forEach( member -> {
				        Key typeKey	= Key.of( StringUtil.pascalCase( member.type().getKey().getName() ) );
				        String memberName = member.name();
				        if ( memberName == null || memberName.isEmpty() ) {
					        memberName = StringUtils.replaceOnceIgnoreCase( elem.getSimpleName().toString(), member.type().getKey().getName(), "" );
				        }
				        memberName = memberName.substring( 0, 1 ).toLowerCase() + memberName.substring( 1 );
				        if ( !typesData.containsKey( typeKey ) ) {
					        typesData.put( typeKey, new Struct( TYPES.LINKED ) );
					        typesData.getAsStruct( typeKey ).put( Key.functions, new Struct( TYPES.LINKED ) );
					        // Find our BoxLangType Class representation
					        Element typeClass = docsEnvironment.getSpecifiedElements()
					            .stream()
					            .filter( classElem -> {
						            return classElem.getKind().equals( ElementKind.CLASS )
						                &&
						                classElem.getEnclosingElement().getSimpleName().toString().equals( "types" )
						                &&
						                classElem.getSimpleName().toString().toLowerCase().equals( typeKey.getName().toLowerCase() );
					            } ).findFirst().orElse( null );

					        // Retrieve our description
					        if ( typeClass != null ) {
						        DocCommentTree commentTree = docsEnvironment.getDocTrees().getDocCommentTree( typeClass );
						        String	description	= "";
						        if ( commentTree != null ) {
							        description = ( commentTree.getFirstSentence().toString() + "\n\n"
							            + commentTree.getPreamble().toString() + commentTree.getBody().toString().trim() ).trim();
						        }
						        typesData.put( typeKey, new Struct( StructUtil.getCommonComparators().get( Key.of( "textAsc" ) ) ) );
						        typesData.getAsStruct( typeKey ).put( Key.description, description );
						        typesData.getAsStruct( typeKey ).put( Key.functions, new Struct( TYPES.SORTED ) );

						        // Append any functions with the exposed annotation
						        typeClass.getEnclosedElements().stream().filter( enclosedElem -> enclosedElem.getKind().equals( ElementKind.METHOD )
						            && enclosedElem.getAnnotationsByType( BoxMemberExpose.class ).length > 0 ).forEach( fn -> {
							            ExecutableElement functionBlock = ( ExecutableElement ) fn;
							            DocCommentTree functionComments = docsEnvironment.getDocTrees().getDocCommentTree( functionBlock );
							            typesData.getAsStruct( typeKey ).getAsStruct( Key.functions ).put(
							                Key.of(
							                    functionBlock.getSimpleName().toString() ),
							                Struct.of(
							                    Key.description,
							                    functionComments != null ? ( functionComments.getFirstSentence().toString() + "\n\n"
							                        + functionComments.getPreamble().toString() + functionComments.getBody().toString().trim() ).trim() : "",
							                    Key.arguments,
							                    functionBlock.getParameters().size() > 0
							                        ? functionBlock.getParameters()
							                            .stream()
							                            .map( parameter -> ( VariableElement ) parameter )
							                            .map( parameter -> {
								                            return Map.entry(
								                                Key.of( parameter.getSimpleName().toString() ),
								                                ( Object ) Struct.of(
								                                    Key.type, "any",
								                                    Key.required, true,
								                                    Key.defaultValue, null,
								                                    Key.validators, parameter.getEnclosingElement().toString()
								                                )
								                            );
							                            } )
							                            .collect( BLCollector.toStruct( TYPES.LINKED ) )
							                        : new Struct( TYPES.LINKED )
							                )
							            );
						            } );

					        } else {
						        typesData.getAsStruct( typeKey ).put( Key.description, "" );
					        }

				        }
				        IStruct functions = typesData.getAsStruct( typeKey ).getAsStruct( Key.functions );
				        functions.put( Key.of( memberName ), getMemberFunctionData( elem, member, docsEnvironment ) );
			        } );
		    } );

		typesData.keySet().stream()
		    .forEach( key -> generateTypeTemplate( key, typesData.getAsStruct( key ) ) );

		String inserts = typesData.keySet()
		    .stream()
		    .sorted(
		        ( a, b ) -> ortus.boxlang.runtime.operators.Compare.invoke( StringCaster.cast( a.getName() ), StringCaster.cast( b.getName() ), false ) )
		    .map( key -> {
			    return "    * [" + key.getName().toLowerCase() + "](boxlang-language/reference/types/" + key.getName().toLowerCase() + ".md)";
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
			    idx -> objectArg.length() == 0 ? idx != 0 : ! ( ( Argument ) argsArray.get( idx ) ).name().equals( Key.of( objectArg ) )
			).mapToObj( idx -> ( Argument ) argsArray.get( idx ) )
			    .forEach( arg -> {
				    IStruct argData = Struct.of(
				        Key.type, arg.type(),
				        Key.required, arg.required(),
				        Key.defaultValue, arg.hasDefaultValue() ? arg.defaultValue().toString() : "",
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
		String	typeDocs			= blankTypeTemplate;
		String	typeDescription		= typeData.getAsString( Key.description );
		String	typeMethods			= "";
		String	samplesPath			= "workbench/samples/types";

		// Retrive any samples in our convention location
		String	typeSamples			= samplesPath + "/" + typeKey.getName().toLowerCase() + ".md";
		String	typeSamplesContent	= "";
		if ( FileSystemUtil.exists( typeSamples ) ) {
			typeSamplesContent = StringCaster.cast( FileSystemUtil.read( typeSamples ) );
		}

		typeMethods += typeData.getAsStruct( Key.functions ).keySet().stream().reduce( "", ( content, memberKey ) -> {
			IStruct	memberData			= typeData.getAsStruct( Key.functions ).getAsStruct( memberKey );
			String	memberDescription	= memberData.getAsString( Key.description );
			IStruct	memberArgs			= memberData.getAsStruct( Key.arguments );
			String	argsInline			= "";
			String	argsTable			= "This function does not accept any arguments";
			if ( memberArgs.size() > 0 ) {
				argsTable	= "\n| Argument | Type | Required | Default |\n";
				argsTable	+= "|----------|------|----------|---------|\n";
				argsTable	+= memberArgs.entrySet().stream()
				    .map( argEntry -> {
								    Key	argKey			= argEntry.getKey();
								    IStruct argData		= StructCaster.cast( argEntry.getValue() );
								    String argDescription = argData.getAsString( Key.description );
								    argDescription = ( argDescription != null ? argDescription : "" ).replace( "\n",
								        "<br>" );
								    String defaultValue = argData.getAsString( Key.defaultValue );
								    if ( defaultValue != null ) {
									    defaultValue = "`" + defaultValue + "`";
								    }
								    return "| `" + argKey.getName() + "` | `" + argData.get( Key.type ) + "` | `"
								        + argData.get( Key.required ) + "` | "
								        + defaultValue + " |";
							    } )
				    .collect( Collectors.joining( "\n" ) );

				argsInline	= memberArgs.entrySet().stream()
				    .map( argEntry -> ( argEntry.getKey().getName() + "=[" + StructCaster.cast( argEntry.getValue() ).getAsString( Key.type )
				        + "]" ) )
				    .collect( Collectors.joining( ", " ) );
			}

			String memberSamples = samplesPath + "/member/" + typeKey.getName().toLowerCase() + "/" + memberKey.getName() + ".md";

			if ( FileSystemUtil.exists( memberSamples ) ) {
				memberDescription += "\n\n Examples:\n" + StringCaster.cast( FileSystemUtil.read( memberSamples ) );
			}

			// Create a collapsible section for each member function using GitBook syntax
			return content + "<details>\n<summary><code>" + memberKey.getName() + "(" + argsInline + ")" + "</code></summary>\n" + memberDescription
			    + ( !memberArgs.isEmpty() ? "\n\n Arguments:\n" + argsTable + "\n\n" : "" ) + "\n</details>\n";
		},
		    ( a, b ) -> a + b );

		if ( typeSamplesContent.length() > 0 ) {
			typeDescription += "\n\n## Examples\n\n" + typeSamplesContent;
		}

		typeDocs	= typeDocs.replace( "{TypeName}", typeKey.getName() );
		typeDocs	= typeDocs.replace( "{TypeDescription}", typeDescription == null ? "" : typeDescription );
		typeDocs	= typeDocs.replace( "{TypeMethods}", typeMethods );
		FileSystemUtil.write( TypeDocsPath + "/" + typeKey.getName().toLowerCase() + ".md", typeDocs, "utf-8", true );
	}
}
