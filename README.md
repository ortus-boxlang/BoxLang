# Project Jericho

----

Because of God's grace, this project exists. If you don't like this, then don't read it, it's not for you.

>"Therefore being justified by faith, we have peace with God through our Lord Jesus Christ:
By whom also we have access by faith into this grace wherein we stand, and rejoice in hope of the glory of God.
And not only so, but we glory in tribulations also: knowing that tribulation worketh patience;
And patience, experience; and experience, hope:
And hope maketh not ashamed; because the love of God is shed abroad in our hearts by the
Holy Ghost which is given unto us. ." Romans 5:5

----

## Projects

The project is split into multiple sub-projects:

- **compiler** - The BoxLang Compiler/Parser and ByteCode Generator
- **runtime** - The BoxLang Runtime

## JDK Targets

- JDK 17 LTS is our compiled code JDK Baseline which will be supported until 2026 with extended support until 2029.
- https://www.oracle.com/java/technologies/java-se-support-roadmap.html

## Basic Gradle Tasks

> The output folder used for the builds are `build/**` which can be found in each of the multi-project folders.

| Task              | Description                                                                                                        	|
|-------------------|-----------------------------------------------------------------------------------------------------------------------|
| `assemble`        | Build all tasks: `build/libs, build/scripts, build/distributions`														|
| `build`           | The default lifecycle task that triggers the build process, including tasks like `clean`, `assemble`, and others. 	|
| `clean`           | Deletes the `build` folders. It helps ensure a clean build by removing any previously generated artifacts.			|
| `compileJava`     | Compiles Java source code files located in the `src/main/java` directory												|
| `compileTestJava` | Compiles Java test source code files located in the `src/test/java` directory											|
| `getDependencies` | Downloads all the dependencies defined in `build.gradle` and puts them in the `build/dependencies` folder 			|
| `jar`             | Packages your project's compiled classes and resources into a JAR file `build/libs` folder							|
| `tasks`			| Show all the available tasks in the project																			|
| `test`            | Executes the unit tests in your project and produces the reports in the `build/reports/tests` folder					|


## Core Dependencies

Here is a listing of all of our core dependencies.  If you add one, make sure you document it here.

| Dependency | Version | License | Description |
|------------|---------|---------|-------------|
| [boxlang-compiler](https://github.com/ortus-solutions-private/boxlang-compiler) | 1.0.0 | Apache2 | The BoxLang Parser, Compiler, and BytCode Generator |

## Dev Dependencies

Here is a listing of all of our dev dependencies.  If you add one, make sure you document it here.

| Library   	| Description                                                  |
| ------------- | ------------------------------------------------------------ |
| **JUnit**     | Testing Framework: [JUnit User Guide](https://junit.org/junit5/docs/current/user-guide/) |
| **Mockito**   | Mocking library we use: [Mockito](https://www.baeldung.com/mockito-series) |
| **Truth**     | Extended assertion library: [Truth](https://github.com/google/truth) |

## Contributing

- All code should be formatted using either our Java Formatter or the CFFormatter.
- All code should have a license/copyright header based on [CodeHeader.txt](workbench/CodeHeader.txt)
