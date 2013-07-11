package uk.ac.manchester.cs.ore.output;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
	
	/**
	 * Constructor
	 * @param files	Set of results files
	 */
	public ResultComparator(List<File> files) {
		this.files = files;
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
			allCorrect = sameEntailments();
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
	private boolean sameEntailments() {
		boolean allEquiv = true;
		for(int i = 0; i < files.size(); i++) {
			File f1 = files.get(i), f2 = files.get(i+1);
			ChangeSet cs = getDiff(f1, f2);
			if(!cs.isEmpty()) {
				allEquiv = false;
				System.out.println("File " + f1.getName() + " different from " + f2.getName());
				
				Set<OWLAxiom> adds = null, rems = null;
				if(cs instanceof LogicalChangeSet) {
					adds = ((LogicalChangeSet)cs).getEffectualAdditionAxioms();
					rems = ((LogicalChangeSet)cs).getEffectualRemovalAxioms();
				}
				else if(cs instanceof StructuralChangeSet) {
					adds = ((StructuralChangeSet)cs).getAddedAxioms();
					rems = ((StructuralChangeSet)cs).getRemovedAxioms();
				}
				// TODO
			}		
		}
		return allEquiv;
	}
	
	
	/**
	 * Verify whether two ontology files are equivalent, first w.r.t. structural diff and,
	 * subsequently, because of the equivalent vs dual subsumptions issue, logical diff
	 * @param resultFile	Results file
	 * @param baseFile	Base file
	 * @return true if ontologies are logically equivalent
	 */
	private ChangeSet getDiff(File resultFile, File baseFile) {
		ChangeSet changeSet = null;
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		try {
			OWLOntology result = man.loadOntologyFromOntologyDocument(resultFile);
			OWLOntology base = man.loadOntologyFromOntologyDocument(baseFile);
			
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
			// TODO: minority vs majority reports
	}
}
