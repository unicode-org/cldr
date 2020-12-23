// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

// This version is for use with dojo. For the non-dojo version, see cldrDeferHelp.js

deferredHelp = (function () {
  const defaultEndpoint = "https://dbpedia.org/sparql/";
  const format = "JSON";
  const abstractLang = "en";

  /**
   * run a sparql query
   * @param query - SPARQL query
   * @param endpoint - endpoint, defaults to defaultEndpoint
   * @returns Promise<Object>
   */
  const sparqlQuery = function sparqlQuery(query, endpoint) {
    endpoint = endpoint || defaultEndpoint;
    return $.getJSON(endpoint, { query, format });
  };

  const subloadAbstract = function subloadAbstract(resource) {
    const absDiv = $("<div/>", { class: "helpAbstract" });
    const absContent = $("<p/>", { text: `Loading ${resource}` });
    absDiv.append(absContent);

    // This query simply returns the abstract result for the specific resource.
    // The query is so small that browsers persistently cache the GET request.
    sparqlQuery(
      `PREFIX  dbo:  <http://dbpedia.org/ontology/>
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
SELECT ?abstract ?primaryTopic
WHERE {
	<${resource}> dbo:abstract ?abstract .
	<${resource}> foaf:isPrimaryTopicOf ?primaryTopic
    FILTER langMatches(lang(?abstract), "${abstractLang}")
} LIMIT 1`
    ).then(
      ({ results }) => {
        let seeAlso = resource;
        if (
          results.bindings[0].primaryTopic &&
          results.bindings[0].primaryTopic.value
        ) {
          seeAlso = results.bindings[0].primaryTopic.value;
        }
        absContent.text(results.bindings[0].abstract.value);
        absDiv.append(
          $("<a/>", {
            text: "(more)",
            title: resource,
            target: "_blank",
            href: seeAlso,
          })
        );
      },
      (err) => {
        absContent.text(`Err loading ${resource}: ${err}`);
      }
    );

    return absDiv;
  };

  /**
   */
  const addDeferredHelpTo = function addDeferredHelpTo(
    fragment,
    helpHtml,
    resource
  ) {
    // Always have help (if available).
    const theHelp = $("<div/>", {
      class: "alert alert-info fix-popover-help vote-help",
    });
    // helpHtml is loaded immediately in the DataSection, no separate query needed
    if (helpHtml) {
      theHelp.append(
        $("<span/>", {
          html: helpHtml,
          class: "helpHtml",
        })
      );
    }

    // fetch the abstract- may be cached.
    if (resource) {
      const absDiv = subloadAbstract(resource);
      theHelp.append(absDiv);
    }

    $(fragment).append(theHelp);
  };

  return {
    addDeferredHelpTo,
    defaultEndpoint,
    sparqlQuery,
  };
})();
