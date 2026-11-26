### Using IsDefined

Checking for the existence of a `form` variable.


```java
<bx:if isDefined( "form.submit" ) >...</bx:if>
```


### Scope Evaluation Order and Unscoped Variables

Beware of scope evaluation order when checking for an unscoped variable name.

<a href="https://try.boxlang.io/?code=eJwrLcrRc%2FP3V7BVUCoFMtPy85W40vKLcmGCYDZYtLwosyQ1v7SkoLREQ0HJs1hBHSisrpCSmpaZl5pir6CkoKaQWewC4WqAdOYrKWgqaKJqxKICpFUDpBvIB5JKmkoKVgpKQBkA5xcthQ%3D%3D" target="_blank">Run Example</a>

```java
url.FOO = "url.foo"
form.FOO = "form.foo"
writeoutput( "Is 'foo' defined? " & isDefined( "foo" ) )
writeoutput( isDefined( "foo" ) ? " (" & foo & ")" : "" )

```

Result: Is 'foo' defined? YES (url.foo)

### Dot-notation Variable Names

Potentially unexpected behavior when checking for a dot-notation variable containing a scope name.

<a href="https://try.boxlang.io/?code=eJyNjrsKwjAYhfc%2BxeEMbbL4AKIWxKWDkzpJB2tbCKRWcsHXl7ZLEkUc%2F3P7Pz3eb%2FoK9qMZVtY3g3JEjS2oZ6cIjKJmNp%2Bny%2F5YnadQWMteRrlu9O7pnQAri6iMtuvVo2tLIoeyh%2BUSyWsJGe%2F8SJYgxLQWQuWgJNYg0yVuGrP7VCfOFBEp4194X8gWKYJ6A7cVcvI%3D" target="_blank">Run Example</a>

```java
local[ "form.submit" ] = "local['form.submit']"
form.SUBMIT = "form.submit"
writeoutput( "Is 'form.submit' defined?" & isDefined( "form.submit" ) )
writeoutput( isDefined( "form.submit" ) ? " (" & form.SUBMIT & ")" : "" )
writeoutput( "<br>" )
writeoutput( "Is 'submit' defined? " & isDefined( "submit" ) )
writeoutput( isDefined( "submit" ) ? " (" & submit & ")" : "" )

```

Result: Is 'form.submit' defined? YES (local['form.submit']) Is 'submit' defined? YES(form.submit)

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxVjLEKwjAURWfzFZdMdWm%2BwEFsN1fda3OrD2wiyYsViv9u7SJyl3PhcJxDyRKukNxwkECPSfSGPgYvKjEga6ccGdTIUP20CnaIaawTM9Viu2w2G%2BdwiJ7QiAvRvtgXpTfmbbIm7DCvUJ%2F3x1O7XKvMas2URNmU8fHf%2F5rP7l645g2WuKbCD%2BR6OX8%3D" target="_blank">Run Example</a>

```java
// using isDefined with condition statement
if( isDefined( "form.reset" ) ) {
	// Code to be Executed

}
str = {}
str.VALUE = "test"
writeDump( isDefined( "str.value" ) )
 // true

```


