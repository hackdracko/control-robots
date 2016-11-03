package com.evolve.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
//import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author DarthAE
 */
public class CommonErrorFile {

    private static final Logger logger = Logger.getLogger(CommonErrorFile.class.getName());

    public CommonErrorFile() {
    }

    public void writeCommonWebTxtFile(String path, String content) {
        //PrintWriter pw = null;

        try {
            File file = new File(path);

            if (!file.exists()) {
                file.createNewFile();
            }

            FileWriter fw = new FileWriter(file);
            BufferedWriter bw = new BufferedWriter(fw);

            bw.write(content + "\n");
            bw.close();

        } catch (Exception e) {
            logger.severe(e.getMessage());
        }
    }
    
    public void writeDateFailFile(String path, String fecha) {
        //PrintWriter pw = null;

        try {
            File file = new File(path);
            FileWriter fw = new FileWriter(file, true);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(fecha + "\n");
            bw.close();

        } catch (Exception e) {
            logger.severe(e.getMessage());
        }
    }
    
    public void writePathFile(String pathFile, String pathNameFile) {
        //PrintWriter pw = null;

        try {
            File file = new File(pathFile);
            FileWriter fw = new FileWriter(file, true);
            try (BufferedWriter bw = new BufferedWriter(fw)) {
                bw.write(pathNameFile + "\n");
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error al escribir path de archivo a unir{0}", e.getMessage());
        }
    }
}
