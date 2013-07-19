package uk.ac.manchester.cs.ore.output;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import uk.ac.manchester.cs.diff.axiom.LogicalDiff;
import uk.ac.manchester.cs.diff.axiom.StructuralDiff;
import uk.ac.manchester.cs.diff.axiom.changeset.ChangeSet;
import uk.ac.manchester.cs.diff.axiom.changeset.LogicalChangeSet;
import uk.ac.manchester.cs.diff.axiom.changeset.StructuralChangeSet;

/**
 * @author Rafael S. Goncalves <br/>
 * Information Management Group (IMG) <br/>
 * School of Computer Science <br/>
 * University of Manchester <br/>
 */
public class ResultComparator {
	private List<File> files;
	private List<String> reasonerList;
	private String outputFolder;
	private BufferedWriter log;
	private Map<String,String> map;
	
	/**
	 * Constructor
	 * @param files	Set of results files
	 * @param outputFolder	Output folder
	 */
	public ResultComparator(List<File> files, String outputFolder) {
		this.files = files;
		this.outputFolder = outputFolder;
		map = new HashMap<String,String>();
		reasonerList = getReasonerList();
		log = initWriter("log.txt");
	}
	
	
	/**
	 * Verify whether the given output is the correct one w.r.t. a base file
	 * @param opName	Operation name
	 * @return true if all results are the same, false otherwise
	 * @throws IOException 
	 */
	public boolean checkResultCorrectness(String opName) throws IOException {
		verifyFiles();
		boolean allCorrect = false;
		if(opName.equalsIgnoreCase("classification"))
			allCorrect = compareEntailmentSets(opName);
		else if(opName.equalsIgnoreCase("sat"))
			allCorrect = compareCSVResults(opName);
		else if(opName.equalsIgnoreCase("consistency"))
			allCorrect = compareCSVResults(opName);
		return allCorrect;
	}
	
	
	/**
	 * Verify if given files exist
	 */
	public void verifyFiles() {
		Set<File> toRemove = new HashSet<File>();
		for(File f : files) {
			if(!f.exists()) {
				map.put(getReasonerName(f),"nofile");
				toRemove.add(f);
			}
		}
		files.removeAll(toRemove);
	}
	
	
	/**
	 * Compare CSV-based results files (i.e. sat and consistency)
	 * @return true if all results are equal, false otherwise
	 * @throws IOException 
	 */
	private boolean compareCSVResults(String opName) throws IOException {
		boolean allCorrect = true;
		Set<File> equiv = new HashSet<File>();
		Set<File> non_equiv = new HashSet<File>();
		String ontName = "";
		for(int i = 0; i < files.size(); i++) {
			File f1 = files.get(i);
			if(ontName == "") ontName = f1.getParentFile().getName();
			for(int j = (i+1); j < files.size(); j++) {
				File f2 = files.get(j);
				boolean equal;
				if(opName.equals("sat")) equal = compareCSVLines(f1, f2);
				else equal = compareCSVLine(f1, f2);
				
				if(equal) {
					equiv.add(f1);
					equiv.add(f2);
				}
				else {
					allCorrect = false;
					non_equiv.add(f1);
					non_equiv.add(f2);
				}
			}
		}
//		produceOutput(ontName, opName, equiv, non_equiv);
		return allCorrect;
	}
	
	
	/**
	 * Compare sat results files
	 * @param f1	File 1
	 * @param f2	File 2
	 * @return true if both files report the same results for all sat tests
	 */
	private boolean compareCSVLines(File f1, File f2) {
		boolean equal = true;
		try {
			BufferedReader br1 = new BufferedReader(new FileReader(f1)), br2 = new BufferedReader(new FileReader(f2));
			String line1 = br1.readLine(), line2 = br2.readLine();
			while(line1 != null && line2 != null) {
				StringTokenizer tokenizer1 = new StringTokenizer(line1, ","), tokenizer2 = new StringTokenizer(line2, ",");
				
				String cName1 = tokenizer1.nextToken(), cName2 = tokenizer2.nextToken();
				String result1 = tokenizer1.nextToken(), result2 = tokenizer2.nextToken();
				
				if(cName1.equals(cName2) && !result1.equals(result2)) {
					equal = false;
					System.out.println(getComparisonStatement(f1, f2));
					System.out.println("Concept 1: " + cName1 + "\tConcept 2: " + cName2);
					System.out.println("Result 1: " + result1 + "\tResult 2: " + result2);
					System.out.println("   File 1 reports " + result1 + " for " + cName1 + " while File 2 reports " + result2 + "\n");
				}
				
				line1 = br1.readLine();
				line2 = br2.readLine();
			}
			br1.close(); br2.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return equal;
	}
	
	
	/**
	 * Compare consistency results files
	 * @param f1	File 1
	 * @param f2	File 2
	 * @return true if result is the same, false otherwise
	 */
	private boolean compareCSVLine(File f1, File f2) {
		boolean equal = true;
		try {
			BufferedReader br1 = new BufferedReader(new FileReader(f1)), br2 = new BufferedReader(new FileReader(f2));
			String line1 = br1.readLine(), line2 = br2.readLine();
			while(line1 != null && line2 != null) {
				if(!line1.equals(line2)) {
					equal = false;
					System.out.println(getComparisonStatement(f1, f2));
					System.out.println("   File 1 reports " + line1 + " while File 2 reports " + line2 + "\n");
				}
				line1 = br1.readLine();
				line2 = br2.readLine();
			}
			br1.close();
			br2.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return equal;
	}
	
	
	/**
	 * Verify whether all given files have the same entailments
	 * @return true if all files are equivalent, false otherwise
	 * @throws IOException 
	 */
	private boolean compareEntailmentSets(String opName) throws IOException {
		boolean allEquiv = true;
		String ontName = "", equivalent = "   Equivalent", 
				sep = "----------------------------------------------------";
		List<Set<File>> clusters = new ArrayList<Set<File>>();
		Set<File> clustered = new HashSet<File>();
		
		if(!files.isEmpty()) {
			System.out.println("\nComparing results files...\n");
			LinkedList<File> list = new LinkedList<File>(files);
			while(!list.isEmpty()) {
				File f1 = list.pop();
				clustered.add(f1);
				
				if(ontName == "") { ontName = f1.getParentFile().getName(); log.write(sep + "\nOntology: " + ontName); }
				OWLOntology ont1 = loadOntology(f1);
				if(ont1 == null)
					map.put(getReasonerName(f1), "unparseable");
				else if(!(ont1.getLogicalAxiomCount()>0))
					map.put(getReasonerName(f1), "empty");
				else {
					Set<File> f1Cluster = new HashSet<File>(Collections.singleton(f1));
					for(File f2 : files) {
						if(f1 != f2 && !clustered.contains(f2)) {
							OWLOntology ont2 = loadOntology(f2);
							if(ont2 == null) {
								map.put(getReasonerName(f2), "unparseable");
								list.remove(f2);
								clustered.add(f2);
							}
							else if(!(ont2.getLogicalAxiomCount()>0))
								map.put(getReasonerName(f2), "empty");
							else {
								printComparisonStatement(sep, f1, f2);
								ChangeSet cs = getDiff(ont1, ont2);
								if(!cs.isEmpty()) {
									Set<OWLAxiom> adds = getAdditions(cs), rems = getRemovals(cs);
									if(rems.isEmpty() && adds.isEmpty()) {
										System.out.println(equivalent); log.write(equivalent); // only ineffectual differences
										f1Cluster.add(f2);
										list.remove(f2);
										clustered.add(f2);
									}
									else {
										allEquiv = false;
										logChanges(f1, f2, adds, rems);
									}
								}
								else {
									System.out.println(equivalent); log.write(equivalent);
									f1Cluster.add(f2);
									list.remove(f2);
									clustered.add(f2);
								}
								System.out.println(sep);
								ont2.getOWLOntologyManager().removeOntology(ont2);
							}
						}
					}
					clusters.add(f1Cluster);
					ont1.getOWLOntologyManager().removeOntology(ont1);
				}
			}
		}
		else allEquiv = false;
	
		produceOutput(ontName, opName, clusters);
		return allEquiv;
	}
	
	
	/**
	 * Log differences between the two files
	 * @param f1	File 1
	 * @param f2	File 2
	 * @param rems	Set of removals
	 * @param adds	Set of additions
	 * @throws IOException
	 */
	private void logChanges(File f1, File f2, Set<OWLAxiom> rems, Set<OWLAxiom> adds) throws IOException {
		if(!rems.isEmpty()) {
			String s = "   " + getReasonerName(f1) + " outputs " + rems.size() + " extra entailment(s)";
			System.out.println(s);
			log.write(s + "\n");
			for(OWLAxiom ax : rems)
				log.write("\n\t" + ax);
		}
		if(!adds.isEmpty()) {
			String s = "   " + getReasonerName(f2) + " outputs " + adds.size() + " extra entailment(s)";
			System.out.println(s);
			log.write(s + "\n");
			for(OWLAxiom ax : adds)
				log.write("\n\t" + ax);
		}
	}
	
	
	/**
	 * Get the set of added axioms from the given change set
	 * @param cs	Change set
	 * @return Set of added axioms
	 */
	private Set<OWLAxiom> getAdditions(ChangeSet cs) {
		Set<OWLAxiom> out = null;
		if(cs instanceof LogicalChangeSet)
			out = ((LogicalChangeSet)cs).getEffectualAdditionAxioms();
		else if(cs instanceof StructuralChangeSet)
			out = ((StructuralChangeSet)cs).getAddedAxioms();
		return out;
	}
	
	
	/**
	 * Get the set of removed axioms from the given change set
	 * @param cs	Change set
	 * @return Set of removed axioms
	 */
	private Set<OWLAxiom> getRemovals(ChangeSet cs) {
		Set<OWLAxiom> out = null;
		if(cs instanceof LogicalChangeSet)
			out = ((LogicalChangeSet)cs).getEffectualRemovalAxioms();
		else if(cs instanceof StructuralChangeSet)
			out = ((StructuralChangeSet)cs).getRemovedAxioms();
		return out;
	}
	
	
	/**
	 * Print to stdout a summary of equivalent vs non-equivalent sets of output files
	 * @param ontName	Ontology name
	 * @param opName	Operation name
	 * @param correct	Set of correct result files
	 * @param incorrect	Set of incorrect result files
	 * @throws IOException 
	 */
//	private void produceOutput(String ontName, String opName, Set<File> correct, Set<File> incorrect) throws IOException {
//		Set<File> toRemove = new HashSet<File>();
//		for(File f : incorrect) {
//			if(correct.contains(f))
//				toRemove.add(f);
//		}
//		incorrect.removeAll(toRemove);
//		
//		boolean output[] = new boolean[16];
//
//		// Reasoners with no files		
//		updateList(output, new HashSet<File>(noFiles), false);
//		updateList(output, correct, true);
//		updateList(output, incorrect, false);
//		
//		String out = produceCSV(ontName, opName, output);
//		serialize(out, "results.csv");
//		
//		if(!incorrect.isEmpty()) {
//			log.write("\n\nSummary");
//			printSummary("Equivalent results", correct);
//			printSummary("Non-Equivalent results", incorrect);
//		}
//		
//		try {
//			log.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
	
	
	private void produceOutput(String ontName, String opName, List<Set<File>> clusters) throws IOException {
		int max = 0, index = 0;
		for(int i = 0; i<clusters.size(); i++) {
			Set<File> fileset = clusters.get(i);
			if(fileset.size() > max) {
				max = fileset.size();
				index = i;
			}
			String cluster = "Cluster " + (i+1) + " (size " + fileset.size() + "): ";
			System.out.print(cluster); log.write("\n" + cluster); 
			for(File f : fileset) {
				String r = getReasonerName(f) + " ";
				System.out.print(r);
				cluster += r;
			}
			System.out.println();
		}
		
		Set<File> correct = clusters.get(index);
		Set<File> incorrect = new HashSet<File>();
		for(int i = 0; i<clusters.size(); i++) {
			if(i!=index) {
				for(File f : clusters.get(i))
					incorrect.add(f);
			}
		}
		updateMap(correct, "true");
		updateMap(incorrect, "false");
		if(!incorrect.isEmpty()) {
			printSummary("  Equivalent (majority)", correct);
			printSummary("  Non Equivalent", incorrect);
		}
		serialize(generateCSV(ontName, opName), "results.csv");
		serializeClusterInfo(ontName, opName, clusters);
	}
	
	
	/**
	 * Update results map
	 * @param files	Set of files
	 * @param value	Map value for all files 
	 */
	private void updateMap(Set<File> files, String value) {
		for(File f : files)
			map.put(getReasonerName(f), value);
	}
	
	
	/**
	 * Generate a comma-separated row with the results in the map
	 * @param ontName	Ontology name
	 * @param opName	Operation name
	 * @return Comma-separated string with the results 
	 */
	private String generateCSV(String ontName, String opName) {
		String out = ontName + "," + opName + ",";
		for(String r : reasonerList) {
			out += map.get(r) + ",";
		}
		return out;
	}
	
	
	/**
	 * Get the set of reasoner names for all files
	 * @param files	Set of files
	 * @return Set containing the reasoner names corresponding to each file
	 */
	private Set<String> getReasonerNames(Set<File> files) {
		Set<String> out = new HashSet<String>();
		for(File f : files)
			out.add(getReasonerName(f));
		return out;
	}
	
	
	/**
	 * Get the reasoner name assuming standard folder structure
	 * @param f	File
	 * @return Reasoner name
	 */
	private String getReasonerName(File f) {
		return f.getParentFile().getParentFile().getParentFile().getName();
	}
	
	
	/**
	 * Verify whether two ontology files are equivalent, first w.r.t. structural diff and,
	 * subsequently, because of the equivalent vs dual subsumptions issue, logical diff
	 * @param ont1	Ontology 1
	 * @param ont2	Ontology 2
	 * @return Change set between the two given ontologies
	 */
	private ChangeSet getDiff(OWLOntology ont1, OWLOntology ont2) {
		ChangeSet changeSet = null;
		StructuralDiff sdiff = new StructuralDiff(ont1, ont2, false);
		changeSet = sdiff.getDiff();
		boolean structEquiv = sdiff.isEquivalent();

		if(!structEquiv) {
			LogicalDiff ldiff = new LogicalDiff(ont1, ont2, false);
			changeSet = ldiff.getDiff();
		}
		return changeSet;
	}	
	
	
	/**
	 * Load ontology file
	 * @param f	File
	 * @return OWLOntology
	 */
	private OWLOntology loadOntology(File f) {
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		OWLOntology ont = null;
		try {
			ont = man.loadOntologyFromOntologyDocument(f);
		} catch (OWLOntologyCreationException e) {
			System.out.println("! Unable to parse results file of: " + getReasonerName(f) + " (" + f.getAbsolutePath() + ")");
		}
		return ont;
	}
	
	
	/**
	 * List the given set of files
	 * @param desc	Description, i.e., equivalent or non-equivalent
	 * @param files	Set of files
	 * @throws IOException 
	 */
	private void printSummary(String desc, Set<File> files) throws IOException {
		System.out.println(desc + ":"); log.write("\n  " + desc + ":" + "\n");
		System.out.print("\t"); log.write("\t");
		for(File f : files) {
			System.out.print(getReasonerName(f) + " ");
			log.write(getReasonerName(f) + " ");
		}
		System.out.println();
	}
	
	
	/**
	 * Get file comparison statement
	 * @param f1	File 1
	 * @param f2	File 2
	 * @return String with the comparison statement
	 */
	private String getComparisonStatement(File f1, File f2) {
		return getReasonerName(f1) + " vs " + getReasonerName(f2);
	}
	
	
	/**
	 * Print file comparison statement
	 * @param sep	Separator string
	 * @param f1	File 1
	 * @param f2	File 2
	 * @throws IOException
	 */
	private void printComparisonStatement(String sep, File f1, File f2) throws IOException {
		String st = getComparisonStatement(f1, f2);
		System.out.println(st); log.write("\n" + sep + "\n" + st + "\n");
	}
	
	
	/**
	 * Generate the full list of reasoner names for ORE-2013
	 * @return List of reasoner names
	 */
	private List<String> getReasonerList() {
		String reasoners[] = {"basevisor","chainsaw","elephant","elk-loading-counted","elk-loading-not-counted","fact","hermit",
				"jcel","jfact","konclude","more-hermit","more-pellet","snorocket","treasoner","trowl","wsclassifier"};
		return new ArrayList<String>(Arrays.asList(reasoners));
	}
	
	
	/**
	 * Append a given string to the specified output file
	 * @param out	String to be flushed
	 */
	private void serialize(String out, String filename) {
		BufferedWriter br = initWriter(filename);
		try {
			br.write(out + "\n");
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Serialize a comma-separated file with the cluster information
	 * @param ontName	Ontology name
	 * @param opName	Operation name
	 * @param clusters	List of file clusters
	 */
	private void serializeClusterInfo(String ontName, String opName, List<Set<File>> clusters) {
		String out = ontName + "," + opName;
		for(Set<File> set : clusters) {
			out += ",";
			for(String r : getReasonerNames(set))
				out += r + " ";
		}
		serialize(out, "clusters.csv");
	}
	
	
	/**
	 * Initialize a buffered writer
	 * @param filename	Desired filename
	 * @return Buffered file writer
	 */
	private BufferedWriter initWriter(String filename) {
		BufferedWriter out = null;
		if(!outputFolder.endsWith(File.separator)) outputFolder += File.separator;
		try {
			File file = new File(outputFolder + filename);
			file.getParentFile().mkdirs();
			out = new BufferedWriter(new FileWriter(file, true));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return out;
	}
	
	
	/**
	 * Main
	 * @param 0	Operation name
	 * @param 1	Output folder
	 * @param 1..n	Results files
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String opName = args[0];
		String outputFile = args[1];
		
		List<File> files = new ArrayList<File>();
		for(int i = 2; i < args.length; i++)
			files.add(new File(args[i]));

		String ops[] = {"sat","classification","consistency"};
		List<String> opList = new ArrayList<String>(Arrays.asList(ops));
		if(opList.contains(opName)) {
			ResultComparator comp = new ResultComparator(files, outputFile);
			boolean allSame = comp.checkResultCorrectness(opName);
			if(allSame)
				System.out.println("\nAll results files are equivalent");
			else
				System.out.println("\nNot all results files are equivalent");
		}
		else
			System.out.println("! Unrecognized operation name: " + opName);
	}
}
