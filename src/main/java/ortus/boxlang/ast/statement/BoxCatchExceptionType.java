package ortus.boxlang.ast.statement;

import java.util.Map;

import ortus.boxlang.ast.BoxExpr;
import ortus.boxlang.ast.BoxStatement;
import ortus.boxlang.ast.Position;

public class BoxCatchExceptionType extends BoxStatement {

	public enum CatchType {
		Any,
		Fqn,
		String,
	}

	CatchType	type;
	BoxExpr		name;

	public BoxCatchExceptionType( Position position, String sourceText ) {
		super( position, sourceText );
		this.type = CatchType.Any;
	}

	public BoxCatchExceptionType( BoxExpr name, CatchType type, Position position, String sourceText ) {
		super( position, sourceText );
		this.type	= type;
		this.name	= name;
	}

	public CatchType getCatchType() {
		return this.type;
	}

	public BoxExpr getName() {
		return this.name;
	}

	public static boolean isAny( BoxCatchExceptionType catchType ) {
		return catchType.getCatchType() == CatchType.Any;
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		map.put( "type", enumToMap( type ) );
		map.put( "name", name );

		return map;
	}

}
