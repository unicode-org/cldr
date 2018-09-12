
/*
 * GET home page.
 */

//var CONFIG = require("config").SurveyWatcher;
var CONFIG = {
  ui: "(ui)",
  watcher: "(watcher)"
};

exports.index = function(req, res){
  res.render('index', { ui: CONFIG.ui, watcher: CONFIG.watcher } );
};
