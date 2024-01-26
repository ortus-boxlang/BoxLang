package ortus.boxlang.debugger.event;

import ortus.boxlang.debugger.ISendable;

public class Event implements ISendable {

	public String	type	= "event";
	public String	event;

	public Event( String event ) {
		this.event = event;
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	public String getName() {
		return event;
	}
}
