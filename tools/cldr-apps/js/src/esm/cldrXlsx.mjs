import * as XLSX from "xlsx";
/**
 * Add a comment to an XLSX sheet
 * @param {WorkSheet} ws sheet
 * @param {String|Object} where reference to comment location, such as C1  or {r:1, c:1}
 * @param {String} t Text to push
 */
function pushComment(ws, where, t) {
  if (typeof where === "object") {
    where = XLSX.utils.encode_cell(where);
  }
  ws[where].c = ws[where].c || [];
  ws[where].c.hidden = true;
  ws[where].c.push({ a: "SurveyTool", t });
}

export { pushComment };
