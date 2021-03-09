// This file gets bundled into bundle.js’s cldrBundle global

// new global stylesheet
import "./style.css";

// module stylesheets need to go here. See cldrVueRouter.js
// example: import 'someModule/dist/someModule.css'
import 'ant-design-vue/dist/antd.css';


// local modules
import { showPanel } from "./cldrVueRouter.js";
import { runGui } from "./runGui.js";

// The following will show up in the cldrBundle global
export default {
  runGui,
  showPanel,
};
