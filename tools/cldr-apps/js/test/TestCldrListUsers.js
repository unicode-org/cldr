import * as cldrListUsers from "../src/esm/cldrListUsers.js";

export const TestCldrListUsers = "ok";

const assert = chai.assert;

describe("cldrListUsers.ping", function () {
  const result = cldrListUsers.ping();
  it("should return pong", function () {
    assert(result === "pong", "result equals pong");
  });
});
