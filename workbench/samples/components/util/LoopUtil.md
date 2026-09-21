### Loop over a query with row range

Iterates through a subset of query rows using startRow and endRow.

```java
<bx:loop query="#users#" startRow="1" endRow="10" index="row">
    #row# - #users.name#<br>
</bx:loop>

```

### Loop over a query with maxRows

```java
<bx:loop query="#products#" maxRows="5" index="i">
    #i#: #products.name# - $#products.price#<br>
</bx:loop>

```

### Grouped query loop

Groups query rows by a column and iterates once per group change.

```java
<bx:loop query="#employees#" group="department" index="dept">
    <h2>#employees.department#</h2>
    <bx:loop query="#employees#" group="manager">
        <h3>Manager: #employees.manager#</h3>
        <bx:loop query="#employees#">
            <li>#employees.name#</li>
        </bx:loop>
    </bx:loop>
</bx:loop>

```

### Nested grouped loop with case-insensitive grouping

```java
<bx:loop query="#data#" group="category" groupCaseSensitive="false" index="cat">
    <h2>#data.category#</h2>
    <bx:loop query="#data#">
        <li>#data.item#</li>
    </bx:loop>
</bx:loop>

```
