# Semicolons

In BoxLang, semicolons may used to separate statements in script syntax. This is similar to their use in many other programming languages like JavaScript, Java, or C#.  Statements on separate lines, however, do not require semicolons for separation.

A basic example using semicolons for termination on a single line:

```BoxLang
var x = 10;
var y = 20;
var sum = x + y;
```

In this example, each line is a separate statement, and each statement is terminated with a semicolon. 

Once again, semicolons are optional and may be omitted, especially when each statement is on a new line. If you want to put multiple statements on a single line, however, then semicolons become necessary to separate them:

```BoxLang
var x = 10; var y = 20; var sum = x + y;
```

In this case, without the semicolons, the above code would fail to compile and would be syntactically incorrect.