
/**
 * Module dependencies.
 */

var express = require('express')
  , routes = require('./routes')
  , stwatcher = require('./routes/stwatcher')
  , http = require('http')
  , path = require('path')
  , CONFIG = require('config').SurveyWatcher;

var app = express();

// all environments
app.set('port', CONFIG.port || process.env.PORT || 3000);
app.set('views', __dirname + '/views');
app.set('view engine', 'jade');
app.use(express.favicon());
app.use(express.logger('dev'));
app.use(express.bodyParser());
app.use(express.methodOverride());
app.use(app.router);
app.use(express.static(path.join(__dirname, 'public')));

// development only
if ('development' == app.get('env')) {
  app.use(express.errorHandler());
}

app.get('/', routes.index);
app.get('/latest.json', stwatcher.latest);
app.get('/history.json', stwatcher.history);

http.createServer(app).listen(app.get('port'), function(){
  console.log('Express server listening on port ' + app.get('port'));
});
