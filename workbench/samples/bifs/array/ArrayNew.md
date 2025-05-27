### Create the One dimensional array

Uses the arrayNew function to create the new array

<a href="https://try.boxlang.io/?code=eJx1jkELgkAUhM%2Furxg8KQhieRMDOwR1qIP9gYVesbSt8txlsei%2Ft4rRqcvM4c18bwz5hlmOqCEnP5JPUCCtxNA9aDmJPMeZpRl6pYnhO75L7py54NoxtvsdmKxjAzv2JJIkoG5FNulq1vWsJVLUG7xENH9qyf4PViL6ItlRJd5pArNMzRAqZYa40RpqgCet46niWVk6Odu7QD60p2NLrKRWT%2FqVw4aQ%2FACv%2B0zp" target="_blank">Run Example</a>

```java
newArray = arrayNew( 1 );
someArray =
// Transpiler workaround for BIF return type
(( arg1, arg2, arg3, arg4 ) => {
	arraySet( arg1, arg2, arg3, arg4 );
	return true;
})( newArray, 1, 4, "All is well" );
writeOutput( JSONSerialize( newArray ) );

```

Result: ["All is well", "All is well", "All is well", "All is well"]

### Create the Two dimensional array

Uses the arrayNew function to create the new array

<a href="https://try.boxlang.io/?code=eJzLSy13LCpKrFSwVUgE0X6p5RoKRgqa1lx5UJloBUOFWDABVKPklllUXKJQlphTmqpElhoj%2FGqMkMwJTk3Oz0vBpcgIq6LyosySVP%2FSkoLSEg0Fr2B%2Fv%2BDUoszEnMyqVA0FmAEKmiD%2FAQDN5k2Q" target="_blank">Run Example</a>

```java
newArray = arrayNew( 2 );
newArray[ 1 ][ 1 ] = "First value";
newArray[ 1 ][ 1 ] = "First value";
newArray[ 1 ][ 2 ] = "First value";
newArray[ 2 ][ 1 ] = "Second value";
newArray[ 2 ][ 2 ] = "Second value";
writeOutput( JSONSerialize( newArray ) );

```

Result: [["First value", "First value"],["Second value", "Second value"]]

### Create unsynchronized array

CF2016+ Uses the arrayNew function to create the new unsynchronized array

<a href="https://try.boxlang.io/?code=eJzLSy13LCpKrFSwVUgE0X6p5RoKhjoKaYk5xakKmtZceVAFeokFBal5KRoKSvl5qUogmfKizJJU%2F9KSgtISDQWvYH%2B%2F4NSizMSczKpUDQWYLgVNkEoAs4AhBw%3D%3D" target="_blank">Run Example</a>

```java
newArray = arrayNew( 1, false );
newArray.append( "one" );
writeOutput( JSONSerialize( newArray ) );

```

Result: ["one"]

### Create an array using implicit notation

CF8+ Instead of using arrayNew you can also create an array using square brackets.

<a href="https://try.boxlang.io/?code=eJzLSy13LCpKrFSwVYhW4OJUys9LVdIB0iXl%2BUpcsdZc5UWZJan%2BpSUFpSUaCl7B%2Fn7BqUWZiTmZVakaCnkwvZoKmtZcAEruFrI%3D" target="_blank">Run Example</a>

```java
newArray = [ 
	"one",
	"two"
];
writeOutput( JSONSerialize( newArray ) );

```

Result: ["one", "two"]

### Create an array with data type

CF2018+ When using data types on array creation, items are converted if possible, otherwise an error is thrown.


```java
typedArray = arrayNew[ "boolean" ]( 1 );
typedArray[ 1 ] = true;
typedArray[ 2 ] = "true";
typedArray[ 3 ] = 1;
typedArray[ 4 ] = "1";
typedArray[ 5 ] = "yes";
typelessArray = arrayNew( 1 );
typelessArray[ 1 ] = true;
typelessArray[ 2 ] = "true";
typelessArray[ 3 ] = 1;
typelessArray[ 4 ] = "1";
typelessArray[ 5 ] = "yes";
writeOutput( JSONSerialize( [
	typedArray,
	typelessArray
] ) );

```

Result: [[true,true,true,true,null,true],[true,"true",1,"1",null,"yes"]]

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxLVLBVSCwqSqz0Sy3XUDBU0LTm0tdX8MwtyMlMziyBSCnk5ZcklmTm53El6iUWFKTmpWgoRMdClZZnlmQolCXmlKYWI0tzcSolKukAySQQaQzEJkAcHQskqrk4OZ39fQN8XCMUrBRKikpTuThrgeKFpalFEGcoZabopCSWpCopaHKB7UkpzS3QUEgEMQGlfTAO" target="_blank">Run Example</a>

```java
a = arrayNew( 1 );
// Implicit array notation
a.append( [] );
// with values
a.append( [
	"a",
	"b",
	3,
	4,
	[],
	{
		COMPLEX : true
	},
	queryNew( "id,date" )
] );
dump( a );

```


