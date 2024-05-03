package ortus.boxlang.servlet;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.handlers.ServletRequestContext;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.web.WebRequestExecutor;

public class BoxLangServlet implements Servlet {

	HttpHandler		undertowhandler;
	ServletConfig	config;
	BoxRuntime		runtime;

	public void init( ServletConfig config ) throws ServletException {
		this.config		= config;
		this.runtime	= BoxRuntime.getInstance();
	}

	public void service( ServletRequest req, ServletResponse res ) throws ServletException, IOException {
		HttpServerExchange		exchange				= null;
		ServletRequestContext	servletRequestContext	= ServletRequestContext.current();
		if ( servletRequestContext != null ) {
			exchange = servletRequestContext.getExchange();
		}
		if ( exchange == null ) {
			throw new ServletException( "This servlet only works inside Undertow. " + req.getClass().getName() );
		}
		// FusionReactor automatically tracks servlets
		// Note: web root can be different every request if this is a multi-site server or using ModCFML
		WebRequestExecutor.execute( exchange, config.getServletContext().getRealPath( "/" ), false );
	}

	public void destroy() {
		undertowhandler = null;
		this.runtime.shutdown();
		this.runtime = null;
	}

	public ServletConfig getServletConfig() {
		return this.config;
	}

	public String getServletInfo() {
		return "Ortus BoxLang " + this.runtime.getVersionInfo().getOrDefault( Key.version, "" ).toString();
	}
}
