package com.evolve.main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.firefox.internal.ProfilesIni;

import com.evolve.config.Configurations;
import com.evolve.util.Utileria;
import com.mysql.jdbc.Connection;
import com.mysql.jdbc.Statement;

public class LiverpoolDownload {
		
	static String portal = "liverpool";
	static String cuenta, seccion, finicial, ffinal;
	static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(LiverpoolDownload.class);
	static Configurations config;
	static Properties prop = new Properties();
	static Utileria util;
	
	public static void main(String[] args) {
		
		cuenta = args[0];
		seccion = args[1];
		finicial = args[2];
		ffinal = args[3];
		config = new Configurations(cuenta);
		util = new Utileria(portal, cuenta);		
		
        ProfilesIni profile = new ProfilesIni();
        FirefoxProfile ffprofile = profile.getProfile("firefox");
        WebDriver driver = new FirefoxDriver(ffprofile);   
        JavascriptExecutor jse = (JavascriptExecutor) driver;
		
        log.info("MAIN: Se ejecuta Liverpool "+cuenta);
        util.insertLog(cuenta, portal, "Download - Main: Se ejecuta Liverpool", "success");
        prop = util.getPropertiesPortal();
        
        try {

        	String filePrefix = prop.getProperty(portal+".prefix");									
			ArrayList<String> oldDates = util.getDatesFromLastReportFileFormatSeccion("ddMMyyyy", seccion, finicial, ffinal);
            
            int counter = 1;
            for(int k=0; k < oldDates.size();k++) {
                
                boolean result = util.checkIfExist(portal, filePrefix, oldDates.get(k));
                if (result == false) {
                	log.info("MAIN: Comienza descarga de archivo " + filePrefix + oldDates.get(k));
                	util.insertLog(cuenta, portal, "Download - Main: Comienza descarga de archivo", "success");
                    driver.manage().window().maximize();
                    accessLiverpool(driver, jse, oldDates.get(k), counter, seccion);
                    counter ++;
                    
                } 
                else {
                    if (counter > 1) {
                        log.info("Archivo " + filePrefix + oldDates.get(k)+ " anteriormente descargado");
                        TimeUnit.SECONDS.sleep(3);
                    }
                }
            }
            
            log.info("[-]Termina ejecución del portal Liverpool Spinmaster");
            util.insertLog(cuenta, portal, "Download - Main: Termina ejecución robot Liverpool", "succcess");
            terminaDriver(driver);
            
            //Insertando en la BD
            LiverpoolInsert.insertarBD(cuenta, seccion);
            
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            log.error("[-]Error al ejecutar Liverpool Spinmaster " + e.getMessage());
            util.insertLog(cuenta, portal, "Download - Main: Error al ejecutar Liverpool", "error");
        } catch (InterruptedException ex) {
            Logger.getLogger(LiverpoolDownload.class.getName()).log(Level.SEVERE, null, ex);
        }
	}
	
	public static void accessLiverpool(WebDriver driver, JavascriptExecutor jse, String fecha, int count, String seccion) throws FileNotFoundException, UnsupportedEncodingException {

		int indPortalEjec = 0;     
        String user="", pass="", urlLogin, filePrefix;
        
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
				util.insertLog(cuenta, portal, "GenerateLink - accessLiverpool: ERROR Logeo, Usuario y/o Password incorrectos", "error");
                System.exit(0);
                driver.quit();
			}
			
			urlLogin = prop.getProperty(portal+".urlLogin");
            filePrefix = prop.getProperty(portal+".prefix");

            if (indPortalEjec == 1) {
                
                if (count == 1) {
                    System.err.println("ENTRA NUMERO : " + count);
                    driver.get(urlLogin);
                    driver.findElement(By.id("logonuidfield")).sendKeys(user);
                    driver.findElement(By.id("logonpassfield")).sendKeys(pass);
                    driver.findElement(By.name("uidPasswordLogon")).click();
                    boolean success = driver.getPageSource().contains("Autentificación de usuario fallida");

                    if (success == true) {                        
                        log.warn("ACCESS: ERROR Logeo, Usuario y/o Password incorrectos ");   
                        util.insertLog(cuenta, portal, "Download - AccessLiverpool: Logeo, Usuario y/o Password incorrectos", "error");
                        terminaDriver(driver);
                        
                        //Desactivando el robot
                        try {
            				Statement s = (Statement) conn.createStatement();            				
            				s.executeUpdate ("UPDATE proyectos_cadenas SET activo=0 WHERE nombreProyecto LIKE '%"+cuenta+"%' AND nombreCadena LIKE '%"+portal+"%'");            				            				            				
            			} catch (SQLException e) {				
            				e.printStackTrace();
            			}
                        
                        System.exit(0);
                    } else {
                        
                    driver.get("https://bwsext.liverpool.com.mx/sap/bw/BEx?sap-language=es&sap-client=400&accessibility=&style_sheet=&TEMPLATE_ID=BWR_VTAS_POR_DIA_PROV");
                    TimeUnit.SECONDS.sleep(2);
                    
                    WebElement seccionWeb = driver.findElement(By.xpath("/html/body/form/table/tbody/tr/td/table/tbody/tr/td/table/tbody/tr/td/table/tbody/tr/td/input"));
                    seccionWeb.sendKeys(seccion);
                    
                    driver.findElement(By.xpath("/html/body/form/table/tbody/tr/td/table/tbody/tr[5]/td"
                            + "/table/tbody/tr[1]/td[2]/table/tbody/tr/td/table/tbody/tr/td/a/nobr")).click();

                    boolean opcion = driver.getPageSource().contains("Introduzca un valor p.la variable Período");

                    if (opcion == true) {

                        log.info("ACCESS: Entra Periodo");
                        driver.findElement(By.xpath("//*[@id=\"VAR_VALUE_LOW_EXT_14\"]")).sendKeys(fecha);
                        driver.findElement(By.xpath("//*[@id=\"VAR_VALUE_HIGH_EXT_14\"]")).sendKeys(fecha);
                        driver.findElement(By.xpath("/html/body/form/table/tbody/tr/td/table/tbody/tr[5]/td/table/tbody/tr[1]/td[2]/table/tbody/tr/td/table/tbody/tr/td/a/nobr")).click();
                        TimeUnit.SECONDS.sleep(20);

                    } else {

                            log.info("ACCESS: Sin Periodo");
                            driver.findElement(By.xpath("/html/body/font/table/tbody/tr/td/table/tbody/tr[2]/td/table/tbody/tr/td/table/tbody/tr/td/table/tbody/tr/td[6]/a/img")).click();
                            String winHandleBefore = driver.getWindowHandle();

                            for (String winHandle : driver.getWindowHandles()) {
                                driver.switchTo().window(winHandle);
                            }

                            String url = driver.getCurrentUrl();
                            log.info("ACCESS: url: "+url);
                            driver.findElement(By.id("FILTER"));
                            WebElement input1 = driver.findElement(By.xpath("/html/body/form/table/tbody/tr/td/table/tbody/tr[5]/td[2]/table/tbody/tr[1]/td[4]/input"));
                            WebElement input2 = driver.findElement(By.xpath("/html/body/form/table/tbody/tr/td/table/tbody/tr[5]/td[2]/table/tbody/tr[1]/td[7]/input"));
                            input1.clear();
                            SimpleDateFormat sdfSource = new SimpleDateFormat("ddMMyyyy");
                            Date date = sdfSource.parse(fecha);
                            SimpleDateFormat sdfDestination = new SimpleDateFormat("dd.MM.yyyy");
                            String fechaFmt = sdfDestination.format(date);
                            log.info("Fecha periodo: "+fechaFmt);
                            input1.sendKeys(fechaFmt);//"22.02.2015"
                            input2.clear();
                            input2.sendKeys(fechaFmt);
                            log.info("Enviando peticion click");
                            driver.findElement(By.xpath("/html/body/form/table/tbody/tr/td/table/tbody/tr[21]/td[2]/table/tbody/tr/td[1]/table/tbody/tr/td/table/tbody/tr/td/a/nobr")).click();
                            driver.switchTo().window(winHandleBefore);

                        }

                        jse.executeScript("SAPBW(1,'','','EXPAND','0MATERIAL','Y');");

                        TimeUnit.SECONDS.sleep(15);

                        driver.get("https://bwsext.liverpool.com.mx/sap/bw/BEx?SAP-LANGUAGE=ES&PAGENO=1&REQUEST_NO=4&CMD=EXPORT&DATA_PROVIDER=DATAPROVIDER_4&FORMAT=CSV&SEPARATOR=,");

                        TimeUnit.SECONDS.sleep(15);

                        /*If page is not the page with file*/
                        driver.getCurrentUrl();
                        boolean checkPage = driver.getPageSource().contains("Resultado total");
                        if (checkPage == false) {                            
                            log.warn("ACCESS: ERROR el portal no cargo correctamente, cerrando portal para nuevo intento");
                            util.insertLog(cuenta, portal, "Download - AccessLiverpool: El portal no cargo correctamente, cerrando portal para nuevo intento", "error");
                        } 
                        else {

                            String text = driver.getPageSource()
                                    .replace("<html xmlns=http://www.w3.org/1999/xhtml><head><link title=Wrap Long Lines href=resource://gre-resources/plaintext.css type=text/css rel=alternate stylesheet /></head><body><pre>", "")
                                    .replace("</pre></body></html>", "");
                            
                            String copy = new String();

                            boolean inQuotes = false;

                            for(int i=0; i<text.length(); ++i)
                            {
	                        	if (text.charAt(i)=='"')
	                        	    inQuotes = !inQuotes;
	                        	if (text.charAt(i)==',' && inQuotes)
	                        	    copy += '/';
	                        	else
	                        	    copy += text.charAt(i);
                            }
                            String str = copy.replaceAll(",", "|").replace("\"", "");
                            
                            String pathFile = "";
                            pathFile = config.getPathFolderData() + portal + "\\" + seccion + "\\" + filePrefix + seccion +"_" + fecha + ".txt";
                                                       
                            writeFile(pathFile, str);
                            ArrayList<String> lines = getLinesFromFile(pathFile);
                            writeLiverpoolFile(pathFile, lines);

                        }                                                
                    }
                    
                } 
                else {
                    
                    System.err.println("ENTRA NUMERO : " + count);
                    System.out.println("ENTRA 2");
                    driver.get("https://bwsext.liverpool.com.mx/sap/bw/BEx?sap-language=es&sap-client=400&accessibility=&style_sheet=&TEMPLATE_ID=BWR_VTAS_POR_DIA_PROV");
                    TimeUnit.SECONDS.sleep(2);
                    //driver.findElement(By.xpath("//*[@id=\"VAR_VALUE_LOW_EXT_14\"]")).clear();
                    //driver.findElement(By.xpath("//*[@id=\"VAR_VALUE_HIGH_EXT_14\"]")).clear();
                    
                    driver.findElement(By.xpath("/html/body/form/table/tbody/tr/td/table/tbody/tr[5]/td"
                            + "/table/tbody/tr[1]/td[2]/table/tbody/tr/td/table/tbody/tr/td/a/nobr")).click();

                    boolean opcion = driver.getPageSource().contains("Introduzca un valor p.la variable Período");
                    
                    if (opcion == true) {

                        log.info("ACCESS: Entra Periodo");
                        driver.findElement(By.xpath("//*[@id=\"VAR_VALUE_LOW_EXT_14\"]")).sendKeys(fecha);
                        driver.findElement(By.xpath("//*[@id=\"VAR_VALUE_HIGH_EXT_14\"]")).sendKeys(fecha);
                        driver.findElement(By.xpath("/html/body/form/table/tbody/tr/td/table/tbody/tr[5]/td/table/tbody/tr[1]/td[2]/table/tbody/tr/td/table/tbody/tr/td/a/nobr")).click();
                        TimeUnit.SECONDS.sleep(10);

                    } else {

                        log.info("ACCESS: Sin Periodo");
                        driver.findElement(By.xpath("/html/body/font/table[1]/tbody/tr/td[1]/table/tbody/tr[2]/td/table/tbody/tr/td/table/tbody/tr/td/table/tbody/tr[1]/td[6]/a/img")).click();
                        String winHandleBefore = driver.getWindowHandle();

                        for (String winHandle : driver.getWindowHandles()) {
                            driver.switchTo().window(winHandle);
                        }

                        String url = driver.getCurrentUrl();
                        log.info("ACCESS: url: "+url);
                        driver.findElement(By.id("FILTER"));
                        WebElement input1 = driver.findElement(By.xpath("/html/body/form/table/tbody/tr/td/table/tbody/tr[5]/td[2]/table/tbody/tr[1]/td[4]/input"));
                        WebElement input2 = driver.findElement(By.xpath("/html/body/form/table/tbody/tr/td/table/tbody/tr[5]/td[2]/table/tbody/tr[1]/td[7]/input"));
                        input1.clear();
                        SimpleDateFormat sdfSource = new SimpleDateFormat("ddMMyyyy");
                        Date date = sdfSource.parse(fecha);
                        SimpleDateFormat sdfDestination = new SimpleDateFormat("dd.MM.yyyy");
                        String fechaFmt = sdfDestination.format(date);
                        log.info("Fecha del periodo: "+fechaFmt);
                        input1.sendKeys(fechaFmt);//"22.02.2015"
                        input2.clear();
                        input2.sendKeys(fechaFmt);
                        log.info("Enviando peticion click 2");
                        driver.findElement(By.xpath("/html/body/form/table/tbody/tr/td/table/tbody/tr[21]/td[2]/table/tbody/tr/td[1]/table/tbody/tr/td/table/tbody/tr/td/a/nobr")).click();
                        driver.switchTo().window(winHandleBefore);

                    }

                    jse.executeScript("SAPBW(1,'','','EXPAND','0MATERIAL','Y');");

                    TimeUnit.SECONDS.sleep(10);
                    log.info("Enviando peticion click 3");
                    String curl = driver.getCurrentUrl();
                    System.out.println(curl);
                    String[] pageNo = curl.split("&");
                    System.out.println(pageNo[1]);
                    System.out.println("-----------------");
                    //https://bwsext.liverpool.com.mx/sap/bw/BEx?SAP-LANGUAGE=ES&PAGENO=3&REQUEST_NO=4&CMD=EXPORT&DATA_PROVIDER=DATAPROVIDER_4&FORMAT=CSV&SEPARATOR=,
                    driver.get("https://bwsext.liverpool.com.mx/sap/bw/BEx?SAP-LANGUAGE=ES&"+pageNo[1]+"&REQUEST_NO=4&CMD=EXPORT&DATA_PROVIDER=DATAPROVIDER_4&FORMAT=CSV&SEPARATOR=,");
                    //driver.findElement(By.xpath("/html/body/form/table/tbody/tr/td/table/tbody/tr[21]/td[2]/table/tbody/tr/td[1]/table/tbody/tr/td/table/tbody/tr/td/a/nobr")).click();

                    TimeUnit.SECONDS.sleep(10);

                    /*If page is not the page with file*/
                    driver.getCurrentUrl();
                    boolean checkPage = driver.getPageSource().contains("Resultado total");
                    if (checkPage == false) {                        
                        log.warn("ACCESS: ERROR el portal no cargo correctamente, cerrando portal para nuevo intento");
                        util.insertLog(cuenta, portal, "Download - AccessLiverpool: El portal no cargo correctamente", "error");
                    } else {
                        
                        String text = driver.getPageSource()
                                .replace("<html xmlns=http://www.w3.org/1999/xhtml><head><link title=Wrap Long Lines href=resource://gre-resources/plaintext.css type=text/css rel=alternate stylesheet /></head><body><pre>", "")
                                .replace("</pre></body></html>", "");
                        
                        String copy = new String();

                        boolean inQuotes = false;

                        for(int i=0; i<text.length(); ++i)
                        {
                        	if (text.charAt(i)=='"')
                        	    inQuotes = !inQuotes;
                        	if (text.charAt(i)==',' && inQuotes)
                        	    copy += '/';
                        	else
                        	    copy += text.charAt(i);
                        }
                        String str = copy.replaceAll(",", "|").replace("\"", "");

                        String pathFile = "";
                        pathFile = config.getPathFolderData() + portal + "\\" + seccion + "\\" + filePrefix + seccion +"_" + fecha + ".txt";
                                                                       
                        writeFile(pathFile, str);
                        ArrayList<String> lines = getLinesFromFile(pathFile);
                        writeLiverpoolFile(pathFile, lines);

                    }
                    
                }                               

            } else {
                if (indPortalEjec == 0) {
                    log.info("[-]Portal Liverpool Cesarfer Desactivado");
                    util.insertLog(cuenta, portal, "Download - AccessLiverpool: El portal esta desactivado", "error");
                }
            }

        } catch (NumberFormatException | InterruptedException e) {
            log.info("[Â¡]error al ingresar al portal Liverpool Cesarfer " + e.getMessage());
            util.insertLog(cuenta, portal, "Download - AccessLiverpool: Error al ingresar al portal Liverpool", "error");
        } catch (ParseException ex) {
            java.util.logging.Logger.getLogger(LiverpoolDownload.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void terminaDriver(WebDriver driver) {
        if (driver != null) {
            driver.quit();
        }
    }

    @SuppressWarnings({ "unused", "resource" })
	private static void writeLiverpoolFile(String filePath, ArrayList<String> lines) {

        try {
            PrintWriter pw = null;
            File file = new File(filePath);
            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            try (BufferedWriter bw = new BufferedWriter(fw)) {
                pw = new PrintWriter(file, "UTF-8");

                for (int i = 0; i < lines.size(); i++) {
                    if (!lines.get(i).contains("Resultado")) {
                        String line = lines.get(i);
                        bw.write(line + "\n");
                    }
                }
            }

        } catch (Exception e) {
            log.error("[Â¡] error al escribir archivo Liverpool " + e.getMessage());
            util.insertLog(cuenta, portal, "Download - writeLiverpoolFile: Error al escribir archivo Liverpool " + e.getMessage(), "error");
        }
    }
    
    @SuppressWarnings("resource")
	public static void writeFile(String path, String content) {

    	FileWriter fichero = null;

    	try {
    		PrintWriter pw = new PrintWriter(path, "UTF8");//ISO-8859-1
    		fichero = new FileWriter(path, true);
    		pw = new PrintWriter(fichero);
    		pw.println(content);
    	} catch (Exception e) {
    		log.error("WRITEFILE: Error al generar archivo de datos " + e.getMessage());
    		util.insertLog(cuenta, portal, "Download - writeFile: Error al escribir archivo Liverpool " + e.getMessage(), "error");
    	} 
    	finally {
    		try {
    			if (null != fichero) {
                fichero.close();
              }
    		} catch (Exception e2) {
    			log.error("WRITEFILE: error al cerrar archivo de datos " + e2.getMessage());
    			util.insertLog(cuenta, portal, "Download - writeFile: Error al cerrar el archivo " + e2.getMessage(), "error");
    		}
    	}
    }
    
    @SuppressWarnings({ "unused", "resource" })
	public static ArrayList<String> getLinesFromFile(String filePath) {
        ArrayList<String> lines = new ArrayList<>();

        try {
            BufferedReader br = new BufferedReader(new FileReader(filePath));
            String linea = br.readLine();

            while (linea != null) {
                lines.add(linea);//AGREGAR PIPES
                linea = br.readLine();
            }

        } catch (IOException ioe) {
            log.error("GETLINES: no se pudo recuperar archivo origen", ioe);
        }

        if (lines != null) {
            log.info("GETLINES: Líneas recuperadas del archivo: " + lines.size());
        } else {
            log.info("GETLINES: No se ha recuperado informacion");
        }

        return lines;
    }
}
