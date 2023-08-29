package ourtus.boxlang.ast;

public class ReferenceByName {

	private final String name;
	private Node reference;

	public String getName() {
		return name;
	}

	public Node getReference() {
		return reference;
	}

	public void setReference( Node reference ) {
		this.reference = reference;
	}

	public ReferenceByName( String name ) {
		this.name = name;
	}
}
