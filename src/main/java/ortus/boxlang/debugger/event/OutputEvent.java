package ortus.boxlang.debugger.event;

public class OutputEvent extends Event {

	public OutputBody body;

	private class OutputBody {

		public String	category;
		public String	output;
	}

	public OutputEvent( String category, String output ) {
		super( "output" );

		this.body			= new OutputBody();
		this.body.category	= category;
		this.body.output	= output;
	}

}
