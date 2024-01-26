
package ortus.boxlang.runtime.bifs.global.i18n;

import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;

@BoxBIF
@BoxMember( type = BoxLangType.NUMERIC )

public class LSCurrencyFormat extends LSNumberFormat {

	/**
	 * Constructor
	 */
	public LSCurrencyFormat() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.number ),
		    new Argument( false, "string", Key.type ),
		    new Argument( false, "string", Key.locale )
		};
	}

}
