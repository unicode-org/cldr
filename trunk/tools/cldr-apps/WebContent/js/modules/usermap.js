/**
 * Example special module that shows a forum page. 
 * Modify 'js/special/flagged.js' below to reflect your special page's name.
 * @module forum
 */
define("js/modules/usermap.js", ["dojo/request"], 
		function(request) {

	function UserMap() {
		// constructor
	}
	
	// set up the inheritance before defining other functions 
	//_super = Page.prototype = new SpecialPage();

	var hash = {};
	
	UserMap.prototype.get = function get(id, fn, args) {
		if(hash[id]) {
			console.log('Already had ' + id);
			fn(hash[id].err, args, {entry: hash[id], id: id});
		} else {
			console.log('Fetching user info for #'+id);
			request
			.get('SurveyAjax?s='+surveySessionId+'&what=user_info&u='+id, {handleAs: 'json'})
			.then(function(json) {
				hash[id] = json;
				fn(json.err, args, {entry: json, id: id}); // add err msg
			})
			.otherwise(function(err) {
				hash[id] = { err: err };
				fn(hash[id].err, args, {entry: hash[id], id: id}); // add err msg
			});
		}
	};
	
	//UserMap.prototype.createUser = createUser; // re export this fcn
	
	UserMap.prototype.createGravatar = function createGravatar(user) {
		if(user.emailHash) {
			return $('<img></img>', {
				src: 'http://www.gravatar.com/avatar/'+user.emailHash+'?d=identicon&r=g&s=32',
				title: 'gravatar - http://www.gravatar.com'
			});
		} else {
			return $('<span></span>');
		}
	};

	UserMap.prototype.createGravatar16 = function createGravatar16(user) {
		if(user.emailHash) {
			return $('<img></img>', {
				src: 'http://www.gravatar.com/avatar/'+user.emailHash+'?d=identicon&r=g&s=16',
				title: 'gravatar - http://www.gravatar.com'
			});
		} else {
			return $('<span></span>');
		}
	};

	// this is a port of the same function from survey.js but ported to jquery
	UserMap.prototype.createUser = function createUser(user) {
		var div = $('<div></div>', {class: 'adminUserUser'});
		UserMap.prototype.createGravatar(user).appendTo(div);
		$('<i></i>', {
			text: stui_str("userlevel_"+user.userlevelName.toLowerCase(0)),
			class: "userlevel_"+user.userlevelName.toLowerCase()
		}).appendTo(div);
		
		$('<span></span>', {
			text: user.name,
			class: 'adminUserName'
		}).appendTo(div);
		if(!user.orgName) {
		   user.orgName = user.org;
		}
		$('<span></span>', {
			text: user.orgName + ' #'+user.id,
			class: 'adminOrgName'
		}).appendTo(div);
		$('<address></address>', {
			text: user.email,
			class: 'adminUserAddress'
		}).appendTo(div);
		return div;
	};
	
	// create a thinner (vertically) user
	UserMap.prototype.createUserThin = function createUserThin(user) {
		var div = $('<div></div>', {class: 'thinUser'});
		UserMap.prototype.createGravatar16(user).appendTo(div);
//		$('<i></i>', {
//			text: stui_str("userlevel_"+user.userlevelName.toLowerCase(0)),
//			className: "userlevel_"+user.userlevelName.toLowerCase()
//		}).appendTo(div);
		
		$('<span></span>', {
			text: user.name,
			class: 'adminUserName'
		}).appendTo(div);
		if(!user.orgName) {
		   user.orgName = user.org;
		}
		$('<span></span>', {
			text: user.orgName + ' #'+user.id,
			class: 'adminOrgName'
		}).appendTo(div);
//		$('<address></address>', {
//			text: user.email,
//			className: 'adminUserAddress'
//		}).appendTo(div);
		return div;
	};
	
	return UserMap;
});