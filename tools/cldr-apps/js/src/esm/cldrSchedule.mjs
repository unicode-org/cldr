/*
 * cldrSchedule: for Survey Tool scheduling of http requests.
 */

export class FetchSchedule {
  /**
   * Construct a new FetchSchedule object
   *
   * @param {String} description description or name of caller
   * @param {Number} refreshSeconds postpone requests until this many seconds have elapsed since last request/response
   * @param {Boolean} debug whether to log debugging info to console
   * @returns the new FetchSchedule object
   */
  constructor(description, refreshSeconds, debug) {
    this.description = description;
    this.refreshMillis = refreshSeconds * 1000;
    this.debug = debug;
    this.lastRequestTime = this.lastResponseTime = 0;
  }

  setRequestTime() {
    this.lastRequestTime = Date.now();
    if (this.debug) {
      console.log(
        this.description + " set lastRequestTime = " + this.lastRequestTime
      );
    }
  }

  setResponseTime() {
    this.lastResponseTime = Date.now();
    if (this.debug) {
      console.log(
        this.description + " set lastResponseTime = " + this.lastResponseTime
      );
    }
  }

  reset() {
    this.lastRequestTime = this.lastResponseTime = 0;
  }

  /**
   * Is it too soon to make a request?
   *
   * @returns true if it is too soon, else false
   */
  tooSoon() {
    const now = Date.now();
    if (
      this.lastRequestTime &&
      (now < this.lastResponseTime + this.refreshMillis ||
        now < this.lastRequestTime + this.refreshMillis)
    ) {
      if (this.debug) {
        console.log(
          this.description +
            " postponing request: less than " +
            this.refreshMillis / 1000 +
            " seconds elapsed; now = " +
            now
        );
      }
      return true;
    } else {
      if (this.debug) {
        console.log(this.description + " will make a request; now = " + now);
      }
      return false;
    }
  }
}
