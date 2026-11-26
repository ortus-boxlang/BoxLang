### Simple example

Searches through a structure by a given key and outputs the related value

<a href="https://try.boxlang.io/?code=eJxLzi%2FNKynKTC1WsFWoVuDiVAoNdlRSsFJQCk8szsjMSy%2FJz1Nw0XPWU9IByrmnFuUm5lWC5Z1Si3Iy88DCXokFiXlgwZD87Mx8Ja5arvKizJJU%2F9KSgtISDYXikqLS5BK3zLwUDYVkmH06CgjjNBU0AczkKY0%3D" target="_blank">Run Example</a>

```java
countries = { 
	"USA" : "Washington D.C.",
	"Germany" : "Berlin",
	"Japan" : "Tokio"
}
writeOutput( structFind( countries, "Germany" ) )

```

Result: Berlin

### Additional Examples

<a href="https://try.boxlang.io/?code=eJyVjzFPwzAQhefmVzx5aqSo3ZEyVEGgTKAGifnaXKgVx0ax04AQ%2Fx3bSQkdOjDevXvfvUdadqQscnwhWRVPr7iD6IwRWbJ6Lh%2FDZKRuw1jsXqLIZhTJd7LdojqZEaQUaKIk90P3voaiA6tc7BZBZDhTn88T0mB%2BkLrGkRyk%2FvU3flf4VY7K9cPRhZv1Rc0g%2FLmY3PF1z3ZQzsI0mJ3XCfaL%2Foe34CJPpHO6y%2FPILxu4E6PlT9SGLbRx4A9pXYaRfWwNyw6EmhvyPzxADbxBqb1NWn9g2asHRbqFdb3Ub5vYrtIk1c1%2BNqgiJLvdMhL%2B2XPiRvB13ylO%2BgOmH6Wb" target="_blank">Run Example</a>

```java
animals = { 
	COW : "moo",
	PIG : "oink",
	CAT : "meow"
}
// Show all animals
Dump( label="All animals", var=animals )
// Find cat in animals
findCat = StructFind( animals, "cat" )
// Show results of findCat
Dump( label="Results of StructFind(animals, ""cat"")", var=findCat )
// If the key does not exist, we can set a default value. In this case a blank string.
findSnail = StructFind( animals, "snail", "" )
// Show results of findSnail
Dump( label="Results of StructFind(animals, ""snail"", """")", var=findSnail )

```

Result: the dumps show the whole struct, then "meow" for cat, then an empty string for snail

### Missing key with and without a default

Without a default value a missing key throws; with one, the default is returned.

```java
countries = { "USA" : "Washington D.C." }
writeOutput( structFind( countries, "France", "unknown" ) & " " )
try {
    structFind( countries, "France" )
} catch ( any e ) {
    writeOutput( e.message )
}
```

Result: unknown The key [France] could not be found in the struct
