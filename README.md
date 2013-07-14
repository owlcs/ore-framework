ORE 2013 Competition Framework
====

#### a framework for the OWL Reasoner Evaluation (ORE) Workshop ####

The framework is tuned for UNIX-based operating systems, and requires Java v1.6 (or above) and Ant for successful deployment.

intro
--------------------
The competition framework can be applied to any given reasoner and set of ontologies, so long as these adhere to the [specification](http://ore2013.cs.manchester.ac.uk/competition/reasoner-submissions/) and the following folder structure: for convenience, the framework relies on a *base* folder (defined at the top of the main scripts that are described in **usage**), which is expected to contain three subfolders: 
* **ontologies** should contain the corpus split into **dl**, **el** and **rl** subfolders (i.e., each OWL profile considered here), each of which containing ontology files. Another expected subfolder is **sat**, which contains once again profile subfolders, each of which containing the concept name samples (as .txt files) for all ontologies.
* **reasoners** should contain a folder for each reasoner, where the folder name can be used to trigger that reasoner. Each reasoner folder should contain an *execReasoner* script to execute the reasoner with the given parameters.
* **runner** should contain all the necessary JAR files, i.e., the **components** below, and the launch scripts.


components
--------------------
The main components of the framework are:

* **InputVerifier**: built using Ant via *build-input.xml* (ant -buildfile build-input.xml). It is used to verify whether the given parameters are valid (e.g., whether the concept name occurs in the ontology signature).
* **OutputHandler**: built using *build-output.xml*. It extrapolates from reasoner output the execution time(s), error, and timeout (where applicable) into a comma-separated file.
* **SATSampler**: built using *build-satsampler.xml*. It is used to extract a given number of random concept names, and can be executed via *execSatSampler* for corpus-wide sampling.
* **JFactReasonerWrapper**: built using *build-wrapper*. This is an example reasoner wrapper for the JFact reasoner, which can be triggered by the *execReasoner* script.


usage
--------------------
Both of the following scripts contain a hard-coded **base** folder, which should be changed appropriately. The subfolders of **base** are defined relative to that, so if the structure above is obeyed, all should fall into place by changing only the **base** folder path.

The script that binds the various components is **start**, which can be used as follows:

* sh start `[Operation]` `[Ontology]` `[Output]` `[Reasoner]` (`[ConceptURI]`)
    * `[Operation]`		One of: sat | classification | consistency
    * `[Ontology]`		Absolute ontology file path
    * `[Output]`		Output folder (within reasoner folder)
    * `[Reasoner]`		Reasoner name
    * `[ConceptURI]`		Full concept URI
    
    
For corpus-wide deployment, the **startReasonerTest** (which invokes **start**) is used as follows:

* sh startReasonerTest `[Operation]` `[Output]` `[Reasoner]` `[Profiles]`
    * `[Operation]`		One of: sat | classification | consistency
    * `[Output]`		Output folder (within reasoner folder)
    * `[Reasoner]`		Reasoner name
    * `[Profiles]`		Profiles to be tested, any of: dl | el | rl (space separated)


examples
--------------------
The competition framework comes with a shell script, *execExamples*, that triggers all operations on the supplied Pizza ontology (test/pizza.owl). These are executed using the example reasoner wrapper (for JFact).