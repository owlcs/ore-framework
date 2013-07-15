package uk.ac.manchester.cs.ore.output;

import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
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
		
		for(int i = 0; i < files.size(); i++) {
			File f1 = files.get(i);
			for(int j = 1; j < files.size(); j++) {
				File f2 = files.get(j);
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
						System.out.println("File " + f1.getAbsolutePath() + " has " + rems.size() + " extra entailments:");
						for(OWLAxiom ax : rems)
							System.out.println("\t" + getManchesterRendering(ax));
					}
					
					if(!adds.isEmpty()) {
						System.out.println("File " + f2.getAbsolutePath() + " has " + adds.size() + " extra entailments:");
						for(OWLAxiom ax : adds)
							System.out.println("\t" + getManchesterRendering(ax));
					}
					
					// Add them both to non-equivalent set. If one turns out to be Ok, it'll be handled later
					if(!non_equiv.contains(f1)) non_equiv.add(f1);
					if(!non_equiv.contains(f2)) non_equiv.add(f2);
				}
				else {
					if(!equiv.contains(f1)) equiv.add(f1);
					if(!equiv.contains(f2)) equiv.add(f2);
				}
			}
		}
		
		Set<File> toRemove = new HashSet<File>();
		for(File f : non_equiv) {
			if(equiv.contains(f))
				toRemove.add(f);
		}
		non_equiv.remove(toRemove);
		
		if(!non_equiv.isEmpty()) {
			printSummary("Equivalent files", equiv);
			printSummary("Non-Equivalent files", non_equiv);
		}
		return allEquiv;
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
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		try {
			OWLOntology result = man.loadOntologyFromOntologyDocument(file1);
			OWLOntology base = man.loadOntologyFromOntologyDocument(file2);
			
			StructuralDiff sdiff = new StructuralDiff(result, base, false);
			changeSet = sdiff.getDiff();
			boolean isEquivalent = sdiff.isEquivalent();
			
			if(!isEquivalent) {
				LogicalDiff ldiff = new LogicalDiff(result, base, false);
				changeSet = ldiff.getDiff();
				isEquivalent = ldiff.isEquivalent();
			}
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}
		return changeSet;
	}	
	
	
	private void printSummary(String desc, Set<File> files) {
		System.out.println(desc + ":");
		for(File f : files)
			System.out.println("\t" + f.getAbsolutePath());
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
		
		ResultComparator comp = new ResultComparator(files);
		boolean allEquiv = comp.checkResultCorrectness(opName);
		if(allEquiv)
			System.out.println("All results files are equivalent");
		else
			System.out.println("Not all results files are equivalent");
	}
}
