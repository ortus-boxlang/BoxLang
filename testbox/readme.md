[![Total Downloads](https://forgebox.io/api/v1/entry/testbox/badges/downloads)](https://forgebox.io/view/testbox)
[![Latest Stable Version](https://forgebox.io/api/v1/entry/testbox/badges/version)](https://forgebox.io/view/testbox)
[![Apache2 License](https://img.shields.io/badge/License-Apache2-blue.svg)](https://forgebox.io/view/testbox)

<p align="center">
	<img src="https://raw.githubusercontent.com/Ortus-Solutions/artwork/refs/heads/main/testbox/banners/testbox-site-banner.png" alt="TestBox BDD/TDD Testing Framework"/>
</p>

<p align="center">
	Copyright Since 2005 TestBox by Luis Majano and Ortus Solutions, Corp
	<br>
	<a href="https://www.testbox.run">www.testbox.run</a> |
	<a href="https://www.ortussolutions.com">www.ortussolutions.com</a>
</p>

# TestBox - BDD/TDD Testing Framework for BoxLang & CFML

Professional BDD (Behavior-Driven Development) and TDD (Test-Driven Development) testing framework for BoxLang and CFML applications. TestBox provides a comprehensive testing ecosystem with integrated MockBox mocking capabilities, multiple output formats, and both CLI and web-based test runners.

> **TestBox v6 Now Available**: [Read the announcement »](https://testbox.ortusbooks.com/readme/release-history/whats-new-with-6.0.0)

## 🌟 Features

- **Dual Testing Approaches**: Full support for both BDD (`describe()`, `it()`) and xUnit (`setup()`, `test*()`) styles
- **BoxLang First**: Native support for BoxLang with dedicated CLI runner, plus full CFML compatibility
- **Integrated Mocking**: Built-in MockBox framework with spies, stubs, and verification capabilities
- **Rich Data Generation**: CBMockData integration for realistic test data (names, addresses, dates)
- **Multiple Reporters**: 15+ output formats including HTML, JSON, XML, TAP, JUnit, and Console
- **CLI & Web Runners**: Execute tests via CommandBox CLI or web-based interfaces
- **File Watching**: Automatic test re-execution on code changes during development
- **Code Coverage**: Built-in coverage analysis and reporting
- **Flexible Discovery**: Automatic test bundle detection with customizable patterns
- **Thread-Safe**: Concurrent test execution with proper isolation

## 💻 Requirements

- **BoxLang**: 1.0+
- **Lucee**: 5.0+
- **Adobe ColdFusion**: 2021+

## ⚡ Quick Start

### 1. Installation

```bash
# Install TestBox and CLI tools via CommandBox
box install testbox testbox-cli

# Or install bleeding edge
box install testbox@be testbox-cli
```

### 2. Create Your First Test

```javascript
// tests/specs/UserServiceTest.cfc
class extends="testbox.system.BaseSpec" {

    function run() {
        describe( "UserService", () => {
            beforeEach( () => {
                userService = new models.UserService();
            } );

            it( "should create a new user", () => {
                var user = userService.createUser( "john@example.com", "John Doe" );
                expect( user.getEmail() ).toBe( "john@example.com" );
                expect( user.getName() ).toBe( "John Doe" );
            } );
        } );
    }
}
````

## 🎭 MockBox Integration & Test Data

### Advanced Mocking with MockBox

```javascript
class extends="testbox.system.BaseSpec" {

    function run() {
        describe( "Payment Service", () => {
            beforeEach( () => {
                // Create mocks and spies
                mockGateway = createMock( "services.PaymentGateway" );
                mockLogger = createEmptyMock( "cblogger.models.Logger" );

                // Setup mock behavior
                mockGateway.$( "processPayment" ).$results( {
                    success: true,
                    transactionId: "TXN-12345"
                } );

                paymentService = new models.PaymentService(
                    gateway = mockGateway,
                    logger = mockLogger
                );
            } );

            it( "should process payment and log success", () => {
                var result = paymentService.charge( 100.00, "USD" );

                // Verify method calls
                expect( mockGateway.$times( 1, "processPayment" ) ).toBeTrue();
                expect( mockLogger.$times( 1, "info" ) ).toBeTrue();

                // Verify results
                expect( result.success ).toBeTrue();
                expect( result.transactionId ).toBe( "TXN-12345" );
            } );
        } );
    }
}### Realistic Test Data with CBMockData

```javascript
class extends="testbox.system.BaseSpec" {

    property name="mockData" inject="MockData@cbMockData";

    function run() {
        describe("User Profile Tests", () => {
            it("should handle various user data scenarios", () => {
                // Generate realistic test data
                var testUsers = [];

                for (var i = 1; i <= 10; i++) {
                    arrayAppend(testUsers, {
                        firstName: mockData.fname(),
                        lastName: mockData.lname(),
                        email: mockData.email(),
                        age: mockData.age(),
                        address: {
                            street: mockData.streetaddress(),
                            city: mockData.city(),
                            state: mockData.state(),
                            zipCode: mockData.zipcode()
                        },
                        registrationDate: mockData.datetime()
                    });
                }

                // Test with realistic data
                for (var user in testUsers) {
                    var profile = userService.createProfile(user);
                    expect(profile.isValid()).toBeTrue();
                    expect(profile.getEmail()).toMatch("^[\w\.-]+@[\w\.-]+\.[A-Za-z]{2,}$");
                }
            });
        });
    }
}
```

### 3. Run Your Tests

```bash
# Via CommandBox CLI
box testbox run

# Via BoxLang CLI Runner (fastest execution)
./testbox/run                           # Run default tests.specs
./testbox/run --directory=my.tests      # Specific directory
./testbox/run --bundles=my.bundle       # Specific bundles
./testbox/run --reporter=json           # Custom reporter

# Via web browser
# Navigate to: http://localhost/testbox/system/runners/HTMLRunner.cfm
```

## 🧪 Testing Approaches

### BDD Style (Behavior-Driven Development)

```javascript
```javascript
class extends="testbox.system.BaseSpec" {

    function run() {
        describe( "User Registration", () => {
            beforeEach(() => {
                userService = createMock("models.UserService");
                variables.sut = new handlers.Users();
            });

            describe("When registering a new user", () => {
                it("should validate email format", () => {
                    expect(() => {
                        userService.register("invalid-email", "password");
                    }).toThrow("ValidationException");
                });

                it("should create user with valid data", () => {
                    var result = userService.register("john@test.com", "securePass");
                    expect(result.success).toBeTrue();
                    expect(result.user.email).toBe("john@test.com");
                });
            });
        });
    }
}
```

### xUnit Style (Traditional Unit Testing)

```javascript
```javascript
class extends="testbox.system.BaseSpec" {

    function setup() {
        // Runs before each test
        userService = new models.UserService();
        testData = {
            email: "test@example.com",
            name: "Test User"
        };
    }

    function testUserCreation() {
        var user = userService.createUser( testData.email, testData.name );
        $assert.isEqual( testData.email, user.getEmail() );
        $assert.isEqual( testData.name, user.getName() );
    }

    function testEmailValidation() {
        $assert.throws( () => {
            userService.createUser( "invalid-email", "Test User" );
        }, "ValidationException" );
    }

    function tearDown() {
        // Cleanup after each test
        structDelete( variables, "userService" );
        structDelete( variables, "testData" );
}
```

## ⚙️ Configuration & CLI Usage

### CLI Test Execution

```bash
# Run all tests with default settings
box testbox run

# Run specific test bundles
box testbox run bundles=tests.specs.UserServiceTest

# Run tests with custom reporter
box testbox run reporter=json

# Run tests with labels ( focused testing )
box testbox run labels=unit --excludes=integration

# Watch mode for continuous testing
box testbox watch

# Generate test templates
box testbox create bdd MyNewTest
box testbox create unit MyNewTest
```

### Application Setup

Create an `Application.cfc` in your test directory:

```javascript
class {
    this.name = "MyApp-Tests-" & hash( getCurrentTemplatePath() );
    this.sessionManagement = true;
    this.sessionTimeout = createTimeSpan(0, 0, 15, 0);
    this.applicationTimeout = createTimeSpan(0, 0, 15, 0);
    this.setClientCookies = true;

    // TestBox mappings
    this.mappings[ "/testbox" ] = expandPath( "/testbox" );
    this.mappings[ "/tests" ] = getDirectoryFromPath( getCurrentTemplatePath() );
    this.mappings[ "/models" ] = expandPath( "/models" );

    // Module mappings
    this.mappings[ "/cbstreams" ] = expandPath( "/testbox/system/modules/cbstreams" );
    this.mappings[ "/cbMockData" ] = expandPath( "/testbox/system/modules/cbMockData" );
    this.mappings[ "/globber" ] = expandPath( "/testbox/system/modules/globber" );    function onApplicationStart() {
        application.wirebox = new coldbox.system.ioc.Injector();
        return true;
    }

    function onRequestStart() {
        // Reset ORM on every request for clean testing
        if (structKeyExists(url, "fwreinit")) {
            if (structKeyExists(server, "lucee")) {
                pagePoolClear();
            }
            ormReload();
        }
    }
}
```

### Web Runner Configuration

Create `tests/runner.cfm` for web-based test execution:

```html
<cfsetting showDebugOutput="false">
<!DOCTYPE html>
<html>
<head>
    <title>My Application Test Suite</title>
</head>
<body>
    <cfscript>
        // Create TestBox instance
        testbox = new testbox.system.TestBox(
            options = {
                // Test bundle directories
                bundles = [
                    "tests.specs"
                ],
                // Directories to include/exclude
                directory = {
                    mapping = "tests.specs",
                    recurse = true
                },
                // Test labels
                labels = url.labels ?: "",
                excludes = url.excludes ?: "",
                // Reporter
                reporter = url.reporter ?: "simple",
                // Coverage settings
                coverage = {
                    enabled = true,
                    pathToCapture = expandPath("/models"),
                    whitelist = "*.cfc",
                    blacklist = "*Test*.cfc"
                }
            }
        );

        // Run tests and output results
        writeOutput(testbox.run());
    </cfscript>
</body>
</html>
```

## 📄 License

Apache License, Version 2.0.

## 📋 Versioning

TestBox is maintained under the Semantic Versioning guidelines as much as possible.

Releases will be numbered with the following format:

```html
<major>.<minor>.<patch>
```

And constructed with the following guidelines:

- Breaking backward compatibility bumps the major (and resets the minor and patch)
- New additions without breaking backward compatibility bumps the minor (and resets the patch)
- Bug fixes and misc changes bumps the patch

## 📚 Documentation & Resources

### Source Code

- [GitHub Repository](https://github.com/Ortus-Solutions/TestBox)

### Bug Tracking

- [Issue Tracker](https://ortussolutions.atlassian.net/browse/TESTBOX)

### Community & Support

- [Community Forum](https://community.ortussolutions.com/c/communities/testbox/11)
- [Join BoxTeam Slack](https://boxteam.ortussolutions.com)

### Official Documentation

- [TestBox Documentation](https://testbox.ortusbooks.com)
- [BDD Primer](https://testbox.ortusbooks.com/content/primers/bdd/index.html)
- [xUnit Primer](https://testbox.ortusbooks.com/content/primers/xunit/index.html)

### Community and Support

Join us in our Ortus Community and become a valuable member of this project [TestBox BDD](https://community.ortussolutions.com/c/communities/testbox/11). We are looking forward to hearing from you!!

### Official Sites

- [TestBox Official Site](https://www.testbox.run)
- [Ortus Solutions](https://www.ortussolutions.com/products/testbox)

## 📄 License

Apache License, Version 2.0. See [LICENSE](LICENSE) file for details.

---

## About Ortus Solutions

**TestBox** is a professional open-source project by **Ortus Solutions**.

- **TestBox Framework**: [https://www.testbox.run](https://www.testbox.run)
- **Ortus Solutions**: [https://www.ortussolutions.com](https://www.ortussolutions.com)

---

**Copyright Since 2005 TestBox by Luis Majano and Ortus Solutions, Corp**

**[www.testbox.run](https://www.testbox.run) | [www.ortussolutions.com](https://www.ortussolutions.com)**

---

### ✝️ HONOR GOES TO GOD ABOVE ALL

Because of His grace, this project exists. If you don't like this, then don't read it, its not for you.

> *"Therefore being justified by faith, we have peace with God through our Lord Jesus Christ: By whom also we have access by faith into this grace wherein we stand, and rejoice in hope of the glory of God. And not only so, but we glory in tribulations also: knowing that tribulation worketh patience; And patience, experience; and experience, hope: And hope maketh not ashamed; because the love of God is shed abroad in our hearts by the Holy Ghost which is given unto us."* **Romans 5:5**

### 🍞 THE DAILY BREAD

> *"I am the way, and the truth, and the life; no one comes to the Father, but by me (JESUS)"* **John 14:1-12**
