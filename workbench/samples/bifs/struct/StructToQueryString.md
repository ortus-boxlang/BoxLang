### structToQueryString with the default delimiter

Converting a struct to a query string using the default delimiter (&amp;)

<a href="https://try.boxlang.io/?code=eJwrzs9NDS4pKk0uUbBVqFbg4lRKyywqLvFLzE1VUrBSUPLKz8hT0gEK5yQiibrkpypx1VpzlRdllqS6lOYWaCgUgw0JyQ8sTS2qBJqYmZcOFESYrqmgac0FAIiYI08%3D" target="_blank">Run Example</a>

```java
someStruct = { 
	"firstName" : "John",
	"lastName" : "Doe"
};
writeDump( structToQueryString( someStruct ) );

```

Result: firstName=John&lastName=Doe

