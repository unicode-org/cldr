"use strict";

/**
 * cldrCsvFromTable: enable downloading a table as a CSV file.
 * This works with or without dojo; no dependency on dojo.
 *
 * Use an IIFE pattern to create a namespace for the public functions,
 * and to hide everything else, minimizing global scope pollution.
 * Ideally this should be a module (in the sense of using import/export),
 * but not all Survey Tool JavaScript code is capable yet of being in modules
 * and running in strict mode.
 */
const cldrCsvFromTable = (function () {
  /**
   * Download the table as CSV
   */
  function download(tableId, fileName) {
    const table = document.getElementById(tableId);
    if (!table) {
      return;
    }
    const csv = get(table);
    if (!csv) {
      return;
    }
    const data = btoa(unescape(encodeURIComponent(csv)));
    const link = window.document.createElement("a");
    link.setAttribute("href", "data:text/csv;charset=utf-8;base64," + data);
    link.setAttribute("download", fileName);
    link.click();
  }

  function get(table) {
    let csv = "";
    for (let row of table.rows) {
      let columnsRemaining = row.cells.length;
      for (let cell of row.cells) {
        csv += cell.innerText + (--columnsRemaining ? "," : "\n");
      }
    }
    return csv;
  }

  /*
   * Make only these functions accessible from other files
   */
  return {
    download: download,
    /*
     * The following are meant to be accessible for unit testing only:
     */
    test: {
      get: get,
    },
  };
})();
