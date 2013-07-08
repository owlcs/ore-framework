package uk.ac.manchester.cs.ore.output;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import uk.ac.manchester.cs.diff.axiom.LogicalDiff;
import uk.ac.manchester.cs.diff.axiom.StructuralDiff;

/**
 * @author Rafael S. Goncalves <br/>
 * Information Management Group (IMG) <br/>
 * School of Computer Science <br/>
 * University of Manchester <br/>
 */
public class OutputHandler {
	
	public OutputHandler() {}
	
	/**
	 * Parse the reasoner's output and retrieve the reported times 
	 * @param reasonerOutput	Reasoner output file
	 * @return Reported times by the reasoner and shell
	 * @throws IOException
	 */
	public String parseFile(File reasonerOutput) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(reasonerOutput));
		String line = reader.readLine();
		double opTime = 0, opCpuTime = 0, externalDuration = 0;
		while(line != null) {
			line = line.trim();
			StringTokenizer st = new StringTokenizer(line, ":");
			for(int i = 0; i < st.countTokens(); i++) {
				String token = st.nextToken();
				if(token.equalsIgnoreCase("operation time")) { 
					opTime = Double.parseDouble(st.nextToken());
					System.out.println("\tOperation time: " + opTime + " milliseconds");
				}
				else if(token.equalsIgnoreCase("operation cpu time")) {
					opCpuTime = Double.parseDouble(st.nextToken());
					System.out.println("\tOperation CPU time: " + opCpuTime + " milliseconds");
				}
				else if(token.equalsIgnoreCase("duration")) {
					externalDuration = Double.parseDouble(st.nextToken());
					System.out.println("\tOperation external duration: " + externalDuration + " seconds");
				}
			}
			line = reader.readLine();
		}
		reader.close();
		String out = opTime + ",";
		if(opCpuTime != 0) out += opCpuTime + ",";
		out += externalDuration + ",";
		return out;
	}
	
	
	/**
	 * Verify whether the given output is the correct one w.r.t. a base file
	 * @param opName	Operation name
	 * @param resultFile	Results file
	 * @param baseFile	Base file
	 * @return true if the result is correct, false otherwise
	 */
	public boolean isResultCorrect(String opName, File resultFile, File baseFile) {
		boolean isCorrect = true;
		
		if(opName.equalsIgnoreCase("classification"))
			isCorrect = isEquivalent(resultFile, baseFile);
		else if(opName.equalsIgnoreCase("sat"))
			throw new RuntimeException("NOT IMPLEMENTED");
		else if(opName.equalsIgnoreCase("consistency"))
			throw new RuntimeException("NOT IMPLEMENTED");
		
		return isCorrect;
	}
	
	
	/**
	 * Verify whether two ontology files are equivalent, first w.r.t. structural diff and,
	 * subsequently, because of the equivalent vs dual subsumptions issue, logical diff
	 * @param resultFile	Results file
	 * @param baseFile	Base file
	 * @return true if ontologies are logically equivalent
	 */
	private boolean isEquivalent(File resultFile, File baseFile) {
		boolean isEquivalent = true;
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		try {
			OWLOntology result = man.loadOntologyFromOntologyDocument(resultFile);
			OWLOntology base = man.loadOntologyFromOntologyDocument(baseFile);
			
			StructuralDiff sdiff = new StructuralDiff(result, base, false);
			isEquivalent = sdiff.isEquivalent();
			
			if(!isEquivalent) {
				LogicalDiff ldiff = new LogicalDiff(result, base, false);
				isEquivalent = ldiff.isEquivalent();
			}
		} catch (OWLOntologyCreationException e) {
			isEquivalent = false;
			e.printStackTrace();
		}
		return isEquivalent;
	}
	
	
	/**
	 * Main
	 * @param 0: reasoner output
	 * @param 1: operation name
	 * @param 2: ontology name
	 * @param 3: result file
	 * @param 4: reasoner name
	 * @param 5: concept uri
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String output = "";
		OutputHandler handler = new OutputHandler();
		
		// Ontology filename
		File ontFile= new File(args[2]);
		String ontName = ontFile.getName();
		System.out.println("\tOntology name: " + ontName);
		output += ontName + ",";
		
		// Read in reasoner output
		File f = new File(args[0]);
		System.out.println("\tReasoner output: " + f.getAbsolutePath());
		output += handler.parseFile(f);
		
		// Operation name
		String opName = args[1];
		System.out.println("\tOperation name: " + opName);
		
		// Results file
		File resultFile = new File(args[3]);
		if(resultFile.exists())
			System.out.println("\tResult file: " + resultFile.getAbsolutePath());
		
		// Error file
		File errorFile = new File(args[3] + "_err");
		if(errorFile.exists())
			System.out.println("\tError file: " + errorFile.getAbsolutePath());
		
		// Reasoner name
		String reasonerName = args[4];
		System.out.println("\tReasoner: " + reasonerName);
		
		// Concept uri
		if(args.length > 5) {
			String conceptUri = args[5];
			System.out.println("\tConcept URI: " + conceptUri);
		}
		
		System.out.println("CSV output: " + output);
	}
}
