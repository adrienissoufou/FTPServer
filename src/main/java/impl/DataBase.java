package impl;

import extensions.Encoder;

import java.sql.*;

public class DataBase {

    private static final String url = "jdbc:mysql://localhost:3306/Users";
    private static final String user = "root";
    private static final String password = "root";

    private static Connection con;
    private static Statement stmt;
    private static ResultSet rs;

    public static void init() throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.jdbc.Driver");
        con = DriverManager.getConnection(url, user, password);
        stmt = con.createStatement();

    }

    public static Boolean userExists(String username) throws SQLException {
        String str = "SELECT username FROM  `users` WHERE username = '" + username + "';";
        rs =  stmt.executeQuery(str);
        return rs.next();
    }

    public static Boolean userPasswordCorrect(String username, String password) throws SQLException {
        String hashedPassword = Encoder.md5(password);
        String str = "SELECT password FROM  `users` WHERE username = '" + username + "' AND password = '" + hashedPassword + "';";
        rs =  stmt.executeQuery(str);
        return rs.next();
    }

    public static Connection getCon() {
        return con;
    }

    public static Statement getStmt() {
        return stmt;
    }

    public static ResultSet getRs() {
        return rs;
    }
}
