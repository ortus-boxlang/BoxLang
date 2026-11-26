### Simple example for listToArray function

Uses the listToArray() function to retrieve a list as an array

<a href="https://try.boxlang.io/?code=eJzLySwuUbBVUCpKTdFJL0pNzdPJL0rMS09V4kpPLXEsKkqsBMrmABWF5IN5GmCOgiZXcX5uqlewvx9QGkQFpxZlJuZkVqVqKMA1anKVF2WWpPqXlhSUlmgowHVoAgAqEyeb" target="_blank">Run Example</a>

```java
list = "red,green,orange"
getArray = listToArray( list )
someJSON = JSONSerialize( getArray )
writeOutput( someJSON )

```

Result: ["red", "green", "orange"]

### Example for listToArray function with delimiter

Uses the listToArray() function with a semicolon delimiter to retrieve a list as an array

<a href="https://try.boxlang.io?code=eJzLySwuUbBVUErKr8hJzEu3LsgosM5KLEu0Li7MUbLmSk8tcSwqSqwEKskBqgzJB%2FM0wBwdBSVrJQVNa67i%2FNxUr2B%2FP6AiEBWcWpSZmJNZlaqhANcOVFVelFmS6l9aUlBaoqEA16JpDQAl4isI" target="_blank">Run Example</a>

```java
list = "boxlang;php;java;sql"
getArray = listToArray( list, ";" )
someJSON = JSONSerialize( getArray )
writeOutput( someJSON )

```

Result: ["boxlang", "php", "java", "sql"]

### Example for listToArray function with includeEmptyFields

If includeEmptyFields is true, empty value add in array elements

<a href="https://try.boxlang.io?code=eJzLySwuUbBVUErKr8hJzEu3LsgosLbOSixLtC4uzFGy5kpPLXEsKkqsBKrJASoNyQfzNMAcHQUlayUdhZKi0lQFTWuu4vzcVK9gfz%2BgUhAVnFqUmZiTWZWqoQA3BKiqvCizJNW%2FtKSgtERDAa4FKAEAhEctWQ%3D%3D" target="_blank">Run Example</a>

```java
list = "boxlang;php;;java;sql"
getArray = listToArray( list, ";", true )
someJSON = JSONSerialize( getArray )
writeOutput( someJSON )

```

Result: ["boxlang", "php", " " , "java", "sql"]

### Example for listToArray function with multiCharacterDelimiter

Uses the listToArray() function to retrieve a list as an array with multiCharacterDelimiter

<a href="https://try.boxlang.io/?code=eJw9jDEOwjAMRfecwsqUSL4CQ1cGOpQLGMmUIENC4hSocnhCh05fT3rvSygKB7CX%2BBF6zphuCZtyUbzTQtjKS6yZWYec6dtF6f45buQ2QLDYLMKVpDCC5srgTYkPPk7jqRf%2FmTgHkrCyg%2F3Lm3cOymPVVNXBXvgfjHYxow%3D%3D" target="_blank">Run Example</a>

```java
list = "boxlang,php,|test,java,|sql"
getArray = listToArray( list, ",|", false, true )
someJSON = JSONSerialize( getArray )
writeOutput( someJSON )

```

Result: ["boxlang,php", "test,java", "sql"]

### Additional Examples

<a href="https://try.boxlang.io/?code=eJzLySwuUbBVUCpKTdFJL0pNzdPJL0rMS09V4kpPLXEsKkqsBMrmABWF5IN5GmCOgiZXcX5uqlewvx9QGkQFpxZlJuZkVqVqKMA1anKVF2WWpPqXlhSUlmgowHVoAgAqEyeb" target="_blank">Run Example</a>

```java
list = "red,green,orange"
getArray = listToArray( list )
someJSON = JSONSerialize( getArray )
writeOutput( someJSON )

```


<a href="https://try.boxlang.io/?code=eJzLySwuUbBVUErKr8hJzEu3LsgosM5KLEu0Li7MUeJKTy1xLCpKrASqyAEqDMkH8zTAHB0FJWslBU2u4vzcVK9gfz%2BgGhAVnFqUmZiTWZWqoQDXrclVXpRZkupfWlJQWqKhANehCQCNnCoc" target="_blank">Run Example</a>

```java
list = "boxlang;php;java;sql"
getArray = listToArray( list, ";" )
someJSON = JSONSerialize( getArray )
writeOutput( someJSON )

```


<a href="https://try.boxlang.io/?code=eJzLySwuUbBVUErKr8hJzEu3LsgosLbOSixLtC4uzFHiSk8tcSwqSqwEKskBqgzJB%2FM0wBwdBSVrJR2FkqLSVAVNruL83FSvYH8%2FoEoQFZxalJmYk1mVqqEAN0OTq7wosyTVv7SkoLREQwGuQxMAtjYsYw%3D%3D" target="_blank">Run Example</a>

```java
list = "boxlang;php;;java;sql"
getArray = listToArray( list, ";", true )
someJSON = JSONSerialize( getArray )
writeOutput( someJSON )

```


<a href="https://try.boxlang.io/?code=eJw9jDEOwjAMRfecwsqUSL4CQ1cGOpQLGMmUIENC4hSocnhCh05fT3rvSygKB7CX%2BBF6zphuCZtyUbzTQtjKS6yZWYec6dtF6f45buQ2QLDYLMKVpDCC5srgTYkPPk7jqRf%2FmTgHkrCyg%2F3Lm3cOymPVVNXBXvgfjHYxow%3D%3D" target="_blank">Run Example</a>

```java
list = "boxlang,php,|test,java,|sql"
getArray = listToArray( list, ",|", false, true )
someJSON = JSONSerialize( getArray )
writeOutput( someJSON )

```

### Convert a list to an array

```java
writeOutput( listToArray( "a,b,c" ).len() )

```

Result: 3

### Member function including empty fields

```java
writeOutput( "a,,b".listToArray( ",", true ).len() )

```

Result: 3 (without includeEmptyFields the result has 2 items)
