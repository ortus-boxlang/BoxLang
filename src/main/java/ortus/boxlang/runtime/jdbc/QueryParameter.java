package ortus.boxlang.runtime.jdbc;

public class QueryParameter {

	public int type;
	public Object value;

	public QueryParameter(int type, Object value) {
		this.type = type;
		this.value = value;
	}
}
