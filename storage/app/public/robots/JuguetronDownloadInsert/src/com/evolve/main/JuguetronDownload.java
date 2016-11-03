package com.evolve.main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;

import com.evolve.config.Configurations;
import com.evolve.util.Utileria;
import com.mysql.jdbc.Connection;
import com.mysql.jdbc.Statement;

public class JuguetronDownload {

	static String portal = "juguetron";
	static String cuenta;
	static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(JuguetronDownload.class);
	static Configurations config;
	static Properties prop = new Properties();
	static Utileria util;

	public static void main(String[] args) {

		cuenta = args[0];
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
		JavascriptExecutor jse = (JavascriptExecutor) driver;

		log.info("MAIN: Se ejecuta Juguetron " + cuenta);
		util.insertLog(cuenta, portal, "Download - Main: Se ejecuta Juguetron", "success");
		prop = util.getPropertiesPortal();

		try {

			String filePrefix = prop.getProperty(portal + ".prefix");
			ArrayList<String> oldDates = util.getDatesFromLastReportFileFormat("ddMMyyyy");

			int counter = 1;
			for (int k = 0; k < oldDates.size(); k++) {

				boolean result = util.checkIfExist(portal, filePrefix, oldDates.get(k));
				if (result == false) {
					log.info("MAIN: Comienza descarga de archivo " + filePrefix + oldDates.get(k));
					util.insertLog(cuenta, portal, "Download - Main: Comienza descarga de archivo", "success");
					driver.manage().window().maximize();
					accessJuguetron(driver, jse, oldDates.get(k), counter);
					counter++;

				} else {
					if (counter > 1) {
						log.info("Archivo " + filePrefix + oldDates.get(k) + " anteriormente descargado");
						TimeUnit.SECONDS.sleep(3);
					}
				}
			}

			log.info("[-]Termina ejecución del portal Juguetron Spinmaster");
			util.insertLog(cuenta, portal, "Download - Main: Termina ejecución robot Spinmaster", "succcess");
			terminaDriver(driver);

			// Insertando en la BD
			JuguetronInsert.insertarBD(cuenta);

		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			log.error("[-]Error al ejecutar Liverpool Cesarfer " + e.getMessage());
			util.insertLog(cuenta, portal, "Download - Main: Error al ejecutar liverpool", "error");
		} catch (InterruptedException ex) {
			Logger.getLogger(JuguetronDownload.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public static void accessJuguetron(WebDriver driver, JavascriptExecutor jse, String fecha, int count)
			throws FileNotFoundException, UnsupportedEncodingException {

		int indPortalEjec = 0;
		String user = "", pass = "", urlLogin;

		// obteniendo el archivo de propiedades
		Properties prop = util.getPropertiesPortal();

		// Obteniendo si el robot esta activo, usuario y password del portal
		Connection conn = (Connection) config.getConnectionGeneral();
		ResultSet rs;
		try {
			Statement s = (Statement) conn.createStatement();
			rs = s.executeQuery("SELECT * FROM proyectos_cadenas WHERE nombreProyecto LIKE '%" + cuenta
					+ "%' AND nombreCadena LIKE '%" + portal + "%' LIMIT 1");
			rs.next();
			user = rs.getString("usuario");
			pass = rs.getString("password");
			indPortalEjec = rs.getInt("activo");
		} catch (SQLException e) {
			e.printStackTrace();
			util.insertLog(cuenta, portal,
					"GenerateLink - accessJuguetron: ERROR Logeo, Usuario y/o Password incorrectos", "error");
			System.exit(0);
			driver.quit();
		}
		
		urlLogin = prop.getProperty(portal + ".urlLogin");

		try {
			if (indPortalEjec == 1) {

				// SimpleDateFormat sdfSource = new
				// SimpleDateFormat("ddMMyyyy");
				// Date date = sdfSource.parse(fecha);
				// SimpleDateFormat sdfDestination = new
				// SimpleDateFormat("yyyyMMdd");
				// String fechaDownload = sdfDestination.format(date);
				String fechaDownload = fecha;

				driver.get(urlLogin);
				driver.findElement(By.name("user_name")).sendKeys(user);
				driver.findElement(By.name("user_pwd")).sendKeys(pass);
				driver.findElement(By.name("Login")).click();

				boolean success = driver.getPageSource()
						.contains("La dirección de correo electrónico o la contraseña son incorrectas.");

				if (success == true) {
					log.warn("ERROR Logeo, Usuario y/o Password incorrectos ");
					util.insertLog(cuenta, portal, "Download - Main: Usuario/Password incorrectos", "error");
					terminaDriver(driver);
					System.exit(0);
				} else {
					System.out.println("Paso->1");
					TimeUnit.SECONDS.sleep(3);
					log.info("Fecha de descarga: " + fechaDownload);
					
                    String fdDay = fechaDownload.substring(0, 2);
                    String fdMonth = fechaDownload.substring(2, 4);
                    String fdYear = fechaDownload.substring(4, 8);
                    /*log.info("Fecha de fdDay: "+fdDay);
                    log.info("Fecha de fdMonth: "+fdMonth);
                    log.info("Fecha de fdYear: "+fdYear);*/

					driver.get("http://kpionline5.bitam.com/fbm/mqcvs.php?query=juguetronProveedor.sql&"
								+"where="+fdYear+fdMonth+fdDay+"%20000000"+fdYear+fdMonth+fdDay+"%200000001195&"
								+"file=JuguetronProveedor.csv"
								+"&server=kpionline9.bitam.com:3306&"
								+"dbname=fbm_dwh_1491&artuspath=gen8");

					log.info("http://kpionline5.bitam.com/fbm/mqcvs.php?query=juguetronProveedor.sql&"
							+"where="+fdYear+fdMonth+fdDay+"%20000000"+fdYear+fdMonth+fdDay+"%200000001195&"
							+"file=JuguetronProveedor.csv"
							+"&server=kpionline9.bitam.com:3306&"
							+"dbname=fbm_dwh_1491&artuspath=gen8");
					
					TimeUnit.SECONDS.sleep(5);

					isZip(fecha);
				}

			} else {
				if (indPortalEjec == 0) {
					log.info("[-]Portal Comercial Cesarfer Desactivado");
					util.insertLog(cuenta, portal, "Download - Main: Portal comercial desactivado", "error");
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage());
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
			util.insertLog(cuenta, portal,
					"Download - writeLiverpoolFile: Error al escribir archivo Liverpool " + e.getMessage(), "error");
		}
	}

	@SuppressWarnings("resource")
	public static void writeFile(String path, String content) {

		FileWriter fichero = null;

		try {
			PrintWriter pw = new PrintWriter(path, "UTF8");// ISO-8859-1
			fichero = new FileWriter(path, true);
			pw = new PrintWriter(fichero);
			pw.println(content);
		} catch (Exception e) {
			log.error("WRITEFILE: Error al generar archivo de datos " + e.getMessage());
			util.insertLog(cuenta, portal,
					"Download - writeFile: Error al escribir archivo Liverpool " + e.getMessage(), "error");
		} finally {
			try {
				if (null != fichero) {
					fichero.close();
				}
			} catch (Exception e2) {
				log.error("WRITEFILE: error al cerrar archivo de datos " + e2.getMessage());
				util.insertLog(cuenta, portal, "Download - writeFile: Error al cerrar el archivo " + e2.getMessage(),
						"error");
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
				lines.add(linea);// AGREGAR PIPES
				linea = br.readLine();
			}

		} catch (IOException ioe) {
			log.error("GETLINES: no se pudo recuperar archivo origen", ioe);
		}

		if (lines != null) {
			log.info("GETLINES: LÃ­neas recuperadas del archivo: " + lines.size());
		} else {
			log.info("GETLINES: No se ha recuperado informacion");
		}

		return lines;
	}

	public static void isZip(String fecha) throws IOException, Exception {

		String path = config.getPathDownloads() + "\\";

		String nameFileInv = "juguetron-INV-" + fecha + "";
		String nameFileVta = "juguetron-VTA-" + fecha + "";
		String nameZip = "";
		String fileZip = "";
		String nameZipDelete = "";

		File folder = new File(path);
		File[] listOfFiles = folder.listFiles();
		
		System.out.println("Paso->1");
		
		for (int i = 0; i < listOfFiles.length; i++) {
			System.out.println("Paso->2");
			if (listOfFiles[i].isFile()) {
				System.out.println("Paso->3");
				nameZip = listOfFiles[i].getName();
				System.out.println("Name->"+nameZip);
				if (nameZip.startsWith("JuguetronProveedor")) {
					System.out.println("Paso->4");
					nameZipDelete = nameZip;
					fileZip = path + nameZip;
					System.out.println("nameZipDelete-> "+nameZipDelete);
					System.out.println("fileZip-> "+fileZip);
					unZipTarGz(fileZip, nameFileInv, fecha, nameZip, nameZipDelete);
				} else if (nameZip.startsWith("VED") || nameZip.startsWith("VEM")) {
					nameZipDelete = nameZip;
					fileZip = path + listOfFiles[i].getName();
					unZipTarGz(fileZip, nameFileVta, fecha, nameZip, nameZipDelete);
				}
			}
		}
	}

	public static void unZipTarGz(String fileZip, String nameFile, String fecha, String nameZip, String nameZipDelete)
			throws Exception {

		File archive = new File(fileZip);
		nameFile = nameFile.replace("juguetron-", "juguetron_");

		// obteniendo el archivo de propiedades
		Properties prop = util.getPropertiesPortal();

		String filePrefix = prop.getProperty(portal + ".prefix");
		String pathFile = config.getPathFolderData() + portal + "\\" + filePrefix + fecha + ".txt";

		byte[] buffer = new byte[1024];

		try {

			ZipInputStream zis = new ZipInputStream(new FileInputStream(archive));

	    	ZipEntry ze = zis.getNextEntry();

	    	while(ze!=null){
	           File newFile = new File(pathFile);

	           System.out.println("file unzip : "+ newFile.getAbsoluteFile());

	            //create all non exists folders
	            //else you will hit FileNotFoundException for compressed folder
	            new File(newFile.getParent()).mkdirs();

	            FileOutputStream fos = new FileOutputStream(newFile);

	            int len;
	            while ((len = zis.read(buffer)) > 0) {
	       		fos.write(buffer, 0, len);
	            }

	            fos.close();
	            ze = zis.getNextEntry();
	    	}

	        zis.closeEntry();
	    	zis.close();

			log.info("Termina la descompresion del archivo Zip " + nameZip);

			checkFiles(nameZipDelete);

		} catch (IOException e) {
			log.error(e.getMessage());
		}

	}

	public static void checkFiles(String nameZipDelete) {
		String path = config.getPathDownloads() + "\\";
		File zipFile = new File(path + nameZipDelete);

		boolean bool = false;

		try {

			bool = zipFile.exists();

			if (bool == true) {
				zipFile.delete();

				log.info("[-]Se elimino archivo Zip de descarga " + nameZipDelete);
			}
			bool = zipFile.exists();

		} catch (Exception e) {
			log.error(e.getMessage());
		}
	}
}
