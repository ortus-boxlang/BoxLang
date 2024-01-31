/**
 * This is a BoxLang only BIF
 *
 * Annotations you can use on a BIF:
 * <pre>
 * // The alias of the BIF, defaults to the name of the Class
 * @BoxBIF 'myBifAlias'
 * @BoxBIF [ 'myBifAlias', 'myOtherBifAlias' ]
 * @BoxMember 'string'
 * @BoxMember { 'string' : { name : '', objectArgument : '' }, 'array' : { name : '', objectArgument : '' } }
 * </pre>
 *
 * The runtime needs to inject the following into the variables	scope:
 * - functionName 		: The name of the function being executed
 * - isMemberExecution : A boolean indicating if the function is being executed as a member function
 * - runtime 			: BoxLangRuntime
 * - log 				: A logger
 * - functionService 	: The BoxLang FunctionService
 * - interceptorService : The BoxLang InterceptorService
 */
@BoxBIF 'moduleHelloWorld'
@BoxMember 'string'
component{

	/**
	 * The execution of this BIF with amazing BoxLang arguments
	 */
	function invoke( required name, numeric age = 0 ){
		// Execute the function
		return "Hello World, my name is #arguments.name# and I am #arguments.age# years old";
	}

}
