import "./style.css";
// This is the entrypoint for the new SurveyTool app

const Helloer = require("./helloer").default;

function component() {
  const element = document.createElement("div");

  // Lodash, currently included via a script, is required for this line to work
  element.innerHTML = Helloer.getHello();
  element.classList.add("hello");

  return element;
}

document.body.appendChild(component());
