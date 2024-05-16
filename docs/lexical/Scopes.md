# Scopes

BoxLang has a number of implicit scopes, some which are available only in certain runtimes:

1. `Variables`: This is the default scope for all variables that are not explicitly scoped. Variables in this scope are available only within the page or component where they are created or, in the case of included templates, within the child templates.

2. `Arguments`: This scope contains all the arguments passed to a function. They are available only within the function where they are defined.

3. `This`: This scope is used within a component to refer to the component itself. It can be used to access the public properties and methods of the component.

4. `Local`: This scope is used within a function to define variables that are only available within that function. It's similar to the `Variables` scope but is limited to the function where it's defined.

5. `Thread`: This scope is used within a `thread` component to define variables that are only available within that thread.

6. `Request`: This scope is available throughout the entire request, regardless of page boundaries. It's useful for storing data that needs to be accessed across multiple pages during a single request.

7. `Session`: This scope is available throughout the user's session. It's useful for storing data that needs to be accessed across multiple requests by the same user.

8. `Application`: This scope is available to all pages within the application. It's useful for storing data that needs to be accessed by all users of the application.

9. `Server`: This scope is available to all pages on the server, regardless of the application. It's rarely used, but can be useful for storing data that needs to be accessed by all applications on the server.

10. `Form` (Web runtime only): This scope contains all the form fields submitted in a POST request.

11. `URL` (Web runtime only): This scope contains all the parameters passed in the URL.

12. `CGI` (Web runtime only): This scope contains information about the request, such as the IP address of the client, the user agent string, and so on.

Remember that scoping your variables explicitly is a good practice as it makes your code easier to read and understand, as well as preventing the need for "scope walking" ( attempting to find a variable reference in nearby scopes ). Explicit scoping of your variables provides significant increases in performance.