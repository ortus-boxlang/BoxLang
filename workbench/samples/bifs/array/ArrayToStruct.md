### Convert an array to a struct using arrayToStruct()



<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQ8Ar29wtOLcpMzMmsStVQSCwqSqwMyQ8uKSpNBspGKyglKukoKCUpKcQqaIIgAKW1E0g%3D" target="_blank">Run Example</a>

```java
writeOutput( JSONSerialize( arrayToStruct( [ "a", "b" ] ) ) )

```

Result: {"1":"a","2":"b"}

### Using the member function

The struct keys are the one-based array positions.

```java
letters = [ "x", "y" ]
s = letters.toStruct()
writeOutput( s[ "1" ] & s[ "2" ] )
```

Result: xy

### Additional Examples


```java
arr = [ 
	"a",
	"b",
	"c",
	"d",
	"e",
	"f",
	"g"
]
dump( arrayToStruct( arr ) )
// member function
dump( arr.toStruct() )

```


<a href="https://try.boxlang.io/?code=eJxLLCpSsFWIVuDirObi5PRz9HVVsFJQSlTi4qzVQRVLwiKWjEUsBYtYKkiMK5YrpTS3QEMhsagosTIkP7ikqDS5BMxV0FTQ5NLXV8hNzU1KLVJIK81LLsnMz0Oo1yuBKQeqBACh%2FyxS" target="_blank">Run Example</a>

```java
arr = [ 
	{
		NAME : "a"
	},
	{
		NAME : "b"
	},
	{
		NAME : "c"
	},
	{
		NAME : "d"
	},
	{
		NAME : "e"
	}
]
dump( arrayToStruct( arr ) )
// member function
dump( arr.toStruct() )

```


