package uk.ac.manchester.cs.ore.output;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;

import uk.ac.manchester.cs.diff.axiom.LogicalDiff;
import uk.ac.manchester.cs.diff.axiom.StructuralDiff;
import uk.ac.manchester.cs.diff.axiom.changeset.ChangeSet;
import uk.ac.manchester.cs.diff.axiom.changeset.LogicalChangeSet;
import uk.ac.manchester.cs.diff.axiom.changeset.StructuralChangeSet;
import uk.ac.manchester.cs.owl.owlapi.mansyntaxrenderer.ManchesterOWLSyntaxObjectRenderer;

/**
 * @author Rafael S. Goncalves <br/>
 * Information Management Group (IMG) <br/>
 * School of Computer Science <br/>
 * University of Manchester <br/>
 */
public class ResultComparator {
	private List<File> files, noFiles;
	private SimpleShortFormProvider p;
	private List<String> reasonerList;
	private String outputFile;
	
	/**
	 * Constructor
	 * @param files	Set of results files
	 */
	public ResultComparator(List<File> files, List<File> noFiles, String outputFile) {
		this.files = files;
		this.noFiles = noFiles;
		this.outputFile = outputFile;
		p = new SimpleShortFormProvider();
		reasonerList = getReasonerList();
	}
	
	
	/**
	 * Verify whether the given output is the correct one w.r.t. a base file
	 * @param opName	Operation name
	 * @param resultFile	Results file
	 * @param baseFile	Base file
	 * @return true if the result is correct, false otherwise
	 */
	public boolean checkResultCorrectness(String opName) {
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
	 */
	private boolean compareCSVResults(String opName) {
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
					printComparisonStatement(f1, f2);
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
					printComparisonStatement(f1, f2);
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
	 */
	private boolean compareEntailmentSets(String opName) {
		boolean allEquiv = true;
		Set<File> equiv = new HashSet<File>();
		Set<File> non_equiv = new HashSet<File>();
		String ontName = "";
		if(!files.isEmpty()) {
			for(int i = 0; i < files.size(); i++) {
				File f1 = files.get(i);
				if(ontName == "") ontName = f1.getParentFile().getName();
				for(int j = (i+1); j < files.size(); j++) {
					File f2 = files.get(j);
					printComparisonStatement(f1, f2);
					ChangeSet cs = getDiff(f1, f2);
					if(!cs.isEmpty()) {
						allEquiv = false;
						Set<OWLAxiom> adds = null, rems = null;
						if(cs instanceof LogicalChangeSet) {
							adds = ((LogicalChangeSet)cs).getEffectualAdditionAxioms();
							rems = ((LogicalChangeSet)cs).getEffectualRemovalAxioms();
						}
						else if(cs instanceof StructuralChangeSet) {
							adds = ((StructuralChangeSet)cs).getAddedAxioms();
							rems = ((StructuralChangeSet)cs).getRemovedAxioms();
						}
						if(!rems.isEmpty()) {
							System.out.println("   File 1 has " + rems.size() + " extra entailments:");
							for(OWLAxiom ax : rems)
								System.out.println("\t" + getManchesterRendering(ax));
						}

						if(!adds.isEmpty()) {
							System.out.println("   File 2 has " + adds.size() + " extra entailments:");
							for(OWLAxiom ax : adds)
								System.out.println("\t" + getManchesterRendering(ax));
						}

						// Add them both to non-equivalent set. If one turns out to be Ok w.r.t. the remainder, it'll be handled later
						if(!non_equiv.contains(f1)) non_equiv.add(f1);
						if(!non_equiv.contains(f2)) non_equiv.add(f2);
					}
					else {
						System.out.println("   Equivalent");
						if(!equiv.contains(f1)) equiv.add(f1);
						if(!equiv.contains(f2)) equiv.add(f2);
					}

					System.out.println("--------------------------------------------------------");
				}
			}
		}
		else allEquiv = false;
		
		produceOutput(ontName, opName, equiv, non_equiv);
		return allEquiv;
	}
	
	
	/**
	 * Print to stdout a summary of equivalent vs non-equivalent sets of output files
	 * @param ontName	Ontology name
	 * @param opName	Operation name
	 * @param equiv	Set of equivalent result files
	 * @param non_equiv	Set of non-equivalent result files
	 */
	private void produceOutput(String ontName, String opName, Set<File> equiv, Set<File> non_equiv) {
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
		serialize(out);
		
		if(!non_equiv.isEmpty()) {
			printSummary("  Equivalent files", equiv);
			printSummary("  Non-Equivalent files", non_equiv);
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
	 * @param file1	1st file
	 * @param file2	2nd file
	 * @return true if ontologies are logically equivalent
	 */
	private ChangeSet getDiff(File file1, File file2) {
		ChangeSet changeSet = null;
		OWLOntologyManager man1 = OWLManager.createOWLOntologyManager();
		OWLOntologyManager man2 = OWLManager.createOWLOntologyManager();
		try {
			OWLOntology ont1 = man1.loadOntologyFromOntologyDocument(file1);
			OWLOntology ont2 = man2.loadOntologyFromOntologyDocument(file2);
			
			StructuralDiff sdiff = new StructuralDiff(ont1, ont2, false);
			changeSet = sdiff.getDiff();
			boolean isEquivalent = sdiff.isEquivalent();
			
			if(!isEquivalent) {
				LogicalDiff ldiff = new LogicalDiff(ont1, ont2, false);
				changeSet = ldiff.getDiff();
				isEquivalent = ldiff.isEquivalent();
			}
			man1.removeOntology(ont1);
			man2.removeOntology(ont2);
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}
		return changeSet;
	}	
	
	
	/**
	 * List the given set of files
	 * @param desc	Description, i.e., equivalent or non-equivalent
	 * @param files	Set of files
	 */
	private void printSummary(String desc, Set<File> files) {
		System.out.println(desc + ":");
		for(File f : files)
			System.out.println("\t" + f.getAbsolutePath());
		System.out.println();
	}
	
	
	/**
	 * Print file comparison statement
	 * @param f1	File 1
	 * @param f2	File 2
	 */
	private void printComparisonStatement(File f1, File f2) {
		System.out.println("File 1: " + f1.getAbsolutePath() + "  VS  File 2: " + f2.getAbsolutePath());
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
	private void serialize(String out) {
		try {
			BufferedWriter br = new BufferedWriter(new FileWriter(new File(outputFile), true));
			br.write(out + "\n");
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Get Manchester syntax of an OWL object
	 * @param obj	OWL object
	 * @return A string with the object's conversion to Manchester syntax 
	 */
	public String getManchesterRendering(OWLObject obj) {
		StringWriter wr = new StringWriter();
		ManchesterOWLSyntaxObjectRenderer render = new ManchesterOWLSyntaxObjectRenderer(wr, p);
		obj.accept(render);

		String str = wr.getBuffer().toString();
		str = str.replace("<", "");
		str = str.replace(">", "");
		return str;
	}
	
	
	/**
	 * Main
	 * @param 0	Operation name
	 * @param 1	Output file
	 * @param 1..n	Results files
	 */
	public static void main(String[] args) {
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
				System.out.println("All results files are equivalent");
			else
				System.out.println("Not all results files are equivalent");
		}
		else
			System.out.println("! Unrecognized operation name: " + opName);
	}
}
