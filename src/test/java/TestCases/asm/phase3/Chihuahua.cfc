component extends="Dog" {
    results.append('Chihuahua pseudo ' & getFileFromPath( getCurrentTemplatePath()))

    function init() {
        super.init();
        results.append('Chihuahua init ' & getFileFromPath( getCurrentTemplatePath()))
    }

    function speak() {
        return "Yip Yip!";
    }

    function getScientificName() {
        return "barkus annoyus " & super.getScientificName();
    }

 }