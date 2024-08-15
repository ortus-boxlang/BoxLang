package ortus.boxlang.compiler.asmboxpiler.transformer;

public enum ReturnValueContext {

	EMPTY( true, false ),
	VALUE( false, false ),
	VALUE_OR_NULL( false, true ),
	EMPTY_UNLESS_JUMPING( true, true );

	public final boolean	empty;
	public final boolean	nullable;

	private ReturnValueContext( boolean empty, boolean nullable ) {
		this.empty		= empty;
		this.nullable	= nullable;
	}
}
