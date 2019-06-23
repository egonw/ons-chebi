// license: MIT
// copyright 2015-2019  Egon Willighagen <egon.willighagen@maastrichtuniversity.nl>
//
// Run Bioclipse with extra memory: -ea -Xmx2048M -XX:MaxPermSize=512m -Declipse.buildId="2.6.0.vqualifier"
@Grab(group='io.github.egonw.bacting', module='managers-rdf', version='0.0.5')
@Grab(group='io.github.egonw.bacting', module='managers-ui', version='0.0.5')

workspaceRoot = ".."
ui = new net.bioclipse.managers.UIManager(workspaceRoot);
rdf = new net.bioclipse.managers.RDFManager(workspaceRoot);
bioclipse = new net.bioclipse.managers.BioclipseManager(workspaceRoot);

println "Bioclipse " + bioclipse.version()
bioclipse.requireVersion("2.6.2")

// create a knowledge base
knowledgebase = "/ChEBI Extraction/chebi.owl";
kbFormat = "RDF/XML";

base = rdf.createInMemoryStore(false) // needs enough memory, see above
println "OWL exists: " + ui.fileExists(knowledgebase)
if (ui.fileExists(knowledgebase) && rdf.size(base) < 100) {
  rdf.importFile(base, knowledgebase, kbFormat);
}
println "Triples loaded: " + rdf.size(base)

query = """
  PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
  PREFIX owl: <http://www.w3.org/2002/07/owl#>
  PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
  SELECT ?sub WHERE {
    ?sub owl:deprecated "true"^^xsd:boolean
  }
 """;
results = rdf.sparql(base, query)
println "results count: " + results.rowCount

query = """
  PREFIX obo: <http://purl.obolibrary.org/obo/>
  SELECT (substr(str(?oldIRI),38) AS ?old) (substr(str(?newIRI),38) AS ?new) WHERE {
    ?oldIRI obo:IAO_0100001 ?newIRI
  }
 """;
results = rdf.sparql(base, query)
println "results count: " + results.rowCount

def renewFile(file) {
  if (ui.fileExists(file)) ui.remove(file)
  ui.newFile(file)
  return file
}
qsFile = "/ChEBI Extraction/deprecated.csv"
jbitsFile = "/ChEBI Extraction/deprecated.javabits"
renewFile(qsFile)
renewFile(jbitsFile)

println "old identifiers: " + results.rowCount
1.upto(results.rowCount) { counter ->
  oldID = results.get(counter, "old")
  newID = results.get(counter, "new")
  ui.append(qsFile, oldID + "," + newID + "\n")
  ui.append(jbitsFile, "    put(\"${oldID}\", \"${newID}\");\n")
}

