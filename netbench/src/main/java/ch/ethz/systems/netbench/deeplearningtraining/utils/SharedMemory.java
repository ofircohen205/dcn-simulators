package ch.ethz.systems.netbench.deeplearningtraining.utils;

import ch.ethz.systems.netbench.core.run.MainFromProperties;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.Map;

public class SharedMemory {

    public static Map<Long, Integer> receivePathAssignmentsFromController(
            String writeFilePath, String readFilePath, String content, String runDirectory, boolean isLpSolver
    ) {
        SharedMemory.writeFile(writeFilePath, content);
        executeProgram(runDirectory, isLpSolver);
        JsonReader reader = SharedMemory.readFile(readFilePath);
        Gson gson = new Gson();
        Map<Long, Integer> assignedPaths = gson.fromJson(reader, getTypeToken());
        new File(readFilePath).delete();
        return assignedPaths;
    }

    public static void writeFile(String path, String content) {
        try {
            PrintWriter writer = new PrintWriter(path, "UTF-8");
            writer.println(content);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static JsonReader readFile(String path) {
        File file = new File(path);
        try {
            while (!file.exists() || (file.exists() && Files.size(file.toPath()) == 0)) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Thread.sleep(100);
            return new JsonReader(new FileReader(file));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private static void executeProgram(String runDirectory, boolean isLpSolver) {
        String pythonCmd = "external.edge_coloring.main";
        String[] cmdArray = new String[]{"poetry", "run", "python", "-m", pythonCmd, runDirectory};

        if (isLpSolver) {
            pythonCmd = "external.ilp_solver.main";
            cmdArray = new String[]{"poetry", "run", "python", "-m", pythonCmd, runDirectory};
        }
        MainFromProperties.runCommand(cmdArray, false);
    }

    private static Type getTypeToken() {
        return new TypeToken<Map<Long, Integer>>() {
        }.getType();
    }
}
