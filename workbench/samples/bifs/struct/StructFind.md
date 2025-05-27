### Simple example

Searches through a structure by a given key and outputs the related value

<a href="https://try.boxlang.io/?code=eJxLzi%2FNKynKTC1WsFWoVuDiVAoNdlRSsFJQCk8szsjMSy%2FJz1Nw0XPWU9IByrmnFuUm5lWC5Z1Si3Iy88DCXokFiXlgwZD87Mx8Ja5aa67yosySVP%2FSkoLSEg2F4pKi0uQSt8y8FA2FZJiFOgoI8zQVNK25AEUEKg0%3D" target="_blank">Run Example</a>

```java
countries = { 
	"USA" : "Washington D.C.",
	"Germany" : "Berlin",
	"Japan" : "Tokio"
};
writeOutput( structFind( countries, "Germany" ) );

```

Result: Berlin

### Additional Examples

<a href="https://try.boxlang.io/?code=eJyVj8FOwzAQRM%2FNV4x8aqWovYNyqIKKcmpFkDhvmw214tgodggI8e%2FYTkroASSOu7P7Zoa0bElZZPhAssj3T7iBaI0RabI4FPdhMlI3Ycy3j1FkM4jk8zbZbFCezQBSCjRikru%2BfVlC0ZFVJrazIFK8UpdNE1bxeyd1hRM5SP0NqP0u96sMpev6kws3y4uaQvhzMb1H845tr5yFqTG9Xmd4mPUfwJkXgWI15bu4jwZFDXdmNPyOyrCFNg78Jq1LMbAPrmHZgVBxTd7EE1TPaxTav0nrDyx79ahIN7Cuk%2Fp5HfuVmqT6taENqgjR%2FugZEf9sOoIj%2BbrxmMebfQGdfacH" target="_blank">Run Example</a>

```java
animals = { 
	COW : "moo",
	PIG : "oink",
	CAT : "meow"
};
// Show all animals
Dump( label="All animals", var=animals );
// Find cat in animals
findCat = StructFind( animals, "cat" );
// Show results of findCat
Dump( label="Results of StructFind(animals, ""cat"")", var=findCat );
// If the key does not exist, we can set a default value. In this case a blank string.
findSnail = StructFind( animals, "snail", "" );
// Show results of findSnail
Dump( label="Results of StructFind(animals, ""snail"", """")", var=findSnail );

```


