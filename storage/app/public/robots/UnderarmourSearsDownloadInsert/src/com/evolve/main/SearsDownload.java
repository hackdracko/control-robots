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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;

import com.evolve.config.Configurations;
import com.evolve.util.Utileria;
import com.mysql.jdbc.Connection;
import com.mysql.jdbc.Statement;

public class SearsDownload {

	static String portal = "sears";
	static String cuenta;
	static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(SearsDownload.class);
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
		util.insertLog(cuenta, portal, "Download - Main: Se ejecuta Marti", "success");
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
					accessMarti(driver, jse, oldDates.get(k), counter);
					counter++;

				} else {
					if (counter > 1) {
						log.info("Archivo " + filePrefix + oldDates.get(k) + " anteriormente descargado");
						TimeUnit.SECONDS.sleep(3);
					}
				}
			}

			log.info("[-]Termina ejecución del portal Sears Underamour");
			util.insertLog(cuenta, portal, "Download - Main: Termina ejecución robot Underarmour", "succcess");
			terminaDriver(driver);

			// Insertando en la BD
			SearsInsert.insertarBD(cuenta);

		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			log.error("[-]Error al ejecutar Liverpool Cesarfer " + e.getMessage());
			util.insertLog(cuenta, portal, "Download - Main: Error al ejecutar sears", "error");
		} catch (InterruptedException ex) {
			Logger.getLogger(SearsDownload.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public static void accessMarti(WebDriver driver, JavascriptExecutor jse, String fecha, int count)
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
					"GenerateLink - accessSears: ERROR Logeo, Usuario y/o Password incorrectos", "error");
			System.exit(0);
			driver.quit();
		}
		
		urlLogin = prop.getProperty(portal + ".urlLogin");
		System.out.println("--------"+urlLogin);

		try {
			if (indPortalEjec == 1) {
				String fechaDownload = fecha;

				driver.get(urlLogin);
				driver.findElement(By.id("txtUsuario")).sendKeys(user);
				driver.findElement(By.id("txtContrasena")).sendKeys(pass);
				driver.findElement(By.id("btnEntrar")).click();

				boolean success = driver.getPageSource()
						.contains("Contraseña Incorrecta");

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
                    log.info("Fecha de fdDay: "+fdDay);
                    log.info("Fecha de fdMonth: "+fdMonth);
                    log.info("Fecha de fdYear: "+fdYear);
                    //CREANDO ARCHIVO
                    String filePrefix = prop.getProperty(portal+".prefix");
                    String pathFile = "";
                    pathFile = config.getPathFolderData() + portal + "\\" + filePrefix + fechaDownload + ".txt";
        			File file = new File(pathFile);
        			FileWriter fw = new FileWriter(file.getAbsoluteFile());
        			
					driver.get("http://proveedores.sears.com.mx/lib/CriteriosGen.asp?Padre=1&Hijo=4");
					driver.findElement(By.name("SearchSubCon")).click();
					String winHandleBefore = driver.getWindowHandle();
                    for (String winHandle : driver.getWindowHandles()) {
                        driver.switchTo().window(winHandle);
                    }
                    driver.findElement(By.name("Criterio_r3_c2")).click();
                    driver.findElement(By.name("Criterio_r2_c1")).click();
					driver.switchTo().window(winHandleBefore);
					String[] elementos = { "207|ACAPULCO",
							//"204|AGUASCALIENTES",
							"231|AGUASCALIENTES ALTARIA",
							"874|ALMACEN CLARO SHOP",
							//"993|ALMACEN FISCAL CANCUN",
							//"992|ALMACEN FISCAL CD. JUAREZ",
							//"994|ALMACEN FISCAL PLAYA DEL CARMEN",
							//"990|ALMACEN FISCAL VALLEJO",
							//"900|ALMACEN FISCAL VALLEJO",
							//"878|ALMACEN INTERNET",
							"111|ANGELOPOLIS",
							"122|BUENAVISTA",
							//"483|C AUTOMOTRIZ AGUASCALIENTES ALTAIRA",
							"220|CANCUN",
							//"404|CARPA ACAPULCO",
							"218|CELAYA",
							"245|CELAYA GALERIAS",
							//"830|CENTRAL DE INSTALACIONES",
							//"438|CENTRO AUTOMOTRIZ ACAPULCO",
							//"435|CENTRO AUTOMOTRIZ AGUASCALIENTES",
							//"431|CENTRO AUTOMOTRIZ ANAHUAC",
							//"467|CENTRO AUTOMOTRIZ ANGELOPOLIS",
							//"456|CENTRO AUTOMOTRIZ BUENAVISTA",
							//"465|CENTRO AUTOMOTRIZ CANCUN PLAZA",
							//"448|CENTRO AUTOMOTRIZ CELAYA",
							//"543|CENTRO AUTOMOTRIZ CELAYA GALERIAS",
							//"471|CENTRO AUTOMOTRIZ CENTRO HISTORICO",
							//"561|CENTRO AUTOMOTRIZ CHIMALHUACAN",
							//"475|CENTRO AUTOMOTRIZ CIUDAD JUAREZ",
							//"480|CENTRO AUTOMOTRIZ COACALCO",
							//"477|CENTRO AUTOMOTRIZ COATZACOALCOS",
							//"447|CENTRO AUTOMOTRIZ COLIMA",
							//"460|CENTRO AUTOMOTRIZ CORDOBA",
							//"548|CENTRO AUTOMOTRIZ COSMOPOL",
							//"554|CENTRO AUTOMOTRIZ CUATRO CAMINOS",
							//"489|CENTRO AUTOMOTRIZ CUAUTITLAN",
							//"439|CENTRO AUTOMOTRIZ CUERNAVACA",
							//"443|CENTRO AUTOMOTRIZ CULIACAN",
							//"455|CENTRO AUTOMOTRIZ DURANGO",
							//"476|CENTRO AUTOMOTRIZ ECATEPEC",
							//"472|CENTRO AUTOMOTRIZ FORUM CULIACAN",
							//"544|CENTRO AUTOMOTRIZ GAL ZACATECAS",
							//"499|CENTRO AUTOMOTRIZ GALERIAS ATIZAPAN",
							//"493|CENTRO AUTOMOTRIZ GALERIAS MALL SON",
							//"540|CENTRO AUTOMOTRIZ GALERIAS MAZATLAN",
							//"441|CENTRO AUTOMOTRIZ GOMEZ PALACIO",
							//"433|CENTRO AUTOMOTRIZ GUADALAJARA",
							//"429|CENTRO AUTOMOTRIZ GUADALAJARA PLAZA",
							//"474|CENTRO AUTOMOTRIZ HEMISFERIA GUADAL",
							//"440|CENTRO AUTOMOTRIZ HERMOSILLO",
							//"421|CENTRO AUTOMOTRIZ INSURGENTES",
							//"463|CENTRO AUTOMOTRIZ INTERLOMAS",
							//"453|CENTRO AUTOMOTRIZ IRAPUATO",
							//"550|CENTRO AUTOMOTRIZ IRAPUATO CIBELES",
							//"459|CENTRO AUTOMOTRIZ JALAPA",
							//"442|CENTRO AUTOMOTRIZ LEON PLAZA",
							//"426|CENTRO AUTOMOTRIZ LINDAVISTA",
							//"486|CENTRO AUTOMOTRIZ MERIDA ALTABRISA",
							//"457|CENTRO AUTOMOTRIZ MERIDA CENTRO",
							//"446|CENTRO AUTOMOTRIZ MERIDA PLAZA",
							//"466|CENTRO AUTOMOTRIZ METEPEC",
							//"461|CENTRO AUTOMOTRIZ MINATITLAN",
							//"432|CENTRO AUTOMOTRIZ MONTERREY",
							//"484|CENTRO AUTOMOTRIZ MONTERREY CITADEL",
							//"481|CENTRO AUTOMOTRIZ MONTERREY GALERIA",
							//"454|CENTRO AUTOMOTRIZ MORELIA",
							//"490|CENTRO AUTOMOTRIZ MORELIA P AMERICA",
							//"547|CENTRO AUTOMOTRIZ MTY ESFERA",
							//"488|CENTRO AUTOMOTRIZ NEZAHUALCOYOTL",
							//"417|CENTRO AUTOMOTRIZ NUEVO VERACRUZ",
							//"445|CENTRO AUTOMOTRIZ OAXACA PLAZA",
							//"422|CENTRO AUTOMOTRIZ PABELLON POLANCO",
							//"444|CENTRO AUTOMOTRIZ PACHUCA",
							//"485|CENTRO AUTOMOTRIZ PACHUCA PLAZA Q",
							//"496|CENTRO AUTOMOTRIZ PASEO DURANGO",
							//"542|CENTRO AUTOMOTRIZ PASEO LOS MOCHIS",
							/*"491|CENTRO AUTOMOTRIZ PASEO MORELIA",
							"425|CENTRO AUTOMOTRIZ PERISUR",
							"495|CENTRO AUTOMOTRIZ PLAYA DEL CARMEN",
							"452|CENTRO AUTOMOTRIZ PLAZA CENTRAL",
							"468|CENTRO AUTOMOTRIZ PLAZA CHIHUAHUA",
							"424|CENTRO AUTOMOTRIZ PLAZA SATELITE",
							"434|CENTRO AUTOMOTRIZ PUEBLA CENTRO",
							"462|CENTRO AUTOMOTRIZ QUERETARO",
							"470|CENTRO AUTOMOTRIZ QUERETARO PLAZA",
							"428|CENTRO AUTOMOTRIZ SAN AGUSTIN",
							"553|CENTRO AUTOMOTRIZ SAN ANGEL",
							"450|CENTRO AUTOMOTRIZ SAN LUIS POTOSI",
							"430|CENTRO AUTOMOTRIZ SANTA FE",
							"494|CENTRO AUTOMOTRIZ SEARS MERIDA",
							"419|CENTRO AUTOMOTRIZ SLP PLAZA",
							"436|CENTRO AUTOMOTRIZ TAMPICO",
							"563|CENTRO AUTOMOTRIZ TAMPICO ALTAMA",
							"437|CENTRO AUTOMOTRIZ TANGAMANGA",
							"487|CENTRO AUTOMOTRIZ TEPIC",
							"482|CENTRO AUTOMOTRIZ TEZONTLE",
							"549|CENTRO AUTOMOTRIZ TLALNEPANTLA",
							"469|CENTRO AUTOMOTRIZ TORREON",
							"478|CENTRO AUTOMOTRIZ TUXTLA",
							"423|CENTRO AUTOMOTRIZ UNIVERSIDAD",
							"464|CENTRO AUTOMOTRIZ VALLEJO",
							"451|CENTRO AUTOMOTRIZ VERACRUZ CENTRO",
							"449|CENTRO AUTOMOTRIZ VERACRUZ PLAZA",
							"545|CENTRO AUTOMOTRIZ VIA VALLEJO",
							"427|CENTRO AUTOMOTRIZ VILLACOAPA",
							"418|CENTRO AUTOMOTRIZ VILLAHERMOSA",
							"473|CENTRO AUTOMOTRIZ WORLD TRADE CENTE",
							"479|CENTRO AUTOMOTRIZ XALAPA PLAZA",
							"498|CENTRO AUTOMOTRIZ ZENTRALIA",
							"962|CENTRO DE ACOPIO GUADALAJARA",
							"960|CENTRO DE ACOPIO MERIDA",
							"871|CENTRO DE DEVOLUCIONES",
							"963|CENTRO DE DEVOLUCIONES GUADALAJARA",
							"961|CENTRO DE DEVOLUCIONES MERIDA",
							"877|CENTRO DE DEVOLUCIONES MONTERREY",
							"872|CENTRO DE DISTRBUCION Y PARTES",
							"870|CENTRO DE DISTRIBUCION A TIENDAS",
							"876|CENTRO DE DISTRIBUCION DEL NORTE",
							"112|CENTRO HISTORICO",
							"968|CERTIFICADOS DE REGALOS",*/
							"223|CHIHUAHUA PLAZA",
							"256|CHIMALHUACAN",
							"227|CIUDAD JUAREZ",
							"116|COACALCO",
							"125|COACALCO COSMOPOL",
							"107|COAPA",
							"228|COATZACOALCOS",
							//"217|COLIMA",
							"244|COLIMA ZENTRALIA",
							"317|CORDOBA",
							//"130|CUATRO CAMINOS",
							"121|CUAUTITLAN",
							"208|CUERNAVACA",
							"225|CULIACAN FORUM",
							"212|CULIACAN GALERIAS",
							"306|DURANGO CENTRO",
							"115|ECATEPEC",
							//"246|GALERIAS ATIZAPAN",
							"210|GOMEZ PALACIO",
							"202|GUADALAJARA CENTRO",
							"226|GUADALAJARA GALERIAS",
							"109|GUADALAJARA PLAZA",
							//"209|HERMOSILLO CENTRO",
							"216|HERMOSILLO GALERIAS MALL",
							"101|INSURGENTES",
							"123|INTERLOMAS",
							/*"570|INTERNET",
							"572|INTERNET ORACLE COMMERCE",
							"571|INTERNET VIP",*/
							"304|IRAPUATO",
							"253|IRAPUATO CIBELES",
							//"313|JALAPA CENTRO",
							//"602|JEANIOUS PLAZA CARSO",
							//"5700|KIOSKOS USA",
							"211|LEON PLAZA",
							"106|LINDAVISTA",
							//"152|MATERIA PRIMA",
							"247|MAZATLAN GALERIAS",
							"234|MERIDA ALTABRIZA",
							"311|MERIDA CENTRO",
							"263|MERIDA LAS AMERICAS",
							"215|MERIDA PLAZA",
							"221|METEPEC",
							"318|MINATITLAN",
							"113|MONTERREY ANAHUAC",
							"201|MONTERREY CENTRO",
							"232|MONTERREY CITADEL",
							"252|MONTERREY ESFERA",
							"117|MONTERREY GALERIAS",
							"108|MONTERREY SAN AGUSTIN",
							//"305|MORELIA",
							"237|MORELIA ALTOZANO",
							"236|MORELIA LAS AMERICAS",
							//"162|MTY SN AGUSTIN DORIANS",
							//"153|MUESTRAS",
							"119|NEZAHUALCOYOTL",
							//"239|NUEVO VERACRUZ",
							"214|OAXACA PLAZA",
							//"999|OFICINAS CORPORATIVAS",
							"213|PACHUCA OUTLET",
							"233|PACHUCA PLAZA Q",
							"118|PARQUE TEZONTLE",
							"243|PASEO DURANGO",
							"249|PASEO LOS MOCHIS",
							"105|PERISUR",
							/*"369|PHILOSOPHY INSURGENTES",
							"368|PHILOSOPHY TLALNEPANTLA",
							"705|PIER 1 DURAZNOS",
							"702|PIER 1 LA ISLA",
							"701|PIER 1 MTY",
							"703|PIER 1 PLAZA CARSO",
							"704|PIER ONE CUERNAVACA",*/
							"242|PLAYA DEL CARMEN",
							//"120|PLAZA CENTRAL",
							"102|POLANCO",
							//"127|PUBLICIDAD",
							"203|PUEBLA CENTRO",
							"224|QUERETARO PLAZA",
							//"128|SAN ANGEL",
							"241|SAN LUIS PLAZA",
							"301|SAN LUIS POTOSI CENTRO",
							"110|SANTA FE",
							"104|SATELITE",
							"205|TAMPICO",
							//"254|TAMPICO ALTAMA",
							"206|TANGAMANGA",
							"235|TEPIC",
							"126|TLALNEPANTLA",
							"222|TORREON",
							"229|TUXTLA GUTIERREZ",
							//"388|UNIDAD CLAROSHOP",
							//"600|UNIDAD DE SIG",
							"980|UNIDAD DE TRANSITO",
							"103|UNIVERSIDAD",
							"706|VALLEJO OUTLET",
							/*"400|VENTAS COMERCIALES",
							"402|VENTAS INTRACOMPAÐIAS",
							"403|VENTAS INTRACOMPAÐIAS CONSIGNACION",
							"401|VENTAS LENTO DESPLAZAMIENTO",*/
							"302|VERACRUZ CENTRO",
							"219|VERACRUZ PLAZA LAS AMERICAS",
							"124|VIA VALLEJO",
							"240|VILLAHERMOSA",
							"114|WORLD TRADE CENTER",
							"230|XALAPA PLAZA LAS AMERICAS",
							"250|ZACATECAS GALERIAS"
							};   
					for (String s: elementos) {
					        //Do your stuff here
						driver.findElement(By.xpath("//select[@name='LisPadre']/option[@value='"+s+"']")).click();
					
						driver.findElement(By.name("cmdGenerar")).click();
						TimeUnit.SECONDS.sleep(5);
						String winHandleBefore2 = driver.getWindowHandle();
	                    for (String winHandle : driver.getWindowHandles()) {
	                        driver.switchTo().window(winHandle);
	                    }
	                    List<WebElement> inputs = driver.findElements(By.tagName("input"));
	
	                    for (WebElement input : inputs) {
	                        ((JavascriptExecutor) driver).executeScript(
	                                    "arguments[0].removeAttribute('readonly','readonly')",input);
	                    }
	                    driver.findElement(By.name("cmdVerFiltroPeriodo")).click();
	                    driver.findElement(By.name("txtFechaIni")).clear();
	                    driver.findElement(By.name("txtFechaFin")).clear();
	                    driver.findElement(By.name("txtFechaIni")).sendKeys(fdDay+"-"+fdMonth+"-"+fdYear);
	                    driver.findElement(By.name("txtFechaFin")).sendKeys(fdDay+"-"+fdMonth+"-"+fdYear);
	                    driver.findElement(By.name("cmdPeriodo")).click();
	    				boolean checkError = driver.getPageSource()
	    						.contains("An error occurred on the server when processing the URL.  Please contact the system administrator");
	    				TimeUnit.SECONDS.sleep(3);
	    				if(checkError == true){
	    					driver.close();
	    					TimeUnit.SECONDS.sleep(3);
	    					log.error("ELEMENTO--> "+s);
	    				}else{
		                    Document doc;
		                    String html_content = driver.getPageSource();
							doc = Jsoup.parse(html_content);
							Element table = doc
							        .select("table.tableDetail")
							        .get(0);
							
							Elements rows = table.select("tr");
		
							Elements ths = rows.select("th");
		
							String thstr = "";
							for (Element th : ths) {
							    thstr += th.text() + " ";
							}
							int countElement = 0;
							int countElementtd = 0;
							String lineas = "";
							for (Element row : rows) {
								if(countElement > 3){
								    Elements tds = row.select("td");
								    for (Element td : tds) {
								    	lineas = td.text().concat("|");
								    	if(countElementtd == 15){
								    		lineas = lineas.replaceAll("|", "");
								    		lineas = lineas.concat("\n");
								    	}
								    	try {
								    	    Files.write(Paths.get(pathFile), lineas.getBytes(), StandardOpenOption.APPEND);
								    	}catch (IOException e) {
								    	    //exception handling left as an exercise for the reader
								    	}
								        //System.out.println(lineas); // --> This will print them indiviadually
								        //System.out.println("+++++++++++"+countElementtd);
								        countElementtd++;
								    }
								    countElementtd = 0;
								    //System.out.println(tds.text()); // -->This will pring everything
								}
							                                    // in the row
							    countElement++;
							    
							}
							// System.out.println(table);
							driver.close();
							TimeUnit.SECONDS.sleep(3);
	    				}
						driver.switchTo().window(winHandleBefore2);
					}
					System.out.println("Termina descarga de archivo");					
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

	
	public static void moveFile (String fecha){
		String path = config.getPathDownloads() + "\\";
		String nameOriginal = "InvenVtas.txt";
		String downloadedFile = path+nameOriginal;
		String filePrefix = prop.getProperty(portal + ".prefix");
		String pathFile = config.getPathFolderData() + portal + "\\" + filePrefix + fecha + ".txt";
		File afile =new File(downloadedFile);
		
		//MOVIENDO ARCHIVO
		if(afile.renameTo(new File(pathFile))){
			System.out.println("Archivo movido correctamente!");
		}else{
			log.error("Fallo al mover Archivo!");
		}
		checkFiles(nameOriginal);
	}
	
	public static void checkFiles(String nameFileDelete) {
		String path = config.getPathDownloads() + "\\";
		File file = new File(path + nameFileDelete);
		System.out.println("<<<>>>"+file);

		boolean bool = false;

		try {

			bool = file.exists();

			if (bool == true) {
				file.delete();

				log.info("[-]Se elimino archivo de descarga " + nameFileDelete);
			}
			bool = file.exists();

		} catch (Exception e) {
			log.error(e.getMessage());
		}
	}
}
