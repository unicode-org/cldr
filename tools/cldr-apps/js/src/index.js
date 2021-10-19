// This file gets bundled into bundle.js’s cldrBundle global

// global stylesheets
import "./css/cldrForum.css";

// module stylesheets need to go here. See cldrVueRouter.js
// example: import 'someModule/dist/someModule.css'
import "ant-design-vue/dist/antd.css";

// local modules
import { showPanel, createCldrApp } from "./cldrVueRouter.js";
import { runGui } from "./runGui.js";

// The following will show up in the cldrBundle global
export default {
  createCldrApp,
  runGui,
  showPanel,
};
