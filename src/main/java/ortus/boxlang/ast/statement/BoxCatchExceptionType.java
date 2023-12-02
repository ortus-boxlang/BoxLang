package ortus.boxlang.ast.statement;

import java.util.Map;

import ortus.boxlang.ast.BoxExpr;
import ortus.boxlang.ast.BoxStatement;
import ortus.boxlang.ast.Position;

public class BoxCatchExceptionType extends BoxStatement {

	BoxExpr name;

	public BoxCatchExceptionType( Position position, String sourceText ) {
		super( position, sourceText );
	}

	public BoxCatchExceptionType( BoxExpr name, Position position, String sourceText ) {
		super( position, sourceText );
		this.name = name;
	}

	public BoxExpr getName() {
		return this.name;
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		map.put( "name", name );

		return map;
	}

}
