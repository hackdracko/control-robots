package com.evolve.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Configurations {
	private static String ENVIROMENT;
	private static String CUENTA;		
	
	public Configurations(String cuenta) {				
		CUENTA = cuenta;		
		ENVIROMENT = "production";
	}
	
	public String getEnviroment() {
		return ENVIROMENT;
	}
	
	public String getPathProyect() {					
		String pathDev = "C:\\Users\\SISTEMA\\Downloads\\evolve-web-master\\evolve-web-master\\ProyectosJava";
		String pathProd = "C:\\Users\\Administrator\\Desktop\\control-robots\\storage\\app\\public\\robots";		
		if (ENVIROMENT == "development")
			return pathDev;
		else 
			return pathProd;
	}
	
	public String getPathFolderData() {
		String pathDev = "C:\\Users\\SISTEMA\\Downloads\\evolve-web-master\\evolve-web-master\\ProyectosJava\\FilesData\\"+CUENTA+"\\";
		String pathProd = "D:\\DataRobots\\"+CUENTA+"\\";
		
		if (ENVIROMENT == "development")
			return pathDev;
		else 
			return pathProd;
	}
	
	public String getPathDownloads() {
		String pathDev = "C:\\Users\\Sistema\\Downloads";
		String pathProd = "C:\\Users\\Administrator\\Downloads";
		
		if (ENVIROMENT == "development")
			return pathDev;
		else 
			return pathProd;
	}
	
	public Connection getConnection() {
		Connection con = null;
		
		String JDBC_CONNECTION_URL="", JDBC_USERNAME="", JDBC_PASSWORD="", DRIVER="";
		DRIVER = "com.mysql.jdbc.Driver";
		JDBC_USERNAME = "root";
		
		switch (CUENTA) {
			case "cesarfer":
				JDBC_CONNECTION_URL = "jdbc:mysql://localhost:3306/cuboCesarfer?useUnicode=true&amp;characterEncoding=UTF-8";				
				if (ENVIROMENT == "development") 
					JDBC_PASSWORD = "";
				else 
					JDBC_PASSWORD = "eVoRooT12.34.2016";			    			    
			break;
			
			case "spinmaster":
				JDBC_CONNECTION_URL = "jdbc:mysql://localhost:3306/cuboSpinMaster?useUnicode=true&amp;characterEncoding=UTF-8";				
				System.out.println(JDBC_CONNECTION_URL);
				if (ENVIROMENT == "development") 
					JDBC_PASSWORD = "";
				else 
					JDBC_PASSWORD = "eVoRooT12.34.2016";			    			    
			break;
			
			case "underarmour":
				JDBC_CONNECTION_URL = "jdbc:mysql://localhost:3306/cuboUnderarmour?useUnicode=true&amp;characterEncoding=UTF-8";				
				if (ENVIROMENT == "development") 
					JDBC_PASSWORD = "";
				else 
					JDBC_PASSWORD = "eVoRooT12.34.2016";			    			    
			break;
			case "timberland":
				JDBC_CONNECTION_URL = "jdbc:mysql://localhost:3306/cuboTimberland?useUnicode=true&amp;characterEncoding=UTF-8";				
				if (ENVIROMENT == "development") 
					JDBC_PASSWORD = "";
				else 
					JDBC_PASSWORD = "eVoRooT12.34.2016";			    			    
			break;
		}		
  
        try {        	
            Class.forName(DRIVER);
            con = DriverManager.getConnection(JDBC_CONNECTION_URL, JDBC_USERNAME, JDBC_PASSWORD);            
        } catch (ClassNotFoundException | SQLException e) {
        	System.out.println("No se pudo connectar a la BD "+e.getMessage());
        }

        return con;
	}	
	
	public Connection getConnectionGeneral() {
		Connection con = null;
		
		String JDBC_CONNECTION_URL="", JDBC_USERNAME="", JDBC_PASSWORD="", DRIVER="";
		DRIVER = "com.mysql.jdbc.Driver";
		
		JDBC_CONNECTION_URL = "jdbc:mysql://localhost:3306/cubogeneral?useUnicode=true&amp;characterEncoding=UTF-8";
		JDBC_USERNAME = "root";
		if (ENVIROMENT == "development") 
			JDBC_PASSWORD = "";
		else 
			JDBC_PASSWORD = "eVoRooT12.34.2016";
  
        try {        	
            Class.forName(DRIVER);
            con = DriverManager.getConnection(JDBC_CONNECTION_URL, JDBC_USERNAME, JDBC_PASSWORD);            
        } catch (ClassNotFoundException | SQLException e) {
        	System.out.println("No se pudo connectar a la BD "+e.getMessage());
        }

        return con;
	}	
}
