package com.evolve.main;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.firefox.internal.ProfilesIni;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.evolve.config.Configurations;
import com.evolve.util.Utileria;
import com.mysql.jdbc.Connection;
import com.mysql.jdbc.Statement;

public class PalacioDownload {
		
	static String portal = "palacio";
	static String cuenta, seccion, finicial, ffinal;
	static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(PalacioDownload.class);
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
		
		FirefoxProfile profile = new FirefoxProfile();

		String path = config.getPathDownloads()+"\\";
		profile.setPreference("browser.download.folderList", 2);
		profile.setPreference("browser.download.dir", path);
		profile.setPreference("browser.download.manager.alertOnEXEOpen", false);
		profile.setPreference("browser.helperApps.neverAsk.saveToDisk", "application/msword, application/csv, application/ris, text/csv, image/png, application/pdf, text/html, text/plain, application/zip, application/x-zip, application/x-zip-compressed, application/download, application/octet-stream, application/x-gzip");
		profile.setPreference("browser.download.manager.showWhenStarting", false);
		profile.setPreference("browser.download.manager.focusWhenStarting", false);  
		profile.setPreference("browser.download.useDownloadDir", true);
		profile.setPreference("browser.helperApps.alwaysAsk.force", false);
		profile.setPreference("browser.download.manager.alertOnEXEOpen", false);
		profile.setPreference("browser.download.manager.closeWhenDone", true);
		profile.setPreference("browser.download.manager.showAlertOnComplete", false);
		profile.setPreference("browser.download.manager.useWindow", false);
		profile.setPreference("services.sync.prefs.sync.browser.download.manager.showWhenStarting", false);
		profile.setPreference("pdfjs.disabled", true);
		
		System.setProperty("webdriver.gecko.driver", "C:\\Users\\SISTEMA\\Downloads\\geckodriver-v0.10.0-win64\\geckodriver.exe");

		WebDriver driver = new FirefoxDriver(profile);
		//JavascriptExecutor jse = (JavascriptExecutor) driver;
		
        log.info("MAIN: Se ejecuta Palacio "+cuenta);
        util.insertLog(cuenta, portal, "Download - Main: Se ejecuta palacio", "success");
        prop = util.getPropertiesPortal();
        
        try {

        	String filePrefix = prop.getProperty(portal+".prefix");									
			ArrayList<String> oldDates = util.getDatesFromLastReportFileFormatPalacio("ddMMyyyy", finicial, ffinal, seccion);
            
            int counter = 1;
            for(int k=0; k < oldDates.size();k++){
                
                boolean result = util.checkIfExist(portal, filePrefix, oldDates.get(k));
                if (result == false) {
                	log.info("Comienza descarga de archivo " + filePrefix + oldDates.get(k));
                    driver.manage().window().maximize();
                    palacioAccess(driver, oldDates.get(k), counter, seccion);
                    counter ++;
                    
                } 
                else {
                	log.info("Archivo " + filePrefix + oldDates.get(k)+ " anteriormente descargado");
                	util.insertLog(cuenta, portal, "Download - Main: Archivo " + filePrefix + oldDates.get(k)+ " anteriormente descargado", "success");
                	TimeUnit.SECONDS.sleep(2);                	                    
                }
            }
            
            log.info("[-]Termina ejecuci�n del portal Palacio Cesarfer");
            util.insertLog(cuenta, portal, "Download - Main: Termina ejecuci�n robot palacio", "success");
            terminaDriver(driver);
            
            //Insertando en la BD
            PalacioInsert.insertarBD(cuenta, seccion, finicial, ffinal);
            
        } catch (InterruptedException ex) {
            Logger.getLogger(PalacioDownload.class.getName()).log(Level.SEVERE, null, ex);
        }
	}
	
	public static void palacioAccess(WebDriver driver, String fecha, int count, String seccion) {

		int iIndPortalEjec = 0;     
        String user="", pass="", urlLogin, filePrefix, nameSeccion = null, idSeccion = null;
        
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
				iIndPortalEjec = rs.getInt("activo");
			} catch (SQLException e) {				
				e.printStackTrace();
			}
			
			urlLogin = prop.getProperty(portal+".urlLogin");
            filePrefix = prop.getProperty(portal+".prefix");

            if (iIndPortalEjec == 1) {

                if (count == 1) {
                    driver.get(urlLogin);
                    driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
                    driver.findElement(By.id("logonuidfield")).sendKeys(user);
                    driver.findElement(By.id("logonpassfield")).sendKeys(pass);
                    driver.findElement(By.name("uidPasswordLogon")).click();

                    TimeUnit.SECONDS.sleep(3);
                    boolean success = driver.getPageSource().contains("Autentificaci�n de usuario fallida");
                    if (success == true) {                        
                        log.warn("ERROR Logeo, Usuario y/o Password incorrectos");
                        util.insertLog(cuenta, portal, "Download - palacioAccess: Usuario/Password incorrectos", "error");
                        //Desactivando el robot
                        try {
            				Statement s = (Statement) conn.createStatement();            				
            				s.executeUpdate ("UPDATE proyectos_cadenas SET activo=0 WHERE nombreProyecto LIKE '%"+cuenta+"%' AND nombreCadena LIKE '%"+portal+"%'");            				            				            				
            			} catch (SQLException e) {				
            				e.printStackTrace();
            			}
                        driver.quit();
    					System.exit(0);
                    } 
                    else {
                        String dia = fecha.substring(0, 2);
                        String mes = fecha.substring(2, 4);
                        String anio = fecha.substring(4);
                        if(seccion.equals("1")){
                        	nameSeccion = "NINOS";
                        	idSeccion = "5B0310418";
                        }
                        if(seccion.equals("2")){
                        	nameSeccion = "NINAS";
                        	idSeccion = "5B0310414";
                        }
                        if(seccion.equals("3")){
                        	nameSeccion = "CALZADO+BRIDGE";
                        	idSeccion = "5B0310210";
                        }
                        if(seccion.equals("4")){
                        	nameSeccion = "CALZADO+CABALLEROS+Y+ACCESORIOS";
                        	idSeccion = "5B0310302";
                        }
                        if(seccion.equals("5")){
                        	nameSeccion = "MUJER+SPORT";
                        	idSeccion = "5B0310201";
                        }

                        String url = "https://wdbop.palaciohierro.com.mx/BOE/OpenDocument/1609021408/AnalyticalReporting/opendoc/openDocument.jsp?iDocID=FknBR1Ccbw4AzHYAAEDZ5EQBAFBWvQBx&sIDType=CUID&sType=wid&lsS1.Fecha%20Inicio%20(Desde)="+dia+"%2F"+mes+"%2F"+anio+"+12%3A00%3A00+p%2Em%2E&lsS2.Fecha%20Fin%20(Hasta%2090%20d%C3%ADas)="+dia+"%2F"+mes+"%2F"+anio+"+12%3A00%3A00+p%2Em%2E&lsMDepartamento="+nameSeccion+"&lsIDepartamento=%5B0CM%5FCDT3%5D%2E%"+idSeccion+"%5D";
                        log.info("URL! " + url);
                        TimeUnit.SECONDS.sleep(5);
                        driver.get(url);
                        
                        WebDriverWait waitPage = new WebDriverWait(driver, 10);
                        waitPage.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt("openDocChildFrame"));
                        waitPage.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt("webiViewFrame"));
                        waitPage.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt("Report"));

                        WebDriverWait wait = new WebDriverWait(driver, 40);
                        WebElement pathElement = driver.findElement(By.xpath("//*[@class=' nwt db']"));
                        wait.until(ExpectedConditions.visibilityOf(pathElement));

                        driver.switchTo().defaultContent();
                        driver.switchTo().frame("openDocChildFrame");
                        driver.switchTo().frame("webiViewFrame");
                        
                        boolean exists = driver.findElements( By.id("dlg_txt_alertDlg") ).size() != 0;
                        System.out.println("-------------");
                        if(!exists){

	                        WebElement element = driver.findElement(By.id("IconImg_iconMenu_icon__dhtmlLib_264"));
	                        element.click();
	                        WebElement element2 = driver.findElement(By.id("iconMenu_menu__dhtmlLib_264_span_text__menuAutoId_24"));
	                        element2.click();
	
	                        TimeUnit.SECONDS.sleep(2);
	
	                        WebElement select = driver.findElement(By.id("cbColSep"));
	                        Select dropDown = new Select(select);
	
	                        List<WebElement> Options = dropDown.getOptions();
	                        for (WebElement option : Options) {
	                            if (option.getText().equals(";")) {
	                                option.click();
	                            }
	                        }
	
	                        Robot robott = new Robot();
	                        robott.keyPress(KeyEvent.VK_ENTER);
	
	                        TimeUnit.SECONDS.sleep(5);
	
	                        WebElement button = driver.findElement(By.id("RealBtn_csvopOKButton"));
	                        button.click();
	
	                        TimeUnit.SECONDS.sleep(10);
	
	                        //com.evolve.webbot.file.FileReader fr = new com.evolve.webbot.file.FileReader();                        
	                        String pathFile = config.getPathFolderData() + portal + "\\" + nameSeccion + "\\" + filePrefix + fecha + ".txt";
	                        System.out.println(pathFile);
	                        String pathDownload = config.getPathDownloads()+"\\Ventas_Netas_diarias_Detallado.csv";
	
	                        File file = new File(pathDownload);
	                        String txtFilePath = pathFile;
	
	                        readPalacioFile(file, txtFilePath);
	
	                        TimeUnit.SECONDS.sleep(5);
	                        if (file.delete())
	                        	   System.out.println("El fichero ha sido borrado satisfactoriamente");
	                        	else
	                        	   System.out.println("El fichero no puede ser borrado");
	                        TimeUnit.SECONDS.sleep(5);
	                        //util.checkFilePalacio();
                        }else{
                        	System.out.println("No existe informacion con "+fecha);
                        }
                    }
                    
                } 
                else {
                	String dia = fecha.substring(0, 2);
                    String mes = fecha.substring(2, 4);
                    String anio = fecha.substring(4);

                    
                    if(seccion.equals("1")){
                    	nameSeccion = "NINOS";
                    	idSeccion = "5B0310418";
                    }
                    if(seccion.equals("2")){
                    	nameSeccion = "NINAS";
                    	idSeccion = "5B0310414";
                    }
                    if(seccion.equals("3")){
                    	nameSeccion = "CALZADO+BRIDGE";
                    	idSeccion = "5B0310210";
                    }
                    if(seccion.equals("4")){
                    	nameSeccion = "CALZADO+CABALLEROS+Y+ACCESORIOS";
                    	idSeccion = "5B0310302";
                    }
                    if(seccion.equals("5")){
                    	nameSeccion = "MUJER+SPORT";
                    	idSeccion = "5B0310201";
                    }
                    
                    String url = "https://wdbop.palaciohierro.com.mx/BOE/OpenDocument/1609021408/AnalyticalReporting/opendoc/openDocument.jsp?iDocID=FknBR1Ccbw4AzHYAAEDZ5EQBAFBWvQBx&sIDType=CUID&sType=wid&lsS1.Fecha%20Inicio%20(Desde)="+dia+"%2F"+mes+"%2F"+anio+"+12%3A00%3A00+p%2Em%2E&lsS2.Fecha%20Fin%20(Hasta%2090%20d%C3%ADas)="+dia+"%2F"+mes+"%2F"+anio+"+12%3A00%3A00+p%2Em%2E&lsMDepartamento="+nameSeccion+"&lsIDepartamento=%5B0CM%5FCDT3%5D%2E%"+idSeccion+"%5D";
                    TimeUnit.SECONDS.sleep(8);
                    log.info("URL! " + url);

                    driver.get(url);
                    WebDriverWait waitPage = new WebDriverWait(driver, 10);
                    waitPage.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt("openDocChildFrame"));
                    waitPage.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt("webiViewFrame"));
                    waitPage.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt("Report"));

                    WebDriverWait wait = new WebDriverWait(driver, 40);
                    WebElement pathElement = driver.findElement(By.xpath("//*[@class=' nwt db']"));
                    wait.until(ExpectedConditions.visibilityOf(pathElement));

                    driver.switchTo().defaultContent();
                    driver.switchTo().frame("openDocChildFrame");
                    driver.switchTo().frame("webiViewFrame");
                    boolean exists = driver.findElements( By.id("dlg_txt_alertDlg") ).size() != 0;
                    System.out.println("-------------");
                    if(!exists){
	                    System.out.println("aqui1");
	                    TimeUnit.SECONDS.sleep(8);
	                    WebElement element = driver.findElement(By.id("IconImg_iconMenu_icon__dhtmlLib_264"));
	                    element.click();
	                    System.out.println("aqui2");
	                    WebElement element2 = driver.findElement(By.id("iconMenu_menu__dhtmlLib_264_span_text__menuAutoId_24"));
	                    element2.click();
	
	                    TimeUnit.SECONDS.sleep(2);
	
	                    WebElement select = driver.findElement(By.id("cbColSep"));
	                    Select dropDown = new Select(select);
	
	                    List<WebElement> Options = dropDown.getOptions();
	                    for (WebElement option : Options) {
	                        if (option.getText().equals(";")) {
	                            option.click();
	                        }
	                    }
	
	                    Robot robott = new Robot();
	                    robott.keyPress(KeyEvent.VK_ENTER);
	
	                    TimeUnit.SECONDS.sleep(10);
	
	                    WebElement button = driver.findElement(By.id("RealBtn_csvopOKButton"));
	                    button.click();
	
	                    TimeUnit.SECONDS.sleep(10);                        
	
	                    String pathFile = config.getPathFolderData() + portal + "\\" + nameSeccion + "\\" + filePrefix + fecha + ".txt";
	                    String pathDownload = config.getPathDownloads()+"\\Ventas_Netas_diarias_Detallado.csv";
	
	                    File file = new File(pathDownload);
	                    String txtFilePath = pathFile;
	
	                    readPalacioFile(file, txtFilePath);
	
	                    TimeUnit.SECONDS.sleep(5);
	                    if (file.delete())
	                 	   System.out.println("El fichero ha sido borrado satisfactoriamente");
	                 	else
	                 	   System.out.println("El fichero no puede ser borrado");
	                    
	                    TimeUnit.SECONDS.sleep(5);
	                    //util.checkFilePalacio();
                    }else{
                    	System.out.println("No existe informacion con "+fecha);
                    }
                }

            } 
            else {
            	if (iIndPortalEjec == 0) {
                    log.info("[-]Portal Palacio Desactivado");
                    util.insertLog(cuenta, portal, "Download - palacioAccess: Portal desactivado", "error");
                    driver.quit();
					System.exit(0);
                }
            }

        } catch (NumberFormatException | InterruptedException | AWTException | IOException e) {
            log.error("[-]error al ingresar al portal Palacio" + e.getMessage());
            util.insertLog(cuenta, portal, "Download - palacioAccess: Error al ingresar al portal", "error");
        } 

    }
	
	public static void readPalacioFile(File file, String path) throws FileNotFoundException, IOException {
        BufferedReader br = new BufferedReader(new java.io.FileReader(file));
        BufferedWriter bw = new BufferedWriter(new java.io.FileWriter(path));

        try {

            String line = null;
            while ((line = br.readLine()) != null) {

                line = line.replace("\"", "");
                line = line.replaceAll(";", "|");
                line = line.replaceAll(",", ".");
                bw.write(line + "\n");
            }

            br.close();
            bw.close();

            log.info("[-]Generación de archivo txt completa");
        } catch (FileNotFoundException e0) {
            log.error("archivo de datos Palacio no encontrado ", e0);
        } catch (Exception e1) {
            log.error("ha ocurrido un error al recuperar el archivo de datos de Palacio ", e1);
        } finally {
            try {
                br.close();
                bw.close();
            } catch (Exception e2) {
                log.error("ha ocurrido un error al recuperar el archivo de datos de Palacio ", e2);
            }
        }
    }

    public void readPalacioInvFile(File file, String path) throws FileNotFoundException, IOException {
        BufferedReader br = new BufferedReader(new java.io.FileReader(file));
        BufferedWriter bw = new BufferedWriter(new java.io.FileWriter(path));

        try {

            String line = null;
            while ((line = br.readLine()) != null) {

                line = line.replace("\"", "");
                line = line.replaceAll(";", "|");
                line = line.replaceAll(",", ".");
                if (line.startsWith("Proveedor|Costo")) {
                    line = line.replaceAll(line, "");
                }
                bw.write(line + "\n");
            }

            br.close();
            bw.close();

            log.info("[-]Generación de archivo txt completa");
        } catch (FileNotFoundException e0) {
            log.error("archivo de datos Palacio no encontrado ", e0);
        } catch (Exception e1) {
            log.error("ha ocurrido un error al recuperar el archivo de datos de Palacio ", e1);
        } finally {
            try {
                br.close();
                bw.close();
            } catch (Exception e2) {
                log.error("ha ocurrido un error al recuperar el archivo de datos de Palacio ", e2);
            }
        }

    }

    public static void terminaDriver(WebDriver driver) {
        if (driver != null) {
            driver.quit();
        }
    }    
}
