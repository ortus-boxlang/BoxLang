package ortus.boxlang.runtime.interop;

import ortus.boxlang.runtime.types.IStruct;

public class VarArgsExample {

	private String[] values;

	public VarArgsExample( String... values ) {
		this.values = values;
	}

	public VarArgsExample( IStruct map, String... values ) {
		this.values = values;
	}

	public String[] getValues() {
		return values;
	}

	public void setValues( String... values ) {
		this.values = values;
	}
}