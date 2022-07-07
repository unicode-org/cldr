/**
 * Add a comment to an XLSX sheet
 * @param {WorkSheet} ws sheet
 * @param {String} where reference to comment location, such as C1
 * @param {String} t Text to push
 */
function pushComment(ws, where, t) {
  ws[where].c = ws[where].c || [];
  ws[where].c.hidden = true;
  ws[where].c.push({ a: "SurveyTool", t });
}

export { pushComment };
