abstract component implements="IClass" {
    string function echo( required string value ) {
        return value;
    }
    // greet() intentionally left for subclasses
}