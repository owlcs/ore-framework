package uk.ac.manchester.cs.ore.util;

import java.io.File;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLFunctionalSyntaxOntologyFormat;
import org.semanticweb.owlapi.io.OWLXMLOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

/**
 * @author Rafael S. Goncalves <br/>
 * Information Management Group (IMG) <br/>
 * School of Computer Science <br/>
 * University of Manchester <br/>
 */
public class SyntaxConverter {

	/**
	 * @param 0: ontology file
	 * @param 1: output folder
	 * @param 2: syntax, one of: functional | owlxml
	 */
	public static void main(String[] args) {
		File f = new File(args[0]);
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		OWLOntology ont = null;
		try {
			ont = man.loadOntologyFromOntologyDocument(f);
			System.out.println("Loaded ontology");
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}
		if(ont != null) {
			String out = args[1];
			if(!out.endsWith(File.separator)) out += File.separator;
			out += f.getName();
			
			String syntax = args[2];
			try {
				System.out.println("Serializing...");
				OWLOntologyFormat format = null;
				if(syntax.equals("functional"))
					format = new OWLFunctionalSyntaxOntologyFormat();
				else if(syntax.equals("owlxml"))
					format = new OWLXMLOntologyFormat();

				man.saveOntology(ont, format, IRI.create("file:" + out));
			} catch (OWLOntologyStorageException e) {
				e.printStackTrace();
			}
		}
		System.out.println("Done");
	}
}
