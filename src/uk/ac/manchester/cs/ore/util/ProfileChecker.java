package uk.ac.manchester.cs.ore.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.profiles.OWL2DLProfile;
import org.semanticweb.owlapi.profiles.OWL2ELProfile;
import org.semanticweb.owlapi.profiles.OWL2QLProfile;
import org.semanticweb.owlapi.profiles.OWL2RLProfile;

/**
 * @author Rafael S. Goncalves <br/>
 * Information Management Group (IMG) <br/>
 * School of Computer Science <br/>
 * University of Manchester <br/>
 * <p>
 * OWL 2 profile checker: checks whether the given ontology is in the EL, RL, QL or DL profile. It ignores missing term declarations.
 * </p>
 */
public class ProfileChecker {
	private OWLOntology ont;
	
	/**
	 * Constructor
	 * @param ont	OWL ontology
	 */
	public ProfileChecker(OWLOntology ont) {
		this.ont = ont;
	}
	
	
	/**
	 * Check if ontology is in the OWL 2 QL profile
	 * @return true if ontology is in OWL 2 QL, false otherwise
	 */
	public boolean isQL() {
		OWL2QLProfile qlProfile = new OWL2QLProfile();
		return qlProfile.checkOntology(ont).isInProfile();
	}
	
	
	/**
	 * Check if ontology is in the OWL 2 RL profile
	 * @return true if ontology is in OWL 2 RL, false otherwise
	 */
	public boolean isRL() {
		OWL2RLProfile rlProfile = new OWL2RLProfile();
		return rlProfile.checkOntology(ont).isInProfile();
	}
	
	
	/**
	 * Check if ontology is in the OWL 2 DL profile
	 * @return true if ontology is in OWL 2 DL, false otherwise
	 */
	public boolean isDL() {
		OWL2DLProfile dlProfile = new OWL2DLProfile();
		return dlProfile.checkOntology(ont).isInProfile();
	}
	
	
	/**
	 * Check if ontology is in the OWL 2 EL profile
	 * @return true if ontology is in OWL 2 EL, false otherwise
	 */
	public boolean isEL() {
		OWL2ELProfile elProfile = new OWL2ELProfile();
		return elProfile.checkOntology(ont).isInProfile();
	}
	
	
	/**
	 * Missing declarations throw off the profile detectors, this adds whichever are missing 
	 */
	public void addMissingDeclarations() {
		OWLOntologyManager man = ont.getOWLOntologyManager();
		OWLDataFactory df = man.getOWLDataFactory();
		List<AddAxiom> adds = new ArrayList<AddAxiom>();
		for(OWLEntity e : ont.getSignature()) {
			OWLDeclarationAxiom dec = df.getOWLDeclarationAxiom(e);
			if(!ont.containsAxiom(dec))
				adds.add(new AddAxiom(ont, dec));
		}
		man.applyChanges(adds);
		if(!adds.isEmpty())
			System.out.println("\tInjected " + adds.size() + " missing declarations");
	}
	
	
	/**
	 * Main
	 * 
	 * Parameter list (index positions):
	 * 0	Ontology filepath
	 */
	public static void main(String[] args) {
		File f = new File(args[0]);
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		OWLOntology ont = null;
		try {
			ont = man.loadOntologyFromOntologyDocument(f);
			System.out.println("Loaded ontology: " + f.getName());
		} catch (Exception e) {
			System.out.println("Unable to load ontology: " + f.getAbsolutePath());
			e.printStackTrace();
		}
		if(ont != null) {
			ProfileChecker checker = new ProfileChecker(ont);
			checker.addMissingDeclarations();
			System.out.println("Checking profiles:");
			System.out.println("\tDL: " + checker.isDL());
			System.out.println("\tEL: " + checker.isEL());
			System.out.println("\tRL: " + checker.isRL());
			System.out.println("\tQL: " + checker.isQL());
		}
		System.out.println("Done");
	}
}
