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
import java.util.HashSet;
import java.util.List;
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
	private List<File> files, noFiles;
	private List<String> reasonerList;
	private String outputFolder;
	private BufferedWriter log;
	
	/**
	 * Constructor
	 * @param files	Set of results files
	 */
	public ResultComparator(List<File> files, List<File> noFiles, String outputFile) {
		this.files = files;
		this.noFiles = noFiles;
		this.outputFolder = outputFile;
		reasonerList = getReasonerList();
		initLogWriter();
	}
	
	
	/**
	 * Verify whether the given output is the correct one w.r.t. a base file
	 * @param opName	Operation name
	 * @param resultFile	Results file
	 * @param baseFile	Base file
	 * @return true if the result is correct, false otherwise
	 * @throws IOException 
	 */
	public boolean checkResultCorrectness(String opName) throws IOException {
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
		produceOutput(ontName, opName, equiv, non_equiv);
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
		Set<File> equiv = new HashSet<File>();
		Set<File> non_equiv = new HashSet<File>();
		String equivalent = "   Equivalent", sep = "----------------------------------------------------";
		String ontName = "";
		if(!files.isEmpty()) {
			System.out.println("\nComparing results files...\n");
			for(int i = 0; i < files.size(); i++) {
				File f1 = files.get(i);
				if(ontName == "") {
					ontName = f1.getParentFile().getName();
					log.write(sep + "\nOntology: " + ontName); 
				}
				int equivTo = 0, nonEquivTo = 0;
				OWLOntology ont1 = loadOntology(f1);
				if(ont1 == null) non_equiv.add(f1);
				else {
					for(int j = (i+1); j < files.size(); j++) {
						File f2 = files.get(j);
						OWLOntology ont2 = loadOntology(f2);
						if(ont2 == null) non_equiv.add(f2);
						else {
							String st = getComparisonStatement(f1, f2);
							System.out.println(st); log.write("\n" + sep + "\n" + st + "\n");

							ChangeSet cs = getDiff(ont1, ont2);
							if(!cs.isEmpty()) {
								Set<OWLAxiom> adds = getAdditions(cs), rems = getRemovals(cs);
								if(rems.isEmpty() && adds.isEmpty()) {
									System.out.println(equivalent); log.write(equivalent); // only ineffectual differences
									equiv.add(f1); equiv.add(f2);
									equivTo++;
								}
								else {
									allEquiv = false;
									if(!rems.isEmpty()) {
										String s = "   " + getReasonerName(f1) + " outputs " + rems.size() + " extra entailments";
										System.out.println(s);
										log.write(s + "\n");
										for(OWLAxiom ax : rems)
											log.write("\n\t" + ax);
									}
									if(!adds.isEmpty()) {
										String s = "   " + getReasonerName(f2) + " outputs " + adds.size() + " extra entailments";
										System.out.println(s);
										log.write(s + "\n");
										for(OWLAxiom ax : adds)
											log.write("\n\t" + ax);
									}
									// Add them both to non-equivalent set. If one turns out to be Ok w.r.t. the remainder, it'll be handled later
									non_equiv.add(f1); non_equiv.add(f2);
									nonEquivTo++;
								}
							}
							else {
								System.out.println(equivalent); log.write(equivalent);
								equiv.add(f1); equiv.add(f2);
								equivTo++;
							}
							System.out.println(sep);
						}
					}
				}
				
				if(nonEquivTo > equivTo) {
					equiv.remove(f1);
					non_equiv.add(f1);
				}
			}
		}
		else allEquiv = false;
		
		produceOutput(ontName, opName, equiv, non_equiv);
		return allEquiv;
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
	 * @param equiv	Set of equivalent result files
	 * @param non_equiv	Set of non-equivalent result files
	 * @throws IOException 
	 */
	private void produceOutput(String ontName, String opName, Set<File> equiv, Set<File> non_equiv) throws IOException {
		Set<File> toRemove = new HashSet<File>();
		for(File f : non_equiv) {
			if(equiv.contains(f))
				toRemove.add(f);
		}
		non_equiv.removeAll(toRemove);
		
		boolean output[] = new boolean[16];

		// Reasoners with no files		
		updateList(output, new HashSet<File>(noFiles), false);
		updateList(output, equiv, true);
		updateList(output, non_equiv, false);
		
		String out = produceCSV(ontName, opName, output);
		serialize(out, "results.csv");
		
		if(!non_equiv.isEmpty()) {
			log.write("\n\nSummary");
			printSummary("Equivalent results", equiv);
			printSummary("Non-Equivalent results", non_equiv);
		}
		
		try {
			log.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Generate a comma-separated line with the results for all reasoners
	 * @param ontName	Ontology name
	 * @param opName	Operation name
	 * @param output	Result list
	 * @return Comma-separated string with all results
	 */
	private String produceCSV(String ontName, String opName, boolean[] output) {
		String out = ontName + "," + opName + ",";
		for(int i = 0; i < output.length; i++)
			out += output[i] + ",";
		return out;
	}
	
	
	/**
	 * Update list with the given reasoners and result
	 * @param output	Result list
	 * @param names	Set of reasoner names
	 * @param answer	Correctness answer
	 * @return Updated list of results
	 */
	private boolean[] updateList(boolean[] output, Set<File> files, boolean answer) {
		for(String reasoner : getReasonerNames(files)) {
			if(reasonerList.contains(reasoner)) 
				output[reasonerList.indexOf(reasoner)] = answer;
			else 
				System.out.println("Unknown reasoner: " + reasoner);
		}
		return output;
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
	 * @return true if ontologies are logically equivalent
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
		ont1.getOWLOntologyManager().removeOntology(ont1);
		ont2.getOWLOntologyManager().removeOntology(ont2);
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
			System.out.println("! Unable to parse results file of: " + getReasonerName(f) + "(" + f.getAbsolutePath() + ")");
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
	 * Print file comparison statement
	 * @param f1	File 1
	 * @param f2	File 2
	 */
	private String getComparisonStatement(File f1, File f2) {
		return getReasonerName(f1) + " VS " + getReasonerName(f2);
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
		if(!outputFolder.endsWith(File.separator)) outputFolder += File.separator;
		try {
			BufferedWriter br = new BufferedWriter(new FileWriter(new File(outputFolder + filename), true));
			br.write(out + "\n");
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Initialize log output writer
	 */
	private void initLogWriter() {
		if(!outputFolder.endsWith(File.separator)) outputFolder += File.separator;
		try {
			log = new BufferedWriter(new FileWriter(new File(outputFolder + "log.txt"), true));
		} catch (IOException e) {
			e.printStackTrace();
		}
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
		List<File> noFiles = new ArrayList<File>();
		for(int i = 2; i < args.length; i++) {
			File f = new File(args[i]);
			if(f.exists()) files.add(f);
			else noFiles.add(f);
		}

		String ops[] = {"sat","classification","consistency"};
		List<String> opList = new ArrayList<String>(Arrays.asList(ops));
		if(opList.contains(opName)) {
			ResultComparator comp = new ResultComparator(files, noFiles, outputFile);
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
