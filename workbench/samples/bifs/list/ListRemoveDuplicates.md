### Basic usage (case-sensitive)

Basic usage. Optional arguments left as defaults.

<a href="https://try.boxlang.io/?code=eJzLySwuCUrNzS9LdSktyMlMTixJLdZQUMrPS9UpKc%2FXKckoSk3VScsvLdJJyyxL1QGJgxlgCSUFTWsuAESpF6U%3D" target="_blank">Run Example</a>

```java
listRemoveDuplicates( "one,two,three,four,five,one,five,three" );

```

Result: one,two,three,four,five

### Optional arguments usage (ignore case = true)

Optional arguments being set. Ignore case set to true

<a href="https://try.boxlang.io/?code=eJzLySwuCUrNzS9LdSktyMlMTixJLdZQUMrPS9UpKc%2FXKckoSk3VScsvLdJJyyxL1fH3c9UJCffXCfEIcnVV0lFQ0gESJUWlqQqa1lwAH%2BQYvQ%3D%3D" target="_blank">Run Example</a>

```java
listRemoveDuplicates( "one,two,three,four,five,ONE,TWO,THREE", ",", true );

```

Result: one,two,three,four,five

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxtjjEKwzAMRfecQnhqwJADJO3UsVNvUGIRG5TI2HJ9%2FSgmdAjdpPefPqIscAczf8SuXDJax4s9tpmrXViH7BFjQxqZsaspCHKRWOQGpOf9hZnJp4c58DDAhhUoqFWDeHAlUtAqzJBw5S%2B6jtoDL1XejTx%2Fyv%2F2k%2B3vsj9X" target="_blank">Run Example</a>

```java
lst = "cat,mouse,dog,cat,cow,goat,sheep,cat,dog";
writeoutput( lst );
writeoutput( "<hr>" );
// new list with duplicates removed
lst = ListRemoveDuplicates( lst );
writeoutput( lst );

```


