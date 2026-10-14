// Real, standalone Groovy file fixture used by GroovyRealFileTest, exercising the
// "multi-class-only file" shape (2+ top-level classes, no other statements at
// all): the textually first class (Circle) becomes the file's own loadable
// BoxClass; the second (Square) becomes a BoxLocalClass peer in its body - see
// GroovyParser#toAst.
class Circle {

	def radius

	Circle( r ) {
		radius = r
	}

	def area() {
		return radius * radius * 3
	}
}

class Square {

	def side

	Square( s ) {
		side = s
	}

	def area() {
		return side * side
	}
}
