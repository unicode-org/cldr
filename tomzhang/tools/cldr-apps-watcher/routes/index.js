
/*
 * GET home page.
 */

var CONFIG = require("config").SurveyWatcher;

exports.index = function(req, res){
  res.render('index', { ui: CONFIG.ui, watcher: CONFIG.watcher } );
};
