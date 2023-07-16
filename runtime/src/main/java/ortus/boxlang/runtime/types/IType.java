package ortus.boxlang.runtime.types;

/**
 * Represents a base type
 *
 * Type Hierarchy
 * - Struct
 *  - sorted, ordered, etc
 * - Array
 * - XML
 * - Query
 * - Simple
 *   - String
 *   - Numeric
 *   - Boolean
 *   - List
 *   - Date
 */
public interface IType {

    /**
     * Represent as string, or throw exception if not possible
     */
    public String asString();

    // These come from the Object class, but will be important

    // toString()

    // hashcode()

    // equals()
}