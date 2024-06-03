package ortus.boxlang.runtime.util.conversion;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.util.ListUtil;

public class XMLValidationHandler extends DefaultHandler {

	private Struct	response;

	private Array	warnings;
	private Array	errors;
	private Array	fatals;

	public XMLValidationHandler( Struct response ) {
		this.response	= response;
		this.warnings	= ArrayCaster.cast( response.get( Key.warning ) );
		this.errors		= ArrayCaster.cast( response.get( Key.errors ) );
		this.fatals		= ArrayCaster.cast( response.get( Key.fatalErrors ) );
	}

	@Override
	public void warning( SAXParseException ex ) {
		parseError( warnings, ex );
	}

	@Override
	public void error( SAXParseException ex ) {
		response.put( Key.status, false );
		parseError( errors, ex );
	}

	@Override
	public void fatalError( SAXParseException ex ) throws SAXException {
		response.put( Key.status, false );
		parseError( fatals, ex );
	}

	private void parseError( Array messages, SAXParseException ex ) {
		StringBuffer	message	= new StringBuffer();
		String			id		= ex.getSystemId();
		if ( id != null ) {
			Array idList = ListUtil.asList( id, "/" );
			message.append( StringCaster.cast( idList.getAt( idList.size() - 1 ) ) + ':' );
		}
		message.append( ex.getLineNumber() + ':' )
		    .append( ex.getColumnNumber() + ':' )
		    .append( ex.getMessage() + ": " );
		messages.push( message.toString() );
	}

}
