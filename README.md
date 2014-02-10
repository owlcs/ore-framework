ORE 2013 Competition Framework
====

#### a framework for the OWL Reasoner Evaluation (ORE) Workshop ####

The framework is tuned for UNIX-based operating systems. It requires Java v1.6 or above and Ant for successful deployment.

intro
--------------------
The competition framework can be applied to any given reasoner and set of ontologies, so long as these adhere to the [specification](http://ore2013.cs.manchester.ac.uk/competition/reasoner-submissions/) and the following folder structure: for convenience, the framework relies on a *base* folder (defined at the top of the main scripts that are described in **usage**), which is expected to contain three subfolders: 
* **ontologies** should contain the corpus split into the desired syntaxes **functional** and **owlxml**, and as subfolders of the syntaxes there should be the **dl**, **el** and **rl** profiles subfolders, each of which containing the ontology files. Another expected subfolder of **ontologies** is **sat**, which contains once again profile subfolders, each of which containing the concept name samples (as .txt files) for all ontologies. Random concept name samples can be obtained via the **execSATSampler** script.
* **reasoners** should contain a folder for each reasoner, where the folder name can be used to trigger that reasoner. Each reasoner folder should contain an *execReasoner* script to execute the reasoner with the given parameters.
* **runner** should contain all the necessary JAR files, i.e., the **components** below, and the launch scripts supplies here.

Further documentation is supplied within the shell scripts, as well as the Java code.


components
--------------------
The main components of the framework are listed below. In order to build the necessary JAR file(s), use `ant -buildfile build-xyz.xml` where `xyz` is specified inline with the corresponding component.

* **InputVerifier**: built using Ant via *build-input.xml*. It is used to verify whether the given parameters are valid (e.g., whether the concept name occurs in the ontology signature).
* **OutputHandler**: built using *build-output.xml*. It extrapolates from reasoner output the execution time(s), error, and timeout (where applicable) into a comma-separated file.
* **ResultChecker**: built using *build-resultchecker.xml*. This verifies which reasoners' results is correct (by consensus).
* **SATSampler**: built using *build-satsampler.xml*. It is used to extract a given number of random concept names, and can be executed via *execSATSampler* for corpus-wide sampling.
* **JFactReasonerWrapper**: built using *build-wrapper.xml*. This is an example reasoner wrapper for the JFact reasoner, which can be triggered by the *execReasoner* script.

The shell scripts to execute the various tests that were part of the ORE 2013 reasoner competition are explained in *usage*, and briefly described below.

* **start**: starts off a single specified reasoner on a single ontology
* **startReasonerTest**: starts off a single specified reasoner on multiple ontologies
* **startResultVerificationSolo**: starts the verification of all reasoners' results on a single ontology
* **startResultVerification**: starts the verification of all reasoners' results on all ontologies
* **execSATSampler**: performs a sampling of concept names from the given ontology
* **execReasoner**: starts off the reasoner (to be provided by each system submission, though an example one is included)
* **execExamples**: performs a test run of a reasoner on the example ontology supplied

All of these components were tested in Mac OS X (v10.8.5) and Fedora, running (respectively) Oracle Java v1.7 and v1.6, and Apache Ant 1.8.2.


usage
--------------------
Both of the following scripts contain a hard-coded **base** folder, which should be changed appropriately. The subfolders of **base** are defined relative to that, so if the structure above is adhered to, all should fall into place by changing only the **base** folder path.

The script that binds the various components is **start**, which can be used as follows:

* sh start `[Operation]` `[Ontology]` `[Output]` `[Reasoner]` `[CSVOutput] (`[ConceptURI]`)
    * `[Operation]`		One of: sat | classification | consistency
    * `[Ontology]`		Absolute ontology file path
    * `[Output]`		Output folder (within reasoner folder)
    * `[Reasoner]`		Reasoner name
    * `[CSVOutput]`		Folder where the csv result file should be serialised to (absolute path)
    * `[ConceptURI]`		Full concept URI
    
    
For corpus-wide deployment, the **startReasonerTest** (which invokes **start**) is used as follows:

* sh startReasonerTest `[Operation]` `[Output]` `[Reasoner]` `[Syntax]` `[Profiles]`
    * `[Operation]`		One of: sat | classification | consistency
    * `[Output]`		Output folder (within reasoner folder)
    * `[Reasoner]`		Reasoner name
    * `[Syntax]`		One of: functional | owlxml
    * `[Profiles]`		Profiles to be tested, any of: dl | el | rl (space separated)


The verification of the output of all reasoners can be done via the **startResultVerification** to process all ontologies, or the **startResultVerificationSolo** variant to process a single ontology. Note that in both cases all declared reasoners are tested, and these are hard-coded into the script (thus may need changing if the reasoner set is different). The script to process all ontologies can be used as follows:

* sh startResultVerification `[Operation]` `[Output]` `[Profiles]`
	* `[Operation]`		One of: sat | classification | consistency
	* `[Output]`		Output folder (absolute path)
	* `[Profiles]`		Profiles to be tested, any of: dl | el | rl (space separated)


The **startResultVerificationSolo** script is used as follows:

* sh startResultVerification `[Operation]` `[Ontology]` `[Output]` `[Profiles]`
	* `[Operation]`		One of: sat | classification | consistency
    * `[Ontology]`		Absolute ontology file path
	* `[Output]`		Output folder (absolute path)
	* `[Profile]`		Profile to be tested, any of: dl | el | rl (space separated)
	

examples
--------------------
The competition framework comes with a shell script, *execExamples*, that triggers all operations on the supplied Pizza ontology (test/pizza.owl). These are executed using the example reasoner wrapper (for JFact).