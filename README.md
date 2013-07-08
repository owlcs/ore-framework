# Using the framework

The benchmark framework comes with a shell script, *execExamples*, that triggers all but the query answering tasks on the supplied Pizza ontology (*test/pizza.owl*). These are executed using the example reasoner wrapper *execReasoner*, which uses JFact.

The *InputVerifier* JAR can be build using Ant via *build-input.xml* ('ant -buildfile build-input.xml'). It is used to verify whether the given parameters are valid (e.g., whether the concept name occurs in the ontology signature).

The *JFactReasonerWrapper*, triggered by the *execReasoner* script, is an example reasoner wrapper for the JFact reasoner, which supports all tasks except query answering. The JAR can be built using the *build-wrapper.xml* Ant script.

Participants should either create or alter the *execReasoner* script according to their system. Then the *start* script can be used to test the system. Note that the *start* script limits system resources, specifically memory (to a maximum of 8GB) and duration of the process (up to 6 minutes). This can be commented out, if necessary.

**Using the start script:**

* sh start `[Operation]` `[OntologyFile]` `[Output]` (`[ConceptURI]` | `[QueryFile]`)
    * `[Operation]`		One of: sat | classification | consistency | query
    * `[OntologyFile]`		Absolute ontology file path
    * `[Output]`		Output file path
    * `[ConceptURI]`		Concept URI, as declared in the ontology
    * `[QueryFile]`		Absolute query file path