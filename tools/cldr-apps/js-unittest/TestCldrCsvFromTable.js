import * as cldrTest from "./TestCldrTest.js";

import * as cldrCsvFromTable from "../src/main/webapp/js/esm/cldrCsvFromTable.js";

const assert = chai.assert;

describe("cldrCsvFromTable.get", function () {
  const table = document.createElement("table");

  table.innerHTML =
    "<tr><td>a</td><td>b</td><td>c</td></tr>" +
    "<tr><td>d</td><td>e</td><td>f</td></tr>";

  const expectedOutput = "a,b,c\nd,e,f\n";

  const actualOutput = cldrCsvFromTable.get(table);

  it("should not return null", function () {
    assert(actualOutput != null, "actualOutput should not be null.");
  });

  it("should return expected CSV", function () {
    assert.deepEqual(
      actualOutput,
      expectedOutput,
      "actualOutput should match expectedOutput"
    );
  });
});
