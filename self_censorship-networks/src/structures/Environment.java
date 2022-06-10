package structures;

import groups.NetworkGroup;
import model.Model;
import sim.engine.SimState;
import sim.field.continuous.Continuous2D;
import sim.util.Bag;
import sim.util.Double2D;
import spaces.Spaces;
import structures.NetworkStructure.InvalidNetworkTypeException;
//import structures.NetworkStructure;

public class Environment extends Model {
	boolean gaussian = false;
	double rotation = .2;
	double gaussanStandardDeviation = 1.0;
	double stepSize = 1.0;
	public NetworkStructure network;
	public boolean useNetwork = true;
	// *Aviva* switch between possible network types - "meanK" is random, "pref" is preferential, "lPref" is linear preferential
	public String networkType = "random";
	// *Aviva* number of nodes in each separate component of the network
	public int[] networkSplit;
	// *Aviva* number of nodes shared between components
	public int networkShare = 0;
	public double meanK = 1;
	// *Aviva* set to 1 so that it doesn't rely on a variable from SimSweep
	double active = 1; //This is the probability of being active on each time step
	double distance = 0.2; //distance to try to maintain
	public double initialProtesters = 0.2; // 40% agents disagree with government policy
	// *Aviva* - sensitivity defaulted to 0 for the censorship model
	public double initialSensitivity = 0; //sensitivity legvel is 0.5 
	public int n = 1000; //the number of agents 
	// *Aviva* - space for the agents to be in, since my Model class doesn't set that up
	public Continuous2D continuousSpace;
	// *Aviva* - as well as the dimension of the space
	public int gridWidth;
	public int gridHeight;
	
	// *Aviva* - censorship parameters
	// whether agents learn sensitivity or just the censorship line
	public boolean sensitivity = false;
	// probability of censorship
	public double censorshiprate = .5;
	// censorship threshold
	public double censorshipline = .8;
	// initial perceived censorship line
	public double perceivedcensor = 1;
	// learning rate
	public double censorlrate = .5;
	
	// *Aviva* some additional variables for gathering aggregate data during the simulation
	public int positive_signals;
	public int negative_signals;
	public int silent_signals;
	public int totalsensitivity;
	public int totalpercievedcensor;
	public int totalsignal;

	/*
	 * Modified by Aviva (05/12/2022) - removed arguments to match Model
	 */
	public Environment() {
		super();
	}
	
	/*
	 * @author Aviva - constructor for reading parameters from a file
	 */
	public Environment(String fname) {
		super(fname);
	}
	
	
	/*
	 * @author Aviva - necessary helper function for Model so that it can get the fields from this class and the agent class
	 */
	public void setClasses() {
		// the class that runs the simulation and handles most parameters
		this.subclass = Environment.class;
		// the agent class for gathering agent-level data
		this.agentclass = Agent.class;
	}
	

	public double getInitialProtesters() {
		return initialProtesters;
	}

	public void setInitialProtesters(double initialProtesters) {
		this.initialProtesters = initialProtesters;
	}




	public double getInitialSensitivity() {
		return initialSensitivity;
	}



	public void setInitialSensitivity(double initialSensitivity) {
		this.initialSensitivity = initialSensitivity;
	}


	public double getDistance() {
		return distance;
	}

	public void setDistance(double distance) {
		this.distance = distance;
	}

	public double getActive() {
		return active;
	}

	public void setActive(double active) {
		this.active = active;
	}

	public boolean isGaussian() {
		return gaussian;
	}

	public void setGaussian(boolean gaussian) {
		this.gaussian = gaussian;
	}

	public double getRotation() {
		return rotation;
	}

	public void setRotation(double rotation) {
		this.rotation = rotation;
	}

	public double getGaussanStandardDeviation() {
		return gaussanStandardDeviation;
	}

	public void setGaussanStandardDeviation(double gaussanStandardDeviation) {
		this.gaussanStandardDeviation = gaussanStandardDeviation;
	}

	public double getStepSize() {
		return stepSize;
	}

	public void setStepSize(double stepSize) {
		this.stepSize = stepSize;
	}

	public int getN() {
		return n;
	}

	public void setN(int n) {
		this.n = n;
	}

	public NetworkGroup getNetwork() {
		return network;
	}

	//public void setNetwork(NetworkStructure network) {
	//		this.network = network;
	//	}

	public boolean isUseNetwork() {
		return useNetwork;
	}

	public void setUseNetwork(boolean useNetwork) {
		this.useNetwork = useNetwork;
	}

	public double getMeanK() {
		return meanK;
	}

	public void setMeanK(double meanK) {
		this.meanK = meanK;
	}

	
	public String getNetworkType() {
		return networkType;
	}

	public void setNetworkType(String networkType) {
		this.networkType = networkType;
	}


	public int getNetworkShare() {
		return networkShare;
	}

	public void setNetworkShare(int networkShare) {
		this.networkShare = networkShare;
	}



	/*
	 * Modified by Aviva (05/06/2022) - made initialProtesters a starting proportion rather than a probability
	 */
	public void makeAgents() {
		// *Aviva* Initialize list of agents for gathering data
		this.agents = new Object[n];
		for(int i=0;i<n;i++) {
			double x = random.nextDouble()*gridWidth;
			double y = random.nextDouble()*gridHeight;
			Agent a = new Agent(this,x,y,stepSize);
			this.continuousSpace.setObjectLocation(a, new Double2D(x,y));
			a.event = schedule.scheduleRepeating(a);
			// *Aviva* add this agent to the list for data gathering
			this.agents[i] = a;
			//a.event = schedule.scheduleRepeating(1.0,a, scheduleTimeInterval); //this allows us to schedule for explicit time intervals
			// *Aviva* if there are less than the initial proportion of protesters, make them disagree, otherwise make them agree 
			if(i < initialProtesters*n) {
				a.belief = 0.5 + 0.5 * random.nextDouble(true, false); //have a value of [0.5,1), biased towards strategyB
			}
			else {
				a.belief = 0.5 * random.nextDouble(true, false);  //have a value of [0,0.5), biased towards strategyA
			}
			// *Aviva* only vary agents' sensitivity if they're actually going to be learning it
			a.sensitivity = initialSensitivity;
			if(this.sensitivity) {
				a.sensitivity += (random.nextDouble(true, false) - 0.5)/10;
			}
			// *Aviva* add the agent's initial sensitivity and signal to the corresponding counts
			this.totalsensitivity += a.sensitivity;
			a.countSignal(this, 1);
			// *Aviva* set the agents' perceived censorship to the initial value and add it to the count
			a.censorship = this.perceivedcensor;
			this.totalpercievedcensor += a.censorship;
		}

	}
	

	/*
	 * Modified by Aviva (05/06/2022) - moved selection of network structure into makeNet function
	 * Modified again (05/12/2022) - changed code to create the space to match Model
	 */
	public void start() {
		super.start();
		// *Aviva* initializes space directly
		this.continuousSpace = new Continuous2D(1, gridWidth, gridHeight);
		makeAgents();
		if(useNetwork) {
			network = new NetworkStructure();
			network.addMembers(this.continuousSpace.allObjects);
			// *Aviva* actually make the networks (throws an error if an invalid network type is given)
			try {
				// *Aviva* if there's no network split just add all the agents to a network of the designated type
				if(this.networkSplit == null || this.networkSplit.length == 0) {
					// *Aviva* make a network of the designated type
					network.makeNet(this, network.allNodes, this.networkType);
				} else if(this.networkShare > 0 && this.networkSplit.length == 2){
					// *Aviva* otherwise, if there are nodes shared between two components, randomly divide up the network with shared nodes
					network.shareSplit(this, this.networkSplit[0], this.networkSplit[1], this.networkShare, this.networkType);
				} else {
					// *Aviva* otherwise, just randomly divide up the nodes into a multi-component network
					network.splitNetwork(this, this.networkSplit, this.networkType);
				}
			} catch (InvalidNetworkTypeException e) {
				System.out.println("Invalid network type");
				System.exit(0);
			}
		}
	}
	
	/*
	 * @author Aviva - sets the values of parameters that the superclass can't set automatically (e.g. arrays)
	 */
	public void setParamVal(Class c, String pname, String pval) {
		// so far, this is just for converting a list of ints (in the form 1,2,3) into an array of splits
		if(c == this.subclass && pname.contentEquals("networkSplit")) {
			// start by splitting the provided value into an array of Strings
			String[] splits = pval.split(",");
			// this can be used to initialize the array of splits to the right length
			this.networkSplit = new int[splits.length];
			// then I can loop through the strings, convert them to ints, and add them to the array
			for(int i = 0; i < splits.length; i++) {
				this.networkSplit[i] = Integer.parseInt(splits[i]);
			}
		} else {
			// otherwise just do what the superclass would do
			super.setParamVal(c, pname, pval);
		}
	}
	
	
	/*
	 * @author Aviva
	 * Main method for running the simulation
	 */
	public static void main(String[] args) {
		Environment env = new Environment("test.txt");
	}

}
