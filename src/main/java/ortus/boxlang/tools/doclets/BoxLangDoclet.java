package ortus.boxlang.tools.doclets;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.StandardDoclet;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxIOException;
import ortus.boxlang.runtime.util.FileSystemUtil;
import ortus.boxlang.tools.util.BIFDocumentationGenerator;
import ortus.boxlang.tools.util.ComponentDocumentationGenerator;
import ortus.boxlang.tools.util.TypeDocumentationGenerator;

public class BoxLangDoclet extends StandardDoclet {

	private static final String	docsBasePath		= "docs/";
	private static final String	templatesBasePath	= "workbench/templates/";
	private static final String	navTemplate			= templatesBasePath + "NavTemplate.md";
	private static final String	summaryPath			= docsBasePath + "Summary.md";

	@Override
	public String getName() {
		return getClass().getSimpleName();
	}

	@Override
	public boolean run( DocletEnvironment environment ) {
		try {
			System.out.println( "Removing previous documentation artifacts" );
			String typesDirectory = docsBasePath + "types";
			if ( FileSystemUtil.exists( typesDirectory ) ) {
				FileSystemUtil.deleteDirectory( typesDirectory, true );
			}
			String componentsDirectory = docsBasePath + "components";
			if ( FileSystemUtil.exists( componentsDirectory ) ) {
				FileSystemUtil.deleteDirectory( componentsDirectory, true );
			}

			String bifsDirectory = docsBasePath + "built-in-functions";
			if ( FileSystemUtil.exists( bifsDirectory ) ) {
				FileSystemUtil.deleteDirectory( bifsDirectory, true );
			}

			String summaryFile = docsBasePath + "Summary.md";
			if ( FileSystemUtil.exists( summaryFile ) ) {
				FileSystemUtil.deleteFile( summaryFile );
			}

			String summaryContents = StringCaster.cast( FileSystemUtil.read( navTemplate ) );
			// BIF Docs
			System.out.println( "Generating BIF documentation" );
			IStruct bifNav = BIFDocumentationGenerator.generate( environment );
			summaryContents = StringUtils.replace( summaryContents, bifNav.getAsString( Key.token ), bifNav.getAsString( Key.inserts ) );

			// // Component Docs
			System.out.println( "Generating Component documentation" );
			IStruct componentNav = ComponentDocumentationGenerator.generate( environment );
			summaryContents = StringUtils.replace( summaryContents, componentNav.getAsString( Key.token ), componentNav.getAsString( Key.inserts ) );

			// TypeDocs
			System.out.println( "Generating Types documentation" );
			IStruct typesNav = TypeDocumentationGenerator.generate( environment );
			summaryContents = StringUtils.replace( summaryContents, typesNav.getAsString( Key.token ), typesNav.getAsString( Key.inserts ) );

			// Write out the menu with the new links
			FileSystemUtil.write( summaryPath, summaryContents, "utf-8", true );
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}
		return true;
	}

}
