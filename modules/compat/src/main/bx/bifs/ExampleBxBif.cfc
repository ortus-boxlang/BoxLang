/**
 * This is a BOXLANG BIF
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
 * The runtime injects the following into the `variables` scope:
 * - boxRuntime : BoxLangRuntime
 * - log : A logger
 * - functionService : The BoxLang FunctionService
 * - interceptorService : The BoxLang InterceptorService
 * - moduleRecord : The ModuleRecord instance
 */
@BoxBIF "ExampleBxBIF"
component {

    /**
     * An example BOXLANG BIF
     *
     * @name
     * @age
     */
    function invoke() {
        return "Hello from an ExampleBxBIF!";
    }

}
