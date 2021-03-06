package com.evolve.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import au.com.bytecode.opencsv.CSVReader;

import com.evolve.config.Configurations;
import com.evolve.util.Utileria;
import com.mysql.jdbc.Statement;

public class LiverpoolInsert {
		
	static String portal = "liverpool";
	static String cuenta, idProyecto, idCadena;	
	static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(LiverpoolInsert.class);
	static Configurations config;
	static Utileria util;		
	static Connection connection;
	static char separator = '|';
	
	public static void setGlobalParams(String cnta) {
		config = new Configurations(cnta);
		util = new Utileria(portal, cnta);
		cuenta = cnta;
		connection = config.getConnection();				
	}
	
	public static void insertarBD(String cuenta) {
		
		setGlobalParams(cuenta);
		
		log.info("INSERT; Se ejecuta Liverpool insert en cuenta: "+cuenta);
		util.insertLog(cuenta, portal, "Insert - insertarBD: insertando en bd", "success");
		System.out.println("Paso1");
		//Obteniendo la ultima fecha insertada en la BD
		String ultimaFecha = "";		
		Statement s;
		try {
			System.out.println("Paso2");
			s = (Statement) connection.createStatement();
			System.out.println("Paso3");
			ResultSet rs = s.executeQuery ("SELECT FechaCreacion AS fecha FROM templiverpool GROUP BY FechaCreacion ORDER BY FechaCreacion DESC LIMIT 1 ;");
			System.out.println(rs);
			rs.next();
			ultimaFecha = rs.getString("fecha");
			
			//formateando la fecha obtenida
			String[] split = ultimaFecha.split("-");
			ultimaFecha = split[2]+split[1]+split[0];	
			//ultimaFecha = "26082016";
		} catch (SQLException e1) {			
			e1.printStackTrace();
		}		
				
		log.info("ultima fecha: "+ultimaFecha);
		ArrayList<String> oldDates = util.getDatesForLastInsert(ultimaFecha, "ddMMyyyy");

        for (int k = 0; k < oldDates.size(); k++) {

            try {
                log.info("INSERT: Comienza insersión de portal Liverpool Cesarfer venta diaria");
                util.insertLog(cuenta, portal, "Insert - insertarBD: Comienza el proceso de insersión", "success");
                String fecha = oldDates.get(k);
                util.renameFile(fecha, config);
                readDataLiverpool(fecha);
                log.info("CALL: COMIENZA STORE");
                insertDataLiverpoolCesarfer(fecha);
                log.info("INSERT: Termina inserci�n de portal Liverpool Cesarfer venta diaria"); 
                util.insertLog(cuenta, portal, "Insert - insertarBD: Termina el proceso de inserci�n", "success");
            } catch (Exception e) {
                log.info(e.getMessage());
            }
        }
	}
	
	public static void readDataLiverpool(String fecha) {
		String pathFiles = config.getPathFolderData();		
		Properties prop = util.getPropertiesPortal();		
		
        try {

            TimeUnit.SECONDS.sleep(2);
            String[] headerRow = {"id", "fechaCreacion", "fechaCarga", "formato", "centroid", "centro", "articuloid", "articulo", "estado", "modelo", "eanupc", "temporada", "ventasuni", "ventasimp", "borrar"};
            Boolean isDoble = false;
            
            String urlFile = pathFiles+portal+"\\"+prop.getProperty(portal+".prefix")+fecha + ".csv";
            File csvFile = new File(urlFile);                      
            
            loadCSVLiverpoolCesarfer(urlFile, "tempLiverpool", true, headerRow, isDoble, fecha);
            
            log.info("Registro de archivos en bd");
            //Registrando la carga de archivos en el historial
        	Connection genConn = config.getConnectionGeneral();
        	Statement s = (Statement) genConn.createStatement();
        	ResultSet pc = getProyectosCadenas();
        	pc.next();
        	
        	//Reordenando fecha
        	log.info("Fecha split");        	
        	String nuevaFecha = fecha.substring(4, 8)+"-"+fecha.substring(2, 4)+"-"+fecha.substring(0, 2);
        	log.info("antes sql");
        	String qsql = "INSERT INTO archivoscargados(idProyecto, idCadena, archivo, fechaArchivo, fechaCarga, descripcion, tamano) VALUES("+pc.getInt("idProyecto")+", "+pc.getInt("idCadena")+", '"+urlFile+"', '"+nuevaFecha+"', CURRENT_TIMESTAMP, '"+portal+"-"+cuenta+"', '"+csvFile.length()+"');";
        	log.info("El sql es: "+qsql);
        	s.executeUpdate(qsql);
                        
        } catch (Exception e) {
            log.info(e.getMessage());
        }
    }
	
	@SuppressWarnings("resource")
	public static void loadCSVLiverpoolCesarfer(String csvFile, String tableName, boolean truncateBeforeLoad, String[] headerRow, boolean isDoble, String fecha) throws Exception {
		String SQL_INSERT = "INSERT INTO ${table}(${keys}) VALUES(${values})";
	    String TABLE_REGEX = "\\$\\{table\\}";
	    String KEYS_REGEX = "\\$\\{keys\\}";
	    String VALUES_REGEX = "\\$\\{values\\}";
		
        CSVReader csvReader = null;
        if (null == connection) {
            throw new Exception("LOAD: La conexion no es valida");
        }
        try {

            csvReader = new CSVReader(new FileReader(csvFile), separator);

        } catch (Exception e) {
            log.info(e.getMessage());
            throw new Exception("LOAD: Error al executar archivo CSV"+e.getMessage());
        }

        if (null == headerRow) {
            throw new FileNotFoundException("LOADER: No existen columnas definidas en este CSV. Por favor revisar el formato del CSV.");
        }

        String questionmarks = StringUtils.repeat("?,", headerRow.length);
        questionmarks = (String) questionmarks.subSequence(0, questionmarks.length() - 1);

        String query = SQL_INSERT.replaceFirst(TABLE_REGEX, tableName);
        query = query
                .replaceFirst(KEYS_REGEX, StringUtils.join(headerRow, ","));
        query = query.replaceFirst(VALUES_REGEX, questionmarks);
        
        System.out.println("<<->>"+query.toString());

        String[] nextLine;        
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = connection;
            con.setAutoCommit(false);
            ps = con.prepareStatement(query);

            final int batchSize = 1000;
            int count = 0;

            String dia = fecha.substring(0, 2);
            String mes = fecha.substring(2, 4);
            String anio = fecha.substring(4);//FECHA LIVERPOOL | PALACIO | COMERCIAL

            String fechaNueva = anio + "-" + mes + "-" + dia;
//            logger.log(Level.INFO, "FECHA QUE VA: {0}", fechaNueva);

            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Calendar cal = Calendar.getInstance();
            String fechaSistema = dateFormat.format(cal.getTime());
//            logger.log(Level.INFO, "FECHA DEL SISTEMA: {0}", fechaSistema);

            if (isDoble == true) {
                nextLine = csvReader.readNext();
                nextLine = csvReader.readNext();
                nextLine = csvReader.readNext();
            } else {
                nextLine = csvReader.readNext();
            }

            while ((nextLine = csvReader.readNext()) != null) {

                if (null != nextLine) {
                    int index = 5;
                    for (String string : nextLine) {
                    	//logger.log(Level.INFO, "{0}---{1}", new Object[]{string, index});
                        ps.setString(1, null);
                        ps.setString(2, fechaNueva);
                        ps.setString(3, fechaSistema);
                        ps.setString(4, "LIVERPOOL");
                        ps.setString(15, "0");
                        //System.out.println("piragna " + nextLine[12]);
                        //System.out.println("Aqui me quedo " + ps);
                        if (string.isEmpty()) {
                            string = "0";
                        }
                        ps.setString(index++, string);
                    }
                    ps.addBatch();
                }
                if (++count % batchSize == 0) {
                    ps.executeBatch();
                }
            }
            ps.executeBatch();
            con.commit();                       

            log.info("LOAD: Registros de datos de CSV Liverpool Cesarfer a BD exitoso");

        } catch (SQLException | IOException e) {
            con.rollback();                        
            log.info("Error en este query->"+ps);
            log.info(e.getMessage());
            throw new Exception("Error ocurrido mientras se cargaban datos del archivo Liverpool Cesarfer a la base de datos." + e.getMessage());
        } finally {
            if (null != ps) {
                ps.close();
            }           
            csvReader.close();
        }
    }
	
	public static void insertDataLiverpoolCesarfer(String fecha) throws Exception {

        if (null == connection) {
            throw new Exception("INSERT: La conexion no es valida");
        }

        String dia = fecha.substring(0, 2);
        String mes = fecha.substring(2, 4);
        String anio = fecha.substring(4);//FECHA LIVERPOOL | PALACIO | COMERCIAL

        String fechaNueva = anio + "-" + mes + "-" + dia;

        //DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        //Calendar cal = Calendar.getInstance();
        //String fechaSistema = dateFormat.format(cal.getTime());

        Connection con = null;
        CallableStatement callableStatement = null;
        log.info("INSERT: Comienza a Cargar el Store para concentradov -"+cuenta);
        String tempStore = "{call concentradoTempLiverpool(?)}";

        try {
            con = connection;
            con.setAutoCommit(false);
            log.info("INSERT: Comienza Inserci�n de datos Liverpool-"+cuenta);
            callableStatement = con.prepareCall(tempStore);
            callableStatement.setInt(1, 1);
            callableStatement.executeUpdate();
            con.commit();
            
        } catch (SQLException e) {
            con.rollback();            
            throw new Exception("Error ocurrido mientras se insertaban datos del archivo Liverpool Cesarfer a la base de datos." + e.getMessage());
        } finally {
            if (null != callableStatement) {
            	callableStatement.close();
            }            
        }
    }
	
	public static ResultSet getProyectosCadenas() {		
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
}
