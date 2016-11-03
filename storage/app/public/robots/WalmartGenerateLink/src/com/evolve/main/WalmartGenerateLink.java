package com.evolve.main;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

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

public class WalmartGenerateLink {
		
	static String portal = "walmart";
	static String cuenta;
	static Configurations config;
	static Utileria util;
	static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(WalmartGenerateLink.class);	
	static Properties prop = new Properties();		
	
	public static void main(String[] args) {				
		
		cuenta = args[0];
		config = new Configurations(cuenta);
		util = new Utileria(portal, cuenta);
		
        ProfilesIni profile = new ProfilesIni();
        FirefoxProfile ffprofile = profile.getProfile("firefox");
        WebDriver driver = new FirefoxDriver(ffprofile);
        JavascriptExecutor jse = (JavascriptExecutor) driver;
                
		
		log.info("Se ejecuta Walmart Link Generator Cesarfer");	
		util.insertLog(cuenta, portal, "GenerateLink - Main: Se ejecuta walmart generate link", "success");
		prop = util.getPropertiesPortal();

		try {         			
			String filePrefix = prop.getProperty(portal+".prefix");						
			String pathFiles = config.getPathFolderData()+portal;
			
			ArrayList<String> oldDates = util.getDatesFromLastReportFile();			
			
			int counter = 1;						
			for(int k=0; k < oldDates.size();k++) {              
				boolean result = util.checkIfExist(pathFiles, filePrefix, oldDates.get(k));				
				
				if (result == false) {
					log.info("Comienza descarga de archivo " + filePrefix + oldDates.get(k));
					util.insertLog(cuenta, portal, "GenerateLink - Main: Comienza generacion link de archivo " + filePrefix + oldDates.get(k), "success");
					driver.manage().window().maximize();
					TimeUnit.SECONDS.sleep(10);
					retailLinkAccess(driver, jse, oldDates.get(k), counter);                              	                    
					counter++;					
				} 
				else {
					log.info("Archivo " + filePrefix + oldDates.get(k)+ " anteriormente descargado");
					util.insertLog(cuenta, portal, "GenerateLink - Main: Archivo " + filePrefix + oldDates.get(k)+ " anteriormente descargado", "success");
					TimeUnit.SECONDS.sleep(3);                                                  
				}
			}
          
			log.info("[-]Termina ejecución del portal Walmart Spinmaster");
			util.insertLog(cuenta, portal, "GenerateLink - Main: Termina ejecución de Walmart generate link", "success");
			
			terminaDriver(driver);
          
		} catch (InterruptedException e) {
			log.error("[-]Error al ejecutar Walmart LinkGenerator Spinmaster " + e.getMessage());
			util.insertLog(cuenta, portal, "GenerateLink - Main: Error al ejecutar el generador de link", "error");
		} catch (ParseException ex) {
			Logger.getLogger(WalmartGenerateLink.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	
	public static void retailLinkAccess(WebDriver driver, JavascriptExecutor jse, String fecha, int counter) throws ParseException {

        int indPortalEjec = 0; 
        String user="";
        String pass="";    
        String urlLinkGenerator;
        
        Utileria util = new Utileria(portal, cuenta);
        
        //obteniendo el archivo de propiedades
        Properties prop = util.getPropertiesPortal();              

        try {
        	
        	//Obteniendo si el robot esta activo, usuario y password del portal
        	Connection conn = (Connection) config.getConnectionGeneral();        	
			ResultSet rs;
			try {
				Statement s = (Statement) conn.createStatement();				
				rs = s.executeQuery ("SELECT * FROM proyectos_cadenas WHERE nombreProyecto LIKE '%"+cuenta+"%' AND nombreCadena LIKE '%"+portal+"%' LIMIT 1");				
				rs.next();
				user = rs.getString("usuario");
				pass = rs.getString("password");
				indPortalEjec = rs.getInt("activo");
			} catch (SQLException e) {				
				e.printStackTrace();
			}			
        	                               
            urlLinkGenerator = prop.getProperty(portal+".urlLinkGenerator");            
            
            if (indPortalEjec == 1) {

                if (counter == 1) {
                    System.err.println("ENTRA NUMERO : " + counter);
                    TimeUnit.SECONDS.sleep(7);
                    
                    driver.get(urlLinkGenerator);
                    
                    TimeUnit.SECONDS.sleep(10);

                    driver.findElement(By.id("txtUser")).sendKeys(user);
                    driver.findElement(By.id("txtPass")).sendKeys(pass);
                    driver.findElement(By.id("Login")).click();

                    TimeUnit.SECONDS.sleep(5);

                    boolean success = driver.getPageSource().contains("Su ID de Usuario o su contraseña están incorrectas.");

                    if (success == true) {                        
                        log.error("ERROR Logeo, Usuario y/o Password incorrectos");
                        util.insertLog(cuenta, portal, "GenerateLink - retailLinkAccess: ERROR Logeo, Usuario y/o Password incorrectos", "error");                        
                        
                        //Desactivando el robot
                        try {
            				Statement s = (Statement) conn.createStatement();            				
            				String sql = "UPDATE proyectos_cadenas SET activo=0 WHERE nombreProyecto LIKE '%"+cuenta+"%' AND nombreCadena LIKE '%"+portal+"%'";
            				log.info(sql);
            				s.executeUpdate(sql);            				            				            				
            			} catch (SQLException e) {				
            				e.printStackTrace();
            			}       
                        
                        System.exit(0);
                        driver.quit();
                        //email.setParamError(user, pass, portal, cliente, idMsj, nameFile, fecha1);
                        //util.setProperties(properties, RET_CLIEN_NAME, pathProperties);
                    } else {

                        driver.findElement(By.id("frmMain"));
                        driver.switchTo().frame("content_6");
                        driver.switchTo().frame("SubmitWindow");
                        driver.findElement(By.xpath("//input[@name='Description']"));
                        driver.findElement(By.name("Description"));

                        jse.executeScript("document.forms[0]['Description'].value ='evolve_webbot_" + fecha + "'");
                        jse.executeScript("document.forms[0]['submitnow'].value = 'true'");
                        jse.executeScript("return document.getElementById('DivisionId').innerHTML");
                        jse.executeScript("document.forms[0]['DivisionId'].value = '1'");
                        jse.executeScript("return document.getElementById('CountryCode').innerHTML");
                        jse.executeScript("document.forms[0]['CountryCode'].value = 'MX'");
                        jse.executeScript("for(var i = 0; i < 10; i++){ document.forms[0]['Criteria'].value = document.forms[0]['Criteria'].value.replace('10-26-2014',' " + fecha + " ');}");
                        jse.executeScript("document.forms[0].submit();");                       
                    }
                
                } else {
                    
                    System.err.println("ENTRA NUMERO : " + counter);
                    TimeUnit.SECONDS.sleep(5);
                    driver.get(urlLinkGenerator);                    
                    TimeUnit.SECONDS.sleep(3);
                    
                    driver.findElement(By.id("frmMain"));
                    driver.switchTo().frame("content_6");
                    driver.switchTo().frame("SubmitWindow");
                    driver.findElement(By.xpath("//input[@name='Description']"));
                    driver.findElement(By.name("Description"));

                    jse.executeScript("document.forms[0]['Description'].value ='evolve_webbot_" + fecha + "'");
                    jse.executeScript("document.forms[0]['submitnow'].value = 'true'");
                    jse.executeScript("return document.getElementById('DivisionId').innerHTML");
                    jse.executeScript("document.forms[0]['DivisionId'].value = '1'");
                    jse.executeScript("return document.getElementById('CountryCode').innerHTML");
                    jse.executeScript("document.forms[0]['CountryCode'].value = 'MX'");
                    jse.executeScript("for(var i = 0; i < 10; i++){ document.forms[0]['Criteria'].value = document.forms[0]['Criteria'].value.replace('10-26-2014',' " + fecha + " ');}");
                    jse.executeScript("document.forms[0].submit();");                    
                }
                

            } else if (indPortalEjec == 0) {
                log.info("Portal Retail Link Walmart Spinmaster desactivado");
                util.insertLog(cuenta, portal, "GenerateLink - retailLinkAccess: Robot Desactivado", "error");
            }

        } catch (NumberFormatException | InterruptedException e) {
            log.warn("Error al navegar en el Portal Retail Link Walmart Spinmaster " + fecha + e.getMessage());
            util.insertLog(cuenta, portal, "GenerateLink - retailLinkAccess: No se puede navegar", "error");
        }
    }
	
	public static void terminaDriver(WebDriver driver) {
    	if (driver != null) driver.quit(); 
    }	
}
