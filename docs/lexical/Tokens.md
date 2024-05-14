# Tokens

Tokens in BoxLang, include the following:

1. **Identifiers**: These are names used for variables, functions, and other user-defined items. They must start with a letter and can contain letters, numbers, and underscores. Dashes are not allowed within root-level variable names.

    Example: `myVariable`, `calculated_sum`

2. **Keywords**: These are [reserved words that have special meaning](Keywords.md) in BoxLang. They include control flow keywords like `if`, `else`, `for`, `while`, and many others.

    Example: `if`, `component`, `function`

3. **Operators**: These are symbols that perform operations on one or more operands. They include arithmetic operators (`+`, `-`, `*`, `/`), comparison operators (`==`, `!=`, `<`, `>`), logical operators (`&&`, `||`), and others.

    Example: `+`, `==`, `&&`

4. **Literals**: These are [fixed values](Literals.md) that can be numbers, strings, booleans, or null.

    Example: `123`, `"Hello, World!"`, `true`, `null`

5. **Punctuation**: These are symbols that separate different parts of the code. They include parentheses (`(`, `)`), brackets (`[`, `]`), braces (`{`, `}`), semicolons (`;`), commas (`,`), and others.  Some symbols may also function as literal indicators.

    Example: `;`, `{`, `}`

6. **Comments**: These are notes in the code that are ignored by the BoxLang interpreter. Single-line comments start with `//`, and multi-line comments are enclosed between `/*` and `*/`.

    Example: `// This is a comment`, `/* This is a multi-line comment */`
