package ch.ethz.systems.netbench.xpt.meta_node;

public class MockMetaNodeToken extends MetaNodeToken {
    public MockMetaNodeToken(long bytes, int source, int middleHop, int dest, long tokenTimeout) {
        super(bytes, source, middleHop, dest, tokenTimeout);
    }

    @Override
    protected MockMNController getController(){
        return MockMNController.getInstance();
    }
}
