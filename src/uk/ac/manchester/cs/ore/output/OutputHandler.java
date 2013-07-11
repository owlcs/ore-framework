package uk.ac.manchester.cs.ore.output;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

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
	 * Parse error file and return a curated string with the error
	 * @param errorFile	Error file
	 * @return String with the error, stripped of commas (for proper CSV consumption)
	 * @throws IOException
	 */
	public String parseErrorFile(File errorFile) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(errorFile));
		String output = "";
		String line = reader.readLine();
		while(line != null) {
			line = line.replaceAll(",", ";");
			output += line + " ";
			line = reader.readLine();
		}
		reader.close();
		return output;
	}
		
	
	/**
	 * Main
	 * @param 0: reasoner output
	 * @param 1: operation name
	 * @param 2: ontology name
	 * @param 3: result file
	 * @param 4: base result file
	 * @param 5: reasoner name
	 * @param 6: concept uri
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String row = "";
		OutputHandler handler = new OutputHandler();
		
		// Ontology filename
		File ontFile= new File(args[2]);
		String ontName = ontFile.getName();
		System.out.println("\tOntology name: " + ontName);
		row += ontName + ",";
		
		// Read in reasoner output
		File f = new File(args[0]);
		System.out.println("\tReasoner output: " + f.getAbsolutePath());
		row += handler.parseFile(f);
		
		// Operation name
		String opName = args[1];
		System.out.println("\tOperation name: " + opName);
		
		// Error file
		File errorFile = new File(args[3] + "_err");
		if(errorFile.exists()) {
			System.out.println("\tError file: " + errorFile.getAbsolutePath());
			String error = handler.parseErrorFile(errorFile);
			if(!error.isEmpty())
				row += error;
		}
		
		// Reasoner name
		String reasonerName = args[4];
		System.out.println("\tReasoner: " + reasonerName);
		
		// Concept uri
		if(args.length > 5) {
			String conceptUri = args[5];
			System.out.println("\tConcept URI: " + conceptUri);
		}
		
		System.out.println("CSV output: " + row);
	}
}
