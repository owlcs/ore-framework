/*******************************************************************************
 * This file is part of ecco.
 * 
 * ecco is distributed under the terms of the GNU Lesser General Public License (LGPL), Version 3.0.
 *  
 * Copyright 2011-2013, The University of Manchester
 *  
 * ecco is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 *  
 * ecco is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even 
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser 
 * General Public License for more details.
 *  
 * You should have received a copy of the GNU Lesser General Public License along with ecco.
 * If not, see http://www.gnu.org/licenses/.
 ******************************************************************************/
package uk.ac.manchester.cs.diff.axiom;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import uk.ac.manchester.cs.diff.axiom.changeset.StructuralChangeSet;

/**
 * @author Rafael S. Goncalves <br/>
 * Information Management Group (IMG) <br/>
 * School of Computer Science <br/>
 * University of Manchester <br/>
 */
public class StructuralDiff implements AxiomDiff {
	private OWLOntology ont1, ont2;
	private String ont1name, ont2name;
	private StructuralChangeSet changeSet;
	private ThreadMXBean bean;
	private double diffTime;
	private boolean verbose;
	
	/**
	 * Constructor
	 * @param ont1	Ontology 1
	 * @param ont2	Ontology 2
	 */
	public StructuralDiff(OWLOntology ont1, OWLOntology ont2, boolean verbose) {
		this.ont1 = ont1;
		this.ont2 = ont2;
		this.verbose = verbose;
		bean = ManagementFactory.getThreadMXBean();
	}
	
	
	/**
	 * Constructor 2
	 * @param ont1	Ontology 1
	 * @param ont2	Ontology 2
	 */
	public StructuralDiff(OWLOntology ont1, OWLOntology ont2, String ont1name, String ont2name, boolean verbose) {
		this.ont1 = ont1;
		this.ont2 = ont2;
		this.ont1name = ont1name;
		this.ont2name = ont2name;
		this.verbose = verbose;
		bean = ManagementFactory.getThreadMXBean();
	}
	
	
	/**
	 * Get structural changes between ontologies
	 * @return Structural change set
	 */
	@SuppressWarnings("deprecation")
	public StructuralChangeSet getDiff() {
		if(changeSet != null) return changeSet;
		
		if(verbose) System.out.print("Computing structural diff... ");
		Set<OWLLogicalAxiom> o1axs = ont1.getLogicalAxioms();
		Set<OWLLogicalAxiom> o2axs = ont2.getLogicalAxioms();
		
		Set<OWLAxiom> additions = new HashSet<OWLAxiom>();
		Set<OWLAxiom> removals = new HashSet<OWLAxiom>();
		Set<OWLAxiom> shared = new HashSet<OWLAxiom>();
		
		long start = bean.getCurrentThreadCpuTime();
		for(OWLAxiom ax : o1axs) {
			if(!o2axs.contains(ax)) {
				if(ax instanceof OWLSubClassOfAxiom) {
					if(!((OWLSubClassOfAxiom)ax).getSuperClass().isTopEntity())
						removals.add(ax);
				}
				else removals.add(ax);
			}
			else shared.add(ax);
		}
		o2axs.removeAll(shared);
		for(OWLAxiom ax : o2axs) {
			if(!o1axs.contains(ax)) {
				if(ax instanceof OWLSubClassOfAxiom) {
					if(!((OWLSubClassOfAxiom)ax).getSuperClass().isTopEntity())
						additions.add(ax);
				} 
				else additions.add(ax);
			}
			else shared.add(ax);
		}
		
		long end = bean.getCurrentThreadCpuTime();
		diffTime = (end-start)/1000000000.0;

		changeSet = new StructuralChangeSet(additions, removals, shared);
		addOntologyFileNames(); changeSet.setDiffTime(diffTime);
		
		if(verbose) { System.out.println("done"); printDiff(); }
		return changeSet;
	}
	
	
	/**
	 * Print diff results
	 */
	public void printDiff() {
		System.out.println("   Structural diff time: " + diffTime + " seconds");
		System.out.println("   Structural changes:" + 
				"\n\tAdditions: " + changeSet.getAddedAxioms().size() +
				"\n\tRemovals: " + changeSet.getRemovedAxioms().size() + 
				"\n\tShared: " + changeSet.getShared().size());
	}
	
	
	/**
	 * Record file names of given ontologies in the change set 
	 */
	@SuppressWarnings("deprecation")
	private void addOntologyFileNames() {
		if(ont1name == null) changeSet.setOntologyName(1, "Ont1");
		else changeSet.setOntologyName(1, ont1name);
		
		if(ont2name == null) changeSet.setOntologyName(2, "Ont2");
		else changeSet.setOntologyName(2, ont2name);
	}
	
	
	/**
	 * Determine if ontologies are structurally equivalent (thus logically equivalent)
	 * @return true if ontologies are structurally equivalent, false otherwise
	 */
	public boolean isEquivalent() {
		if(changeSet == null) changeSet = getDiff();
		
		if(changeSet.isEmpty()) return true;
		else return false;
	}
	
	
	/**
	 * Get the time to compute the diff
	 * @return Diff time (in seconds)
	 */
	public double getDiffTime() {
		return diffTime;
	}
}
