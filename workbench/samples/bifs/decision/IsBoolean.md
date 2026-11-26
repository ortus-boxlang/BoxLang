### Yes

`Yes` is considered a boolean that is `true`

<a href="https://try.boxlang.io/?code=eJzLLHbKz89JTczTUFCqTC1WUtAEAD05BcM%3D" target="_blank">Run Example</a>

```java
isBoolean( "yes" )

```

Result: true

### No

`No` is considered a boolean that is `false`

<a href="https://try.boxlang.io/?code=eJzLLHbKz89JTczTUFDKy1dS0AQANnkFTw%3D%3D" target="_blank">Run Example</a>

```java
isBoolean( "no" )

```

Result: true

### Maybe

`Maybe` is not considered a boolean

<a href="https://try.boxlang.io/?code=eJzLLHbKz89JTczTUFDKTaxMSlVS0AQASw8GgA%3D%3D" target="_blank">Run Example</a>

```java
isBoolean( "maybe" )

```

Result: false

### True

`true` is a boolean

<a href="https://try.boxlang.io/?code=eJzLLHbKz89JTczTUCgpKk1V0AQAOVoF7g%3D%3D" target="_blank">Run Example</a>

```java
isBoolean( true )

```

Result: true

### False

`false` is a boolean

<a href="https://try.boxlang.io/?code=eJzLLHbKz89JTczTUEhLzClOVdAEAD9xBjk%3D" target="_blank">Run Example</a>

```java
isBoolean( false )

```

Result: true

### Zero

`0` is considered a boolean that is `false`

<a href="https://try.boxlang.io/?code=eJzLLHbKz89JTczTUDBQ0AQAJkYEXg%3D%3D" target="_blank">Run Example</a>

```java
isBoolean( 0 )

```

Result: true

### Non Zero Integer

`23` is considered a boolean that is `true`

<a href="https://try.boxlang.io/?code=eJzLLHbKz89JTczTUDAyVtAEACr8BJM%3D" target="_blank">Run Example</a>

```java
isBoolean( 23 )

```

Result: true

### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrL8osSXUpzS3QUPAsdsrPz0lNzNNQKCkqTVXQVNDkKscqnZaYU4xP3gCPnK6hgZ4JHnljPTM8skqVqcVK%2BOTz8vFKgzyGVwHYa3hVGOCVBXsPrwqgB8HyAEDmc%2FE%3D" target="_blank">Run Example</a>

```java
writeDump( IsBoolean( true ) )
writeDump( IsBoolean( false ) )
writeDump( IsBoolean( 0 ) )
writeDump( IsBoolean( -10.4 ) )
writeDump( IsBoolean( 3.6 ) )
writeDump( IsBoolean( "yes" ) )
writeDump( IsBoolean( "no" ) )
writeDump( IsBoolean( "true" ) )
writeDump( IsBoolean( "false" ) )
writeDump( IsBoolean( "0" ) )
writeDump( IsBoolean( "-10.4" ) )
writeDump( IsBoolean( "3.6" ) )

```


<a href="https://try.boxlang.io/?code=eJwrL8osSXUpzS3QUPAsdsrPz0lNzNNQ8Msv19BUAEKucqzy1bV4JKNj8UgGlqYWVfqllmsoKCkp4LMCIo1LMlGhuKQoMy8drAgAKog9Vw%3D%3D" target="_blank">Run Example</a>

```java
writeDump( IsBoolean( Now() ) )
writeDump( IsBoolean( {} ) )
writeDump( IsBoolean( [] ) )
writeDump( IsBoolean( QueryNew( "" ) ) )
writeDump( IsBoolean( "" ) )
writeDump( IsBoolean( "a string" ) )

```


