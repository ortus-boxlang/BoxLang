### Tag Syntax




```java
<bx:set MyNewArray = arrayNew( 1 ) > 
<!--- ArrayToList does not function properly if the Array has not been initialized with arraySet. ---> 
<bx:set temp = <!--- Transpiler workaround for BIF return type --->(( <bx:argument>, <bx:argument>, <bx:argument>, <bx:argument> ) => <bx:set arraySet( arg1, arg2, arg3, arg4 ) ><bx:return true>)( MyNewArray, 1, 6, "Initial Value" ) > 
<bx:output>#ArrayToList( myNewArray, ", " )#</bx:output>
```

Result: Initial Value, Initial Value, Initial Value, Initial Value, Initial Value, Initial Value

### Additional Examples

<a href="https://try.boxlang.io/?code=eJzLSy13LCpKrFSwVUgE0X6p5RoKhgqa1lxgbnBqiYZCHlSNjoKhjoKJjoKSY06OQmaxQnlqTo4SSGlKaW4BQhlIRF9fITc1Nym1SCGtNC%2B5JDM%2FjwsmrVcMMhNokhHQpJzUkmKF5IzEvPRUhcwSHIYBAK6VMzo%3D" target="_blank">Run Example</a>

```java
newArray = arrayNew( 1 );
arraySet( newArray, 1, 4, "All is well" );
dump( newArray );
// member function
newArray.set( 1, 2, "lets change it" );
dump( newArray );

```


