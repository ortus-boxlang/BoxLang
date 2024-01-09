component extends="Animal" {
    results.append('Dog pseudo ' & getFileFromPath( getCurrentTemplatePath()))
    variables.inDog = true;
    // Our variables scope contains the variables from the parent component
    results.append( "dog sees variables.inAnimal as: " & variables.inAnimal )

    function init() {
        super.init();
        results.append('Dog init ' & getFileFromPath( getCurrentTemplatePath()))
    }

    function speak() {
        return "Woof!";
    }

    function getScientificName() {
        return "Canis lupus " & super.getScientificName();
    }

 }