"use strict";

{
  const assert = chai.assert;

  describe("cldrListUsers.test.ping", function () {
    const result = cldrListUsers.test.ping();
    it("should return pong", function () {
      assert(result === "pong", "result equals pong");
    });
  });
}
