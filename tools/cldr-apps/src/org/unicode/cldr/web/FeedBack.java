package org.unicode.cldr.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class FeedBack
 */
public class FeedBack extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String TABLE_FEEDBACK = "FEEDBACK";

    /**
     * @see HttpServlet#HttpServlet()
     */
    public FeedBack() {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // TODO Auto-generated method stub
        this.createTable();
        Connection conn = DBUtils.getInstance().getDBConnection();
        try {
            Statement ps = conn.createStatement();
            ResultSet rs = ps.executeQuery("SELECT email,content, date FROM " + this.TABLE_FEEDBACK + " ORDER BY date DESC");
            PrintWriter out = response.getWriter();
            while (rs.next()) {
                out.println("======");
                out.println("Email: " + rs.getString(1));
                out.println("Date: " + rs.getTimestamp(3).toLocaleString());
                out.println("Content : " + rs.getString(2));
            }

        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // TODO Auto-generated method stub
        this.createTable();

        String email = request.getParameter("email");
        String content = request.getParameter("content");
        Connection conn = DBUtils.getInstance().getDBConnection();
        PreparedStatement ps;
        try {
            ps = conn.prepareStatement("INSERT INTO " + this.TABLE_FEEDBACK + " (email,content, date) VALUES (?,?,CURRENT_TIMESTAMP)");
            ps.setString(1, email);
            ps.setString(2, content);
            ps.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        response.getWriter().print("Ok");
    }

    private void createTable() {
        if (!DBUtils.hasTable(this.TABLE_FEEDBACK)) {
            Statement s;
            Connection conn = DBUtils.getInstance().getDBConnection();
            try {
                s = conn.createStatement();
                s.execute("CREATE TABLE " + this.TABLE_FEEDBACK + " (id int not null " + DBUtils.DB_SQL_IDENTITY + ", email varchar(230) not null, content "
                    + DBUtils.DB_SQL_BIGTEXT + " not null, date TIMESTAMP not null)");
                s.execute("CREATE UNIQUE INDEX " + this.TABLE_FEEDBACK + "_id ON " + this.TABLE_FEEDBACK + " (id) ");
                s.close();

                conn.commit();
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

}
