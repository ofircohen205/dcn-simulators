package ch.ethz.systems.netbench.xpt.meta_node.v2;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class CongestionAlg {

    public static void initTransportRules(List<Demand> demands, long bitsCapacity){
        HashMap<Integer, Long> sourcesState = new HashMap<>();
        HashMap<Integer, Long> targetssState = new HashMap<>();
        for(Demand demand: demands){
            Pair p = new ImmutablePair(demand.getSourceServer(), demand.getDestServer());
            long currentReceiving = targetssState.getOrDefault(demand.getDestServer(), 0l);
            long currentSending = sourcesState.getOrDefault(demand.getSourceServer(),0l);
            long sendRule = Math.min(Math.min(demand.getBits(), bitsCapacity), bitsCapacity - currentReceiving);
            sendRule = Math.min(sendRule, bitsCapacity - currentSending);
            sourcesState.put(demand.getSourceServer(), sendRule + currentSending);
            targetssState.put(demand.getDestServer(), sendRule + currentReceiving);
            if(sendRule > 0){
                demand.addTransportRule(new TransportRule(demand.getSourceServer(),demand.getFlowId(), sendRule/8));

            }
        }
    }

    public static class TransportRule{
        public final int source;
        public final long flowId;
        public final long bytesToSend;
        private boolean allowed;
        private TransportRule(int source, long flowId, long bytesToSend){
            this.source = source;
            this.flowId = flowId;
            this.bytesToSend = bytesToSend;
        }

        @Override
        public String toString(){
            return "Source " + source + " flowId " + flowId + " bytesToSend " + bytesToSend + "\n";
        }

        public void allow() {
            allowed = true;
        }

        public boolean isAllowed(){
            return allowed;
        }
    }
}
