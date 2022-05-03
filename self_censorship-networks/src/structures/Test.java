package structures;

import sim.engine.SimState;
import sim.util.Bag;

public class Test {

	public static void main(String[] args) {
		NetworkStructure ng = new NetworkStructure(false);
		Bag agents = new Bag();
		for(int i=0;i<20;i++) {
			agents.add(i+1);
		}
		SimState state = new SimState(System.currentTimeMillis());
		ng.preferentialNetwork(state,agents,1.2, null);
		Bag nodes = ng.allNodes;
		for(int i=0;i<nodes.numObjs;i++) {
			Object node = nodes.objs[i];
			Bag neighbors = ng.UndirNeighbors(node);
			//System.out.println(neighbors);
		}
	
}

}
