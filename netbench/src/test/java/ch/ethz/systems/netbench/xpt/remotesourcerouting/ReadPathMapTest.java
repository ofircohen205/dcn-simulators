package ch.ethz.systems.netbench.xpt.remotesourcerouting;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class ReadPathMapTest {
    HashMap<Integer,HashMap<Integer,List<List<Integer>>>> mPathMap;
    String path;
    @Before
    public void setup() throws IOException {
        path = "/cs/labs/schapiram/inonkp/ksp/paths/xpander_n333_d8_v2/10";
        mPathMap = new HashMap<>();
        for(int i=0; i<333; i++){
            mPathMap.put(i,readMap(i));
        }
    }

    protected HashMap<Integer,List<List<Integer>>> readMap(int identifier) {
        HashMap<Integer,List<List<Integer>>> map = new HashMap<>();
        List<Integer>[] paths;
        try {
            paths = readListFromFile(path +"/" + identifier +"_obj");


        } catch (IOException e) {
            throw new RuntimeException();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException();
        }

        for(List<Integer> p : paths){
            if(p==null) continue;
            if(map.get(p.get(p.size()-1))==null){
                map.put(p.get(p.size()-1),new LinkedList<>());
            }

            map.get(p.get(p.size()-1)).add(removeCycles(p));
        }
        return map;
    }

    private List<Integer> removeCycles(List<Integer> p) {
        HashMap<Integer,Boolean> visited = new HashMap<>();
        LinkedList<Integer> ret = new LinkedList<>();
//        if(this.identifier==332 && p.get(p.size()-1)==150){
//            System.out.println(p.toString());
//        }

        for(int i = 0; i<p.size(); i++){
            if(visited.get(p.get(i))==null || !visited.get(p.get(i))){
                ret.addLast(p.get(i));
                visited.put(p.get(i),true);
            }else{
//                System.out.println("removing cycle");
                while(!ret.getLast().equals(p.get(i))){
                    ret.removeLast();

                }
            }
        }
//        if(this.identifier==332 && p.get(p.size()-1)==150){
//            System.out.println(ret);
//        }
        return ret;
    }

    protected List<Integer>[] readListFromFile(String path) throws IOException, ClassNotFoundException {
        FileInputStream f = new FileInputStream(new File(path));
        ObjectInputStream inputStream = new ObjectInputStream(f);
        List<Integer>[] paths = (List<Integer>[]) inputStream.readObject();
        return paths;
    }

    @Test
    public void checkPathsLengths(){
        int pathLenghts[] = new int[20];
        for(int i =0; i<20; i++){
            pathLenghts[i] = 0;
        }
        for(int i=0; i<333; i++){
            HashMap<Integer,List<List<Integer>>> map = mPathMap.get(i);
            for(int index : map.keySet()){
                if(index==i){
                    continue;
                }
                for(List paths: map.get(index)){
                    pathLenghts[paths.size()]++;
                }

            }

        }
        for(int i =0; i<20; i++){
            System.out.println("path of length " + i + " " + pathLenghts[i]);
        }
    }


}
