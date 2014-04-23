package org.unicode.cldr.web;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.unicode.cldr.util.Log;
import org.unicode.cldr.util.VettingViewer.Choice;
import org.unicode.cldr.web.DBUtils.Table;


//save hidden line 
public class ReviewHide {

    private HashMap<String, List<Integer>> hiddenField;
    private Connection conn;
        
    public ReviewHide() {
        this.hiddenField = new HashMap<String, List<Integer>>();
        this.conn = DBUtils.getInstance().getDBConnection(); 
    }
    
    //create the table (path, locale, type of notifications as key to get unique line)
    public static void createTable(Connection conn) throws SQLException {
        if(!DBUtils.hasTable(DBUtils.Table.REVIEW_HIDE.toString())) {
            Statement s;
                s = conn.createStatement();
                s.execute("CREATE TABLE "+DBUtils.Table.REVIEW_HIDE+" (id int not null "+DBUtils.DB_SQL_IDENTITY+", path int not null, choice varchar(20) not null, user_id int not null, locale varchar(20) not null, FOREIGN KEY (user_id) REFERENCES "+UserRegistry.CLDR_USERS+"(id) ON DELETE CASCADE)");
                s.execute("CREATE UNIQUE INDEX " + DBUtils.Table.REVIEW_HIDE + "_id ON " + DBUtils.Table.REVIEW_HIDE + " (id) ");
                s.close();
                
                conn.commit();
               
        
        }
    }
    
    //get all the field for an user and locale
    public HashMap<String, List<Integer>> getHiddenField(int userId, String locale) {
        if(this.hiddenField.isEmpty()) {
            try {
                PreparedStatement s = conn.prepareStatement("SELECT * FROM "+DBUtils.Table.REVIEW_HIDE+" WHERE user_id=? AND locale=?");
                s.setInt(1, userId);
                s.setString(2, locale);
                ResultSet rs = s.executeQuery();
                while(rs.next()){       
                        String choice = rs.getString("choice");
                        List<Integer> paths = this.hiddenField.get(choice);
                        if(paths == null)
                            paths = new ArrayList<Integer>();
                        paths.add(rs.getInt("path"));
                        
                        this.hiddenField.put(choice,paths);
                }
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        return this.hiddenField;
    }
    
    //get the hidden field as JSON
    public JSONObject getJSONReviewHide(int userId, String locale) {
        JSONObject notification = new JSONObject();
        for(Entry <String, List<Integer>> entry : this.getHiddenField(userId, locale).entrySet()) {
            try {
                notification.accumulate(entry.getKey(), entry.getValue());
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return notification;
    }
    
    //insert or delete a line to hide/show
    public void toggleItem(String choice, int path, int user, String locale) {
        try {
            PreparedStatement ps = this.conn.prepareStatement("SELECT * FROM "+DBUtils.Table.REVIEW_HIDE+" WHERE path=? AND user_id=? AND choice=? AND locale=?");
            PreparedStatement updateQuery;
            ResultSet rs;
            ps.setInt(1, path);
            ps.setInt(2, user);
            ps.setString(3, choice);
            ps.setString(4, locale);
            rs = ps.executeQuery();
            
            if(!rs.next()) {
                //the item is currently shown, not in the table, we can hide it
                updateQuery = this.conn.prepareStatement("INSERT INTO "+DBUtils.Table.REVIEW_HIDE+" (path, user_id,choice,locale) VALUES(?,?,?,?)");
            }
            else {
                updateQuery = this.conn.prepareStatement("DELETE FROM "+DBUtils.Table.REVIEW_HIDE+" WHERE path=? AND user_id=? AND choice=? AND locale=?");
            }
            
            updateQuery.setInt(1, path);
            updateQuery.setInt(2, user);
            updateQuery.setString(3, choice);
            updateQuery.setString(4, locale);
            updateQuery.executeUpdate();
            this.conn.commit();
            
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
  }
}
