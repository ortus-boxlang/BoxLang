// Real, standalone Groovy class fixture used by GroovyRealFileTest - loaded as an
// actual file from disk (not an inline Java string), exercising real-world-shaped
// Groovy syntax: a nested enum, an explicit constructor, closures over an instance
// field, a Range smart-switch case, a regex Pattern smart-switch case, and a real
// Java interop anonymous class (java.lang.Runnable).
class BankAccount {

	enum AccountType { CHECKING, SAVINGS, BUSINESS }

	def owner
	def type
	def balance = 0
	def history = []

	BankAccount( accountOwner, accountType ) {
		owner = accountOwner
		type = accountType
	}

	def deposit( amount ) {
		balance = balance + amount
		history.add( "deposit:${amount}" )
		return balance
	}

	def withdraw( amount ) {
		balance = balance - amount
		history.add( "withdraw:${amount}" )
		return balance
	}

	def totalDeposits() {
		return history.findAll { it.contains( "deposit" ) }.size()
	}

	// Range smart-switch case
	def feeTier() {
		switch ( balance ) {
			case 0..99:
				return "basic"
			case 100..999:
				return "standard"
			default:
				return "premium"
		}
	}

	// Regex Pattern smart-switch case. An instance method deliberately, not "static" -
	// a real static method on a Groovy-compiled class is untested territory in this
	// dialect (the only other "static" example in the whole suite is a no-op static
	// FIELD - see GroovyVisitor#visitFieldDeclaration's own "static" notes), so this
	// fixture stays within what's actually proven rather than risking an unrelated
	// failure obscuring what this file means to demonstrate.
	def isValidAccountNumber( id ) {
		switch ( id ) {
			case ~/^[A-Z]{2}-\d{6}$/:
				return true
			default:
				return false
		}
	}

	// Real JDK dynamic proxy - genuinely implements java.lang.Runnable, not just
	// duck-typed, so it's callable (and instanceof-checkable) from outside BoxLang too.
	// Deliberately self-contained (no reference to the enclosing instance's own fields):
	// this dialect's anonymous classes are modeled as static nested classes, with no
	// implicit capture of an enclosing instance - see GroovyVisitor#buildAnonymousLocalClass.
	def notifier() {
		return new Runnable() {
			void run() {
				return "notified"
			}
		}
	}

	def describe() {
		return "${owner} (${type}) balance=${balance} tier=${feeTier()}"
	}
}
