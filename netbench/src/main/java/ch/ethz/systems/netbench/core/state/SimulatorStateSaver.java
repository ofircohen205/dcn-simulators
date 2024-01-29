package ch.ethz.systems.netbench.core.state;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.core.config.NBProperties;
import ch.ethz.systems.netbench.core.log.LogFailureException;
import ch.ethz.systems.netbench.core.log.SimulationLogger;
import ch.ethz.systems.netbench.core.network.TransportLayer;
import ch.ethz.systems.netbench.core.run.routing.remote.RemoteRoutingController;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.text.DecimalFormat;

public class SimulatorStateSaver {

    public static void save(String dirPath) {
        new File(dirPath).mkdirs();

        try {
            saveToDir(dirPath, null);
        } catch (IOException e) {
            throw new LogFailureException(e);
        }
    }

    public static void save(NBProperties configuration) {
        RemoteRoutingController rrc;
        rrc = RemoteRoutingController.getInstance();
        if (rrc == null) {
            System.out.println("Only central routing is currently supported");
            return;
        }

        long time = Simulator.getCurrentTime();
        long totalRunTIme = Simulator.getTotalRunTimeNs();
        DecimalFormat df = new DecimalFormat("#.##");
        double percent = ((double) time) / ((double) totalRunTIme) * 100;
        String dumpFolderName = "dumps" + "/" + "test_dump";
        //String dumpFolderName = SimulationLogger.getRunFolderFull() + "/" + df.format(percent) + "%";
        new File(dumpFolderName).mkdirs();
        String confFileName = configuration.getFileName();
        File confFile = new File(confFileName);

        try {
            saveToDir(dumpFolderName, rrc);
        } catch (IOException e) {
            throw new LogFailureException(e);
        }
    }

    public static JSONObject loadJson(String fileName) {
        File f = new File(fileName);
        if (f.exists()) {
            try {
                InputStream is = new FileInputStream(fileName);
                String jsonTxt = IOUtils.toString(is, "UTF-8");
                JSONParser parser = new JSONParser();
                JSONObject json = (JSONObject) parser.parse(jsonTxt);
                return json;
            } catch (IOException | ParseException e) {
                e.printStackTrace();
            }

        }
        throw new RuntimeException("Failure loading json from file " + fileName);
    }

    public static Object readObjectFromFile(String filename) {

        Object obj = null;

        FileInputStream fin = null;
        ObjectInputStream ois = null;

        try {

            fin = new FileInputStream(filename);
            ois = new ObjectInputStream(fin);
            obj = ois.readObject();

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {

            if (fin != null) {
                try {
                    fin.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (ois != null) {
                try {
                    ois.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        if (obj == null) {
            throw new RuntimeException("Error reading object from file " + filename);
        }
        return obj;

    }

    private static void saveToDir(String dirPath, RemoteRoutingController rrc) throws IOException {
        Simulator.dumpState(dirPath);
        SimulationLogger.dumpState(dirPath);
        TransportLayer.dumpState(dirPath);
        if (rrc != null) {
            rrc.dumpState(dirPath);
        }
    }

}
