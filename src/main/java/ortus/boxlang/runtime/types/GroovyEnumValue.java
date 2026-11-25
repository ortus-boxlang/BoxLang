/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.runtime.types;

import java.io.Serializable;

/**
 * Backs the Groovy parser's {@code enum} declarations with real type identity, instead of the
 * plain-string-constant desugaring this replaced. Each enum constant (e.g. {@code Color.RED})
 * becomes an instance of this class, carrying its own name and ordinal - enabling real Groovy's
 * {@code .name()}/{@code .ordinal()} member calls (dispatched generically via BoxLang's own Java
 * interop, since this is a plain Java object with public {@code name()}/{@code ordinal()}
 * methods - no BoxLang-specific plumbing needed for that part).
 * <p>
 * Deliberately kept comparable/interchangeable with a plain string, matching the previous
 * desugaring's behavior and the overwhelmingly common idiom of comparing an enum constant against
 * a string literal or using it directly in a switch/case or string interpolation:
 * <ul>
 * <li>{@link #equals(Object)} treats a {@link String} equal to this constant's name as equal,
 * symmetrically in both comparison directions - see {@code StringCasterStrict#cast} and
 * {@code Compare#attempt}, both of which recognize this type directly so
 * {@code "RED" == Color.RED} and {@code Color.RED == "RED"} agree.</li>
 * <li>{@link #toString()} returns the constant's name, so string interpolation/concatenation
 * renders exactly as the previous plain-string desugaring did.</li>
 * <li>{@link #compareTo(GroovyEnumValue)} orders by ordinal (not name), so the spaceship operator
 * between two constants of the same enum - {@code Color.RED <=> Color.GREEN} - now reflects real
 * declaration order instead of a lexical string comparison.</li>
 * </ul>
 * Two constants only compare equal to EACH OTHER (not just by name) when they share the same
 * enum type name, so constants from two different Groovy enums that happen to share a member
 * name (e.g. {@code Suit.CLUBS} vs some other enum's own {@code CLUBS}) are never confused with
 * one another - only with a plain string.
 */
public class GroovyEnumValue implements Comparable<GroovyEnumValue>, Serializable {

	private final String	enumTypeName;
	private final String	name;
	private final int		ordinal;

	private GroovyEnumValue( String enumTypeName, String name, int ordinal ) {
		this.enumTypeName	= enumTypeName;
		this.name			= name;
		this.ordinal		= ordinal;
	}

	/**
	 * Factory used by the Groovy parser's enum-declaration desugaring - see
	 * {@code GroovyVisitor#visitEnumDeclaration}.
	 *
	 * @param enumTypeName The simple name of the declaring enum (e.g. "Color")
	 * @param name         The constant's own name (e.g. "RED")
	 * @param ordinal      The constant's zero-based declaration order
	 *
	 * @return A new constant instance
	 */
	public static GroovyEnumValue of( String enumTypeName, String name, int ordinal ) {
		return new GroovyEnumValue( enumTypeName, name, ordinal );
	}

	/**
	 * @return This constant's own name, matching real Groovy's {@code Enum.name()}
	 */
	public String name() {
		return name;
	}

	/**
	 * @return This constant's zero-based declaration order, matching real Groovy's
	 *         {@code Enum.ordinal()}
	 */
	public int ordinal() {
		return ordinal;
	}

	/**
	 * @return The simple name of the enum type this constant belongs to
	 */
	public String getEnumTypeName() {
		return enumTypeName;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public boolean equals( Object other ) {
		if ( this == other ) {
			return true;
		}
		if ( other instanceof GroovyEnumValue otherValue ) {
			return enumTypeName.equals( otherValue.enumTypeName ) && name.equals( otherValue.name );
		}
		if ( other instanceof String otherString ) {
			return name.equals( otherString );
		}
		return false;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public int compareTo( GroovyEnumValue other ) {
		return Integer.compare( ordinal, other.ordinal );
	}

}
