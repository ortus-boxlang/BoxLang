### Example of using abort() to stop processing after an error occurs

In this example we demonstrate using the abort() function to stop any further processing after we deliberately call a non-existent function.

<a href="https://try.boxlang.io/?code=eJw1jrEOwjAMROfkKzymA1SsIBaEkFhYEB%2BQpm4b0SaV49JWiH8nKWXznZ99xzTDW4o8h04%2FETQY3bbAPk7Ouw1ONjA6hmpwhq13UlSWAt90h3CEISDdkV7W4LZGfkR5mq%2Blgh1kybj8WZUd5EfGFKPZNKDdDEjkKcjVUYuVrV3KoeuBG%2FxBqU4SBfkxJkgxkmU8R0YBQvycTnThKdWkSBL05A2GYF0tRTHtl2Vq8AU1S07t" target="_blank">Run Example</a>

```java
try {
	// make a call to a non-existent function
	firstName = userService.getUserById( 1 ).getFirstName();
}
// catch any errors
 catch (any e) {
	// dump the error to the browser
	writeDump( e );
	// abort further processing
	bx:abort;
}

```

Result: Error page: Variable USERSERVICE is undefined.

### Example of using <cfabort> to stop processing after an error occurs

In this example we demonstrate using the <cfabort> tag to stop any further processing after we deliberately call a non-existent function.


```java
<bx:try>
	<!--- make a call to a non-existent function --->
	<bx:set firstName = userService.getUserById( 1 ).getFirstName() >
<!--- catch any errors --->

<bx:catch type="any">
	<!--- dump the error to the browser --->
	<bx:dump var="#bxcatch#">
	<!--- abort further processing --->
	<bx:abort>
</bx:catch></bx:try>
```

Result: Error page: Variable USERSERVICE is undefined.

