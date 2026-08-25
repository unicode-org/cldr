/**
* Allow editing SVG file's source code without having to save them locally (aka "download") them.
* @docu https://commons.wikimedia.org/wiki/User_talk:Rillke/SVGedit.js
*
* @rev 1 (2014-03-22)
* @rev 2 (2015-05-29)
* @author Rillke, 2014-2015
*/
// List the global variables for jsHint-Validation. Please make sure that it passes http://jshint.com/
// Scheme: globalVariable:allowOverwriting[, globalVariable:allowOverwriting][, globalVariable:allowOverwriting]
/* global jQuery:false, mediaWiki:false, MwJSBot:false, CodeMirror:false */
// Set jsHint-options. You should not set forin or undef to false if your script does not validate.
/* jshint forin:true, noarg:true, noempty:true, eqeqeq:true, bitwise:true, strict:true,
 undef:true, curly:false, browser:true, multistr:true */
/* eslint indent:["error","tab",{"outerIIFEBody":0}] */

(function ($, mw) {
'use strict';

var svgEdit,
	i,
	MYSELF = 'SVGEdit',
	conf = mw.config.get([
		'wgDBname',
		'wgPageName',
		'wgNamespaceNumber',
		'wgRevisionId',
		'wgTitle'
	]),
	isCommonsWiki = conf.wgDBname === 'commonswiki',
	random = Math.round(Math.random() * 0x1000000000),
	commonwWikiKey = 'commonswiki' + random,
	commonsWiki = {},
	modules = [
		['ext.gadget.jquery.blockUI', 'ver1_svg', [], null, commonwWikiKey],
		['ext.gadget.libAPI', 'ver1_svg', ['user.options'], null, commonwWikiKey],
		['ext.gadget.editDropdown', 'ver1_svg', ['jquery.client', 'user.options'], null, commonwWikiKey]
	];

svgEdit = {
	version: '0.0.15.2',
	init: function () {
		var $activationLinks = $();
		// File namespace?
		if (conf.wgNamespaceNumber !== 6 || !/\.svg$/i.test(conf.wgPageName))
			return svgEdit.log('Not a SVG-file. Aborting initialization.');

		if (mw.user.isAnon())
			return svgEdit.log('Anonymous users cannot upload files. Aborting initialization.');

		// if (!conf.wgRevisionId || !$('.filehistory').find('td.filehistory-selected').length) return svgEdit.log('Page or file does not exist.');

		$activationLinks = $activationLinks.add(mw.libs.commons.ui.addEditLink('#SVGedit', 'Edit SVG', 'e-edit-raw-SVG', 'Edit SVG source code'));

		$activationLinks.click(function (e) {
			e.preventDefault();
			svgEdit.run();
			$activationLinks.addClass('ui-state-disabled');
		});
		if (mw.util.getParamValue('svgrawedit'))
			svgEdit.run();
	},
	registerModules: function () {
		// Register custom modules
		if (!mw.loader.getState('mediawiki.commons.MwJSBot')) {
			mw.loader.implement('mediawiki.commons.MwJSBot', ['//commons.wikimedia.org/w/index.php?action=raw&ctype=text/javascript&title=User:Rillke/MwJSBot.js'],
				{ /* no styles*/ }, { /* no messages*/ });
		}
	},
	run: function () {
		// Create GUI
		svgEdit.registerModules();

		mw.loader.using(['mediawiki.commons.MwJSBot', 'user.options'], function () {
			svgEdit.gui();
		});
	},
	gui: function () {
		var $gui = $('<form action="/">'),
			$preview = $('<div>')
				.appendTo($gui),
			$diffContainer = $('<div>')
				.css({ border: '1px solid grey' })
				.text('Diff: ')
				.hide()
				.appendTo($gui),
			$validationWrapper = $('<div>')
				.css({
					'border': '1px solid grey',
					'min-height': '2em',
					'max-height': '40em',
					'resize': 'both',
					'overflow': 'auto'
				})
				.hide()
				.appendTo($gui),
			$validationDoctypeLabel = $('<div>')
				.css({
					'float': 'right',
					'background': '#FFD',
					'padding': '.3em',
					'font-family': 'monospace'
				})
				.attr({ title: 'document type used for validation' })
				.appendTo($validationWrapper),
			$validationContainer = $('<ul>')
				.appendTo($validationWrapper),
			$validationContainer2 = $('<ul>')
				.appendTo($validationWrapper),
			$diff = $('<div>')
				.css({ font: '12px "Monaco","Menlo","Ubuntu Mono","Consolas","source-code-pro",monospace' })
				.appendTo($diffContainer),
			$imgPreviewContainer = $('<div>')
				.css({
					position: 'relative',
					overflow: 'hidden',
					display: 'inline-block'
				})
				.html('<a href="https://en.wikipedia.org/wiki/Librsvg" target="_blank">RSVG</a> rendering:<br>')
				.hide()
				.appendTo($preview),
			$imgPreview = $('<img>')
				.attr({ title: 'rsvg preview' })
				.css({ 'vertical-align': 'top' })
				.addClass('com-svgedit-preview')
				.appendTo($imgPreviewContainer),
			$imgPreview2Container = $('<div>')
				.css({
					position: 'relative',
					overflow: 'hidden',
					display: 'inline-block'
				})
				.html('Browser rendering (iframe):<br>')
				.hide()
				.appendTo($preview),
			$imgPreview2Overlay = $('<div>')
				.attr({ title: 'browser preview' })
				.css({
					'position': 'absolute',
					'left': 0,
					'top': 0,
					'bottom': 0,
					'right': 0,
					'z-index': 1
				})
				.appendTo($imgPreview2Container),
			$imgPreview2 = $('<iframe>')
				.attr({
					sandbox: 'sandbox',
					title: 'browser preview'
				})
				.css({
					'border': '1px solid #EEE',
					'width': 0,
					'height': 0,
					'resizable': 'both',
					'vertical-align': 'top'
				})
				.addClass('com-svgedit-preview')
				.appendTo($imgPreview2Container),
			$taWrap = $('<div>')
				.appendTo($gui),
			$ta = $('<textarea>').attr({
				rows: mw.user.options.get('rows'),
				cols: mw.user.options.get('cols'),
				disabled: 'disabled'
			}).css({ width: '99%' }).appendTo($taWrap),
			$sum = $('<input type="text" style="width:99%" maxlength="200" pattern=".{3,}" required placeholder="upload summary (changes, techniques, 3-200 characters)" title="3-200 letters, please">')
				.appendTo($gui),
			$buttonPane = $('<div>')
				.addClass('com-svg-edit-buttonpane')
				.appendTo($gui),
			$saveBtn = $('<button>').attr({
				type: 'submit',
				role: 'submit',
				disabled: 'disabled'
			}).text('Save SVG').appendTo($buttonPane),
			$loadCodeEditorBtn = $('<button>').attr({
				type: 'button',
				role: 'button',
				disabled: 'disabled',
				title: 'Loads a code editor (XML mode)'
			}).text('Load CodeMirror').appendTo($buttonPane),
			$previewBtn = $('<button>').attr({
				type: 'button',
				role: 'button',
				disabled: 'disabled',
				title: 'Render a preview'
			}).text('Preview').appendTo($buttonPane),
			$diffBtn = $('<button>').attr({
				type: 'button',
				role: 'button',
				disabled: 'disabled',
				title: 'Show difference between saved and working copy'
			}).text('Diff').appendTo($buttonPane),
			$validationDoctype = $('<select>')
				.html(
'<option value="Inline" selected="">(detect automatically)</option>\
<option value="SVG 1.0">SVG 1.0</option>\
<option value="SVG 1.1">SVG 1.1</option>\
<option value="SVG 1.1 Tiny">SVG 1.1 Tiny</option>\
<option value="SVG 1.1 Basic">SVG 1.1 Basic</option>'
				)
				.hide()
				.appendTo($buttonPane),
			$validateButton = $('<button>').attr({
				type: 'button',
				role: 'button',
				disabled: 'disabled',
				title: 'Check for glitches against validators'
			}).text('Validate').appendTo($buttonPane),
			$uploadButton = $('<input type="file">').attr({
				disabled: 'disabled',
				title: 'Replace editor contents with file contents'
			}).appendTo($buttonPane),
			allowCloseWindow,
			timeout,
			getCurrentValue,
			setCurrentValue,
			getOriginal,
			$fetchCB;
		mw.util.addCSS('.com-svgedit-preview:hover, .com-svgedit-preview-hover { \
			background: url("//upload.wikimedia.org/wikipedia/commons/5/5d/Checker-16x16.png") repeat scroll }');
		$('<div>').css({
			'float': 'right',
			'color': '#DDD'
		}).text('Version: ' + this.version).appendTo($buttonPane);

		getCurrentValue = function () {
			return svgEdit.CodeMirror ?
				svgEdit.CodeMirror.getValue() :
				$ta.val();
		};
		setCurrentValue = function (val) {
			if (svgEdit.CodeMirror)
				svgEdit.CodeMirror.setValue(val);
			else
				$ta.val(val);

		};
		getOriginal = function () {
			return $ta.data('orignal-svg');
		};
		$fetchCB = function (r) {
			$ta.val(r);
			$ta.data('orignal-svg', r);
			$saveBtn
				.add($ta)
				.add($loadCodeEditorBtn)
				.add($previewBtn)
				.add($diffBtn)
				.add($validateButton)
				.add($uploadButton)
				.removeAttr('disabled');
			timeout = setTimeout(function () {
				mw.loader.using('mediawiki.confirmCloseWindow', function () {
					allowCloseWindow = mw.confirmCloseWindow({ test: function () {
						return getCurrentValue() !== getOriginal();
					} });
				});
			}, 5000);
		};

		$ta.val('Loading SVG');
		this.fileUrl = '';

		$('#file').find('a').each(function (i, el) {
			var href = $(el).attr('href'),
				fileDomainPos = href.indexOf('upload.wikimedia.org');
			if (fileDomainPos < 10 && fileDomainPos !== -1 && /\.svg$/i.test(href)) {
				svgEdit.fileUrl = href;
				return false;
			}
		});

		if (!this.fileUrl) {
		// Get filepath if in edit-mode
			$.ajax({
				url: mw.config.get('wgServer') + mw.util.wikiScript('api') + '?action=query&format=json&prop=imageinfo&titles=' + mw.util.wikiUrlencode(conf.wgPageName) + '&iiprop=url&iilimit=1',
				dataType: 'json',
				success: function (r) {
					if (r && r.query && r.query.pages) {
						r = r.query.pages;
						for (var id in r) {
							if (r[id].imageinfo[0] && r[id].imageinfo[0].url) {
								svgEdit.fileUrl = r[id].imageinfo[0].url;
								return svgEdit.$fetch().done($fetchCB);
							} else {
								svgEdit.failURL();
							}
						}
					} else {
						svgEdit.failURL();
					}
				}
			});
		} else {
			this.$fetch().done($fetchCB);
		}

		$imgPreview2Overlay.click(function () {
			if (prompt('DANGER ZONE: For your security, we added \
				an overlay over the iframe protecting you from accidental \
				interactions with the potentially evil/ harmful SVG code. \
				Type "sudo" to disable this security-layer. \
				(Otherwise just cancel)') === 'sudo')
				$imgPreview2Overlay.hide();

		}).hover(function () {
			$imgPreview2.addClass('com-svgedit-preview-hover');
		}, function () {
			$imgPreview2.removeClass('com-svgedit-preview-hover');
		});

		$gui.submit(function (e) {
			e.preventDefault();
			$saveBtn.add($sum).attr('disabled', 'disabled');
			svgEdit.save(
				svgEdit.CodeMirror ?
					svgEdit.CodeMirror.getValue() :
					$ta.val(),
				$sum.val()
			).done(function (httpStatus, response) {
				if (response && window.JSON)
					response = JSON.parse(response);

				if (response && response.error) {
					alert('API Error ' + response.error.code + ':\n' + response.error.info);
					$saveBtn.add($sum).removeAttr('disabled');
					$taWrap.attr('noblock', 1).unblock();
				} else {
					clearTimeout(timeout);
					if (allowCloseWindow)
						allowCloseWindow.release();

					svgEdit.reload();
				}
			}).fail(function () {
				alert('Server error: Something went wrong');
				$saveBtn.add($sum).removeAttr('disabled');
				$taWrap.attr('noblock', 1).unblock();
			});
			svgEdit.block($taWrap);
		});

		$loadCodeEditorBtn.click(function () {
			$(this).attr('disabled', 'disabled');
			svgEdit.loadCodeEditor($ta);
		});

		$previewBtn.click(function () {
			var val = getCurrentValue(),
				blob,
				URL,
				dataUrl,
				typedArray,
				v,
				w,
				h,
				m;
			URL = window.URL || window.webkitURL;

			blob = new Blob([val], { type: 'image/svg+xml' });
			dataUrl = URL.createObjectURL(blob);
			// Naive RegExp matching (avoids parsing the whole document)
			// and possible security or malformed SVG troubles
			v = val.slice(4, 5000);
			m = v.match(/height\s*=\s*["']([\d.]+)["']/);
			if (!(m && (h = m[1]) && (h = Number(h)) && h > 15))
				h = 500;

			m = v.match(/width\s*=\s*["']([\d.]+)["']/);
			if (!(m && (w = m[1]) && (w = Number(w)) && w > 15))
				w = 500;

			$previewBtn.attr('disabled', 'disabled');

			$imgPreview2Container.show();
			$imgPreviewContainer.css({
				height: 500,
				width: 500
			}).show();
			svgEdit.block($imgPreviewContainer);
			svgEdit.block($imgPreview2Container);

			$imgPreview2.one('load', function () {
				if ($imgPreview2Container.unblock)
					$imgPreview2Container.unblock();

			}).attr('src', dataUrl).css({
				width: w,
				height: h
			});

			svgEdit
				.fetchPreview(val)
				.done(function (statusText, response) {
					typedArray = new Uint8Array(response);
					blob = new Blob([typedArray], { type: 'image/jpeg' });
					dataUrl = URL.createObjectURL(blob);
					$imgPreviewContainer.css({
						height: 'auto',
						width: 'auto'
					});
					$imgPreview.attr('src', dataUrl);
					setTimeout(function () {
						$imgPreview2.css({
							width: $imgPreview.width(),
							height: $imgPreview.height()
						});
					}, 1000);
				})
				.fail(function (/* r*/) {
					$imgPreview.attr('src', '//upload.wikimedia.org/wikipedia/commons/thumb/5/55/Bug_blank.svg/200px-Bug_blank.svg.png');
				})
				.always(function () {
					$previewBtn.removeAttr('disabled');
					$imgPreviewContainer.add($imgPreview2Container).unblock();
				});
		});
		$diffBtn.click(function () {
			svgEdit.block($diffContainer.show());
			svgEdit.$usingScharkDiff().done(function () {
				$diff.html(mw.libs.schnarkDiff.htmlDiff(
					getOriginal(),
					getCurrentValue(),
					true));
				$diffContainer.unblock();
			});
		});
		$validateButton.click(function () {
			if ($validationDoctype.css('display') === 'none')
				return $validationDoctype.fadeIn('fast');

			svgEdit.block($validationWrapper.show());
			svgEdit.$validate(getCurrentValue(), $validationDoctype.val()).done(function (textStatus, r) {
				$validationWrapper.unblock();
				$validationContainer.add($validationContainer2).text('');
				try {
					r = JSON.parse(r);
				} catch (invalidJSON) {}
				if (r.source)
					$validationDoctypeLabel.text(r.source.doctype);

				if (r.svgcheck && r.svgcheck.length) {
					$.each(r.svgcheck, function (i, msg) {
						$validationContainer2.append(svgEdit.$validationItem2(msg));
					});
				}
				if (r.messages) {
					$.each(r.messages, function (i, msg) {
						$validationContainer.append(svgEdit.$validationItem(msg));
					});
					if (!r.messages.length)
						$validationContainer.append($('<li>Well done :)</li>'));

				} else if (r.response) {
					$validationContainer.html(r.response);
				} else {
					$validationContainer.text(JSON.stringify(r));
				}
			});
		});
		$uploadButton.on('change', function () {
			var file = $uploadButton[0].files[0];
			if (!file)
				return;

			var size = file.size;
			if (size > 15 * 1024 * 1024)
				return alert('Selected file is > 15 MiB. Aborting.');

			var reader = new FileReader();
			reader.onload = function () {
				// Clear upload button
				$uploadButton.val('');
				if (getCurrentValue() !== $ta.data('orignal-svg')) {
					if (!confirm('The editor contents changed from the stored revision. Are you sure you want to replace the editor contents with the contents loaded from the file selected?')) {
						return; // Cancel: Do nothing!
					}
				}
				setCurrentValue(reader.result);
			};
			reader.readAsText(file);
		});
		$gui.prependTo('#mw-content-text');
	},
	block: function ($el) {
		mw.loader.using('ext.gadget.jquery.blockUI', function () {
			if ($el.attr('noblock'))
				return;

			$el.block({
				message: '<img src="//upload.wikimedia.org/wikipedia/commons/1/10/Loading-special.gif" height="15" width="128">',
				css: {
					border: 'none',
					background: 'none'
				}
			});
		});
	},
	$validationItem: function (validatorMsg) {
		var p = 'com-svgedit-validation-',
			$l = $('<code>').addClass(p + 'line').text('L.' + validatorMsg.lastLine),
			$col = validatorMsg.lastColumn ? $('<code>').addClass(p + 'col')
				.text('col.' + validatorMsg.lastColumn) : '',
			$msg = $('<span>').addClass(p + 'message').text(validatorMsg.message),
			$msgId = $('<span>').addClass(p + 'messageid').text(validatorMsg.messageid),
			$li = $('<li>').append($l, ' ', $col, ': ', $msg, ' (', $msgId, ')');
		return $li;
	},
	$validationItem2: function (validatorMsg) {
		$.each(validatorMsg.issues, function (i, issue) {
			validatorMsg.issues[i] = mw.html.escape(issue)
				.replace(/\*\*(.+?)\*\*/, '<b><i>$1</i></b>')
				.replace(/\*(.+?)\*/, '<i>$1</i>');
		});
		var p = 'com-svgedit-validation-',
			$l = $('<code>').addClass(p + 'line').text('L.' + validatorMsg.line),
			$msg = $('<span>').addClass(p + 'message')
				.html(validatorMsg.issues.join(', ')),
			$li = $('<li>').append($l, ': ', $msg);
		return $li;
	},
	$validate: function (svg, doctype) {
		return svgEdit.bot.multipartMessageForUTF8Files()
			.appendPart('svgcheck', 'on')
			.appendPart('doctype', doctype)
			.appendPart('file', svg, 'input.svg')
			.$send('//validator.toolforge.org/w3.php');
	},
	$usingScharkDiff: function () {
		var $deferred = $.Deferred();
		if (mw.libs.schnarkDiff && mw.libs.schnarkDiff.htmlDiff) {
			$deferred.resolve();
		} else {
			mw.hook('userjs.load-script.diff-core').add(function () {
				mw.libs.schnarkDiff.style.set('ins', 'text-decoration: underline; font-weight: bold; font-size:1.2em; color: #020; background-color: #ABE; -moz-text-decoration-color:#474;');
				mw.libs.schnarkDiff.style.set('del', 'font-size:1.2em; color: #200; background-color: #FD9; text-decoration-color:#744;');
				mw.util.addCSS(mw.libs.schnarkDiff.getCSS());
				mw.libs.schnarkDiff.config.set('minMovedLength', 20);
				mw.libs.schnarkDiff.config.set('tooShort', 3);
				$deferred.resolve();
			});
			mw.loader.load('//de.wikipedia.org/w/index.php?title=Benutzer:Schnark/js/diff.js/core.js&action=raw&ctype=text/javascript');
		}
		return $deferred.promise();
	},
	failURL: function (err) {
		err = err || 'Unable to extract file URL.';
		svgEdit.log(err);
		throw new Error(err);
	},
	$fetch: function () {
		// Fetch SVG source code
		svgEdit.bot = new MwJSBot();

		if (!svgEdit.fileUrl)
			return svgEdit.failURL();

		// Assuming the SVG is UTF-8-encoded
		return $.ajax({
			url: svgEdit.fileUrl,
			cache: false,
			beforeSend: function (xhr) {
				xhr.overrideMimeType('text/plain; charset=UTF-8');
			}
		});
	},
	loadCodeEditor: function ($textArea/* , $parent*/) {
		// Just in case someone complains about the license ...
		var mirrors = [
				'//commons.wikimedia.org/w/index.php?',
				'//tools-static.wmflabs.org/rillke/CodeMirror/',
				'//mol-static.wmflabs.org/CodeMirror/'
			],
			scripts = ['lib/codemirror.js', 'mode/xml/xml.js'],
			styles = ['lib/codemirror.css'],
			params = {
				action: 'raw', ctype: 'text/javascript', title: '?'
			},

			rlScripts = $.map(scripts, function (el) {
				params.title = 'User:Rillke/CodeMirror/' + el;
				return mirrors[0] + $.param(params);
			});
		params.ctype = 'text/css';
		var rlStyles = $.map(styles, function (el) {
			params.title = 'User:Rillke/CodeMirror/' + el;
			return mirrors[0] + $.param(params);
		});

		if (!mw.loader.getState('mediawiki.commons.CodeMirror')) {
			mw.loader.implement('mediawiki.commons.CodeMirror',
				rlScripts, { url: { screen: rlStyles } },
				{ /* no messages*/ });
		}

		mw.loader.using('mediawiki.commons.CodeMirror', function () {
			var h = $textArea.parent().height(),
				m = $textArea.val()
					.slice(0, 6000)
					.match(/.+\n([\t ]+)<\S+(?:.|\n)*\n\1</),
				settings = {
					lineNumbers: true,
					mode: 'xml',
					viewportMargin: 120
				},
				l;

			if (m) {
				l = m[1].length;
				if (l > 0 && l < 9) {
					if (/ /.test(m[1])) {
						svgEdit.log('Indention with spaces');
						$.extend(true, settings, {
							extraKeys: { Tab: function () {
								svgEdit.CodeMirror.execCommand('insertSoftTab');
							} },
							tabSize: l
						});
					} else if (/\t/.test(m[1])) {
						svgEdit.log('Indention with tabs');
						$.extend(true, settings, {
							indentWithTabs: true,
							tabSize: 2
						});
					}
				}
			}
			svgEdit.CodeMirror = CodeMirror.fromTextArea($textArea[0], settings);
			$(svgEdit.CodeMirror.display.scroller).css({ height: (h - 5) + 'px' });
			$(svgEdit.CodeMirror.display.wrapper).css({
				border: '1px solid #EEE',
				height: 'auto'
			});
		});
	},
	save: function (text, summary) {
		if (summary)
			summary += ' // ';

		var message = svgEdit.bot.multipartMessageForUTF8Files()
			.appendPart('format', 'json')
			.appendPart('action', 'upload')
			.appendPart('filename', conf.wgTitle)
			.appendPart('comment', summary + 'Editing SVG source code using [[c:User:Rillke/SVGedit.js]]')
			.appendPart('file', text, conf.wgTitle)
			.appendPart('ignorewarnings', 1)
			.appendPart('token', mw.user.tokens.get('csrfToken'));
		if (isCommonsWiki)
			message.appendPart('tags', 'rillke-mw-js-bot');

		return message.$send();
	},
	fetchPreview: function (svg) {
		return svgEdit.bot.multipartMessageForUTF8Files()
			.appendPart('file', svg, 'input.svg')
			.$send('//convert.toolforge.org/svg2png.php', 'arraybuffer');
	},
	reload: function () {
		window.location.href = mw.util.getUrl(conf.wgPageName);
	},
	log: function () {
		var args = Array.prototype.slice.call(arguments);
		args.unshift(MYSELF);
		mw.log.apply(mw.log, args);
	}
};

// Register globally
if (!isCommonsWiki || conf.wgDBname !== 'commonsarchivewiki') {
	// mw.loader.addSource has a check for source key uniqueness
	// that if it fails, throws an error.
	// Since I am offering many scripts, I would like to be able to register
	// a source from multiple code positions. However the loader has no
	// accessors to its internally maintained list of sources. Therefore
	// ensure with high probabiltiy that every source key added is unique.
	commonsWiki[commonwWikiKey] = '//commons.wikimedia.org/w/load.php';
	mw.loader.addSource(commonsWiki);

	// Register Commons RL modules
	for (i = 0; i < modules.length; i++) {
		if (!mw.loader.getState(modules[i][0]))
			mw.loader.register([modules[i]]);
	}
}

// Expose globally
mw.libs.svgRawEditor = svgEdit;

mw.loader.using(['mediawiki.util', 'mediawiki.user', 'ext.gadget.editDropdown'], svgEdit.init);

}(jQuery, mediaWiki));
