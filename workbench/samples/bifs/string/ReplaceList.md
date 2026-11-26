### Replace a list of values ignoring case

```java
myString = "My test string"
mySubstring1 = "Test, String"
mySubString2 = "Replaced, Sentence"
writeOutput( replaceListNoCase( myString, mySubstring1, mySubString2 ) )

```

Result: My Replaced Sentence

### Replace each value with the value at the same position

```java
writeOutput( replaceListNoCase( "Hi USER!, Have a nice day.", "hi,user", "Welcome,buddy" ) )

```

Result: Welcome buddy!, Have a nice day.

### Case-sensitive replacement

```java
writeOutput( replaceList( "Hello hello", "Hello", "Bye" ) )

```

Result: Bye hello

### Member function

```java
str = "xxxAbCdefghijxxXabcDefghij"
writeOutput( str.replaceListNoCase( "a,b,c,d,e,f,g,h,i,j", "0,1,2,3,4,5,6,7,8,9" ) )

```

Result: xxx0123456789xxX0123456789
