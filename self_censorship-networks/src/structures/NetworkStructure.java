package structures;

import ec.util.MersenneTwisterFast;
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
		//System.out.println(node + "  ");
		Object from = node;
		Bag bag = new Bag();
		Bag Edgebag = getEdges(from, bag);
		Bag Nodes = new Bag();
		for(int i=0; i<Edgebag.numObjs;i++) {
			Edge e = (Edge)Edgebag.objs[i];
			Object neighbor =  e.getOtherNode(from);
			//System.out.println("neighbor " + neighbor);
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
	
	
	/*
	 * @author Aviva (05/06/2022) - utility to modify a given portion of the network according to a given algorithm
	 */
	public void makeNet(Environment env, Bag agents, String nettype) throws InvalidNetworkTypeException {
		// if networkType is meanK, make a random network, pref is preferential, lPref is linear preferential
		if(nettype.contentEquals("random") || nettype.contentEquals("meanK")) {
			randomNetworkMeanK(env, agents, env.meanK, null);//random network
		} else if(nettype.contentEquals("pref")) {
			preferentialNetwork(env, agents, 1.2, null); //preferential attachment network
		} else if(nettype.contentEquals("lPref")) {
			// linear preferential attachment network
			preferentialNetworkLinear(env, agents, null);
		}else {
			// if no valid type has been given, throw and error that can be dealt with above
			throw new InvalidNetworkTypeException("Invalid network type");
		}
	}
	
	/*
	 * @author Aviva (05/17/2022) - utility to divide the network into components of a given size using a given algorithm
	 * @throws - passes along invalid network type exception from makeNet 
	 */
	public void splitNetwork(Environment env, int[] compsizes, String nettype) throws InvalidNetworkTypeException {
		// copy a Bag of all the nodes in the network so nodes can be removed as they're added to components
		Bag nodes = new Bag(allNodes);
		for(int i = 0; i < compsizes.length; i++) {
			// bag to hold this component
			Bag comp = new Bag();
			// move a random set of nodes from the bag of nodes
			moveRandItems(env.random, nodes, new Bag[] {comp}, compsizes[i]);
			// make a network of the designated type using the selected nodes
			makeNet(env, comp, nettype);
		}
	}
	
	
	/*
	 * @author Aviva (05/06/2022) - utility to divide the network into components of a given size with the given number of shared nodes 
	 * @throws - passes along invalid network type exception from makeNet
	 */
	public void shareSplit(Environment env, int comp1size, int comp2size, int sharednodes, String nettype) throws InvalidNetworkTypeException {
		// bags to hold the two components
		Bag comp1 = new Bag();
		Bag comp2 = new Bag();
		// copy a list of all the nodes in the network so that I can remove nodes from it as I go
		Bag nodes = new Bag(allNodes);
		// first add the shared nodes to both
		moveRandItems(env.random, nodes, new Bag[] {comp1, comp2}, sharednodes);
		// then finish constructing component one (of the given size)
		moveRandItems(env.random, nodes, new Bag[] {comp1}, comp1size-sharednodes);
		makeNet(env, comp1, nettype);
		// and component two (that contains the rest of the nodes)
		moveRandItems(env.random, nodes, new Bag[] {comp2}, comp2size-sharednodes);
		makeNet(env, comp2, nettype);
	}
	
	
	/*
	 * @author Aviva - simple utility for moving some number of random items from one Bag to other(s)
	 */
	public void moveRandItems(MersenneTwisterFast rnd, Bag from, Bag[] to, int objs) {
		for(int i = 0; i < objs; i++) {
			// if the bag to remove from is empty, just end the loop
			if(from.isEmpty()) {
				break;
			}
			// otherwise, get a random object from the bag and add it to the other Bag(s)
			Object o = from.remove(rnd.nextInt(from.numObjs));
			for(int b = 0; b < to.length; b++) {
				to[b].add(o);
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
	
	/*
	 * @author Aviva - invalid network type exception so that it can be caught at multiple levels
	 */
	class InvalidNetworkTypeException extends Exception {
		
		public InvalidNetworkTypeException() {
			// empty constructor
		}
		
		// according to stack overflow it needs one of these too
		public InvalidNetworkTypeException(String message) {
			super(message);
		}
		
	}
	
}

