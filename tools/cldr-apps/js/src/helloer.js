/**
 * A friendly module
 */
class Helloer {
  /**
   * Load the greeting
   */
  static getHello() {
    return ["Hello", "from", "webpack", "module"].join(" ");
  }
}

export default Helloer;
