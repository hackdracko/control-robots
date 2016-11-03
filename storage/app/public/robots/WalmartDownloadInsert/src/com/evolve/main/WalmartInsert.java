package com.evolve.main;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
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

public class WalmartInsert {
		
	static String portal = "walmart";
	static String cuenta, idProyecto, idCadena;	
	static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(WalmartInsert.class);
	static Configurations config;
	static Utileria util;		
	static Connection connection;
	
	public static void setGlobalParams(String cnta) {
		config = new Configurations(cnta);
		util = new Utileria(portal, cnta);
		cuenta = cnta;
		connection = config.getConnection();				
	}
	
	public static void insertarBD(String cuenta) {
		
		setGlobalParams(cuenta);							                               
		
		log.info("INSERT; Se ejecuta Walmart insert");
		util.insertLog(cuenta, portal, "Insert - Main: Se ejectuta walmart insert", "success");
		
		//Obteniendo la ultima fecha insertada en la BD
		String ultimaFecha = "";		
		Statement s;
		System.out.println("paso1");
		try {			
			
			s = (Statement) connection.createStatement();
			System.out.println("paso2");
			System.out.println("SELECT FechaCreacion AS fecha FROM tempwalmart GROUP BY FechaCreacion ORDER BY FechaCreacion DESC LIMIT 1 ;");
			ResultSet rs = s.executeQuery ("SELECT FechaCreacion AS fecha FROM tempwalmart GROUP BY FechaCreacion ORDER BY FechaCreacion DESC LIMIT 1 ;");
			System.out.println("paso3");
			rs.next();
			//ultimaFecha = rs.getString("fecha");
			ultimaFecha = "2016-08-24";
			//formateando la fecha obtenida
			//prod: 2016/03/01
			//Prod: anio-mes-dia
			log.info("Ultima Fecha: "+ultimaFecha);
			String[] split = ultimaFecha.split("-");
			ultimaFecha = split[1]+"-"+split[2]+"-"+split[0];			
		} catch (SQLException e1) {			
			e1.printStackTrace();
		}		
				
		ArrayList<String> oldDatesW = util.getDatesForLastInsert(ultimaFecha, "MM-dd-yyyy");					
        
        for (int k = 0; k < oldDatesW.size(); k++) {
            try {
                log.info("INSERT: Comienza inserción de portal Walmart Cesarfer venta diaria");  
                util.insertLog(cuenta, portal, "Download - Main: Comienza inserción de walmart venta diaria", "success");
                //Obteniendo la primera fecha del arreglo
                String fecha = oldDatesW.get(k);
                //Renombrtando el archivo
                util.renameFile(fecha, config);                
                readDataWalmartCesarfer(fecha);                
            } catch (Exception e) {
                log.info(e.getMessage());
            }
        }	
        
        if (null != connection) {
        	try {
				connection.close();
			} catch (SQLException e) {				
				e.printStackTrace();
			}
        }
        
        log.info("[-]Termina insersiÃ³n de portal Walmart Cesarfer venta diaria");
        util.insertLog(cuenta, portal, "Download - Main: Termina robot", "success");
	}
	
	public static void readDataWalmartCesarfer(String fecha) {
		String pathFiles = config.getPathFolderData();		
		Properties prop = util.getPropertiesPortal();
		
    	try {
    		TimeUnit.SECONDS.sleep(2);
            String[] headerRow = {"id", "fechaCreacion", "fechaCarga", "formato", "departamento", "vendedorname", "cadena", "tienda", "numtienda", "ean", "vendedornum", "descripcion", "unitretail", "unitcost", "mu", "posqty", "possales", "invonhand", "invonhandretail", "invinwhse", "invinwhseretail", "invintransit", "invintransitretail", "invonorder", "invonorderretail"};
            Boolean isDoble = false;
            
            String urlFile = pathFiles+portal+"\\"+prop.getProperty(portal+".prefix")+fecha + ".csv";
            File csvFile = new File(urlFile);
            
            if (csvFile.exists()) {
            	log.info("READ: El archivo "+fecha+" existe");
            	loadCSVWalmartCesarfer(urlFile, "tempwalmart", true, headerRow, isDoble, fecha);
            	insertDataWalmartCesarfer(fecha);
            	
            	//Registrando la carga de archivos en el historial
            	Connection genConn = config.getConnectionGeneral();
            	Statement s = (Statement) genConn.createStatement();
            	ResultSet pc = getProyectosCadenas();
            	pc.next();
            	
            	//Reordenando fecha
            	String[] tmp = fecha.split("-");
            	String nuevaFecha = tmp[2]+"-"+tmp[0]+"-"+tmp[1];
            	            	
            	s.executeUpdate("INSERT INTO archivoscargados(idProyecto, idCadena, archivo, fechaArchivo, fechaCarga, descripcion, tamano) VALUES("+pc.getInt("idProyecto")+", "+pc.getInt("idCadena")+", '"+urlFile+"', '"+nuevaFecha+"', CURRENT_TIMESTAMP, '"+portal+"-"+cuenta+"', '"+csvFile.length()+"');");            	
            }
            else 
            	log.info("READ: El archivo con fecha: "+fecha+" no existe o no se a descargado aun. Pasando al siguiente");

        } catch (Exception e) {
            log.info(e.getMessage());
        }
    }	
	
	@SuppressWarnings("resource")
	public static void loadCSVWalmartCesarfer(String csvFile, String tableName, boolean truncateBeforeLoad, String[] headerRow, boolean isDoble, String fecha) throws Exception {		
		char separator = '|'; 
		String SQL_INSERT = "INSERT INTO ${table}(${keys}) VALUES(${values})";
		String TABLE_REGEX = "\\$\\{table\\}";
		String KEYS_REGEX = "\\$\\{keys\\}";
		String VALUES_REGEX = "\\$\\{values\\}";				
		
		log.info("LOAD: procesando archivo: "+csvFile);
		
        CSVReader csvReader = null;
        if (null == connection) {
            throw new Exception("La conexion no es valida");
        }
        try {        	
            csvReader = new CSVReader(new FileReader(csvFile), separator);
        } catch (Exception e) {
            log.info(e.getMessage());
            throw new Exception("Error al ejecutar archivo CSV "+e.getMessage());
        }

        if (null == headerRow) {
            throw new FileNotFoundException("No existen columnas definidas en este CSV. "+"Por favor revisar el formato del CSV.");
        }

        String questionmarks = StringUtils.repeat("?,", headerRow.length);
        questionmarks = (String) questionmarks.subSequence(0, questionmarks.length() - 1);

        String query = SQL_INSERT.replaceFirst(TABLE_REGEX, tableName);
        query = query
                .replaceFirst(KEYS_REGEX, StringUtils.join(headerRow, ","));
        query = query.replaceFirst(VALUES_REGEX, questionmarks);

        String[] nextLine;        
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = connection;
            con.setAutoCommit(false);
            ps = con.prepareStatement(query);

            final int batchSize = 1000;
            int count = 0;

            String dia = fecha.substring(3, 5);
            String mes = fecha.substring(0, 2);
            String anio = fecha.substring(6);//FECHA WALMART

            String fechaNueva = anio + "-" + mes + "-" + dia;
            //logger.log(Level.INFO, "FECHA QUE VA: {0}", fechaNueva);

            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Calendar cal = Calendar.getInstance();
            String fechaSistema = dateFormat.format(cal.getTime());
            //logger.log(Level.INFO, "FECHA DEL SISTEMA: {0}", fechaSistema);

            if (isDoble == true) {
                nextLine = csvReader.readNext();
                nextLine = csvReader.readNext();
                nextLine = csvReader.readNext();
            } 
            else {
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
                        ps.setString(4, "WALMART");
                        ps.setString(15, "0");

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
            
            log.info("LOAD: Registros de datos de CSV Walmart Cesarfer a BD exitoso");

        } 
        catch (SQLException | IOException e) {
        	con.rollback();                       
            log.info(e.getMessage());
            throw new Exception("LOAD: Error ocurrido mientras se cargaban datos del archivo Walmart-Cesarfer a la base de datos. "+e.getMessage());
        } finally {            
            csvReader.close();
        }
    }
	
	public static void insertDataWalmartCesarfer(String fecha) throws Exception {
		log.info("Fecha para insertar en concentradov: "+fecha);
        if (null == connection) {
            throw new Exception("La conexion no es valida");
        }

        String dia = fecha.substring(3, 5);
        String mes = fecha.substring(0, 2);
        String anio = fecha.substring(6);//FECHA WALMART

        String fechaNueva = anio + "-" + mes + "-" + dia;        

        Connection con = null;
        PreparedStatement ps = null;

        try {
            con = connection;
            con.setAutoCommit(false);
            log.info("INSERT: Comienza InserciÃ³n de datos Walmart-Cesarfer");        
            
            String queryInsert="";
            if (cuenta.equals("cesarfer")) {
            	queryInsert = "INSERT INTO concentradov (proyecto,archivoCadena,idTienda,idTiendaReal,grupo,formato,cadena,sucursal,promotoria,chksumt,idProducto,upc,item,division,categoria,subCategoria,modelo,material,chksump,nombre,costoUnidadMB,ventasUnidades,ventasImporte,existenciasUnidades,existenciasImporte,fecha,fechaCarga,idArchivoCarga,tiendaFalta,productoFalta) "+
                    	"SELECT "+
                        "  'cesarfer', 'WALMART', t.id AS idTienda, t.idTiendaReal, t.grupo, t.formato, t.cadena, t.sucursal, t.promotoria, t.chksum, p.id, p.upc, p.item, p.division, p.categoria, p.subCategoria, p.modelo, p.material, p.chksum, p.nombre, p.costoUnidad, x.posQty, x.posSales, x.invOnHand, x.invOnHandRetail, FechaCreacion, CURRENT_TIMESTAMP, '9', '0', '0' "+
                        "FROM tempwalmart x "+
                            "INNER JOIN cubogeneral.cattiendas t ON (t.grupo IN ('11') AND x.numtienda=t.idTiendaReal) "+
                            "INNER JOIN catproductos p ON (x.ean=p.wm) "+
                        "WHERE x.fechaCreacion = '"+fechaNueva+"' AND x.posQty>0 "+
                    "GROUP BY x.id";
            }            
            
            if (cuenta.equals("spinmaster")) {
            	queryInsert = "INSERT INTO concentradov (proyecto,archivoCadena,idTienda,idTiendaReal,grupo,formato,cadena,sucursal,promotoria,chksumt,idProducto,upc,item,division,categoria,subCategoria,modelo,material,chksump,nombre,costoUnidadMB,ventasUnidades,ventasImporte,existenciasUnidades,existenciasImporte,fecha,fechaCarga,idArchivoCarga,tiendaFalta,productoFalta) "+
					"SELECT "+ 
					    "'spinmaster', 'WALMART', ct.id AS idTienda, "+ 
					    "ct.idTiendaReal, ct.grupo, ct.formato, ct.cadena, "+ 
					    "ct.sucursal, ct.promotoria, ct.chksum, "+
					    "cp.id, cp.upc, cp.item, cp.division, "+
					    "cp.categoria, cp.subCategoria, "+
					    "cp.modelo, cp.material, cp.chksum, "+ 
					    "cp.nombre, cp.costoUnidad, "+
					    "x.posQty, x.posSales, x.invOnHand, "+ 
					    "x.invOnHandRetail, "+ 
					    "FechaCreacion, CURRENT_TIMESTAMP, "+ 
					    "'9', '0', '0' "+ 
					"FROM "+ 
					    "tempwalmart x, cubogeneral.cattiendas ct, catproductos cp "+ 
					"WHERE "+ 
					    "ct.idTiendaReal = x.numTienda AND "+ 
					    "CONCAT('00',cp.skuWalmart) = x.ean AND "+ 
					    "ct.grupo = 11 AND "+ 
					    "x.fechaCreacion = '"+fechaNueva+"' AND "+
					    "x.posQty>0 GROUP BY x.id";
            }
            
            log.info("INSERT SQL: "+queryInsert);            

            ps = con.prepareStatement(queryInsert);

            ps.executeUpdate();
            con.commit();                      

        } catch (SQLException e) {
            con.rollback();           
            throw new Exception("INSERT: Error ocurrido mientras se insertaban datos del archivo Walmart Cesarfer a la base de datos."+e.getMessage());
        } finally {
            
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
	
	public static String getFecha() {
        String Fecha;
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("ddMMyyyy");
        calendar.add(Calendar.DATE, -1);
        Fecha = df.format(calendar.getTime());
        return Fecha;
    }
}
