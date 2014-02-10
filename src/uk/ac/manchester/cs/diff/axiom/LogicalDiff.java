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

import java.util.HashSet;
import java.util.Set;

import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import uk.ac.manchester.cs.diff.axiom.changeset.LogicalChangeSet;
import uk.ac.manchester.cs.diff.axiom.changeset.StructuralChangeSet;
import uk.ac.manchester.cs.diff.output.XMLReport;

/**
 * @author Rafael S. Goncalves <br/>
 * Information Management Group (IMG) <br/>
 * School of Computer Science <br/>
 * University of Manchester <br/>
 */
public class LogicalDiff implements AxiomDiff {
	private OWLOntology ont1, ont2;
	private StructuralChangeSet structChangeSet;
	private LogicalChangeSet logicalChangeSet;
	private OWLReasoner ont1reasoner, ont2reasoner;
	private boolean verbose;
	
	/**
	 * Constructor
	 * @param ont1	Ontology 1
	 * @param ont2	Ontology 2
	 * @param verbose	true if all output messages should be printed, false otherwise
	 */
	public LogicalDiff(OWLOntology ont1, OWLOntology ont2, boolean verbose) {
		this.ont1 = ont1;
		this.ont2 = ont2;
		this.verbose = verbose;
	}
	
	
	/**
	 * Constructor that takes a structural change set
	 * @param ont1	Ontology 1
	 * @param ont2	Ontology 2
	 * @param changeSet	Structural change set
	 * @param verbose	true if all output messages should be printed, false otherwise
	 */
	public LogicalDiff(OWLOntology ont1, OWLOntology ont2, StructuralChangeSet changeSet, boolean verbose) {
		this.ont1 = ont1;
		this.ont2 = ont2;
		this.structChangeSet = changeSet;
		this.verbose = verbose;
	}

	
	/**
	 * Get logical changes between ontologies given a reasoner instance per ontology
	 * @param ont1reasoner	Instance of a reasoner loaded with ontology 1 
	 * @param ont2reasoner	Instance of a reasoner loaded with ontology 2
	 * @return Logical change set
	 */
	public LogicalChangeSet getDiff(OWLReasoner ont1reasoner, OWLReasoner ont2reasoner) {
		this.ont1reasoner = ont1reasoner;
		this.ont2reasoner = ont2reasoner;
		return getDiff();
	}

	
	/**
	 * Get logical changes between ontologies
	 * @return Logical change set
	 */
	public LogicalChangeSet getDiff() {
		if(logicalChangeSet != null) return logicalChangeSet;
		if(structChangeSet == null) structChangeSet = new StructuralDiff(ont1, ont2, verbose).getDiff();
		
		if(ont1reasoner == null) ont1reasoner = new Reasoner(ont1);
		if(ont2reasoner == null) ont2reasoner = new Reasoner(ont2);
		
		if(verbose) System.out.print("Computing logical diff... ");
		
		Set<OWLAxiom> ineffectualAdditions = getIneffectualChanges(structChangeSet.getAddedAxioms(), ont1reasoner);
		Set<OWLAxiom> effectualAdditions = new HashSet<OWLAxiom>(structChangeSet.getAddedAxioms());
		effectualAdditions.removeAll(ineffectualAdditions);
		
		Set<OWLAxiom> ineffectualRemovals = getIneffectualChanges(structChangeSet.getRemovedAxioms(), ont2reasoner);
		Set<OWLAxiom> effectualRemovals = new HashSet<OWLAxiom>(structChangeSet.getRemovedAxioms());
		effectualRemovals.removeAll(ineffectualRemovals);
		
		effectualAdditions.removeAll(pruneChanges(effectualAdditions));
		effectualRemovals.removeAll(pruneChanges(effectualRemovals));
	
		logicalChangeSet = new LogicalChangeSet(effectualAdditions, ineffectualAdditions, effectualRemovals, ineffectualRemovals);

		if(verbose) { System.out.println("done"); printDiff(); }
		return logicalChangeSet;
	}
	
	
	/**
	 * Prune set of changes (for ORE)
	 * @param axioms	Set of axioms
	 * @return Axioms to remove
	 */
	public Set<OWLAxiom> pruneChanges(Set<OWLAxiom> axioms) {
		Set<OWLAxiom> toRemove = new HashSet<OWLAxiom>();
		for(OWLAxiom ax : axioms) {
			if(ax.isOfType(AxiomType.RBoxAxiomTypes) || ax.isOfType(AxiomType.ABoxAxiomTypes))
				toRemove.add(ax);
			
			/* 
			 * On 20 July the commented lines below were introduced. They didn't exist before. 
			 * For live competition only!? More investigation needed...
			 */
//			else if(ax.isOfType(AxiomType.EQUIVALENT_CLASSES)) 
//				toRemove.add(ax);
			
			// TODO: deal with equivalences vs dual subsumptions
			
			else if(ax.isOfType(AxiomType.SUBCLASS_OF)) {
				if( ((OWLSubClassOfAxiom)ax).getSubClass().isBottomEntity() || 
						((OWLSubClassOfAxiom)ax).getSuperClass().isTopEntity())
					toRemove.add(ax);
			}
		}
		return toRemove;
	}
	

	/**
	 * Get ineffectual changes
	 * @param axioms	Set of axioms to check
	 * @param ont	OWL ontology
	 * @return Set of ineffectual changes
	 */
	private Set<OWLAxiom> getIneffectualChanges(Set<OWLAxiom> axioms, OWLReasoner reasoner) {
		Set<OWLEntity> ontSig = reasoner.getRootOntology().getSignature();
		Set<OWLAxiom> ineffectual = new HashSet<OWLAxiom>();
		for(OWLAxiom axiom : axioms) {
			if(ontSig.containsAll(axiom.getSignature())) {
				if(reasoner.isEntailed(axiom))
					ineffectual.add(axiom);
			}
		}
		return ineffectual;
	}
	
	
	/**
	 * Print diff results
	 */
	public void printDiff() {
		System.out.println("   Logical changes:" + 
				"\n\tEffectual Additions: " + logicalChangeSet.getEffectualAdditionAxioms().size() +
				"\n\tEffectual Removals: " + logicalChangeSet.getEffectualRemovalAxioms().size() + 
				"\n\tIneffectual Additions: " + logicalChangeSet.getIneffectualAdditionAxioms().size() +
				"\n\tIneffectual Removals: " + logicalChangeSet.getIneffectualRemovalAxioms().size());
	}
	
	
	/**
	 * Get an XML change report for the change set computed by this diff
	 * @return XML change report object
	 */
	public XMLReport getXMLReport() {
		if(logicalChangeSet == null) logicalChangeSet = getDiff();
		return new XMLReport(ont1, ont2, logicalChangeSet);
	}
	
	
	/**
	 * Determine if ontologies are logically equivalent
	 * @return true if ontologies are logically equivalent, false otherwise
	 */
	public boolean isEquivalent() {
		if(logicalChangeSet == null) logicalChangeSet = getDiff();
		if(logicalChangeSet.getEffectualAdditionAxioms().isEmpty() && logicalChangeSet.getEffectualRemovalAxioms().isEmpty()) 
			return true;
		else 
			return false;
	}
	
	
	/**
	 * Convenience method to get the StructuralChangeSet
	 * @return Structural diff change set
	 */
	public StructuralChangeSet getStructuralChangeSet() {
		if(structChangeSet != null)
			return structChangeSet;
		else
			return new StructuralDiff(ont1, ont2, verbose).getDiff();
	}
}
