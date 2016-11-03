package com.evolve.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.firefox.internal.ProfilesIni;

import com.evolve.config.Configurations;
import com.evolve.util.Utileria;
import com.mysql.jdbc.Connection;
import com.mysql.jdbc.Statement;

public class SorianaDownload {
		
	static String portal = "soriana";
	static String cuenta;
	static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(SorianaDownload.class);
	static Configurations config;
	static Properties prop = new Properties();
	static Utileria util;	
	
	public static void main(String[] args) {		
		
		cuenta = args[0];
		config = new Configurations(cuenta);
		util = new Utileria(portal, cuenta);		
		
        ProfilesIni profile = new ProfilesIni();
        FirefoxProfile ffprofile = profile.getProfile("firefox");
        WebDriver driver = new FirefoxDriver(ffprofile);   
        JavascriptExecutor jse = (JavascriptExecutor) driver;
		
        log.info("MAIN: Se ejecuta Soriana "+cuenta);  
        util.insertLog(cuenta, portal, "INICIO - Main: se ejecuta Soriana", "success");
        prop = util.getPropertiesPortal();

        try {
            
        	String filePrefix = prop.getProperty(portal+".prefix");	        	        	
			ArrayList<String> oldDates = util.getDatesFromLastReportFileFormat("ddMMyyyy");			
			
            for(int k=0; k < oldDates.size();k++){                                
                boolean result = util.checkIfExist(portal, filePrefix, oldDates.get(k));
                if (result == false) {
                    log.info("MAIN: Comienza descarga de archivo " + filePrefix + oldDates.get(k));
                    util.insertLog(cuenta, portal, "INICIO - Main: Comienza descarga de archivo " + filePrefix + oldDates.get(k), "success");
                    driver.manage().window().maximize();
                    
                    String fechaUrl = getFormatDate(oldDates.get(k));
                    System.out.println("valor de fechaurl = "+fechaUrl);                 
                    
                    accessSoriana(driver, jse, oldDates.get(k));
                                     
                } else {
                    log.info("MAIN: Archivo " + filePrefix + oldDates.get(k)+ " anteriormente descargado");
                    util.insertLog(cuenta, portal, "INICIO - Main: Archivo " + filePrefix + oldDates.get(k)+ " anteriormente descargado", "success");
                    TimeUnit.SECONDS.sleep(3);
                }
            }                   
            
            log.info("MAIN: Termina ejecución del portal Soriana "+cuenta);
            util.insertLog(cuenta, portal, "INICIO - Main: Terminan ejecución del portal", "success");
            terminaDriver(driver);
            
          //Insertando en la BD
            SorianaInsert.insertarBD(cuenta);
            
        } catch (IOException | InterruptedException e) {
        	log.error("Error al ejecutar portal Soriana" + e.getMessage());
        	util.insertLog(cuenta, portal, "INICIO - Main: Error al ejecutar el portal soriana", "error");
        }        
	}
	
	public static void accessSoriana(WebDriver driver, JavascriptExecutor jse, String fecha) throws IOException {

		int iIndPortalEjec = 0;     
        String user="", pass="", urlLogin;
        
        //obteniendo el archivo de propiedades
        Properties prop = util.getPropertiesPortal();
        
        //Obteniendo si el robot esta activo, usuario y password del portal
    	Connection conn = (Connection) config.getConnectionGeneral();        	
		ResultSet rs;
		try {
			Statement s = (Statement) conn.createStatement();				
			rs = s.executeQuery ("SELECT * FROM proyectos_cadenas WHERE nombreProyecto LIKE '%"+cuenta+"%' AND nombreCadena LIKE '%"+portal+"%' LIMIT 1");
			rs.next();
			user = rs.getString("usuario");
			pass = rs.getString("password");
			iIndPortalEjec = rs.getInt("activo");
		} catch (SQLException e) {				
			e.printStackTrace();
		}
		
		urlLogin = prop.getProperty(portal+".urlLogin");        

        try {                        
            if (iIndPortalEjec == 1) {

                driver.get(urlLogin);
                driver.findElement(By.name("input_Email")).sendKeys(user);
                driver.findElement(By.name("input_Password")).sendKeys(pass);
                driver.findElement(By.name("continuar")).click();

                boolean success = driver.getPageSource().contains("La cuenta de correo fué localizada pero la clave de acceso no corresponde.");
                if (success == true) {                    
                    log.warn("MAIN: Logeo, Usuario y/o Password incorrectos ");                    
                    util.insertLog(cuenta, portal, "INICIO - accessSoriana: Logueo incorrecto", "error");                    
                                                          
                    //Desactivando el robot
                    terminaDriver(driver);
                    try {
        				Statement s = (Statement) conn.createStatement();            				
        				s.executeUpdate ("UPDATE proyectos_cadenas SET activo=0 WHERE nombreProyecto LIKE '%"+cuenta+"%' AND nombreCadena LIKE '%"+portal+"%'");            				            				            				
        			} catch (SQLException e) {				
        				e.printStackTrace();
        			}
                    
                    System.exit(0);
                } else {

                    String fechaUrl = getFormatDate(fecha);                    
                    TimeUnit.SECONDS.sleep(5);
                    driver.get("http://proveedor.soriana.com/sprov/activ/Gen_Env_Arch_Dia.asp?cia=1&Fec_Arch="+fechaUrl+"");
                    log.info("http://proveedor.soriana.com/sprov/activ/Gen_Env_Arch_Dia.asp?cia=1&Fec_Arch="+fechaUrl+"");
                    TimeUnit.SECONDS.sleep(3);
                    String url = driver.getCurrentUrl();
                    driver.get(driver.getCurrentUrl());
                    log.info("URL | " + url);
                    String text = driver.getPageSource().replace("!", "|").replace("<html xmlns=\"http://www.w3.org/1999/xhtml\">"
                            + "<head><link title=\"Wrap Long Lines\" href=\"resource://gre-resources/plaintext.css\" type=\"text/css\" rel=\"alternate stylesheet\" />"
                            + "</head><body><pre>", "").replace("</pre></body></html>", "");
                    
                    String pathFiles = config.getPathFolderData();
                    String urlFile = pathFiles+portal+"\\"+prop.getProperty(portal+".prefix")+fecha + ".txt";
                    File file = new File(urlFile);

                    FileWriter fw;
                    try {
                        fw = new FileWriter(file.getAbsoluteFile());
                        try (BufferedWriter bw = new BufferedWriter(fw)) {
                            bw.write(text);
                        }
                    } catch (IOException ex) {
                        java.util.logging.Logger.getLogger(SorianaDownload.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

            } else {
                if (iIndPortalEjec == 0) {
                    log.info("[-]Portal Soriana Desactivado");
                    util.insertLog(cuenta, portal, "INICIO - accessSoriana: Robot desactivado", "error");
                }
            }

        } catch (NumberFormatException | InterruptedException e) {
            log.error(e.getMessage());
        }
    }

    public static void terminaDriver(WebDriver driver) {
        if (driver != null) {
            driver.quit();
        }
    }
    
    public static String getFormatDate(String fechaN) {
        String fecha;

        String dia = fechaN.substring(0, 2);
        String mes = fechaN.substring(2, 4);
        String anio = fechaN.substring(4);

        fecha = anio + mes + dia;
        log.info("FECHA FOTRMAT: " + fecha);
        return fecha;
    }
}
