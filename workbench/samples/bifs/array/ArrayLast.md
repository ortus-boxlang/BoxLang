### Show the last element of an array

Uses the arrayLast function to retrieve the last element of an array

<a href="https://try.boxlang.io/?code=eJwrzs9NdSwqSqxUsFWIVuDiVErLLCouUdIBsopTk%2FPzUsDMkozMohQlrlhrrpzE4hL%2FvFSg6kSQLh8gV0OhGG6IpjVXeVFmSap%2FaUlBKVAGphwoDgDNgiFr" target="_blank">Run Example</a>

```java
someArray = [ 
	"first",
	"second",
	"third"
];
lastOne = arrayLast( someArray );
writeOutput( lastOne );

```

Result: "third"

### Additional Examples

<a href="https://try.boxlang.io/?code=eJzLS8xNLVawVYhW4OJU8k0sSi4tVtIBMoMTixIzwCyv%2FOLUtMy8VCWuWGuulNLcAg2FPJAmvZzE4hINTQVNay4FfX2F%2FNKSgtKSYgV1mHp1LgC7%2BBrj" target="_blank">Run Example</a>

```java
names = [ 
	"Marcus",
	"Sarah",
	"Josefine"
];
dump( names.last() );
 // outputs 'Josefine'

```



```java
names = array( "Marcus", "Sarah", "Josefine" );
dump( arrayLast( names ) );
 // outputs 'Josefine'

```


