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
import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.BoxComponent;
import ortus.boxlang.runtime.components.ComponentDescriptor;
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.dynamic.casters.KeyCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.ComponentService;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.util.BLCollector;
import ortus.boxlang.runtime.util.FileSystemUtil;
import ortus.boxlang.runtime.validation.Validator;

public class ComponentDocumentationGenerator {

	private static final String	docsBasePath			= "docs/boxlang-language/reference/";
	private static final String	templatesBasePath		= "workbench/templates/";
	private static final String	ComponentDocsPath		= docsBasePath + "components";
	private static final String	blankComponentTemplate	= StringCaster.cast( FileSystemUtil.read( templatesBasePath + "ComponentDocTemplate.md" ) );
	private static final String	navToken				= "(dynamic-components-nav)";

	@SuppressWarnings( "unchecked" )
	public static IStruct generate( DocletEnvironment docsEnvironment ) throws IOException {

		BoxRuntime			runtime				= BoxRuntime.getInstance();
		ComponentService	componentService	= runtime.getComponentService();

		docsEnvironment.getSpecifiedElements()
		    .stream()
		    .filter( elem -> elem.getKind().equals( ElementKind.CLASS ) && elem.getAnnotationsByType( BoxComponent.class ).length > 0 )
		    .forEach( elem -> {
			    componentService.registerComponent( ( Class ) elem.getClass(), null, null );
		    } );

		Array			newComponents	= new Array( componentService.getComponentNames() );

		List<Element>	docElements		= docsEnvironment.getSpecifiedElements()
		    .stream()
		    .filter( elem -> elem.getAnnotationsByType( BoxComponent.class ).length > 0 )
		    .peek( elem -> elem.getSimpleName() )
		    .map( elem -> ( Element ) elem )
		    .collect( Collectors.toList() );

		try {
			Array	componentInfos	= newComponents.stream()
			    .map( componentName -> ( ComponentDescriptor ) componentService.getComponent( StringCaster.cast( componentName ) ) )
			    .map( component -> ensureComponentTemplate( component, docElements, docsEnvironment ) )
			    .collect( BLCollector.toArray() )
			    .stream()
			    .filter( record -> ( ( HashMap<String, String> ) record ).get( "name" ) != null )
			    .collect( BLCollector.toArray() );
			Struct	groupLinks		= new Struct();
			componentInfos.stream()
			    .forEach( componentInfo -> {
				    HashMap<String, String> componentMeta = ( ( HashMap<String, String> ) componentInfo );
				    Key					componentKey	= Key.of( componentMeta.get( "name" ) );
				    Key					groupKey		= Key.of( componentMeta.get( "package" ) );

				    if ( !groupLinks.containsKey( groupKey ) ) {
					    groupLinks.put( groupKey, new Array() );
				    }
				    ArrayCaster.cast( groupLinks.get( groupKey ) ).push( "[" + componentMeta.get( "name" ) + "](" + componentMeta.get( "file" ) + ")" );
			    } );

			String inserts = groupLinks.keySet()
			    .stream()
			    .sorted(
			        ( a, b ) -> ortus.boxlang.runtime.operators.Compare.invoke( StringCaster.cast( a.getName() ), StringCaster.cast( b.getName() ), false ) )
			    .map( key -> {
				    String keyLink = "[" + key.getName() + "](" + ComponentDocsPath.replace( "docs/", "" ) + "/" + key.getName() + "/README.md)";
				    String group = "    * " + keyLink + "\n";
				    group += ArrayCaster.cast( groupLinks.get( key ) )
				        .stream()
				        .map( componentLink -> {
					        return "      * " + componentLink;
				        } )
				        .collect( Collectors.joining( "\n" ) );
				    return group;
			    } )
			    .collect( Collectors.joining( "\n" ) );
			return Struct.of(
			    Key.token, navToken,
			    Key.inserts, inserts
			);
		} catch ( Exception e ) {
			throw new BoxRuntimeException( "An error occurred while attempting to generate the technical documentation.", e );
		} finally {
			runtime.shutdown();
		}
	}

	public static HashMap<String, String> ensureComponentTemplate( ComponentDescriptor component, List<Element> docElements,
	    DocletEnvironment docsEnvironment ) {
		String		name								= component.name.getName();
		String[]	packageParts						= component.componentClass.getName().split( "\\." );
		String		path								= packageParts[ packageParts.length - 2 ];
		String		fileName							= name + ".md";
		String		relativePath						= path + '/' + fileName;
		String		componentFile						= ComponentDocsPath + '/' + relativePath;
		String		ComponentNamePlaceholder			= "{ComponentName}";
		String		ComponentDescPlaceholder			= "{ComponentDescription}";
		String		ComponentAttributesPlaceholder		= "{ComponentAttributes}";
		String		ComponentAttributesTablePlaceholder	= "{ComponentAttributesTable}";
		String		PackageNavPlaceholder				= "{PackageNav}";

		if ( !FileSystemUtil.exists( componentFile ) ) {
			Key		componentKey		= Key.of( name );
			Element	javadocElement		= docElements.stream()
			    .filter( elem -> componentKey.equals( Key.of( elem.getSimpleName() ) )
			        ||
			        Stream.of( elem.getAnnotationsByType( BoxComponent.class ) )
			            .filter(
			                annotation -> componentKey.equals( Key.of( annotation.name() ) )
			            ).count() > 0
			    ).findFirst().orElse( null );

			Array	componentAttributes	= new Array( component.getComponent().getDeclaredAttributes() );
			Struct	attrComments		= new Struct();
			String	description			= null;
			Array	attributesExclude	= new Array();
			if ( javadocElement != null ) {
				Element invokeElement = javadocElement.getEnclosedElements().stream()
				    .filter( elem -> elem.getKind().equals( ElementKind.METHOD ) && elem.getSimpleName().contentEquals( "_invoke" ) )
				    .findFirst().orElse( null );
				if ( invokeElement != null ) {
					DocCommentTree commentTree = docsEnvironment.getDocTrees().getDocCommentTree( invokeElement );
					if ( commentTree != null ) {
						DocTree specificDescription = commentTree.getBlockTags().stream()
						    .filter( tag -> tag.getKind().equals( DocTree.Kind.UNKNOWN_BLOCK_TAG ) && tag.toString().contains( "@component" )
						        && ( ( BlockTagTree ) tag ).getTagName().equals( "component." + name ) )
						    .findFirst().orElse( null );
						if ( specificDescription != null ) {
							description = ( ( BlockTagTree ) specificDescription ).toString()
							    .replace( '@' + ( ( BlockTagTree ) specificDescription ).getTagName(), "" ).trim();
						} else {
							description = ( commentTree.getFirstSentence().toString() + "\n\n"
							    + commentTree.getPreamble().toString() ).trim();
						}
						attributesExclude = ArrayCaster.cast( commentTree.getBlockTags().stream()
						    .filter( tag -> tag.getKind().equals( DocTree.Kind.UNKNOWN_BLOCK_TAG ) && tag.toString().contains( "@component" )
						        && ( ( BlockTagTree ) tag ).getTagName().equals( "component." + name + ".attributes.exclude" ) )
						    .map( tag -> ( ( BlockTagTree ) tag ).toString().replace( '@' + ( ( BlockTagTree ) tag ).getTagName(), "" ).trim() )
						    .findFirst().orElse( "" ).split( "," ) );

						commentTree.getBlockTags().stream()
						    .filter( tag -> tag.getKind().equals( DocTree.Kind.UNKNOWN_BLOCK_TAG ) && tag.toString().contains( "@attribute" ) )
						    .forEach( attribute -> {
							    try {
								    String argName = ( ( BlockTagTree ) attribute ).getTagName().split( "\\." )[ 1 ];
								    String argBody = ( ( BlockTagTree ) attribute ).toString().replace( '@' + ( ( BlockTagTree ) attribute ).getTagName(), "" )
								        .trim();
								    attrComments.put( Key.of( argName ), argBody );
							    } catch ( Throwable e ) {
								    System.err.println( "Failed to parse attribute comment: " + attribute.toString() );
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

			final Array attributesExcludeFinal = attributesExclude;

			if ( attributesExclude.size() > 0 ) {
				componentAttributes = componentAttributes.stream()
				    .map( attribute -> ( Attribute ) attribute )
				    .filter( attribute -> !attributesExcludeFinal.contains( KeyCaster.cast( attribute.name() ).getName() ) )
				    .collect( BLCollector.toArray() );
			}

			if ( description == null ) {
				description = "No description available.";
			}
			String	attributesInline	= "";
			String	attributesTable		= "This tag does not accept any attributes";
			if ( componentAttributes.size() > 0 ) {
				attributesTable		= "\n| Atrribute | Type | Required | Description | Default |\n";
				attributesTable		+= "|----------|------|----------|-------------|---------|\n";
				attributesTable		+= componentAttributes.stream()
				    .map( componentInfo -> ( Attribute ) componentInfo )
				    // .map( attribute -> attribute.name() )
				    // .map( StringCaster::cast )
				    .map( attribute -> {
					    Key	argKey				= attribute.name();
					    String argDescription	= attrComments.getAsString( argKey );
					    String attributeName	= attribute.name().getName();
					    String attributeType	= attribute.type();
					    Boolean attributeRequired = new Array( attribute.validators().toArray() ).stream()
					        .anyMatch( validator -> validator.equals( Validator.REQUIRED ) );
					    argDescription = ( argDescription != null ? argDescription : "" ).replace( "\n", "<br>" );
					    String defaultValue = attribute.defaultValue() != null ? attribute.defaultValue().toString() : "";
					    if ( !defaultValue.isEmpty() ) {
						    defaultValue = "`" + defaultValue + "`";
					    }
					    return "| `" + attributeName + "` | `" + attributeType + "` | `" + attributeRequired + "` | "
					        + argDescription + " | "
					        + defaultValue + " |";
				    } )
				    .collect( Collectors.joining( "\n" ) );

				attributesInline	= componentAttributes.stream()
				    .map( componentInfo -> ( ortus.boxlang.runtime.components.Attribute ) componentInfo )
				    .map( componentInfo -> ( componentInfo.name() + "=[" + componentInfo.type() + "]" ) )
				    .collect( Collectors.joining( "\n" ) );

			}
			String	packageNav	= "";
			String	contents	= blankComponentTemplate.replace( ComponentNamePlaceholder, name )
			    .replace( ComponentDescPlaceholder, description )
			    .replace( ComponentAttributesPlaceholder, attributesInline )
			    .replace( ComponentAttributesTablePlaceholder, attributesTable )
			    .replace( PackageNavPlaceholder, packageNav );
			FileSystemUtil.write( componentFile, contents, "utf-8", true );
			return new HashMap<String, String>() {

				{
					put( "name", name );
					put( "package", path );
					put( "file", "boxlang-language/reference/components/" + relativePath );
				}
			};
		}

		return new HashMap<String, String>();

	}

}
