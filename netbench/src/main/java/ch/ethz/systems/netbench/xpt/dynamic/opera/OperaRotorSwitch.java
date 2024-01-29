package ch.ethz.systems.netbench.xpt.dynamic.opera;

import ch.ethz.systems.netbench.core.Simulator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class OperaRotorSwitch {
    private int id;
    private ArrayList<ArrayList<Integer>> matchings;
    private int currMatching;
    private boolean reconfiguring;
    public OperaRotorSwitch(int id){
        this.id = id;
        this.matchings = new ArrayList<>();
        reconfiguring = false;
    }

    public void loadRotorFromFile(File rotorFile) throws FileNotFoundException {
        Scanner reader = new Scanner(rotorFile);
        while(reader.hasNextLine()){
            String matchingString = reader.nextLine();
            String[] matchingArray = matchingString.split(" ");
            ArrayList<Integer> matching = new ArrayList<>();
            for(String match: matchingArray){
                matching.add(Integer.parseInt(match));
            }
            matchings.add(matching);
        }
        currMatching = 0;
    }

    public void reconfigure() {
        reconfiguring = true;
    }

    public boolean isReconfiguring(){
        return reconfiguring;
    }

    public void start() {
        reconfiguring = false;
        currMatching = (currMatching + 1) % matchings.size();
    }

    public int getId() {
        return id;
    }

    public boolean aboutToReconfigure() {
        OperaController operaController = OperaController.getInstance();
        return Simulator.getTimeFromNow(operaController.getReconfigurationInterval())
                - operaController.getRotorGuardTime()
                <= Simulator.getCurrentTime();
    }

    @Override
    public String toString(){
        return this.matchings.get(currMatching).toString();
    }

    public boolean hasNextMatching() {
        return currMatching < matchings.size() - 1;
    }

    public boolean contains(int dest) {
        return matchings.get(currMatching).contains(dest);
    }

    public boolean containsAt(int source ,int dest) {
        return matchings.get(currMatching).get(source) == dest;
    }

    @Override
    public boolean equals(Object o){
        OperaRotorSwitch ors = (OperaRotorSwitch) o;
        return ors.id == this.id;
    }

    public int getAt(int source) {
        return matchings.get(currMatching).get(source);
    }
}
