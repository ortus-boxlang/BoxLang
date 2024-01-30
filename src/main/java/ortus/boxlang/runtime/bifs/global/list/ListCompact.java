package ortus.boxlang.runtime.bifs.global.list;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.ListUtil;

@BoxBIF
@BoxMember(type = BoxLangType.STRING, name = "listCompact")
public class ListCompact extends BIF {

	/**
	 * Constructor
	 */
	public ListCompact() {
		super();
		declaredArguments =
			new Argument[] {
				new Argument(true, "string", Key.list),
				new Argument(
					false,
					"string",
					Key.delimiter,
					ListUtil.DEFAULT_DELIMITER
				),
				new Argument(
					false,
					"boolean",
					Key.multiCharacterDelimiter,
					false
				),
			};
	}

	/**
	 * Removes the delimiters at the start and end of the list.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.list string list to compact
	 *
	 * @argument.delimiter string the list delimiter
	 *
	 * @argument.multiCharacterDelimiter boolean whether the delimiter is multi-character
	 */
	public Object invoke(IBoxContext context, ArgumentsScope arguments) {
		Boolean multiCharacterDelimiter = arguments.getAsBoolean(
			Key.multiCharacterDelimiter
		);
		String delimiter = arguments.getAsString(Key.delimiter);
		return ListUtil.asString(
			ListUtil.trim(
				ListUtil.asList(
					arguments.getAsString(Key.list),
					delimiter,
					true,
					multiCharacterDelimiter
				)
			),
			multiCharacterDelimiter ? delimiter : delimiter.substring(0, 1)
		);
	}
}
