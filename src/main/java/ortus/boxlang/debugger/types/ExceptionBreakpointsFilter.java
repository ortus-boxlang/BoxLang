package ortus.boxlang.debugger.types;

public class ExceptionBreakpointsFilter {

	public String	filter;
	public String	label;
	public String	description;

	public ExceptionBreakpointsFilter() {

	}

	public ExceptionBreakpointsFilter( String filter, String label, String description ) {
		this.filter			= filter;
		this.label			= label;
		this.description	= description;
	}

}
