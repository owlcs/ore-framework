package uk.ac.manchester.cs.ore.output;

import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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
	private List<File> files;
	private SimpleShortFormProvider p;
	
	/**
	 * Constructor
	 * @param files	Set of results files
	 */
	public ResultComparator(List<File> files) {
		this.files = files;
		p = new SimpleShortFormProvider();
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
			allCorrect = compareEntailmentSets();
		else if(opName.equalsIgnoreCase("sat")) {
			// TODO
		}
		else if(opName.equalsIgnoreCase("consistency")) {
			// TODO
		}
		return allCorrect;
	}
	
	
	/**
	 * Verify whether all given files have the same entailments
	 * @return true if all files are equivalent, false otherwise
	 */
	private boolean compareEntailmentSets() {
		boolean allEquiv = true;
		Set<File> equiv = new HashSet<File>();
		Set<File> non_equiv = new HashSet<File>();
		String ontName = "";
		
		for(int i = 0; i < files.size(); i++) {
			File f1 = files.get(i);
			if(ontName == "") ontName = f1.getParentFile().getName();
			for(int j = (i+1); j < files.size(); j++) {
				File f2 = files.get(j);
				System.out.println("File 1: " + f1.getAbsolutePath() + "  VS  File 2: " + f2.getAbsolutePath());
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
		outputSummary(ontName, "classification", equiv, non_equiv);
		return allEquiv;
	}
	
	
	private void outputSummary(String ontName, String opName, Set<File> equiv, Set<File> non_equiv) {
		Set<File> toRemove = new HashSet<File>();
		for(File f : non_equiv) {
			if(equiv.contains(f))
				toRemove.add(f);
		}
		non_equiv.removeAll(toRemove);
		if(!non_equiv.isEmpty()) {
			printSummary("  Equivalent files", equiv);
			printSummary("  Non-Equivalent files", non_equiv);
		}
		
		String csv = ontName + "," + opName + "," + getReasonerNames(equiv) + "," + getReasonerNames(non_equiv) + "\n";
		System.out.println("CSV: " + csv);
	}
	
	
	/**
	 * Get the reasoner name for each given file, and pipe it to a string
	 * @param files	Set of files
	 * @return String containing the reasoner names corresponding to each file
	 */
	private String getReasonerNames(Set<File> files) {
		String out = "";
		Iterator<File> it = files.iterator();
		while(it.hasNext()) {
			File f = it.next();
			out += f.getParentFile().getParentFile().getParentFile().getName(); // reasoner name assuming standard folder structure
			if(it.hasNext()) out += " : ";
		}
		return out;
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
	 * @param 1..n	Results files
	 */
	public static void main(String[] args) {
		String opName = args[0];
		List<File> files = new ArrayList<File>();
		for(int i = 0; i < args.length; i++) {
			File f = new File(args[i]);
			if(f.exists())
				files.add(f);
		}

		String ops[] = {"sat","classification","consistency"};
		List<String> opList = new ArrayList<String>(Arrays.asList(ops));
		if(opList.contains(opName)) {
			ResultComparator comp = new ResultComparator(files);
			boolean allEquiv = comp.checkResultCorrectness(opName);
			if(allEquiv)
				System.out.println("All results files are equivalent");
			else
				System.out.println("Not all results files are equivalent");
		}
		else
			System.err.println("Unrecognized operation name: " + opName);
	}
}
