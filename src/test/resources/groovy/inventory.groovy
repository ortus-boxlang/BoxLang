// Real, standalone Groovy SCRIPT fixture used by GroovyRealFileTest - a genuine
// top-level script (no enclosing class) weaving together several of this dialect's
// features: an enum, a Range smart-switch case, map literals with dot-access,
// "==~" full-match regex, collect{}/findAll{}, closures reassigning an outer
// script-level variable, tuple destructuring, and a general (non-curated-name)
// bare-identifier command-style call.

enum Priority { LOW, MEDIUM, HIGH }

def classify( quantity ) {
	switch ( quantity ) {
		case 0..4:
			return Priority.LOW
		case 5..19:
			return Priority.MEDIUM
		default:
			return Priority.HIGH
	}
}

def auditLog = []
def audit( msg ) {
	auditLog.add( msg )
}

def items = [
	[ name: "widget", sku: "AB-123456", qty: 2 ],
	[ name: "gadget", sku: "CD-654321", qty: 12 ],
	[ name: "gizmo", sku: "not-a-sku", qty: 30 ]
]

def validSkus = items.findAll { it.sku ==~ /^[A-Z]{2}-\d{6}$/ }
def report = validSkus.collect { "${it.name}:${classify(it.qty)}" }

total = 0
items.each { total = total + it.qty }

// A general bare-identifier command-style call to a lowercase, non-curated
// user-defined function ("audit") - previously only println/print/printf worked
// this way without parentheses.
def note = "scan-complete"
audit note

// NOTE: array indices in this dialect are 1-based (BoxLang's own native Array
// convention), not 0-based like real Groovy/Java - see GroovyExpressionVisitor#
// visitIndexExpr, which passes an index straight through with no rebasing.
def ( first, second ) = [ items[ 1 ].name, items[ 2 ].name ]

return "${first},${second} total=${total} valid=${validSkus.size()} report=${report.toList('|')} audit=${auditLog.toList(',')}"
