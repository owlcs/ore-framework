package uk.ac.manchester.cs.ore.output;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.StringTokenizer;

/**
 * @author Rafael S. Goncalves <br/>
 * Information Management Group (IMG) <br/>
 * School of Computer Science <br/>
 * University of Manchester <br/>
 */
public class OutputHandler {
	private double opTime, opCpuTime, externalDuration;
	
	/**
	 * Constructor: sets all times to 0
	 */
	public OutputHandler() {
		opTime = 0;
		opCpuTime = 0;
		externalDuration = 0;
	}
	
	
	/**
	 * Parse the reasoner's output and retrieve the reported times 
	 * @param reasonerOutput	Reasoner output file
	 * @return Reported times by the reasoner and shell
	 * @throws IOException
	 */
	public String parseFile(File reasonerOutput) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(reasonerOutput));
		String line = reader.readLine();
		while(line != null) {
			line = line.trim();
			StringTokenizer st = new StringTokenizer(line, ":");
			for(int i = 0; i < st.countTokens(); i++) {
				String token = st.nextToken();
				if(token.equalsIgnoreCase("operation time") || token.equalsIgnoreCase("classification time")) { 
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
		return opTime + "," + opCpuTime + "," + externalDuration + ",";
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
		if(line!=null) {
			line = line.trim();
			if(line.equalsIgnoreCase("timeout"))
				output += "timeout";
			else if(line.toLowerCase().contains("inconsistentontology") || line.toLowerCase().contains("stopping konclude")
					|| line.toLowerCase().contains("expressionsplitter")) {
				output += "";
			}
			else {
				while(line != null && output.length()<5000) {
					line = line.trim();
					line = line.replaceAll(",", ";");
					output += line + " ";
					line = reader.readLine();
				}
			}
		}
		reader.close();
		return output + ",";
	}
		
	
	/**
	 * Get operation time
	 * @return Operation time (in milliseconds)
	 */
	public double getOpTime() {
		return opTime;
	}

	
	/**
	 * Get operation CPU time
	 * @return Operation CPU time (in milliseconds)
	 */
	public double getOpCpuTime() {
		return opCpuTime;
	}
	
	
	/**
	 * Main
	 * @param 0: reasoner output (log)
	 * @param 1: operation name
	 * @param 2: ontology name
	 * @param 3: error file
	 * @param 4: output directory
	 * @param 5: concept uri
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String row = "";
		OutputHandler handler = new OutputHandler();
		
		// Ontology filename
		File ontFile= new File(args[2]);
		String ontName = ontFile.getName();
		row += ontName + ",";
		
		// Read in reasoner output (log)
		File f = new File(args[0]);
		row += handler.parseFile(f);
		
		// Operation name
		String opName = args[1];
	
		// Concept uri
		if(args.length > 5) {
			String conceptUri = args[5];
			row += conceptUri + ",";
		}
		
		// Error file
		File errorFile = new File(args[3] + "_err");
		if(errorFile.exists()) {
			String error = handler.parseErrorFile(errorFile);
			if(!error.isEmpty())
				row += error;
		}
		else if(handler.getOpTime() == 0 && handler.getOpCpuTime() == 0) 
			row += "timeout,";
		
		File outputDir = new File(args[4]);
		outputDir.mkdirs();
		String outFile = outputDir.getAbsolutePath();
		if(!outFile.endsWith(File.separator)) outFile += File.separator;
		outFile += "_" + opName + ".csv";
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outFile), true));
		bw.write(row + "\n");
		bw.close();
		System.out.println("\tSaved log at: " + outFile);
	}
}
