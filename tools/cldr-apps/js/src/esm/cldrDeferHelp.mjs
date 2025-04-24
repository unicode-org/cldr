/*
 * cldrDeferHelp: encapsulate code related to showing language descriptions in the Info Panel
 */
import { marked } from "./cldrMarked.mjs";
import * as cldrDom from "./cldrDom.mjs";
const defaultEndpoint = "https://dbpedia.org/sparql/";
const format = "JSON";
const abstractLang = "en";

const USELESS_IN_INFO_PANEL = "see info panel";

function addDeferredHelpTo(fragment, helpHtml, resource, translationHint) {
  if (helpHtml) {
    helpHtml = marked(helpHtml);
  }
  // Always have help (if available).
  const theHelp = $("<div/>", {
    class: "alert alert-info fix-popover-help",
  });
  // helpHtml and translationHint are loaded immediately in the DataPage, no separate query needed
  if (
    translationHint &&
    !translationHint.toLowerCase().includes(USELESS_IN_INFO_PANEL)
  ) {
    const hintHtml =
      "<strong>Translation hint:</strong> " +
      translationHint +
      (helpHtml ? "<hr>" : "");
    theHelp.append(
      $("<span/>", {
        html: hintHtml,
        class: "helpHtml",
      })
    );
  }
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
  cldrDom.setDocTargets(theHelp[0]); // apply to DOM object, not jQuery object
  $(fragment).append(theHelp);
}

function subloadAbstract(resource) {
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
}

/**
 * run a sparql query
 * @param query - SPARQL query
 * @param endpoint - endpoint, defaults to defaultEndpoint
 * @returns Promise<Object>
 */
function sparqlQuery(query, endpoint) {
  endpoint = endpoint || defaultEndpoint;
  return $.getJSON(endpoint, { query, format });
}

function addPlaceholderHelp(fragment, placeholderStatus, placeholderInfo) {
  const absDiv = $("<div/>", { class: "placeholderHelp" });
  absDiv.append(
    $("<h4/>", {
      text: `Placeholders: ${placeholderStatus}`,
      class: "placeholderStatus",
    })
  );
  if (placeholderInfo) {
    const pList = $(`<div class="placeholderInfo">
      <div class="placeholderRow placeholderHeading">
      <span class="placeholderSpan">Placeholder</span>
      <span class="placeholderName">Description</span>
      <span class="placeholderExample">Example</span>
      </div>
    </div>`);

    for (const placeholder of Object.keys(placeholderInfo).sort()) {
      const { name, example } = placeholderInfo[placeholder];
      const pRow = $("<div/>", { class: "placeholderRow" });
      const pHolder = $("<span/>", {
        class: "placeholderSpan",
        text: placeholder,
      });
      const pName = $("<span/>", { class: "placeholderName", text: name });
      const pExample = $("<span/>", {
        class: "placeholderExample",
        text: example,
      });
      pRow.append(pHolder);
      pRow.append(pName);
      pRow.append(pExample);
      pList.append(pRow);
    }
    $(absDiv).append(pList);
  }
  $(fragment).append(absDiv);
}

export { addDeferredHelpTo, addPlaceholderHelp };
