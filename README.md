# Project Jericho Runtime

## Projects

The project is split into multiple sub-projects:

- **runtime** - The BoxLang Runtime

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
