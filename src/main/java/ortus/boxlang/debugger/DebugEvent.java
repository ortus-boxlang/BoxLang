package ortus.boxlang.debugger;

public abstract class DebugEvent {

	public enum Type {
		NullEvent
	}

	Type type;

	protected DebugEvent( Type type ) {
		this.type = type;
	}
}
