package com.evolve.util;

import java.util.Arrays;
import java.util.Calendar;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.StringTokenizer;
import java.util.Date;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.apache.log4j.Logger;

import com.evolve.config.Configurations;
import com.mysql.jdbc.Statement;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.util.Properties;

/**
 *
 * @author Aldo Mara√±on
 */
public class Utileria {		
    static Logger log = Logger.getLogger(Utileria.class);     
    static Properties prop;
    static String portal;
    static String cuenta;

    public Utileria(String namePortal, String cnta) {
    	portal = namePortal;
    	cuenta = cnta;    	
    }   
    
    /**
     * Metodo que regresa las valores del archivo general.properties
     * @return Properties
     */
    public Properties getPropertiesPortal() {    	
    	Configurations config = new Configurations(cuenta);
    	
    	String path = config.getPathProyect();
    	
    	Properties properties = new Properties();
    	InputStream input = null;
    	    	
    	try {
    		input = new FileInputStream(path+"\\general.properties");
    		properties.load(input);    		    		
    	} catch (IOException ex) {
    		log.info("[Error: No se pudo cargar el archivo general.properties]");
    	}        	
    	
    	return properties;
    }    
    
    /**
     * Regresa un array con las fechas a generar el link de a cuerdo al ultimo reporte bajado
     * en formato de fecha mm-dd-yyyy
     * @return ArrayList
     */
    public ArrayList<String> getDatesFromLastReportFile() {
    	Configurations config = new Configurations(cuenta);
    	
    	//obtenemos la ruta de los archivos bajados
    	String pathFiles = config.getPathFolderData()+"\\"+portal;    	
    	
    	//Obteniendo el listado de nombres de archivos ordernados por fecha
    	File folder = new File(pathFiles);
    	File[] listOfFiles = folder.listFiles();
    	Arrays.sort(listOfFiles);
    	
    	//Obtniendo el nombre del ultimo archivo bajado
    	String fileName = listOfFiles[listOfFiles.length-1].getName();
    	log.info(fileName);
    	
    	//Extrayendo la fecha del archivo
    	StringTokenizer token = new StringTokenizer(fileName, "_");
    	String temp = token.nextToken();
    	temp = token.nextToken();
    	token = new StringTokenizer(temp, ".");
    	String fecha = token.nextToken();
    	//String fecha = "08-24-2016";
    	
    	    	
    	//Obteniendo los dias entre la ultima fecha de reporte y la fecha actual
    	SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy");    	    	      
        
    	//Calendar calendar = Calendar.getInstance();
        Calendar calendar1 = Calendar.getInstance();
        Calendar calendar2 = Calendar.getInstance();
        
        //seteando la ultima fecha
        Date oldDate = null;
		try {
			oldDate = sdf.parse(fecha);
		} catch (ParseException e) {
			log.info("Error: no se pudo setear la ultima fecha de reporte");
			e.printStackTrace();
		}
        calendar1.setTime(oldDate);
          
        //seteando la fecha actual
        String cd = sdf.format(calendar2.getTime());
        //String cd = "08-25-2016";
        Date currentDate = null;
		try {
			currentDate = sdf.parse(cd);
		} catch (ParseException e) {
			log.info("Error: no se pudo setear la fecha actual");
			e.printStackTrace();
		}   
        calendar2.setTime(currentDate);                                       
        
        //obteniendo el rango de dias
        int dias = daysBetween(calendar1.getTime(), calendar2.getTime());        
        
        ArrayList<String> oldDates = new ArrayList<>();
        for (int i = 0; i <= dias; i++) {
            calendar1.add(Calendar.DATE, +1);
            log.info("old date: " + calendar1.getTime());
            oldDates.add(sdf.format(calendar1.getTime()));
        }

        return oldDates;
    }


    /**
     * Regresa un array con las fechas a generar el link de a cuerdo a las fechas seÒaladas
     * en formato definido por el usuario
     * @return ArrayList
     */
    public ArrayList<String> getDatesFromDates(String format, String fechainicial, String fechafinal) {
    	//Obteniendo el listado de nombres de archivos ordernados por fecha
    	String fecha = fechainicial;
    	    	
    	//Obteniendo los dias entre la ultima fecha de reporte y la fecha actual
    	SimpleDateFormat sdf = new SimpleDateFormat(format);    	    	      
        
    	//Calendar calendar = Calendar.getInstance();
        Calendar calendar1 = Calendar.getInstance();
        Calendar calendar2 = Calendar.getInstance();
        
        //seteando la ultima fecha
        Date oldDate = null;
		try {
			oldDate = sdf.parse(fecha);
			System.out.println("<<<>>>"+oldDate);
		} catch (ParseException e) {
			log.info("Error: no se pudo setear la ultima fecha de reporte");
			e.printStackTrace();
		}
        calendar1.setTime(oldDate);
          
        //seteando la fecha actual
        String cd = fechafinal;
        Date currentDate = null;
		try {
			currentDate = sdf.parse(cd);
		} catch (ParseException e) {
			log.info("Error: no se pudo setear la fecha actual");
			e.printStackTrace();
		}   
        calendar2.setTime(currentDate);
        //obteniendo el rango de dias
        int dias = daysBetweenDates(calendar1.getTime(), calendar2.getTime());
        
        ArrayList<String> oldDates = new ArrayList<>();
        for (int i = 0; i < dias; i++) {
            calendar1.add(Calendar.DATE, +1);
            log.info("old date: " + calendar1.getTime());
            oldDates.add(sdf.format(calendar1.getTime()));
        }
        return oldDates;
    }
    /**
     * Regresa un array con las fechas a generar el link de a cuerdo al ultimo reporte bajado
     * en formato definido por el usuario
     * @return ArrayList
     */
    public ArrayList<String> getDatesFromLastReportFileFormat(String format) {
    	Configurations config = new Configurations(cuenta);
    	//obtenemos la ruta de los archivos bajados
    	//String pathFiles = "C:\\Users\\SISTEMA\\Downloads\\evolve-web-master\\evolve-web-master\\ProyectosJava\\FilesData\\"+cuenta+"\\"+portal;
    	String pathFiles = config.getPathFolderData()+"\\"+portal;
    	//log.info(pathFiles);
    	//Obteniendo el listado de nombres de archivos ordernados por fecha
    	File folder = new File(pathFiles);
    	File[] listOfFiles = folder.listFiles();
    	Arrays.sort(listOfFiles, new Comparator<Object>() {
    	    public int compare(Object o1, Object o2) {

    	        if (((File)o1).lastModified() > ((File)o2).lastModified()) {
    	            return +1;
    	        } else if (((File)o1).lastModified() < ((File)o2).lastModified()) {
    	            return -1;
    	        } else {
    	            return 0;
    	        }
    	    }
    	});     	
    	
    	//Obtniendo el nombre del ultimo archivo bajado
    	String fileName = listOfFiles[listOfFiles.length-1].getName();
    	
    	//Extrayendo la fecha del archivo
    	StringTokenizer token = new StringTokenizer(fileName, "_");
    	String temp = token.nextToken();
    	temp = token.nextToken();
    	token = new StringTokenizer(temp, ".");
    	String fecha = token.nextToken();
    	//System.out.println("------"+fecha);
    	//System.exit(0);
    	//String fecha = "31052016";
    	
    	//Obteniendo los dias entre la ultima fecha de reporte y la fecha actual
    	SimpleDateFormat sdf = new SimpleDateFormat(format);    	    	      
        
    	//Calendar calendar = Calendar.getInstance();
        Calendar calendar1 = Calendar.getInstance();
        Calendar calendar2 = Calendar.getInstance();
        
        //seteando la ultima fecha
        Date oldDate = null;
		try {
			oldDate = sdf.parse(fecha);
		} catch (ParseException e) {
			log.info("Error: no se pudo setear la ultima fecha de reporte");
			e.printStackTrace();
		}
        calendar1.setTime(oldDate);
          
        //seteando la fecha actual
        String cd = sdf.format(calendar2.getTime());
        //String cd = "01092016";
        //System.out.println("<<<<<<<>>>> "+cd);
        Date currentDate = null;
		try {
			currentDate = sdf.parse(cd);
		} catch (ParseException e) {
			log.info("Error: no se pudo setear la fecha actual");
			e.printStackTrace();
		}   
        calendar2.setTime(currentDate);                                       
        
        //obteniendo el rango de dias
        int dias = daysBetween(calendar1.getTime(), calendar2.getTime());           
        
        ArrayList<String> oldDates = new ArrayList<>();        
        for (int i = 0; i < dias; i++) {
            calendar1.add(Calendar.DATE, +1);
            log.info("old date: " + calendar1.getTime());
            oldDates.add(sdf.format(calendar1.getTime()));
        }
        return oldDates;
    }
    
    /**
     * Regresa un array con las fechas a generar el link de a cuerdo al ultimo reporte bajado
     * en formato definido por el usuario
     * @return ArrayList
     */
    public ArrayList<String> getDatesFromLastReportFileFormatSeccion(String format, String seccion) {
    	Configurations config = new Configurations(cuenta);
    	//obtenemos la ruta de los archivos bajados
    	String pathFiles = "C:\\Users\\SISTEMA\\Downloads\\evolve-web-master\\evolve-web-master\\ProyectosJava\\FilesData\\"+cuenta+"\\"+portal+"\\"+seccion;
    	//String pathFiles = config.getPathFolderData()+"\\"+portal;
    	log.info(pathFiles);
    	//Obteniendo el listado de nombres de archivos ordernados por fecha
    	File folder = new File(pathFiles);
    	File[] listOfFiles = folder.listFiles();
    	Arrays.sort(listOfFiles, new Comparator<Object>() {
    	    public int compare(Object o1, Object o2) {

    	        if (((File)o1).lastModified() > ((File)o2).lastModified()) {
    	            return +1;
    	        } else if (((File)o1).lastModified() < ((File)o2).lastModified()) {
    	            return -1;
    	        } else {
    	            return 0;
    	        }
    	    }
    	});     	
    	
    	//Obtniendo el nombre del ultimo archivo bajado
    	String fileName = listOfFiles[listOfFiles.length-1].getName();
    	
    	//Extrayendo la fecha del archivo
    	StringTokenizer token = new StringTokenizer(fileName, "_");
    	String[] split = fileName.split("_");
    	String temp = token.nextToken();
    	token = new StringTokenizer(temp, ".");
    	String fecha = split[2];
    	//System.out.println("------"+fecha);
    	//System.exit(0);
    	//String fecha = "31052016";
    	    	
    	//Obteniendo los dias entre la ultima fecha de reporte y la fecha actual
    	SimpleDateFormat sdf = new SimpleDateFormat(format);    	    	      
        
    	//Calendar calendar = Calendar.getInstance();
        Calendar calendar1 = Calendar.getInstance();
        Calendar calendar2 = Calendar.getInstance();
        
        //seteando la ultima fecha
        Date oldDate = null;
		try {
			oldDate = sdf.parse(fecha);
		} catch (ParseException e) {
			log.info("Error: no se pudo setear la ultima fecha de reporte");
			e.printStackTrace();
		}
        calendar1.setTime(oldDate);
          
        //seteando la fecha actual
        String cd = sdf.format(calendar2.getTime());
        //String cd = "01092016";
        //System.out.println("<<<<<<<>>>> "+cd);
        Date currentDate = null;
		try {
			currentDate = sdf.parse(cd);
		} catch (ParseException e) {
			log.info("Error: no se pudo setear la fecha actual");
			e.printStackTrace();
		}   
        calendar2.setTime(currentDate);                                       
        
        //obteniendo el rango de dias
        int dias = daysBetween(calendar1.getTime(), calendar2.getTime());           
        
        ArrayList<String> oldDates = new ArrayList<>();        
        for (int i = 0; i < dias; i++) {
            calendar1.add(Calendar.DATE, +1);
            log.info("old date: " + calendar1.getTime());
            oldDates.add(sdf.format(calendar1.getTime()));
        }
        return oldDates;
    }
    
    /**
     * Metodo que regresa un arreglo de fechas desde la ultima insercci√≥n en bd a la fecha actual
     * @param lastInsert
     * @return
     */
    public ArrayList<String> getDatesForLastInsert(String uf, String format) {
    	String fecha = uf;
    	SimpleDateFormat sdf = new SimpleDateFormat(format);
    	Date tmpFecha = new Date();
    	//Calendar calendar = Calendar.getInstance();
    	Calendar ultimaFecha = Calendar.getInstance();
    	Calendar fechaActual = Calendar.getInstance();
    	
    	//seteando ultima fecha
    	try {
			tmpFecha = sdf.parse(fecha);
			//System.out.println("------------- "+tmpFecha);
			ultimaFecha.setTime(tmpFecha);
		} catch (ParseException e) {			
			e.printStackTrace();
		}
    	
    	//seteando fecha actual
    	try {
    		String tmp = sdf.format(fechaActual.getTime());
    		//String tmp = "01092016";
    		//System.out.println("------------- "+tmp);
			tmpFecha = sdf.parse(tmp);
			fechaActual.setTime(tmpFecha);
		} catch (ParseException e) {			
			e.printStackTrace();
		}
    	///System.out.println("<<<<<<<<<<<<<< "+ultimaFecha);
    	//System.out.println("<<<<<<<<<<<<<< "+fechaActual);
    	
    	int dias = daysBetween(ultimaFecha.getTime(), fechaActual.getTime()); 
    	ultimaFecha.add(Calendar.DATE, +1);
        
        ArrayList<String> oldDates = new ArrayList<>();
        for (int i = 0; i <= dias; i++) {        	
            log.info("old date: " + ultimaFecha.getTime());
            oldDates.add(sdf.format(ultimaFecha.getTime()));
            ultimaFecha.add(Calendar.DATE, +1);
        }

        return oldDates;
    }
    
    /**
     * Metodo que regresa un arreglo de fechas de los parametros
     * @param lastInsert
     * @return
     */
    public ArrayList<String> getDatesForDates(String uf, String ffinal, String format) {
    	String fecha = uf;
    	SimpleDateFormat sdf = new SimpleDateFormat(format);
    	Date tmpFecha = new Date();
    	//Calendar calendar = Calendar.getInstance();
    	Calendar ultimaFecha = Calendar.getInstance();
    	Calendar fechaActual = Calendar.getInstance();
    	
    	//seteando ultima fecha
    	try {
			tmpFecha = sdf.parse(fecha);
			//System.out.println("------------- "+tmpFecha);
			ultimaFecha.setTime(tmpFecha);
		} catch (ParseException e) {			
			e.printStackTrace();
		}
    	
    	//seteando fecha actual
    	try {
    		String tmp = ffinal;
			tmpFecha = sdf.parse(tmp);
			fechaActual.setTime(tmpFecha);
		} catch (ParseException e) {			
			e.printStackTrace();
		}
    	
    	int dias = daysBetween(ultimaFecha.getTime(), fechaActual.getTime()); 
    	ultimaFecha.add(Calendar.DATE, +1);
        
        ArrayList<String> oldDates = new ArrayList<>();
        for (int i = 0; i <= dias; i++) {        	
            log.info("old date: " + ultimaFecha.getTime());
            oldDates.add(sdf.format(ultimaFecha.getTime()));
            ultimaFecha.add(Calendar.DATE, +1);
        }

        return oldDates;
    }

    public int daysBetweenDates(Date d1, Date d2){
        return (int)( (d2.getTime() - d1.getTime()) / (1000 * 60 * 60 * 24));
    }
    
    public int daysBetween(Date d1, Date d2){
        return (int)( (d2.getTime() - d1.getTime()) / (1000 * 60 * 60 * 24))-1;
    }

    public boolean checkIfExist(String pathFiles, String prefix, String fecha) {
    	String nameFile = pathFiles+"\\"+prefix+fecha+".txt";
    	
    	File txtFile = new File(nameFile);    	       
    	
        boolean bool = false;
        try {
            bool = txtFile.exists();
            if (bool == true) {
                log.info("CHECKEXIST: Ya existe el archivo: "+prefix+fecha+".txt");
                bool = true;
                return bool;
            } else {
                log.info("CHECKEXIST: Archivo no encontrado...comienza la generaci√≥n/descarga del link/archivo");
            }
        } catch (Exception e) {
            log.info(e.getMessage());
        }

        return bool;
    }            

    public void renameFile(String fecha, Configurations conf) {
    	String pathFiles = conf.getPathFolderData();
    	Properties prop = this.getPropertiesPortal();    	
        File file = new File(pathFiles+portal+"\\"+prop.getProperty(portal+".prefix")+fecha+".txt");
        File file2 = new File(pathFiles+portal+"\\"+prop.getProperty(portal+".prefix")+fecha+".csv");
        log.info("Renombrando archivo "+file+" a "+file2);
        
        if (file.exists()) {
        	log.info("RENAME: El archivo "+fecha+" existe");
	        boolean success = file.renameTo(file2);
	        if (success) {
	            log.info("UTIL: Extencion de archivo "+portal+" con fecha " + fecha + " correctamente cambiado");
	        } else {                        
	            log.error("ERROR al cambiar extensiÛn de archivo "+portal+" con " + fecha + " Ya se renombro o no existe");
	        }
        }
        else 
        	log.info("UTIL: No se renombro el archivo con fecha "+fecha+", ya se renombro o no existe, pasando al siguiente archivo");
    }
    
    public void renameFileSeccion(String fecha, Configurations conf, String seccion) {
    	String pathFiles = conf.getPathFolderData();
    	Properties prop = this.getPropertiesPortal();    	
        File file = new File(pathFiles+portal+"\\"+seccion+"\\"+prop.getProperty(portal+".prefix")+seccion+"_"+fecha+".txt");
        File file2 = new File(pathFiles+portal+"\\"+seccion+"\\"+prop.getProperty(portal+".prefix")+seccion+"_"+fecha+".csv");
        log.info("Renombrando archivo "+file+" a "+file2);
        
        if (file.exists()) {
        	log.info("RENAME: El archivo "+fecha+" existe");
	        boolean success = file.renameTo(file2);
	        if (success) {
	            log.info("UTIL: Extencion de archivo "+portal+" con fecha " + fecha + " correctamente cambiado");
	        } else {                        
	            log.error("ERROR al cambiar extensiÛn de archivo "+portal+" con " + fecha + " Ya se renombro o no existe");
	        }
        }
        else 
        	log.info("UTIL: No se renombro el archivo con fecha "+fecha+", ya se renombro o no existe, pasando al siguiente archivo");
    }
    
    public void insertLog(String cuenta, String cadena, String log, String estatus) {
    	Configurations config = new Configurations(cuenta);
    	Connection conn = config.getConnectionGeneral();
    	
    	try {    		
    		Statement s = (Statement) conn.createStatement();
    		String sql = "INSERT INTO logs(cuenta, cadena, log, estatus) VALUES('"+cuenta+"', '"+cadena+"', '"+log+"', '"+estatus+"')";
    		s.executeUpdate(sql);
    		
    	} catch (SQLException e) {
    		e.printStackTrace();
    	}
    }
    
    public void insertRegistersFiles(String fecha, String urlFile, long fileSize) throws SQLException {
    	Configurations config = new Configurations(cuenta);
    	String nuevaFecha = "";
    	
    	log.info("Registro de archivos en bd");
        //Registrando la carga de archivos en el historial
    	Connection genConn = config.getConnectionGeneral();
    	Statement s = (Statement) genConn.createStatement();
    	ResultSet pc = getProyectosCadenas();
    	pc.next();
    	
    	//Reordenando fecha
    	log.info("Fecha archivos log: " +fecha);
    	switch(portal) {
    		case "soriana":
    			//ddMMyyyy to yyyy-mm-dd
    			nuevaFecha = fecha.substring(4, 8)+"-"+fecha.substring(2, 4)+"-"+fecha.substring(0, 2);
    		break;
    		
    		case "comercial":
    			//yyyyMMdd to yyyy-mm-dd
    			nuevaFecha = fecha;
    		break;
    		
    		case "palacio":
    			//ddMMyyyy to yyyy-mm-dd
    			nuevaFecha = fecha.substring(4, 8)+"-"+fecha.substring(2, 4)+"-"+fecha.substring(0, 2);
    		break;
    		
    		case "chedraui":
    			//ddMMyyyy to yyyy-mm-dd
    			nuevaFecha = fecha.substring(4, 8)+"-"+fecha.substring(2, 4)+"-"+fecha.substring(0, 2);
    		break;
    	}    	    	
    	String qsql = "INSERT INTO archivoscargados(idProyecto, idCadena, archivo, fechaArchivo, fechaCarga, descripcion, tamano) VALUES("+pc.getInt("idProyecto")+", "+pc.getInt("idCadena")+", '"+urlFile+"', '"+nuevaFecha+"', CURRENT_TIMESTAMP, '"+portal+"-"+cuenta+"', '"+fileSize+"');";    	
    	s.executeUpdate(qsql);
    }
    
    public static ResultSet getProyectosCadenas() {
    	Configurations config = new Configurations(cuenta);
    	Connection conn = (Connection) config.getConnectionGeneral();        	
		ResultSet rs = null;
		try {
			Statement s = (Statement) conn.createStatement();				
			rs = s.executeQuery ("SELECT * FROM proyectos_cadenas WHERE nombreProyecto LIKE '%"+cuenta+"%' AND nombreCadena LIKE '%"+portal+"%' LIMIT 1");					
		} catch (SQLException e) {				
			e.printStackTrace();
		} 
		
		return rs;
	}
    
    public ResultSet query(String sql, Connection connection) {    	    	    
		Statement s;
		ResultSet rs = null;
		
		try {						
			s = (Statement) connection.createStatement();
			rs = s.executeQuery (sql);									
		} catch (SQLException e1) {			
			e1.printStackTrace();
		}
				
		return rs;
    }
}
