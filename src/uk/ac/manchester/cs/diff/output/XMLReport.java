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
package uk.ac.manchester.cs.diff.output;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

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
public class XMLReport {
	private final String uuid = UUID.randomUUID().toString();
	private SimpleShortFormProvider sf;
	private GenSymShortFormProvider gp;
	private LabelShortFormProvider lp;
	private Document doc, genSymDoc, labelDoc;
	private HashMap<OWLEntity, String> genSymMap, labelMap;
	private HashMap<OWLAxiom,Integer> axiomIds;
	private OWLOntology ont1, ont2;
	private OWLDataFactory df;
	private DocumentBuilderFactory dbfac;
	private DocumentBuilder docBuilder;
	private ChangeSet changeSet;
	private int changeNr = 1;
	private Set<OWLAxiom> sharedAxioms;
	
	/**
	 * Constructor
	 * @param ont1	Ontology 1
	 * @param ont2	Ontology 2
	 * @param changeSet	Change set
	 */
	public XMLReport(OWLOntology ont1, OWLOntology ont2, ChangeSet changeSet) {
		this.ont1 = ont1;
		this.ont2 = ont2;
		this.dbfac = DocumentBuilderFactory.newInstance();
		this.changeSet = changeSet;
		initMapsAndSFPs();
	}
		
	
	/**
	 * Initialise labels and gensyms maps, and short form providers
	 */
	private void initMapsAndSFPs() {
		try {
			this.docBuilder = dbfac.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		df = ont1.getOWLOntologyManager().getOWLDataFactory();
		genSymMap = new HashMap<OWLEntity,String>();
		labelMap = new HashMap<OWLEntity, String>();
		sf = new SimpleShortFormProvider();
		generateGenSyms(); 		// Prep gen syms output
		mapLabels(ont1); mapLabels(ont2);		// Prep labels output
		gp = new GenSymShortFormProvider(genSymMap);
		lp = new LabelShortFormProvider(labelMap);
	}
	
	
	/**
	 * Get the entity name based XML change report
	 * @return XML change report document
	 */
	public Document getXMLDocumentReport() {
		doc = docBuilder.newDocument();
		prepDocument(doc, "");
		
		if(changeSet instanceof StructuralChangeSet)
			return getStructuralChangeSetReport((StructuralChangeSet)changeSet, doc, sf);
		else if(changeSet instanceof LogicalChangeSet)
			return getLogicalChangeSetReport((LogicalChangeSet)changeSet, doc, sf);
		else
			throw new Error("Invalid change set");
	}
	
	
	/**
	 * Get the rdfs:label based XML change report
	 * @return XML change report document
	 */
	public Document getXMLDocumentReportUsingLabels() {	
		labelDoc = docBuilder.newDocument();
		prepDocument(labelDoc, "-lbl");
		if(changeSet instanceof StructuralChangeSet)
			return getStructuralChangeSetReport((StructuralChangeSet)changeSet, labelDoc, lp);
		else if(changeSet instanceof LogicalChangeSet)
			return getLogicalChangeSetReport((LogicalChangeSet)changeSet, labelDoc, lp);
		else
			throw new Error("Invalid change set");
	}
	
	
	/**
	 * Get the auto generated symbols based XML change report
	 * @return XML change report document
	 */
	public Document getXMLDocumentReportUsingGenSyms() {
		genSymDoc = docBuilder.newDocument();
		prepDocument(genSymDoc, "-gs");
		
		if(changeSet instanceof StructuralChangeSet)
			return getStructuralChangeSetReport((StructuralChangeSet)changeSet, genSymDoc, gp);
		else if(changeSet instanceof LogicalChangeSet)
			return getLogicalChangeSetReport((LogicalChangeSet)changeSet, genSymDoc, gp);
		else
			throw new Error("Invalid change set");
	}
	
	
	/**
	 * Get the XML report of a structural change set
	 * @param changeSet	Structural change set
	 * @param doc	XML document
	 * @param sf	Short form provider
	 * @return XML report of a structural change set
	 */
	private Document getStructuralChangeSetReport(StructuralChangeSet changeSet, Document doc, ShortFormProvider sf) {
		if(axiomIds == null) axiomIds = new HashMap<OWLAxiom,Integer>();
		addElementAndChildren("Additions", "adds", changeSet.getAddedAxioms(), doc, "root", true, sf);
		addElementAndChildren("Removals", "rems", changeSet.getRemovedAxioms(), doc, "root", true, sf);
		addElementAndChildren("Shared", "shared", changeSet.getRemovedAxioms(), doc, "root", true, sf);
		return doc;
	}
	
	
	/**
	 * Get the XML report of a logical change set
	 * @param changeSet	Logical change set
	 * @param doc	XML document
	 * @param sf	Short form provider
	 * @return XML report of a logical change set
	 */
	private Document getLogicalChangeSetReport(LogicalChangeSet changeSet, Document doc, ShortFormProvider sf) {
		if(axiomIds == null) axiomIds = new HashMap<OWLAxiom,Integer>();
		
		addElement("Additions", "adds", changeSet.getAdditions().size(), doc, "root", true);
		addElementAndChildren("Effectual", "effadds", changeSet.getEffectualAdditionAxioms(), doc, "adds", true, sf);
		addElementAndChildren("Ineffectual", "ineffadds", changeSet.getIneffectualAdditionAxioms(), doc, "adds", true, sf);
		
		addElement("Removals", "rems", changeSet.getRemovals().size(), doc, "root", true);
		addElementAndChildren("Effectual", "effrems", changeSet.getEffectualRemovalAxioms(), doc, "rems", true, sf);
		addElementAndChildren("Ineffectual", "ineffrems", changeSet.getIneffectualRemovalAxioms(), doc, "rems", true, sf);
		return doc;
	}
	
	
	/**
	 * Add a given element and, where appropriate, its children included in the specified set of axioms 
	 * @param name	Name of the element
	 * @param id	Id of the element
	 * @param set	Set of children axioms
	 * @param d	Document to be added to
	 * @param parent	Parent of the new element
	 * @param includeSize	Include the size of the children set as an attribute of the new element
	 * @param sf	Short form provider
	 */
	private void addElementAndChildren(String name, String id, Set<OWLAxiom> set, Document d, String parent, boolean includeSize, ShortFormProvider sf) {
		addElement(name, id, set.size(), d, parent, includeSize);
		List<OWLAxiom> orderedList = sortAxioms(set);
		for(OWLAxiom ax : orderedList) {
			if(axiomIds.containsKey(ax))
				addAxiomChange(axiomIds.get(ax) + "", ax, d, id, sf);
			else {
				addAxiomChange(changeNr + "", ax, d, id, sf);
				axiomIds.put(ax, changeNr);
				changeNr++;
			}
		}
	}
	

	/**
	 * Add a change element, which contains an axiom child element
	 * @param name	Name of the change element
	 * @param id	Id of the change element
	 * @param axiom	Child axiom of this change
	 * @param d	Document to be added to
	 * @param parent	Parent element of the change element
	 * @param sf	Short form provider
	 */
	private void addAxiomChange(String id, OWLAxiom axiom, Document d, String parent, ShortFormProvider sf) {
		Element ele = d.createElement("Change");
		ele.setAttribute("id", id);
		ele.setIdAttribute("id", true);
		
		Element root = d.getElementById(parent);
		root.appendChild(ele);

		Element axEle = d.createElement("Axiom");
		if(sharedAxioms != null && sharedAxioms.contains(axiom))
			axEle.setAttribute("shared", "true");
		
		axEle.setTextContent(getManchesterRendering(axiom, sf));
		ele.appendChild(axEle);
	}
	
	
	/**
	 * Add element with given name and Id to an XML document
	 * @param name	Name of the element
	 * @param id	Id of the element
	 * @param size	Size of the elements children (axioms)
	 * @param d	Document to be added to
	 * @param parent	Parent of this new element
	 * @param includeSize	Include the size of the children as an attribute of the new element
	 */
	private void addElement(String name, String id, int size, Document d, String parent, boolean includeSize) {
		Element ele = d.createElement(name);
		if(includeSize)
			ele.setAttribute("size", "" + size);
	
		ele.setAttribute("id", id);
		ele.setIdAttribute("id", true);

		Element root = d.getElementById(parent);
		root.appendChild(ele);
	}

	
	/**
	 * Prepare output XML document
	 * @param d	Document to prepare
	 * @param suffix	Suffix for the document identifier
	 */
	private void prepDocument(Document d, String suffix) {
		Element root = d.createElement("root");
		root.setAttribute("id", "root");
		root.setIdAttribute("id", true);
		
		String id = uuid + suffix;
		root.setAttribute("uuid", id);
		d.appendChild(root);
	}
	
	
	/**
	 * Get Manchester syntax of an OWL object
	 * @param obj	OWL object
	 * @param sf	Short form provider
	 * @return A string with the object's conversion to Manchester syntax 
	 */
	private String getManchesterRendering(OWLObject obj, ShortFormProvider sf) {
		StringWriter wr = new StringWriter();
		ManchesterOWLSyntaxObjectRenderer render = new ManchesterOWLSyntaxObjectRenderer(wr, sf);
		obj.accept(render);

		String str = wr.getBuffer().toString();
		str = str.replace("<", "");
		str = str.replace(">", "");
		return str;
	}
	
	
	/**
	 * Get XML document as a string
	 * @param doc	XML document
	 * @return String version of the XML document
	 * @throws TransformerException 
	 */
	public String getReportAsString(Document doc) throws TransformerException {
		TransformerFactory transfac = TransformerFactory.newInstance();
		Transformer trans = transfac.newTransformer();
		return getXMLAsString(trans, doc);
	}
	
	
	/**
	 * Get XML document transformed into HTML as a string
	 * @param doc	XML document
	 * @param xsltPath	Path to the XSL Transformation file
	 * @return String containing the HTML transformation
	 * @throws TransformerException 
	 */
	public String getReportAsHTML(Document doc, String xsltPath) throws TransformerException {
		TransformerFactory transfac = TransformerFactory.newInstance();
		Transformer trans = transfac.newTransformer(new javax.xml.transform.stream.StreamSource(xsltPath));
		return getXMLAsString(trans, doc);
	}
	
	
	/**
	 * Transform the document using the given transformer and return the transformation as a string
	 * @param trans	Transformer
	 * @param doc	XML document
	 * @return String result of transforming the XML document
	 * @throws TransformerException
	 */
	private String getXMLAsString(Transformer trans, Document doc) throws TransformerException {
		trans.setOutputProperty(OutputKeys.INDENT, "yes");

		// Create string from XML tree
		StringWriter sw = new StringWriter();
		StreamResult result = new StreamResult(sw);
		DOMSource source = new DOMSource(doc);

		trans.transform(source, result);
		return sw.toString();
	}
	
	
	/**
	 * Map entities to their respective rdfs:label, where applicable
	 * @param ont	Ontology
	 */
	private void mapLabels(OWLOntology ont) {
		Set<OWLEntity> ents = ont.getSignature();
		for(OWLEntity e : ents) {
			Set<OWLAnnotation> labels = e.getAnnotations(ont, df.getRDFSLabel());
			if(!labels.isEmpty()) {
				for(OWLAnnotation a : labels) {
					String entry = a.getValue().toString();
					if(entry.startsWith("\"")) {
						entry = entry.substring(1);
						entry = entry.substring(0, entry.indexOf("\""));
					}
					if(!entry.equals(""))
						labelMap.put(e, entry);
					else
						labelMap.put(e, sf.getShortForm(e));
				}
			}
			else labelMap.put(e, sf.getShortForm(e));
		}
	}
	
	
	/**
	 * Sort a given set of axioms into a list
	 * @param set	Set of axioms
	 * @param sf	Short form provider
	 * @return List of ordered axioms 
	 */
	private List<OWLAxiom> sortAxioms(Set<OWLAxiom> set) {
		Map<String,OWLAxiom> map = new HashMap<String,OWLAxiom>();
		for(OWLAxiom axiom : set) {
			String ax = getManchesterRendering(axiom, sf);
			map.put(ax, axiom);
		}
		List<String> axStrings = new ArrayList<String>(map.keySet());
		Collections.sort(axStrings);
		
		List<OWLAxiom> output = new ArrayList<OWLAxiom>();
		for(String s : axStrings) {
			output.add(map.get(s));
		}
		return output;
	}
	
	
	/**
	 * Generate GenSyms for entities
	 */
	private void generateGenSyms() {
		HashSet<OWLEntity> entSet = new HashSet<OWLEntity>();
		entSet.addAll(ont1.getSignature());
		entSet.addAll(ont2.getSignature());
		
		int classCounter = 0, propCounter = 0, indCounter = 0;
		char curChar1 = 'A', curChar2 = 'A', curChar3 = 'A';
		boolean twoChars = false, threeChars = false;
		
		for(OWLEntity e : entSet) {
			if(e.isOWLClass()) {
				if(!twoChars && !threeChars) {
					classCounter++;
					String base = "" + curChar1 + classCounter;
					genSymMap.put(e, base);
					if(curChar1 == 'Z' && classCounter == 9) {
						twoChars = true; curChar1 = 'A'; classCounter = 0;
					}
					else if(classCounter == 9) {
						curChar1++; classCounter = 0;
					}
				}
				else if(twoChars) {
					classCounter++;
					String base = "" + curChar1 + curChar2 + classCounter;
					genSymMap.put(e, base);

					if(curChar1 == 'Z' && curChar2 == 'Z' && classCounter == 9) {
						threeChars = true; twoChars = false; curChar1 = 'A'; curChar2 = 'A'; classCounter = 0;
					}
					else if(curChar2 == 'Z' && classCounter == 9) {
						curChar1 ++; curChar2 = 'A'; classCounter = 0;
					}
					else if(classCounter == 9) {
						curChar2++; classCounter = 0;
					}
				}
				else if(threeChars) {
					classCounter++;
					String base = "" + curChar1 + curChar2 + curChar3 + classCounter;
					genSymMap.put(e, base);
					if(curChar2 == 'Z' && curChar3 == 'Z' && classCounter == 9) {
						curChar1++; curChar2 = 'A'; curChar3 = 'A'; classCounter = 0;
					}
					else if(curChar3 == 'Z' && classCounter == 9) {
						curChar2 ++; curChar3 = 'A'; classCounter = 0;
					}
					else if(classCounter == 9) {
						curChar3++; classCounter = 0;
					}
				}
			}
			else if(e.isOWLDataProperty() || e.isOWLObjectProperty() || e.isOWLAnnotationProperty()) {
				propCounter++; genSymMap.put(e, "prop" + propCounter);
			}
			else if(e.isOWLNamedIndividual()) {
				indCounter++; genSymMap.put(e, "ind" + indCounter);
			}
		}
	}
}
