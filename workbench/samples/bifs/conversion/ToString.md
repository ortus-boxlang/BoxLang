### Base64 value to binary to string



<a href="https://try.boxlang.io/?code=eJxLSixONTMJS8wpTVWwVSjJdwLzNRSUikuKMvPSwRJKCprWXEmZeYlFlUgKwXwNhSQkA4DKkLSBlQWD%2BUBlSNqBysqLMktS%2FUtLCkpLNBSQ9QDlAEx9Mf4%3D" target="_blank">Run Example</a>

```java
base64Value = toBase64( "stringValue" );
binaryValue = toBinary( base64Value );
stringValue = toString( binaryValue );
writeOutput( stringValue );

```

Result: Expected Result: stringValue

### Structure to String



<a href="https://try.boxlang.io/?code=eJwrVrBVqFbg4lRKVFKwUlAyVNIBspPAbCMlrlprrvKizJJU%2F9KSgtISDYWS%2FOCSosy8dA2FYgVNBU1rLgDIOA%2FE" target="_blank">Run Example</a>

```java
s = { 
	"a" : "1",
	"b" : "2"
};
writeOutput( toString( s ) );

```

Result: {a={1},b={2}}

### Member syntax


<a href="https://try.boxlang.io/?code=eJzLK81VsFUwMbLmKi%2FKLEn1Ly0pKC3RUMgrzdUryQ8uKcrMS9fQVNC05gIAHVcNNA%3D%3D" target="_blank">Run Example</a>

```java
num = 42;
writeOutput( num.toString() );

```

Result: 42

### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrL8osSXUpzS3QUCjJDy4pysxL11AwMVXQVNC05kopUbBVyMsv1wCyy7EpBCoAKwQAIS0VhQ%3D%3D" target="_blank">Run Example</a>

```java
writeDump( toString( 45 ) );
dt = now();
writeDump( toString( dt ) );

```


<a href="https://try.boxlang.io/?code=eJzLK81NSi1SsFUoSsxLAeL0VA0FQx0FQwMFTWuu8qLMklSX0twCDYU8sDq9kvzgkqLMvHQNTZB8SglQY15%2BuQaq2pQSNHUAessf7Q%3D%3D" target="_blank">Run Example</a>

```java
number = randrange( 1, 10 );
writeDump( number.toString() );
dt = now();
writeDump( dt.toString() );

```


