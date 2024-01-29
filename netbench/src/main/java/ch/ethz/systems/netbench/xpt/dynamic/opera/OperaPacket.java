package ch.ethz.systems.netbench.xpt.dynamic.opera;

import ch.ethz.systems.netbench.ext.bare.BarePacket;
import ch.ethz.systems.netbench.ext.basic.TcpPacket;
import ch.ethz.systems.netbench.xpt.megaswitch.Encapsulatable;
import ch.ethz.systems.netbench.xpt.tcpbase.FullExtTcpPacket;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.ArrayList;

public class OperaPacket extends FullExtTcpPacket {
    ArrayList<ImmutablePair<Integer,Integer>> path;
    int currIndex;
    boolean secondHop = false;

    public OperaPacket(FullExtTcpPacket p){
        super(p);
        try{
            this.path = ((OperaPacket) p).path;
            this.currIndex = ((OperaPacket) p).currIndex;
            this.secondHop =  ((OperaPacket) p).secondHop;
        }catch (ClassCastException e){

        }
    }


    @Override
    public Encapsulatable encapsulate(int newSource, int newDestination) {
        return null;
    }

    @Override
    public Encapsulatable deEncapsualte() {
        return null;
    }

    public void setPath(ArrayList<ImmutablePair<Integer,Integer>> path){
        currIndex = 0;
        this.path = path;
    }

    public ImmutablePair<Integer, Integer> getNextHop() {

        return this.path.get(currIndex);
    }

    public void advance(){
        this.currIndex+=1;
    }

    public void markSecondHop() {
        secondHop = true;
    }

    public boolean isSecondHop() {
        return secondHop;
    }
}

