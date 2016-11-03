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

public class PalacioInsert {
		
	static String portal = "palacio";
	static String cuenta, idProyecto, idCadena;	
	static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(PalacioInsert.class);
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
		
		log.info("INSERT; Se ejecuta Palacio insert en cuenta: "+cuenta);
		util.insertLog(cuenta, portal, "Insert - insertarBD: insertando en bd", "success");
		
		//Obteniendo la ultima fecha insertada en la BD
		String ultimaFecha = "";		
		Statement s;
		try {						
			s = (Statement) connection.createStatement();
			ResultSet rs = s.executeQuery ("SELECT FechaCreacion AS fecha FROM temppalacio GROUP BY FechaCreacion ORDER BY FechaCreacion DESC LIMIT 1 ;");
			rs.next();
			ultimaFecha = rs.getString("fecha");
			
			//formateando la fecha obtenida
			String[] split = ultimaFecha.split("-");
			ultimaFecha = split[2]+split[1]+split[0];			
		} catch (SQLException e1) {			
			e1.printStackTrace();
		}		
				
		log.info("ultima fecha: "+ultimaFecha);
		ArrayList<String> oldDates = util.getDatesForLastInsert(ultimaFecha, "ddMMyyyy");
						
        for (int k = 0; k < oldDates.size(); k++) {
            try {
                log.info("[+]Comienza insersión de portal Palacio Cesarfer venta diaria");
                util.insertLog(cuenta, portal, "Insert - insertarBD: Comiensa insercción portal", "success");
                
                String fecha = oldDates.get(k);
                util.renameFile(fecha, config);
                
                readDataPalacioCesarfer(fecha);
                log.info("[-]Termina insersión de portal Palacio Cesarfer venta diaria");
                util.insertLog(cuenta, portal, "Insert - insertarBD: Termina Insercción en BD", "success");
            } catch (Exception e) {
                log.info(e.getMessage());
            }
        }
	}
	
	public static void readDataPalacioCesarfer(String fecha) {
		String pathFiles = config.getPathFolderData();		
		Properties prop = util.getPropertiesPortal();
		
        try {
            
            TimeUnit.SECONDS.sleep(2);
            String[] headerRow = {"id", "fechacreacion", "fechaCarga", "formato", "anio", "costoarticulo", "aniotemporada", "canaldistribucion", "centro", "clase", "clavecanaldistribucion", "clavecentro", "claveclase", "clavecomprador", "clavedepartamento", "clavedivision", "clavegrupoarticulos", "clavenegocio", "claveproveedor", "clavesubclase", "comprador", "departamento", "dia", "diasemana", "division", "grupoarticulos", "idjerarquia", "marca", "marcaspropias", "mes", "mesanio", "negocio", "proveedor", "resurtible", "semana", "subclase", "temporada", "tipostock", "trimestre", "cuponesrtl", "dsctoempleadortl", "devolucionventarlt", "devolucionventaum", "rebajasmanualesrtl", "rebajaautomaticartl", "ssmvendidortl", "ventanetacto", "ventanetaum", "ventanetacup", "clavearticulo", "articulo", "estilo", "cfmsi", "upc", "talla", "color", "fechaprimeraentrada", "fechaultimaentrada", "bigticket", "claveestatus", "centrocomparable", "precioventacatalogo", "clavetipoarticulo", "area", "clavearea", "clavemarca", "clavemarcaspropias", "claveestilo", "clavetalla", "clavecolor", "clavebigticket", "indicadorcomp", "clavetallados", "tallados", "estatus", "clavetipostock", "tipoarticulo", "modelo", "estilogrupocompras", "grupocompras"};
            Boolean isDoble = false;
            
            String urlFile = pathFiles+portal+"\\"+prop.getProperty(portal+".prefix")+fecha + ".csv";
            File csvFile = new File(urlFile);
            
            loadCSVPalacioCesarfer(urlFile, "tempPalacio", true, headerRow, isDoble, fecha);
            insertDataPalacioCesarfer(fecha);
            
            //insertando log de archivos subidos
            util.insertRegistersFiles(fecha, urlFile, csvFile.length());

        } catch (Exception e) {
            log.info(e.getMessage());
        }
    }
	
	@SuppressWarnings("resource")
	public static void loadCSVPalacioCesarfer(String csvFile, String tableName, boolean truncateBeforeLoad, String[] headerRow, boolean isDoble, String fecha) throws Exception {
		String SQL_INSERT = "INSERT INTO ${table}(${keys}) VALUES(${values})";
	    String TABLE_REGEX = "\\$\\{table\\}";
	    String KEYS_REGEX = "\\$\\{keys\\}";
	    String VALUES_REGEX = "\\$\\{values\\}";

        CSVReader csvReader = null;
        if (null == connection) {
            throw new Exception("La conexion no es valida");
        }
        try {

            csvReader = new CSVReader(new FileReader(csvFile), separator);

        } catch (Exception e) {
            log.info(e.getMessage());
            throw new Exception("Error al executar archivo CSV" + e.getMessage());
        }

        if (null == headerRow) {
            throw new FileNotFoundException("No existen columnas definidas en este CSV. Por favor revisar el formato del CSV.");
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

            String dia = fecha.substring(0, 2);
            String mes = fecha.substring(2, 4);
            String anio = fecha.substring(4);//FECHA LIVERPOOL | PALACIO | COMERCIAL

            String fechaNueva = anio + "-" + mes + "-" + dia;            

            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Calendar cal = Calendar.getInstance();
            String fechaSistema = dateFormat.format(cal.getTime());

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
                        ps.setString(1, null);
                        ps.setString(2, fechaNueva);
                        ps.setString(3, fechaSistema);
                        ps.setString(4, "PALACIO");
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
            //util.deleteFileLiverpool(bandera);

            log.info("Registros de datos de CSV Palacio a BD exitoso");
            util.insertLog(cuenta, portal, "Insert - loadCSV: Registro de datos CSV palacio exitoso", "success");

        } catch (SQLException | IOException e) {
            con.rollback();            
            throw new Exception("Error ocurrido mientras se cargaban datos del archivo Palacio en temppalacio." + e.getMessage());
        } finally {
            if (null != ps) {
                ps.close();
            }            
            csvReader.close();
        }
    }
	
	public static void insertDataPalacioCesarfer(String fecha) throws Exception {

        if (null == connection) {
            throw new Exception("La conexion no es valida");
        }

        String dia = fecha.substring(0, 2);
        String mes = fecha.substring(2, 4);
        String anio = fecha.substring(4);//FECHA LIVERPOOL | PALACIO | COMERCIAL

        String fechaNueva = anio + "-" + mes + "-" + dia;

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar cal = Calendar.getInstance();
        String fechaSistema = dateFormat.format(cal.getTime());

        Connection con = null;
        PreparedStatement ps = null;

        try {
            con = connection;
            con.setAutoCommit(false);
            log.info("[+]Comienza Inserción de datos Palacio-Cesarfer");
            util.insertLog(cuenta, portal, "Insert - insertarDATA: Comienza Insercción en BD concentradov", "success");

            String queryInsert = "INSERT INTO concentradov (proyecto,archivoCadena,idTienda,idTiendaReal,grupo,formato,cadena,sucursal,promotoria,chksumt,idProducto,upc,item,division,categoria,subCategoria,modelo,material,chksump,nombre,costoUnidadMB,ventasUnidades,ventasImporte,existenciasUnidades,existenciasImporte,fecha,fechaCarga,idArchivoCarga,tiendaFalta,productoFalta) \n"
                    + "SELECT '"+cuenta+"', 'PH', t.id AS idTienda, t.idTiendaReal, t.grupo, t.formato, t.cadena, t.sucursal, t.promotoria, t.chksum, p.id, p.upc, p.item, p.division, p.categoria, p.subCategoria, p.modelo, p.material, p.chksum, p.nombre, p.costoUnidad, x.ventanetaum, x.ventanetacto, 0, 0, '" + fechaNueva + "', '" + fechaSistema + "' , '9', '0', '0' \n"
                    + "FROM tempPalacio x \n"
                    + "INNER JOIN cattiendas t ON (t.grupo IN ('7') AND x.clavecentro=t.idTiendaReal) \n"
                    + "INNER JOIN catproductos p ON (x.upc=p.upc) \n"
                    + "WHERE x.fechaCreacion= '" + fechaNueva + "' \n"
                    + "GROUP BY x.id";

            ps = con.prepareStatement(queryInsert);

            ps.executeUpdate();
            con.commit();

        } catch (SQLException e) {
            con.rollback();            
            log.info(e.getMessage());
            throw new Exception("Error ocurrido mientras se insertaban datos del archivo Palacio en concentradov." + e.getMessage());
        } finally {
            if (null != ps) {
                ps.close();
            }            
        }
    }
}
