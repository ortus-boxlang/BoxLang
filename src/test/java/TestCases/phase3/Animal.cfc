component {
    variables.results = [];
    results.append('animal pseudo ' & getFileFromPath( getCurrentTemplatePath()))
    variables.inAnimal = true;
    variables.inDog = false;

    function init() {
        results.append('Animal init ' & getFileFromPath( getCurrentTemplatePath()))
    }

    function speak() {
        throw( "speak method not implemented" );
    }

    function isWarmBlooded() {
        // this needs to be a reference to the bottom most class
        results.append( "animal this is: " & this.$bx.meta.name )
        // We need to see the variables scope of the bottom most class
        results.append( "animal sees inDog as: " & inDog )
        return true;
    }

    function getScientificName() {
        // this needs to be a reference to the bottom most class
        results.append( "super animal sees: " & getMetadata( this ).name )
        // We need to see the variables scope of the bottom most class
        results.append( "super sees inDog as: " & inDog )

        return "Animal Kingdom";
    }

    function getResults() {
        return results;
    }

 }
