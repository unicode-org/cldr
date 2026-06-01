import { getClient } from "./cldrClient.mjs";
import * as cldrStatus from "./cldrStatus.mjs";

const SEARCH_START_DELAY = 500; // wait before search kicks off
const SEARCH_UPDATE_DELAY = 1000; // wait before search kicks off

/**
 * Client object for the 'search' capability.
 * Create this and keep it around to manage the search function.
 */
class SearchClient {
  /**
   * @param {Object} opts
   * @param {Function} opts.onLoading callback to set 'loading' status
   * @param {Function} opts.onResults callback to set 'results' array
   * @param {Function} opts.onError callback if there is an error
   */
  constructor(opts) {
    const { onLoading, onResults, onError } = opts;
    if (!onLoading || !onResults || !onError) {
      throw new Error(`missing one of onLoading, onResults, or onError!`);
    }

    this.onLoading = onLoading;
    this.onResults = onResults;
    this.onError = onError;

    this.searchTimeout = null; // no timeout yet
    this.priorSearch = null; // no search yet
    this.client = getClient();
    this.priorToken = null; // no search yet.
  }

  /**
   * Update the search
   * @param {String} str
   */
  update(str) {
    if (this.priorSearch === str) {
      return; // do nothing if the text did not change.
    }
    this.stop().catch((err) => this.onError(err)); // stop any prior search, clear timeout, etc
    if (!str) {
      // No string, so no results.
      this.onResults(null);
      return;
    }
    // TODO: lookup cached results here?
    this.startSearchTimeout(str).then(
      () => {},
      (err) => this.onError(err)
    );
  }

  /**
   * Timeout handler when SEARCH_START_DELAY expires
   * @param {String} str
   */
  handleSearchStart(str) {
    const t = this;

    this.searchTimeout = null;
    this.onLoading(true);

    this.doSearchStart(str)
      .then(() => {})
      .catch((err) => {
        console.error(err);
        t.onError(err);
      });
  }

  /**
   * Actually call and create a new search.
   * @param {String} str
   * @returns
   */
  async doSearchStart(str) {
    const client = await this.client;
    const { body } = await client.apis.search.newSearch(
      {
        locale: cldrStatus.getCurrentLocale() || "en", // search in English if not specified.
      },
      {
        requestBody: {
          value: str,
        },
      }
    );

    const { isComplete, isOngoing, lastUpdated, searchStart, token, results } =
      body;
    if (this.priorToken !== null || this.searchTimeout) {
      await this.cancel(token);
      return; // Get out! Someone has started another search.
    }
    this.priorToken = token;
    this.priorSearch = str;

    this.handleResults(results, isOngoing, token);
  }

  /**
   * Handle the actual results, kicking off a new timeout if need be
   * @param {*} results
   * @param {*} isOngoing
   * @param {*} token
   */
  handleResults(results, isOngoing, token) {
    this.onResults(results);
    if (isOngoing) {
      // There may be more results to be had.
      this.searchTimeout = setTimeout(
        (t) =>
          t.doSearchUpdate(token).catch((err) => {
            console.error(err);
            t.onError(err);
          }),
        SEARCH_UPDATE_DELAY,
        this
      );
    } else {
      // We're done!
      this.stop();
    }
  }

  async doSearchUpdate(token) {
    if (this.priorToken !== token) {
      await this.cancel(token);
      return; // Get out! Someone has started another search.
    }
    const client = await this.client;
    const { body } = await client.apis.search.searchStatus({ token });
    const { results, isComplete, isOngoing, lastUpdated, searchStart } = body;
    if (this.priorToken !== token) {
      await this.cancel(token);
      return; // Get out! Someone has started another search. Throw away results.
    }
    this.handleResults(results, isOngoing, token);
  }

  /**
   * Start the 'timeout', that is, the delay to see if the user types anything
   * else before we start the actual search.
   * @param {String} str
   */
  async startSearchTimeout(str) {
    this.searchTimeout = setTimeout(
      (t) => t.handleSearchStart(str),
      SEARCH_START_DELAY,
      this
    );
  }

  /**
   * Stop the search, but leave results alone.
   */
  async stop() {
    this.cancel(this.priorToken);
    this.priorToken = null; // tell any fetch threads to not update
    if (this.searchTimeout) {
      // Cancel any current search thread
      clearTimeout(this.searchTimeout);
      this.searchTimeout = null;
    }
    this.onLoading(false);
    this.priorSearch = null; // reset
  }

  async cancel(token) {
    if (!token) {
      return;
    }
    const client = await this.client;
    await client.apis.search.searchDelete({ token });
  }
}

export { SearchClient };
