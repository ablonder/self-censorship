package structures;

import groups.Groups;
import groups.NetworkGroup;
import sim.engine.SimState;
import sim.engine.Stoppable;
import sim.field.network.Edge;
import sim.util.Bag;
import spaces.Spaces;

public class NetworkStructure extends NetworkGroup {
	int x;
	int y;
	public int size = 0;
	public Stoppable event = null;
	public static int schedule = 2;
	public Groups gDynamics = Groups.FISSION_EXTINCTION;//FISSION_FUSION,FISSION_MIGRATION, FISSION_EXTINCTION
	public Spaces spaces;
	Bag workbag = new Bag();

	//write method, variables here; different method name,otherwise overwrite NetworkGroup
	public NetworkStructure() {
		super();
	}

	public NetworkStructure(boolean directed) {
		super(directed);
	}

	/**
	 * This method get all the network members in an undirected network
	 * @param node
	 */
	public Bag UndirNeighbors(final Object node) {
		System.out.println(node + "  ");
		Object from = node;
		Bag bag = new Bag();
		Bag Edgebag = getEdges(from, bag);
		Bag Nodes = new Bag();
		for(int i=0; i<Edgebag.numObjs;i++) {
			Edge e = (Edge)Edgebag.objs[i];
			Object neighbor =  e.getOtherNode(from);
			System.out.println("neighbor " + neighbor);
			Nodes.add(neighbor);
		} 
		return Nodes;
		
	}
	
	/**
	 * This method constructs a preferential attachment network with alpha = 1
	 * @param state
	 * @param agents
	 * @param info
	 */

	public void preferentialNetworkLinear(SimState state, Bag agents, final Object info) {
		if(agents == null || agents.numObjs < 2) {
			System.out.println("Preferential network could not be constructed.");
			return;
		}
		//add the first link
		addEdge(agents.objs[0],agents.objs[1],null);
		Bag connectedNodes = this.getAllNodes();
		double edgecount = 1;
		//go through all other nodes
		for(int	i=2; i<agents.numObjs;i++) {
			//go through all the connected nodes and add ties at the probability p = k_i/sum(k)
			//question
			//System.out.println("loop" + agents.objs[i]);
			for(int j=0; j<connectedNodes.numObjs;j++) {
				//get the degree of connected node j
				//System.out.println("in network:" + connectedNodes.objs[j]);
				Bag bag = new Bag();
				Bag Edgebag = getEdges(connectedNodes.objs[j], bag);
				//System.out.println("agent"+ connectedNodes.objs[j] + " has "+ Edgebag.numObjs + " ties out of " + edgecount);
				double localDegree = Edgebag.numObjs;
				double p = localDegree/edgecount;
				//System.out.println(p);
				//add node at the probability p = count_j/sum(count)
				if(state.random.nextBoolean(p)) {
					Object to = connectedNodes.objs[j];
					Object from = agents.objs[i];
					addEdge(from, to, null);
					edgecount += 1;
				}
				
			}
		}
	}
			
	
	/**
	 * This method constructs a non-linear preferential attachment network, https://en.wikipedia.org/wiki/Barab%C3%A1si%E2%80%93Albert_model
	 * @param alpha
	 * @param state
	 * @param agents
	 * @param info
	 */

	public void preferentialNetwork(SimState state, Bag agents, Double alpha, final Object info) {
		if(agents == null || agents.numObjs < 2) {
			System.out.println("Preferential network could not be constructed.");
			return;
		}
		//add the first link
		addEdge(agents.objs[0],agents.objs[1],null);
		Bag connectedNodes = this.getAllNodes();
		//go through all other nodes
		for(int	i=2; i<agents.numObjs;i++) {
			//calculate power-sum
			double powerSum=0.0;
			for(int k=0; k<connectedNodes.numObjs;k++) {
				Bag bag = new Bag();
				Bag Edgebag = getEdges(connectedNodes.objs[k],bag);
				powerSum += Math.pow(Edgebag.numObjs, alpha);
			}
			for(int j=0; j<connectedNodes.numObjs;j++) {
				//get the degree of connected node j
				//System.out.println("in network:" + connectedNodes.objs[j]);
				Bag bag = new Bag();
				Bag Edgebag = getEdges(connectedNodes.objs[j], bag);
				//System.out.println("agent"+ connectedNodes.objs[j] + " has "+ Edgebag.numObjs + " ties out of " + edgecount);
				double localDegree = Math.pow(Edgebag.numObjs, alpha);
				double p = localDegree/powerSum;
				System.out.println(localDegree);
				System.out.println(powerSum);
				System.out.println(p);
				if(state.random.nextBoolean(p)) {
					Object to = connectedNodes.objs[j];
					Object from = agents.objs[i];
					addEdge(from, to, null);
				}
				
			}
		}
	}
			

	

//	public static void main(String[] args) {
//			NetworkStructure ng = new NetworkStructure(false);
//			Bag agents = new Bag();
//			for(int i=0;i<10;i++) {
//				agents.add(i+1);
//			}
//			SimState state = new SimState(System.currentTimeMillis());
//			ng.randomNetworkMeanK(state, agents, 3 , null);
//			System.out.print("NetworkStructure Test");
//			Bag nodes = ng.allNodes;
//			for(int i=0;i<nodes.numObjs;i++) {
//				Object node = nodes.objs[i];
//				Bag neighbors = UndirNeighbors(node);
//				System.out.println();
//			}
		
//}
}

