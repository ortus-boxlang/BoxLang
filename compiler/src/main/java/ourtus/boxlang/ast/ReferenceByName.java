package ourtus.boxlang.ast;

/**
 * Represent a reference by the name to a Node
 */
public class ReferenceByName {

	private final String	name;
	private Node			reference;

	/**
	 * Returns the name of the reference
	 * 
	 * @return name of the reference
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the referenced node
	 * 
	 * @return the Node referenced by the name
	 * 
	 * @see Node
	 */
	public Node getReference() {
		return reference;
	}

	/**
	 * Set the referenced node referred by the name
	 * 
	 * @param reference the Node referred
	 * 
	 * @see Node
	 */
	public void setReference( Node reference ) {
		this.reference = reference;
	}

	/**
	 *
	 * @param name
	 */
	public ReferenceByName( String name ) {
		this.name = name;
	}
}
