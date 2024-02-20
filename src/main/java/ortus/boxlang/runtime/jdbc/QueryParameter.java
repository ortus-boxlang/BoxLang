package ortus.boxlang.runtime.jdbc;

public class QueryParameter {

	public int type;
	public Object value;

	public QueryParameter(Object value) {
		this(value, null);
	}

	public QueryParameter(Object value, int type) {
		this.type = type;
		this.value = value;
	}
}
