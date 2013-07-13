ORE 2013 Competition Framework
====

#### a framework for the OWL Reasoner Evaluation (ORE) Workshop ####

The competition framework comes with a shell script, *execExamples*, that triggers all operations on the supplied Pizza ontology (test/pizza.owl). These are executed using the example reasoner wrapper (for JFact).

components
--------------------
The main components of the framework are:

* **InputVerifier**: built using Ant via *build-input.xml* (*ant -buildfile build-input.xml*). It is used to verify whether the given parameters are valid (e.g., whether the concept name occurs in the ontology signature).
* **OutputHandler**: built using *build-output.xml*. It extrapolates from reasoner output the execution time(s), error, and timeout (where applicable) into a comma-separated file.
* **SATSampler**: built using *build-satsampler*. It is used to extract a given number of arbitrary concept names.
* **JFactReasonerWrapper**: built using *build-wrapper*. This is an example reasoner wrapper for the JFact reasoner, which can be triggered by the *execReasoner* script.

usage
--------------------

* sh start `[Operation]` `[Ontology]` `[Output]` `[Reasoner]` (`[ConceptURI]`)
    * `[Operation]`		One of: sat | classification | consistency | query
    * `[Ontology]`		Absolute ontology file path
    * `[Output]`		Output folder
    * `[Reasoner]`		Reasoner name (one of those specified in the script)
    * `[ConceptURI]`		Concept URI, as declared in the ontology