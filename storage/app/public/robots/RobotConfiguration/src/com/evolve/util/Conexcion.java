package com.evolve.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 *
 * @author MarcoGante
 */
public class Conexcion {

    private static final String JDBC_CONNECTION_URL = "jdbc:mysql://localhost:3306/asistencias?useUnicode=true&characterEncoding=utf-8";
    private static final String JDBC_USERNAME = "root";
    private static final String JDBC_PASSWORD = "eVoRooT12.34.5";
    private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(Conexcion.class);

    public static Connection getConnection() {
        Connection con = null;
        try {
            
            Class.forName("com.mysql.jdbc.Driver");
            con = DriverManager.getConnection(JDBC_CONNECTION_URL,JDBC_USERNAME, JDBC_PASSWORD);

            if (con != null) {
                log.info("Conexion exitosa!");
            } else {
                log.error("Conexion fallida!");
            }

        } catch (ClassNotFoundException | SQLException e) {
            log.error(e.getMessage());
        }

        return con;
    }

}
