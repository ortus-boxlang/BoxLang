component {
    public any function onMissingMethod( string missingMethodName, any missingMethodArguments ){
        return missingMethodName & missingMethodArguments[2];
    }

    public function variablesMissingCaller(){
        return variables.doesNotExist( argumentCollection=arguments );
    }

    public function headlessMissingCaller(){
        return doesNotExist( argumentCollection=arguments );
    }
}