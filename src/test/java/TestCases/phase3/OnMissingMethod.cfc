component {
    public any function onMissingMethod( string missingMethodName, any missingMethodArguments ){
        return missingMethodName & missingMethodArguments[2];
    }
}