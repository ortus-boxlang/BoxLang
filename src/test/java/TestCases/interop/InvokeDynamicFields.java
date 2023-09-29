package TestCases.interop;

public class InvokeDynamicFields {

	public static final String	HELLO			= "Hello World";
	public static final int		MY_PRIMITIVE	= 42;

	public String				name			= "luis";

	public InvokeDynamicFields() {
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName( String name ) {
		this.name = name;
	}

	public Boolean hasName() {
		return this.name != null;
	}

	public String hello() {
		return "Hello";
	}

	public String hello( String name ) {
		return "Hello " + name;
	}

	public String hello( String name, int test ) {
		return "Hello " + name + test;
	}

	public Long getNow() {
		return System.currentTimeMillis();
	}

}
