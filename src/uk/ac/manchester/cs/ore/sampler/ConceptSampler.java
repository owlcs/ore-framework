package uk.ac.manchester.cs.ore.sampler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

/**
 * @author Rafael S. Goncalves <br/>
 * Information Management Group (IMG) <br/>
 * School of Computer Science <br/>
 * University of Manchester <br/>
 */
public class ConceptSampler {
	private OWLOntology ont;
	private String output;
	private int sampleSize;
	
	/**
	 * Constructor
	 * @param ont	OWL ontology
	 * @param output	Output file path
	 */
	public ConceptSampler(OWLOntology ont, String output, int sampleSize) {
		this.ont = ont;
		this.output = output;
		this.sampleSize = sampleSize;
	}
	
	
	/**
	 * Get concept sample
	 * @return Set of classes in the sample
	 */
	public Set<OWLClass> getSample() {
		Set<OWLClass> sample = new HashSet<OWLClass>();
		List<OWLClass> list = new ArrayList<OWLClass>(ont.getClassesInSignature());
		System.out.println("\tNumber of classes: " + list.size());
		
		Collections.shuffle(list);
		for(int i = 0; i < sampleSize; i++) {
			if(list.get(i) != null)
				sample.add(list.get(i));
		}
		
		System.out.println("\tNumber of sample elements: " + sample.size());
		return sample;
	}
	
	
	/**
	 * Create a string with all concept URIs and serialize into specified output file
	 * @param sample	Concept sample
	 */
	public void serializeSample(Set<OWLClass> sample) {
		String out = "";
		for(OWLClass c : sample) 
			out += c.getIRI().toString() + "\n";
		
		File outFile = new File(output);
		outFile.getParentFile().mkdirs();
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));
			writer.append(out);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	/**
	 * Main
	 * @param 0	Ontology file path
	 * @param 1	Root folder for sat samples
	 * @param 2	Sample size
	 * @throws OWLOntologyCreationException 
	 */
	public static void main(String[] args) throws OWLOntologyCreationException {
		File f = new File(args[0]);
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		OWLOntology ont = man.loadOntologyFromOntologyDocument(f);
		System.out.println("Loaded ontology: " + f.getAbsolutePath());
		
		String outputDir = args[1];
		if(!outputDir.endsWith(File.separator)) outputDir += File.separator;
		
		String parentFolder = f.getParentFile().getName();
		if(!parentFolder.endsWith(File.separator)) parentFolder += File.separator;
		
		String output = outputDir + parentFolder + f.getName() + "_sat.txt";
		System.out.println("Output to: " + output);
		
		int sampleSize = Integer.parseInt(args[2]);
		ConceptSampler sampler = new ConceptSampler(ont, output, sampleSize);
		sampler.serializeSample(sampler.getSample());
		System.out.println("Done");
	}
}