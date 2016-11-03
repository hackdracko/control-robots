package com.evolve.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.firefox.internal.ProfilesIni;

import com.evolve.config.Configurations;
import com.evolve.util.Utileria;
import com.mysql.jdbc.Connection;
import com.mysql.jdbc.Statement;

public class WalmartDownload {
		
	static String portal = "walmart";
	static String cuenta;
	static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(WalmartDownload.class);
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
		
		log.info("Se ejecuta Walmart download");
		util.insertLog(cuenta, portal, "Download - Main: Se ejecuta download walmart", "success");
		prop = util.getPropertiesPortal();

		try {         			
			String filePrefix = prop.getProperty(portal+".prefix");									
			ArrayList<String> oldDates = util.getDatesFromLastReportFile();							
			
			for(int k=0; k < oldDates.size();k++) {              
				boolean result = util.checkIfExist(portal, filePrefix, oldDates.get(k));
                if (result == false) {
                    log.info("Comienza descarga de archivo " + filePrefix + oldDates.get(k));
                    util.insertLog(cuenta, portal, "Download - Main: Comienza descarga de archivo " + filePrefix + oldDates.get(k), "success");
                    driver.manage().window().maximize();
                    TimeUnit.SECONDS.sleep(5);
                    retailLinkAccess(driver, oldDates.get(k));
                } else {
                    log.info("Archivo " + filePrefix + oldDates.get(k)+ " anteriormente descargado");
                    util.insertLog(cuenta, portal, "Download - Main: Archivo " + filePrefix + oldDates.get(k)+ " anteriormente descargado", "success");
                    TimeUnit.SECONDS.sleep(3);
                }
			}
          
			log.info("[-]Termina ejecución del portal Walmart Spinmaster");
			terminaDriver(driver);
			
			//Se inserta en la BD			
			WalmartInsert.insertarBD(cuenta);
          
		} catch (InterruptedException e) {
			log.error("[-]Error al ejecutar Walmart LinkGenerator Spinmaster " + e.getMessage());
			util.insertLog(cuenta, portal, "Download - Main: Error al ejecutar robot", "error");
		} catch (ParseException ex) {
			Logger.getLogger(WalmartDownload.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	
	public static void retailLinkAccess(WebDriver driver, String fechaLink) throws ParseException {

        int indPortalEjec = 0;     
        String user="", pass="", urlLogin, pathFiles, filePrefix;
        
        //obteniendo el archivo de propiedades
        Properties prop = util.getPropertiesPortal();  
        
        //Obteniendo ruta de la carpeta de archivos/reportes
        pathFiles = config.getPathFolderData()+portal;

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
                                  
            urlLogin = prop.getProperty(portal+".urlLogin");
            filePrefix = prop.getProperty(portal+".prefix");
            
            if (indPortalEjec == 1) {            	 
            	
                    driver.get(urlLogin);
                    TimeUnit.SECONDS.sleep(5);
                    driver.findElement(By.id("txtUser")).sendKeys(user);
                    driver.findElement(By.id("txtPass")).sendKeys(pass);
                    driver.findElement(By.id("Login")).click();

                    TimeUnit.SECONDS.sleep(5);

                    boolean success = driver.getPageSource().contains("Su ID de Usuario o su contraseÃ±a estÃ¡n incorrectas.");

                    if (success == true) {                        
                        log.error("ERROR Logeo, Usuario y/o Password incorrectos"); 
                        util.insertLog(cuenta, portal, "Download - retailLinkAccess: usuario/password incorrectos", "error");
                        
                      //Desactivando el robot
                        try {
            				Statement s = (Statement) conn.createStatement();            				
            				String sql = "UPDATE proyectos_cadenas SET activo=0 WHERE nombreProyecto LIKE '%"+cuenta+"%' AND nombreCadena LIKE '%"+portal+"%'";
            				log.info(sql);
            				s.executeUpdate(sql);            				            				            				
            			} catch (SQLException e) {				
            				e.printStackTrace();
            			}       
                                                
                        driver.quit();
                        System.exit(0);
                    } 
                    else {
                    	log.info("Logueo correcto");
                    	String link = driver.findElement(By.xpath("//div[@class='span3 bold']/span[text()='evolve_webbot_" + fechaLink + "']")).getText();//BUENO                      
                    	WebElement download = driver.findElement(By.xpath("//div[@class='span3 statusResultsActions iconSize16 statusActionPad']/a[@href='#']/i[contains(@id, 'evolve_webbot_" + fechaLink + "')]"));
                    	download.click();
                    	log.info("Reporte : " + link);

                    	String winHandleBefore = driver.getWindowHandle();

                    	driver.getWindowHandles().stream().forEach((winHandle) -> {
                    		driver.switchTo().window(winHandle);
                    	});
                    	
                    	log.info("Url : " + driver.getCurrentUrl());
                        //String codigo = driver.getCurrentUrl().substring(51,60);
                        String text = driver.getPageSource().replace("\t", "|").replace("<html xmlns=\"http://www.w3.org/1999/xhtml\">"
                                + "<head><link title=\"Wrap Long Lines\" href=\"resource://gre-resources/plaintext.css\" type=\"text/css"
                                + "\" rel=\"alternate stylesheet\" /></head><body><pre>", "").replace("</pre></body></html>", "");

                        try {

                        	File file = new File(pathFiles+"\\"+filePrefix+fechaLink+".txt");

                            FileWriter fw = new FileWriter(file.getAbsoluteFile());
                            try (BufferedWriter bw = new BufferedWriter(fw)) {
                                bw.write(text);
                            }

                            log.info("[-]Termina la creación del archivo");
                            util.insertLog(cuenta, portal, "Download - retailLinkAccess: Termina creación de archivo", "error");

                        } 
                        catch (IOException ioe) {
                            log.error(ioe.getMessage());
                        }
                        
                        driver.close();
                        driver.switchTo().window(winHandleBefore);                                              

                        //WebElement delete = driver.findElement(By.xpath("//div[@class='span3 statusResultsActions iconSize16 statusActionPad']/a[@href='#']/i[contains(@id, 'd_" + codigo + "')]"));
                        //delete.click();
                        
                        TimeUnit.SECONDS.sleep(3);
                    }                                              
            } else if (indPortalEjec == 0) {
                log.info("Portal Retail Link Walmart Spinmaster desactivado");
                util.insertLog(cuenta, portal, "Download - retailLinkAccess: robot desactivado", "error");
            }

        } catch (NumberFormatException | InterruptedException e) {
            log.warn("Error al navegar en el Portal Retail Link Walmart Spinmaster " + fechaLink + e.getMessage());
            util.insertLog(cuenta, portal, "Download - retailLinkAccess: No se puede navegar", "error");
        }
    }

    public static void terminaDriver(WebDriver driver) {
    	if (driver != null) driver.quit(); 
    }
}
