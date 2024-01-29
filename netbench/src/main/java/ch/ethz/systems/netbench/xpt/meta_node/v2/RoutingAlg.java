package ch.ethz.systems.netbench.xpt.meta_node.v2;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public class RoutingAlg {

    private static RoutingAlg sInstance;

    private RoutingAlg(int N){
        initOrder(N);
    }
    private LinkedList<Pair<Integer, Integer>> order;
    private int MN;
    public static void reset() {
        sInstance = null;
    }

    public void initOrder(int N){
        this.MN = N;
        order = new LinkedList<>();
        for(int i = 0; i < N; i++){
            for(int j = 0; j < N ; j++){
                if(i==j) continue;
                order.add(new ImmutablePair<>(i,j));
            }
        }
    }

    public List<Pair<Integer, Integer>> getOrder(){
        return order;
    }

    private void allocateEdge(int v, int u, Set<Pair<Integer, Integer>> edges) throws EdgeOccupiedException {
        Pair<Integer, Integer> e = createPair(v,u);
        if(edges.contains(e)){
            throw new EdgeOccupiedException();
        }
        edges.add(e);
    }

    private boolean hasEdge(Pair<Integer, Integer> e, Set<Pair<Integer, Integer>> edges){
        return edges.contains(e);
    }

    private Pair<Integer, Integer> createPair(int v, int u){
        return new ImmutablePair<>(v,u);
    }

    private RoutingRule createRule(int s, int i, int t, Set<Pair<Integer, Integer>> edges) throws InvalidRuleException {
        try{
            if(i==-1){
                allocateEdge(s,t, edges);
            }else{
                allocateEdges(s,i,t,edges);
            }
        }catch (EdgeOccupiedException e){
            throw  new InvalidRuleException();
        }
        return new RoutingRule(s,i,t);
    }

    private void allocateEdges(int s, int i, int t, Set<Pair<Integer, Integer>> edges) throws EdgeOccupiedException {
        Pair<Integer, Integer> e1 = createPair(s,i);
        Pair<Integer, Integer> e2 = createPair(i,t);
        if(edges.contains(e1) || edges.contains(e2)){
            throw new EdgeOccupiedException();
        }

        edges.add(e1);
        edges.add(e2);
    }

    public List<RoutingRule> getRules(HashMap<Pair<Integer, Integer>, Demand.AggregatedDemand> demands){
        Set<Pair<Integer, Integer>> edges = new HashSet<>();
        LinkedList<RoutingRule> rules = new LinkedList<>();
        boolean satisfiedOne;
        do{
            satisfiedOne = false;
            for(Demand.AggregatedDemand demand: demands.values()){

                if(demand.isRouteAllocated()) continue;

                int s = demand.MNSource;
                int t = demand.MNDest;
                if(s==t) continue;

                for(int i = -1; i < MN; i++) {
                    if(i==s || i==t) continue;

                    try{
                        rules.add(createRule(s,i,t,edges));
                        satisfiedOne = true;
                        demand.onRouteAllocated();
                        break;
                    } catch (InvalidRuleException e) {

                    }
                }
            }
        }while(satisfiedOne);

        /**
         *
         */
//        for(int i = 0; i< MN; i++){
//            for(int j= 0; j< MN; j++){
//                if(j==i) continue;
//                try {
//
//                    rules.add(createRule(i,-1,j,edges));
//                } catch (InvalidRuleException e) {
//
//                }
//            }
//        }

        return rules;
    }
//
//    public List<RoutingRule> getRulesFromAggregation(HashMap<Pair<Integer, Integer>, AggregatedDemand> demands){
//        Set<Pair<Integer, Integer>> edges = new HashSet<>();
//        LinkedList<RoutingRule> rules = new LinkedList<>();
//        for(Pair<Integer, Integer> pair: order){
//            AggregatedDemand demand = demands.getOrDefault(pair, new AggregatedDemand(pair.getLeft(),pair.getRight()));
//
//            if(!demand.hasDemand()) continue;
//
//            Pair<Integer, Integer> direct = new ImmutablePair<>(demand.sourceToR, demand.destToR);
//
//
//            if(!edges.contains(direct)){
//                rules.add(new RoutingRule(demand.sourceToR, demand.destToR));
//                edges.add(direct);
//                demand.onPathAllocated();
//            }
//
//            for(int i = 0; i < MN; i++){
//
//                if(!demand.hasDemand()) break;
//
//                if(i == demand.sourceToR || i == demand.destToR) continue;
//                Pair<Integer, Integer> firstHop = new ImmutablePair<>(demand.sourceToR, i);
//                Pair<Integer, Integer> secondHop = new ImmutablePair<>(i, demand.destToR);
//                if(!edges.contains(firstHop) && !edges.contains(secondHop)){
//                    rules.add(new RoutingRule(demand.sourceToR,i, demand.destToR));
//                    edges.add(firstHop);
//                    edges.add(secondHop);
//                    demand.onPathAllocated();
//
//                }
//            }
//        }
//        return rules;
//    }

    public static void init(int N){
        if(sInstance!=null){
            throw new IllegalStateException("Algorithm already initialized");
        }
        sInstance = new RoutingAlg(N);
    }

    public static RoutingAlg getInstance(){
        if(sInstance==null){
            throw new IllegalStateException("Algorithm not initialized");
        }
        return  sInstance;
    }

    public void shiftOrder(){
        this.order.addLast(this.order.pop());
    }

    public static class RoutingRule{
        public final int MNSource;
        public final int MNDest;
        public final int intermediate;
        HashMap<Pair<Integer,Integer>, Long> MNutilization;
        HashMap<Integer, Long> deviceUtilization;

        private RoutingRule(int source, int intermediate, int dest){
            this.MNSource = source;
            this.MNDest = dest;
            this.intermediate = intermediate;
            this.MNutilization = new HashMap<>();
            this.deviceUtilization = new HashMap<>();
        }

        private RoutingRule(int source, int dest){
            this(source,-1,dest);
        }

        public List<Pair<Integer, Integer>> getPath(){
            List<Pair<Integer, Integer>> path = new ArrayList<>();
            if(intermediate==-1){
                path.add(new ImmutablePair<>(MNSource, MNDest));
                return path;
            }
            path.add(new ImmutablePair<>(MNSource,intermediate));
            path.add((new ImmutablePair<>(intermediate, MNDest)));
            return  path;
        }

        @Override
        public String toString(){
            String suffix =  " Devices Utilization " + deviceUtilization + " MN Utilization " + MNutilization + "\n";
            if(intermediate==-1) return MNSource + " -> " + MNDest + suffix;
            return  MNSource + " -> " + intermediate + " -> " + MNDest + suffix;
        }

        public boolean hasIntermidiate() {
            return intermediate!=-1;
        }

        public int getNextMN(int mnid) {
            if(mnid==MNSource){
                return hasIntermidiate() ? intermediate : MNDest;
            }

            assert mnid==intermediate;

            return MNDest;
        }

        public void onUtilization(int networkDeviceId, int sourceMNId, int desinationMNId) {
            long deviceUtil = this.deviceUtilization.getOrDefault(networkDeviceId, 0l);
            deviceUtil++;
            this.deviceUtilization.put(networkDeviceId, deviceUtil);

            Pair p = new ImmutablePair(sourceMNId,desinationMNId);
            long MNUtil = this.MNutilization.getOrDefault(p, 0l);
            MNUtil++;
            this.MNutilization.put(p, MNUtil);
        }


    }

    private class EdgeOccupiedException extends Exception{

    }

    private class InvalidRuleException extends Exception{

    }
}
