package uk.ac.manchester.cs.ore;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.VersionInfo;

/**
 * @author Rafael S. Goncalves <br/>
 * Information Management Group (IMG) <br/>
 * School of Computer Science <br/>
 * University of Manchester <br/>
 */
public class InputVerifier {
	private OWLOntology ont;
	
	public InputVerifier() {}
	
	/*
	 * Check whether the given operation is a valid one
	 */
	public boolean isValidOperation(String op) {
		boolean isValid = false;
		List<String> ops = Arrays.asList("sat","query","classification","consistency");
		for(String s : ops) {
			if(s.equalsIgnoreCase(op)) {
				isValid = true; break;
			}
		}
		if(!isValid)
			System.err.println("\tInvalid operation name '" + op + "'. It must be one of: [ classification | sat | query | consistency ]");
		
		return isValid;
	}
	
	
	/*
	 * Check whether the given ontology file path is a parseable ontology
	 */
	public boolean isParseable(String ontFile) {
		boolean isParseable = false; 
		File f = new File(ontFile);
		if(!f.exists())
			System.err.println("\tThe given ontology filepath '" + ontFile + "' does not point to a valid file");
		else {
			OWLOntologyManager man = OWLManager.createOWLOntologyManager();
			try{ 
				ont = man.loadOntologyFromOntologyDocument(f); 
			} catch(OWLOntologyCreationException e) {
				System.err.println("\tError parsing ontology file '" + ontFile + "' using the OWL API v" + VersionInfo.getVersionInfo().getVersion() + ":");
				e.printStackTrace();
			}
			if(ont != null) isParseable = true;
		}
		return isParseable;
	}
	
	
	/*
	 * Check whether the given concept URI exists in the ontology signature
	 */
	public boolean isValidConceptName(String cName) {
		boolean isValid = false;
		OWLDataFactory df = ont.getOWLOntologyManager().getOWLDataFactory();
		OWLClass c = df.getOWLClass(IRI.create(cName));
		if(ont.containsClassInSignature(c.getIRI()))
			isValid = true;
		else
			System.err.println("\tInvalid concept name. '" + cName + "' is not contained in the ontology signature");
		return isValid;
	}
	
	
	/*
	 * Main
	 */
	public static void main(String[] args) {		
		if(!(args.length >= 3)) {
			System.err.println("\tEmpty or incomplete argument list. Arguments must be: <Operation> <OntologyFile> <Output> (<Concept> | <QueryFile>)");
			System.exit(0);
		}
		
		String operation = args[0]; String ontFile = args[1];
		InputVerifier iv = new InputVerifier();
		
		boolean isValidOp = iv.isValidOperation(operation);
		if(!isValidOp) System.exit(0);
		
		boolean isParseable = iv.isParseable(ontFile);
		if(!isParseable) System.exit(0);
		
		// Consistency and classification
		if((operation.equalsIgnoreCase("classification") || operation.equalsIgnoreCase("consistency")) && args.length == 3)
			System.out.println("Valid parameters");

		// Satisfiability
		else if(operation.equalsIgnoreCase("sat")) {
			if(args.length < 4) System.err.println("\tA concept name URI is required for satisfiability testing");
			else {
				String cName = args[3];
				boolean isValidConceptURI = iv.isValidConceptName(cName);
				if(isValidConceptURI)
					System.out.println("Valid parameters");
			}
		}

		// Query answering
		else if(operation.equalsIgnoreCase("query")) {
			if(args.length < 4) System.err.println("\tThe query file has not been specified");
			else {
				if(!new File(args[3]).isFile()) System.err.println("\tThe given query filepath '" + args[3] + "' does not point to a valid file");
				else System.out.println("Valid parameters");
			}
		}
	}
}