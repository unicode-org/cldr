deferredHelp = (function() {
	const deferHelpData = {};
	
	const showHelp = function showHelp(theHelp, data) {
        theHelp = $(theHelp);
        theHelp.empty();
        if(data.helpHtml) {
            theHelp.append($('<span/>', {
                html: data.helpHtml,
                class: 'helpHtml'
            }));
        }
        if(data.abstract) {
            const absDiv = $('<div/>', {class: 'helpAbstract'});
            absDiv.append($('<p/>', {text: data.abstract.abstract}));
            absDiv.append($('<a/>', {
                text: 'Source: ' + data.abstract.resource,
                href: data.abstract.resource}));
            theHelp.append(absDiv);
        }
    };
	/**
	 */
	const addDeferredHelpTo = function addDeferredHelpTo(fragment, xpath) {
		// Always have help (if available).
		var theHelp = null;
        theHelp = createChunk("", "div", "alert alert-info fix-popover-help vote-help");


		if (deferHelpData[xpath]) {
           showHelp(theHelp, deferHelpData[xpath]);
		} else {
			theHelp.append($('<i>loading...</i>'));

			$.ajax(`${contextPath}/SurveyAjax?what=abstract&xpath=${xpath}`)
			.then(data => {
				deferHelpData[xpath] = data;
				showHelp(theHelp, data);
			}, err => {
				$(theHelp).empty();
				$(theHelp).append($('</i>', {text: `error: ${e}`}));
				});
		}
		if (theHelp) {
			fragment.appendChild(theHelp);
		}

	}

	return {
		addDeferredHelpTo,
		deferHelpData
	};
})();
