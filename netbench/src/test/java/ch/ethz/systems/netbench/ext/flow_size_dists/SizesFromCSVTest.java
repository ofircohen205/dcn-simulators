package ch.ethz.systems.netbench.ext.flow_size_dists;

import ch.ethz.systems.netbench.core.Simulator;
import ch.ethz.systems.netbench.ext.poissontraffic.flowsize.FromCSV;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SizesFromCSVTest {

    @Before
    public void setup(){
        Simulator.setup(0);
    }

    @Test
    public void testReadCSV(){
        MockCSVReader reader = new MockCSVReader("./private/data/csv_dists/flowSize_Facebook_Cache_intra_DC.csv");
        assert(reader.getFlowSize(0.03)==196);
        assert(reader.getFlowSize(0.01)==102);
        assert(reader.getFlowSize(0.35)==12332);
        assert(reader.getFlowSize(0.98)==1661278);
        assert(reader.getFlowSize(0.999)==1661278);
    }

    @After
    public void clear(){
        Simulator.reset();
    }

}
