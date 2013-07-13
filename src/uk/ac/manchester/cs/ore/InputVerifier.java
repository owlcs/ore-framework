package uk.ac.manchester.cs.ore;

import java.io.File;
import java.io.IOException;
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
	
	public InputVerifier(){}
	
	/**
	 * Check whether the given operation is a valid one
	 * @param op	operation name
	 * @return true if operation is valid, false otherwise
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
	
	
	/**
	 * Check whether the given ontology file path is a parseable ontology
	 * @param ontFile	Ontology file
	 * @return true if ontology is parseable, false otherwise
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
	
	
	/**
	 * Check whether the given concept URI exists in the ontology signature
	 * @param cName	Concept name
	 * @return true if concept exists in the ontology, false otherwise
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
	
	
	/**
	 * Determine the output file exists (if not create it) and is writable (if not, make it so)
	 * @param outFile	Output file
	 * @return true if output file is writable, false otherwise 
	 */
	public boolean isWritable(String outFile) {
		File f = new File(outFile);
		if(!f.exists()) {
			f.getParentFile().mkdirs();
			try { f.createNewFile(); } 
			catch (IOException e) { e.printStackTrace(); }
		}
		if(!f.canWrite()) f.setWritable(true);
		return f.canWrite();
	}
	
	
	/**
	 * Main
	 * @param 0: operation name
	 * @param 1: ontology file path
	 * @param 2: output file path
	 * @param 3: [where applicable] concept URI or query file path
	 */
	public static void main(String[] args) {
		if(args.length < 3) {
			System.err.println("\tEmpty or incomplete argument list. Arguments must be: <Operation> <OntologyFile> <Output> (<Concept> | <QueryFile>)");
			System.exit(0);
		}
		
		String operation = args[0], ontFile = args[1], outputFile = args[2];
		InputVerifier iv = new InputVerifier();
		
		boolean isValidOp = iv.isValidOperation(operation);
		if(!isValidOp) {
			System.err.println("! Invalid operation: " + operation);
			System.exit(0);
		}
		
		boolean isParseable = iv.isParseable(ontFile);
		if(!isParseable) {
			System.err.println("! Unable to parse given file: " + ontFile);
			System.exit(0);
		}
		
		boolean isWriteable = iv.isWritable(outputFile);
		if(!isWriteable) System.err.println("! Cannot write to file: " + outputFile);
		
		// Consistency and classification
		if((operation.equalsIgnoreCase("classification") || operation.equalsIgnoreCase("consistency")) && args.length == 3)
			System.out.println("Valid parameters");
		
		// Satisfiability
		else if(operation.equalsIgnoreCase("sat")) {
			if(args.length < 4) System.err.println("\tA concept name URI is required for satisfiability testing");
			else {
				String cName = args[3];
				boolean isValidConceptURI = iv.isValidConceptName(cName);
				if(isValidConceptURI) System.out.println("Valid parameters");
			}
		}
	}
}