### Basic usage (case-sensitive)

Basic usage. Optional arguments left as defaults.

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQyMksLglKzc0vS3UpLcjJTE4sSS3WUFDKz0vVKSnP1ynJKEpN1UnLLy3SScssS9UBiYMZYAklBU0FTQC15Byt" target="_blank">Run Example</a>

```java
writeOutput( listRemoveDuplicates( "one,two,three,four,five,one,five,three" ) )

```

Result: one,two,three,four,five

### Optional arguments usage (ignore case = true)

Optional arguments being set. Ignore case set to true

<a href="https://try.boxlang.io/?code=eJwNxEEKgCAQBdCrDK4K%2FjESWiVE0DriS0Jl2Ixev97itZKUwfQx7eRMr868cuVgz5n2Tfl24vJNaMvQo5CI2QpiqkSYPJY1YBln7x3E4U%2BLUXrpP8NHHcU%3D" target="_blank">Run Example</a>

```java
writeOutput( listRemoveDuplicates( "one,two,three,four,five,ONE,TWO,THREE", ",", true ) )

```

Result: one,two,three,four,five

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxtjTEKwzAMRfecQnhqwJALJJ06duoNSixig1wZW66vH8VDIJBNev%2Fpi4rAAmb9io1cC1rHmz22lZvdWIfiEVNHGpmh5SDIVVKVB5Bej1dkZp%2BfRuk0wQ8bUFCnBfHgaqKgPVggY%2BQ%2FuoH697cqn05ep3Lb3dEOn%2Fo%2BJg%3D%3D" target="_blank">Run Example</a>

```java
lst = "cat,mouse,dog,cat,cow,goat,sheep,cat,dog"
writeoutput( lst )
writeoutput( "<hr>" )
// new list with duplicates removed
lst = ListRemoveDuplicates( lst )
writeoutput( lst )

```

### Case-sensitive de-duplication

```java
writeOutput( listRemoveDuplicates( "a,b,a,A" ) )

```

Result: a,b,A

### Member function ignoring case

```java
writeOutput( "a,b,A,a".listRemoveDuplicates( ",", true ) )

```

Result: a,b
