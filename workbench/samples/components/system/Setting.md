### Script Syntax

Enable cfoutput only

<a href="https://try.boxlang.io/?code=eJxLqrAqTi0pycxLV0jNS0zKSc0vLSkoLcnPy6m0LSkqTbXmAgDtxg1P" target="_blank">Run Example</a>

```java
bx:setting enableoutputonly=true;

```

Result: 

### Script Syntax

Disables debug output

<a href="https://try.boxlang.io/?code=eJxLqrAqTi0pycxLVyjOyC9PSU0qTc8vLSkoLbFNS8wpTrXmAgDuSA05" target="_blank">Run Example</a>

```java
bx:setting showdebugoutput=false;

```

Result: 

### Script Syntax

Set the request timeout to 30 seconds

<a href="https://try.boxlang.io/?code=eJxLqrAqTi0pycxLVyhKLSxNLQayc1PzS0tsjQ2suQC4owso" target="_blank">Run Example</a>

```java
bx:setting requesttimeout=30;

```

Result: 

### Tag Syntax

Enable cfoutput only


```java
<bx:setting enableoutputonly="true">
Foo <bx:output>bar</bx:output>
```

Result: bar

### Tag Syntax

Disables debug output


```java
<bx:setting showdebugoutput="false">
```

Result: 

### Tag Syntax

Set the request timeout to 30 seconds


```java
<bx:setting requesttimeout="30">
```

Result: 

