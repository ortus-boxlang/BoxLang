### Test to see if a struct is empty



<a href="https://try.boxlang.io/?code=eJzLrQwuKSpNLlGwVaiu5SovyixJ9S8tKSgt0VAoBkt4FrvmFpRUaijkwlRqKmgCAC%2FLE6Q%3D" target="_blank">Run Example</a>

```java
myStruct = {}
writeOutput( structIsEmpty( myStruct ) )

```

Result: true

### Test to see if a struct contains something



<a href="https://try.boxlang.io/?code=eJzLrQwuKSpNLgnPLMkIycjMSy9WsFWoVuDiVMrPS1VSsFJQSsvPV9IB8kvK88H8pMQiJa5arvKizJJU%2F9KSgtISDYVisBmexa65BSWVGgq5mIZqKmgCAEtJI0s%3D" target="_blank">Run Example</a>

```java
myStructWithThings = { 
	"one" : "foo",
	"two" : "bar"
}
writeOutput( structIsEmpty( myStructWithThings ) )

```

Result: false

### Using Member Function



<a href="https://try.boxlang.io/?code=eJzLrQwuKSpNLlGwVaiu5SovyixJ9S8tKSgt0VDIhUrpeRa75haUVGpoKmgCAKc1EO0%3D" target="_blank">Run Example</a>

```java
myStruct = {}
writeOutput( myStruct.IsEmpty() )

```

Result: true

### Additional Examples

<a href="https://try.boxlang.io/?code=eJzT11fwy89TSM0tKKlUKC4pKk0u4UrMy8xNzClWsFWo5uJ09g9XsFJQys3PV9Lh4gzwdAfx8jPzspW4arn09RVckXWmJRblgrSBZYLBYp7FYBUaUEM1uVKTM%2FI1FJRsCuwcofZANCtkFkOcATRfQQ1NtwLMTZpAKSUb%2FQI7JQVNTEtA9iPb4AZyDzHGgx2ObDYAOi9Xqw%3D%3D" target="_blank">Run Example</a>

```java
// Non empty struct
animals = {
	COW : "moo",
	PIG : "oink"
}
// Empty struct
farm = {}
// StructIsEmpty(animals)
echo( "<p>Animals struct is empty: " & StructIsEmpty( animals ) & "</p>" )
// StructIsEmpty(farm)
echo( "<p>Farm struct is empty: " & StructIsEmpty( farm ) & "</p>" )

```


