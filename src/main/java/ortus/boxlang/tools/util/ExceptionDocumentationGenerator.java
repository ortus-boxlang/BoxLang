package ortus.boxlang.tools.util;

import java.util.stream.Collectors;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;

import com.sun.source.doctree.DocCommentTree;

import jdk.javadoc.doclet.DocletEnvironment;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.IStruct.TYPES;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxLangException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.FileSystemUtil;

public class ExceptionDocumentationGenerator {

	private static final String	docsBasePath			= "docs/boxlang-language/reference/";
	private static final String	templatesBasePath		= "workbench/templates/";
	private static final String	ExceptionDocPath		= docsBasePath + "Exceptions.md";
	private static final String	blankExceptionTemplate	= StringCaster.cast( FileSystemUtil.read( templatesBasePath + "ExceptionDocTemplate.md" ) );

	public static void generate( DocletEnvironment docsEnvironment ) {
		IStruct exceptionsData = new Struct( TYPES.LINKED );
		docsEnvironment.getSpecifiedElements()
		    .stream()
		    .filter( elem -> elem.getKind().equals( ElementKind.CLASS )
		        && ( ( ( TypeElement ) elem ).getSuperclass().toString().equals( BoxLangException.class.getName() ) )
		        ||
		        ( ( TypeElement ) elem ).getSuperclass().toString().equals( BoxRuntimeException.class.getName() )
		    )
		    .forEach( elem -> {
			    DocCommentTree commentTree = docsEnvironment.getDocTrees().getDocCommentTree( elem );
			    String		description	= "";
			    description = ( commentTree.getFirstSentence().toString() + "\n\n"
			        + commentTree.getPreamble().toString() + commentTree.getBody().toString().trim() ).trim();

			    exceptionsData.put( Key.of( elem.getSimpleName().toString() ), description );
		    } );

		generateExceptionTemplate( exceptionsData );

	}

	private static void generateExceptionTemplate( IStruct exceptionsData ) {
		String	ExceptionDocs	= blankExceptionTemplate;

		String	docContents		= exceptionsData.entrySet().stream().map( set -> {

									String	exceptionKey	= set.getKey().getName();
									String	exceptionDesc	= set.getValue().toString();

									return "<details>\n<summary><code>" + exceptionKey + "</code></summary>\n\n"
									    + exceptionDesc
									    + "\n</details>\n";

								} )
		    .collect( Collectors.joining( "\n" ) );

		ExceptionDocs = ExceptionDocs.replace( "{Exceptions}", docContents );
		FileSystemUtil.write( ExceptionDocPath, ExceptionDocs, "utf-8", true );
	}

}
