### isCustomFunction Example

Here we've example to check the given variable is user defined function or not.

<a href="https://try.boxlang.io/?code=eJxLK81LLsnMz1PIrUwDMjU0Faq5OItSS0qL8hQMuWq5yosyS1JdSnMLNBQyi51Li0vyc92gWjSgehQ0FTQBiP4YrA%3D%3D" target="_blank">Run Example</a>

```java

function myfunc() {
	return 1
}
writeDump( isCustomFunction( myfunc ) )

```

Result: YES

### isCustomFunction Example

Here we've example to check the given variable is user defined function or not.

<a href="https://try.boxlang.io/?code=eJzLrUwrzUtWsFVQMlLiKi%2FKLEl1Kc0t0FDILHYuLS7Jz3UDypZk5udpKORCVGoqaAIA1ZQRog%3D%3D" target="_blank">Run Example</a>

```java
myfunc = "2"
writeDump( isCustomFunction( myfunc ) )

```

Result: NO

### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrL8osSXUpzS3QUMgsdi4tLsnPdSvNSy7JzM%2FTUChKTcwJdXFT0FTQ5CrHp7CiogKsqCS1uAQoqmAL08oVAWQb4tcM00TQlgiwEq40qADMDg1NhWouzqLUktKiPKBdtUgqgO7SUHDMq1Qoy89MUQCpqwUA%2FYlQkQ%3D%3D" target="_blank">Run Example</a>

```java
writeDump( isCustomFunction( realUDF ) )
writeDump( isCustomFunction( xxx ) )
testFun = realUDF
X = 1
writeDump( isCustomFunction( testFun ) )
writeDump( isCustomFunction( X ) )

function realUDF() {
	return 1
}

function xxx( Any void ) {
}

```


