### Simple Example

Trims each item in the list.

<a href="https://try.boxlang.io/?code=eJxLrUjMLchJ9cksLlGwVVDKSM3JyddRKM8vyklR0CnJyCzWUcgsVkjUUdBJhahU4iovyixJ9S8tKSgt0VDIAWr0LEnNDSnKzNVQgKoBCSpoKmgCAPOFIFo%3D" target="_blank">Run Example</a>

```java
exampleList = "hello, world ,this, is a, ,example"
writeOutput( listItemTrim( examplelist ) )

```

Result: hello,world,this,is a,,example

### Using as member function

Trims each item in the list.

<a href="https://try.boxlang.io/?code=eJxLrUjMLchJ9cksLlGwVVDKSM3JyddRKM8vyklR0CnJyCzWUcgsVkjUUdBJhahU4kpF0YLE08sBEp4lqbkhRZm5Gppc5UWZJan%2BpSUFpSUaMHUgJQqaAJVsKd8%3D" target="_blank">Run Example</a>

```java
exampleList = "hello, world ,this, is a, ,example"
exampleList = exampleList.listItemTrim()
writeOutput( examplelist )

```

Result: hello,world,this,is a,,example

### Keep empty items in the list

Trims each item in the list and keep empty items.

<a href="https://try.boxlang.io/?code=eJwty8EJgDAQRNG7VQw5GZgWLEAQvNiA4IILGwzJBi1fRa%2Bf9%2BVaUzaZtDoGhF3MDuI8im2g71oJrVgJkPLZ0J1FXebmuXkPe9bRJS1FU4%2FfvJEIDISXJoiIN5eOI24%3D" target="_blank">Run Example</a>

```java
exampleList = "hello, world ,this, is a,  ,,example"
writeOutput( listItemTrim( examplelist, ",", true ) )

```

Result: hello,world,this,is a,,,example

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxVzk0KwjAQBeB9TvHIKoXBnMCFoEKhruoFWhkkkD%2BSifb4tv4snM1bve%2BNtRhdyJ5xWqYt1bM44dQkNzHwrkovHK7FBQM9tupAGA4jQDSnxU%2FxTkTCVYg0OnTKWlw4zFxwbvEmLkVVpQwrhD009YT3EXx68M%2FYOP2ZPraQDb6V3d8DK%2F8CjKw35g%3D%3D" target="_blank">Run Example</a>

```java
// Simple Example
writeoutput( listItemTrim( "Susi , LAS  ,,boxlang,,,test,," ) )
// Member Function
strList = ",I,      , love,boxlang  ,,"
writeDump( strList.listItemTrim() )

```


