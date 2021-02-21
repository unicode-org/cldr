import * as cldrTest from "./TestCldrTest.js";

import * as cldrListUsers from "../src/main/webapp/js/esm/cldrListUsers.js";

const assert = chai.assert;

describe("cldrListUsers.ping", function () {
  const result = cldrListUsers.ping();
  it("should return pong", function () {
    assert(result === "pong", "result equals pong");
  });
});
