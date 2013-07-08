# Using the framework

The benchmark framework comes with a shell script, *execExamples*, that triggers all but the query answering tasks on the supplied Pizza ontology (test/pizza.owl). These are executed using the example reasoner wrapper *execReasoner*, for JFact.

The *InputVerifier* JAR can be build using Ant via *build-input.xml* ('ant -buildfile build-input.xml'). It is used to verify whether the given parameters are valid (e.g., whether the concept name occurs in the ontology signature).

The *JFactReasonerWrapper*, triggered by the *execReasoner* script, is an example reasoner wrapper for the JFact reasoner, which supports all tasks except query answering. The JAR can be built using the *build-wrapper.xml* Ant script.

The *OutputHandler* JAR can be built using the *build-output.xml* Ant script.

**Using the start script:**

* sh start `[Operation]` `[Ontology]` `[Output]` `[Reasoner]` (`[ConceptURI]` | `[QueryFile]`)
    * `[Operation]`		One of: sat | classification | consistency | query
    * `[Ontology]`		Absolute ontology file path
    * `[Output]`		Output file path
    * `[Reasoner]`		Reasoner name (one of those specified in the script)
    * `[ConceptURI]`		Concept URI, as declared in the ontology
    * `[QueryFile]`		Absolute query file path