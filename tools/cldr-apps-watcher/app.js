
/**
 * Module dependencies.
 */

var appEnv = require('cfenv').getAppEnv();

var express = require('express')
  , routes = require('./routes')
  , stwatcher = require('./routes/stwatcher')
  , http = require('http')
  , path = require('path')
favicon = require('serve-favicon'),
serveStatic = require('serve-static'),
morgan = require('morgan'),
bodyParser = require('body-parser');

var app = express();

// all environments
app.set('port', appEnv.port);
app.set('views', __dirname + '/views');
app.set('view engine', 'jade');
app.enable('trust proxy');
app.use(favicon(__dirname + '/public/favicon.ico'));
app.use(morgan({ format: 'dev', immediate: true }));
app.use(bodyParser());
app.use(require('method-override')());

app.get('/', routes.index);
app.get('/latest.json', stwatcher.latest);
app.get('/history.json', stwatcher.history);

app.use(serveStatic(path.join(__dirname, 'public')));


// development only
if ('development' == app.get('env')) {
  app.use(require('errorhandler')());
}

var port = app.get('port');
console.log('Express server listening on port ' + port);
app.listen(appEnv.port, appEnv.bind);
