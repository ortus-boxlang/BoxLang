# ⚡︎ Project Jericho - BoxLang

> A next generation multi-runtime dynamic programming language for the JVM. InvokeDynamic is our first name!

```
██████   ██████  ██   ██ ██       █████  ███    ██  ██████
██   ██ ██    ██  ██ ██  ██      ██   ██ ████   ██ ██
██████  ██    ██   ███   ██      ███████ ██ ██  ██ ██   ███
██   ██ ██    ██  ██ ██  ██      ██   ██ ██  ██ ██ ██    ██
██████   ██████  ██   ██ ███████ ██   ██ ██   ████  ██████
```

----

Because of God's grace, this project exists. If you don't like this, then don't read it, it's not for you.

>"Therefore being justified by faith, we have peace with God through our Lord Jesus Christ:
By whom also we have access by faith into this grace wherein we stand, and rejoice in hope of the glory of God.
And not only so, but we glory in tribulations also: knowing that tribulation worketh patience;
And patience, experience; and experience, hope:
And hope maketh not ashamed; because the love of God is shed abroad in our hearts by the
Holy Ghost which is given unto us. ." Romans 5:5

----

**BoxLang** is a modern dynamic JVM language that can be deployed on multiple runtimes: operating system (Windows/Mac/*nix/Embedded), web server, lambda, iOS, android, web assembly, and more. **BoxLang** combines many features from different programming languages, including Java, ColdFusion, Python, Ruby, Go, and PHP, to provide developers with a modern and expressive syntax.

**BoxLang** has been designed to be a highly adaptable and dynamic language to take advantage of all the modern features of the JVM and was designed with several goals in mind:

* Be a rapid application development (RAD) scripting language and middleware.
* Unstagnate the dynamic language ecosystem in Java.
* Be dynamic, modular, lightweight, and fast.
* Be 100% interoperable with Java.
* Be modern, functional, and fluent (Think mixing CFML, Node, Kotlin, Java, and Clojure)
* Be able to support multiple runtimes and deployment targets:
  * Native OS Binaries (CLI Tooling, compilers, etc.)
  * MiniServer
  * Servlet Containers - CommandBox/Tomcat/Jetty/JBoss
  * JSR223 Scripting Engines
  * AWS Lambda
  * Microsoft Azure Functions (Coming Soon)
  * Android/iOS Devices (Coming Soon)
  * Web assembly (Coming Soon)
* Compile down to Java ByteCode
* Allow backward compatibility with the existing ColdFusion/CFML language.
* Great IDE, Debugger and Tooling: https://boxlang.ortusbooks.com/getting-started/ide-tooling
* Scripting (Any OS and Shebang) and REPL capabilities

You can find our docs here: https://boxlang.ortusbooks.com/

## License

Apache License, Version 2.0.

## Open-Source & Professional Support

This project is a professional open source project and is available as FREE and open source to use.  Ortus Solutions, Corp provides commercial support, training and commercial subscriptions which include the following:

* Professional Support and Priority Queuing
* Remote Assistance and Troubleshooting
* New Feature Requests and Custom Development
* Custom SLAs
* Application Modernization and Migration Services
* Performance Audits
* Enterprise Modules and Integrations
* Much More

Visit us at [BoxLang.io Plans](https://boxlang.io/plans) for more information.

## JDK Targets

* JDK 21 LTS is our compiled code JDK Baseline

## VSCode Snippets

You will find a `.vscode` folder in the root. This contains our custom shortcuts, mappings, snippets, builds and more.

### Snippets

| Snippet 				| Description 							|
|-----------------------|---------------------------------------|
| `header` 				| Adds a license header 				|
| `testclass` 			| Creates a Junit5 test class 			|

## Basic Gradle Tasks

> The output folder used for the builds are `build/**` which can be found in each of the multi-project folders.

| Task                | Description                                                                                                        	|
|---------------------|---------------------------------------------------------------------------------------------------------------------|
| `assemble`          | Build all tasks: `build/libs, build/scripts, build/distributions`													|
| `build`             | The default lifecycle task that triggers the build process, including tasks like `clean`, `assemble`, and others. 	|
| `clean`             | Deletes the `build` folders. It helps ensure a clean build by removing any previously generated artifacts.			|
| `compileJava`       | Compiles Java source code files located in the `src/main/java` directory											|
| `compileTestJava`   | Compiles Java test source code files located in the `src/test/java` directory										|
| `getDependencies`   | Downloads all the dependencies defined in `build.gradle` and puts them in the `build/dependencies` folder 			|
| `dependencyUpdates` | Checks for updated versions of all dependencies															 			|
| `jar`               | Packages your project's compiled classes and resources into a JAR file `build/libs` folder							|
| `spotlessApply`     | Runs the Spotless plugin to format the code																			|
| `spotlessCheck`     | Runs the Spotless plugin to check the formatting of the code														|
| `tasks`			  | Show all the available tasks in the project																			|
| `test`              | Executes the unit tests in your project and produces the reports in the `build/reports/tests` folder				|

### Example Testing Commands

* Run a single test: `gradle test --tests FooTest`
* Run several tests and then watch them for changes `gradle test --tests FooTest --tests BarTest --continuous`
* Run Tests with a pattern match and watch for changes `gradle test --tests "Array*" --continuous`


## Dependencies

Here is a listing of all of our core dependencies.  If you add one, make sure you document it here.

### Runtime

| Dependency | Version | License | Description |
|------------|---------|---------|-------------|
| [apache-commons-lang3](https://commons.apache.org/proper/commons-lang/) | 3.12.0 | Apache2 | Used for many utilities, class helpers and more |
| [boxlang-compiler](https://github.com/ortus-solutions-private/boxlang-compiler) | 1.0.0 | Apache2 | The BoxLang Parser, Compiler, and ByteCode Generator |
| [caffeine](https://mvnrepository.com/artifact/com.github.ben-manes.caffeine/caffeine) | 3.1.8| Apache2 | Caching engine  |
| [slf4j-api](https://mvnrepository.com/artifact/org.slf4j/slf4j-api) | 2.0.11 | MIT | API for SLF4J (The Simple Logging Facade for Java)  |
| [slf4j-jdk14](https://mvnrepository.com/artifact/org.slf4j/slf4j-jdk14) | 2.0.11 | MIT | SLF4J JDK14 Provider |

### Compiler

| Dependency | Version | License | Description |
|------------|---------|---------|-------------|
| [antlr4-runtime](https://mvnrepository.com/artifact/org.antlr/antlr4-runtime) | 4.12.0 | BSD 3-clause | ANTLR parser |
| [commons-cli](https://mvnrepository.com/artifact/commons-cli/commons-cli) | 1.5.0 | Apache 2 | Apache Commons CLI provides a simple API for presenting, processing and validating a Command Line Interface. |
| [commons-io](https://mvnrepository.com/artifact/commons-io/commons-io) | 2.13.0 | Apache 2 | The Apache Commons IO library contains utility classes, stream implementations, file filters, file comparators, endian transformation classes, and much more. |
| [commons-text](https://mvnrepository.com/artifact/org.apache.commons/commons-text) | 1.10.0 | Apache 2 | The Commons Text library provides additions to the standard JDK text handling. It includes algorithms for string similarity and for calculating the distance between strings. |
| [javaparser-symbol-solver-core](https://github.com/javaparser/javaparser) | 3.25.4 | Apache 2 | Java 1-17 Parser and Abstract Syntax Tree for Java with advanced analysis functionalities. |
| [kolasu-core](https://github.com/Strumenta/kolasu) | 1.5.24 | Apache 2 | Kotlin Language Support – AST Library |
| [slf4j-api](https://mvnrepository.com/artifact/org.slf4j/slf4j-api) | 2.0.11 | MIT | API for SLF4J (The Simple Logging Facade for Java)  |
| [slf4j-jdk14](https://mvnrepository.com/artifact/org.slf4j/slf4j-jdk14) | 2.0.11 | MIT | SLF4J JDK14 Provider |

## Contributing

* All code should be formatted using either our Java Formatter or the CFFormatter.
* All code should have a license/copyright header based on [CodeHeader.txt](workbench/CodeHeader.txt)

Made with ♥️ in USA 🇺🇸, El Salvador 🇸🇻 and Spain 🇪🇸
