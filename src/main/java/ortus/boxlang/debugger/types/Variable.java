package ortus.boxlang.debugger.types;

public class Variable {

	/**
	 * The variable's name.
	 */
	public String	name;

	/**
	 * The variable's value.
	 * This can be a multi-line text, e.g. for a function the body of a function.
	 * For structured variables (which do not have a simple value), it is
	 * recommended to provide a one-line representation of the structured object.
	 * This helps to identify the structured object in the collapsed state when
	 * its children are not yet visible.
	 * An empty string can be used if no value should be shown in the UI.
	 */
	public String	value;

	/**
	 * The type of the variable's value. Typically shown in the UI when hovering
	 * over the value.
	 * This attribute should only be returned by a debug adapter if the
	 * corresponding capability `supportsVariableType` is true.
	 */
	public String	type;

	/**
	 * Properties of a variable that can be used to determine how to render the
	 * variable in the UI.
	 */
	// presentationHint?: VariablePresentationHint;

	/**
	 * The evaluatable name of this variable which can be passed to the `evaluate`
	 * request to fetch the variable's value.
	 */
	// evaluateName?: string;

	/**
	 * If `variablesReference` is > 0, the variable is structured and its children
	 * can be retrieved by passing `variablesReference` to the `variables` request
	 * as long as execution remains suspended. See 'Lifetime of Object References'
	 * in the Overview section for details.
	 */
	public int		variablesReference;

	/**
	 * The number of named child variables.
	 * The client can use this information to present the children in a paged UI
	 * and fetch them in chunks.
	 */
	// namedVariables?: number;

	/**
	 * The number of indexed child variables.
	 * The client can use this information to present the children in a paged UI
	 * and fetch them in chunks.
	 */
	// indexedVariables?: number;

	/**
	 * A memory reference associated with this variable.
	 * For pointer type variables, this is generally a reference to the memory
	 * address contained in the pointer.
	 * For executable data, this reference may later be used in a `disassemble`
	 * request.
	 * This attribute may be returned by a debug adapter if corresponding
	 * capability `supportsMemoryReferences` is true.
	 */
	// memoryReference?: string;
}
