package uk.ac.manchester.cs.ore.util;

import java.io.File;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLFunctionalSyntaxOntologyFormat;
import org.semanticweb.owlapi.io.OWLXMLOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

/**
 * @author Rafael S. Goncalves <br/>
 * Information Management Group (IMG) <br/>
 * School of Computer Science <br/>
 * University of Manchester <br/>
 * <p>
 * Simple syntax converter restricted to Functional and OWL/XML syntaxes.
 * </p>
 */
public class SyntaxConverter {

	/**
	 * Main
	 * 
	 * Parameter list (index positions):
	 * 0	Syntax, one of: functional | owlxml
	 * 1	Output folder
	 * 2	Ontology file(s)
	 */
	public static void main(String[] args) {
		String syntax = args[0];
		System.out.println("Chosen syntax: " + syntax ); 
		String out = args[1];
		if(!out.endsWith(File.separator)) out += File.separator;
		System.out.println("Output folder: " + out);
		
		int counter = 1;
		for(int i = 2; i < args.length; i++) {
			File f = new File(args[i]);
			System.out.println("Input " + counter + ": " + f.getName());
			OWLOntologyManager man = OWLManager.createOWLOntologyManager();
			OWLOntology ont = null;
			try {
				ont = man.loadOntologyFromOntologyDocument(f);
				System.out.println("\tLoaded ontology: " + f.getName());
			} catch (Exception e) {
				System.out.println("\tUnable to load ontology: " + f.getAbsolutePath());
				e.printStackTrace();
			}
			if(ont != null) {
				String output = out + f.getName();
				OWLOntologyFormat format = null;
				if(syntax.equals("functional"))
					format = new OWLFunctionalSyntaxOntologyFormat();
				else if(syntax.equals("owlxml"))
					format = new OWLXMLOntologyFormat();

				System.out.println("\tSerializing to: " + output);
				try {
					man.saveOntology(ont, format, IRI.create("file:" + output));
				} catch (OWLOntologyStorageException e) {
					e.printStackTrace();
				}
				man.removeOntology(ont); ont = null;
			}
			man = null;
			counter++;
			System.out.println("\tDone");
		}
		System.out.println("Finished");
	}
}
