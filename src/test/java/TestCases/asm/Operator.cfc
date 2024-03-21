
component accessors = true {
    property name="operation";

    public numeric function run( required numeric value ){
        return operation( value );
    }
}