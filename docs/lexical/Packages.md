# Packages and Imports

In BoxLang, a package is a collection of related components, functions, and variables. Packages help in organizing code into logical units, making it easier to maintain and understand. They also provide a namespace that prevents naming conflicts between different parts of a program.

By default, the package space of a class is the directory path, in dot-notation, from the webroot.  For example, the default package of a class located at `models/foo/Bar.bx` would be `models.foo`.  This can, however, be defined in the class, separately from the path.

To define a package in BoxLang, you use the `class` keyword and define the package in the attributes of the class. For example:

```BoxLang
class package="com.example" {
    // package code here
}
```

This code defines a package named `com.example`. All components and functions defined within this package will be part of the `com.example` namespace.

Importing in BoxLang is done using the `import` keyword. The `import` statement is used to bring in a package or a specific component from a package into the current scope. For example:

```BoxLang
import "com.example.*";
```

This statement imports all components from the `com.example` package. If you want to import a specific component, you can do so like this:

```BoxLang
import "com.example.MyComponent";
```

This statement imports only the `MyComponent` component from the `com.example` package.

It's important to note that BoxLang is a dynamic language, and as such, it resolves package and component names at runtime, not at compile time. This means that you can use dynamic strings in your `import` statements, like so:

```BoxLang
import "com.example.#componentName#";
```

In this statement, `#componentName#` is a variable that contains the name of the component to import. This feature makes BoxLang very flexible, but it can also lead to runtime errors if the component or package doesn't exist.

Packages and imports in BoxLang provide a way to organize code into logical units and to reuse code across different parts of a program.