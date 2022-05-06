package structures;



import groups.NetworkGroup;
import sim.engine.SimState;
import sim.util.Bag;
import sim.util.Double2D;
import spaces.Spaces;
//import structures.NetworkStructure;
import sweep.SimStateSweep;

public class Environment extends SimStateSweep {
	boolean gaussian = false;
	double rotation = .2;
	double gaussanStandardDeviation = 1.0;
	double stepSize = 1.0;
	NetworkStructure network = null;
	boolean useNetwork = true;
	// *Aviva* switch between possible network types - "meanK" is random, "pref" is preferential, "lPref" is linear preferential
	String networkType = "meanK";
	// *Aviva* number of nodes in the a separate component of the network
	int networkSplit = 0;
	// *Aviva* number of nodes shared between components
	int networkShare = 0;
	double meanK = 1;
	double active = scheduleTimeInterval; //This is the probability of being active on each time step
	double distance = 0.2; //distance to try to maintain
	public double initialProtesters = 0.2; // 40% agents disagree with government policy
	public double initialSensitivity = 0.7; //sensitivity legvel is 0.5 
	public int n = 1000; //the number of agents 
	Experimenter experimenter;

	public Environment(long seed, Class observer) {
		super(seed, observer);
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


	public int getNetworkSplit() {
		return networkSplit;
	}

	public void setNetworkSplit(int networkSplit) {
		this.networkSplit = networkSplit;
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
		for(int i=0;i<n;i++) {
			double x = random.nextDouble()*gridWidth;
			double y = random.nextDouble()*gridHeight;
			Agent a = new Agent(this,x,y,stepSize);
			this.continuousSpace.setObjectLocation(a, new Double2D(x,y));
			a.event = schedule.scheduleRepeating(a);
			//a.event = schedule.scheduleRepeating(1.0,a, scheduleTimeInterval); //this allows us to schedule for explicit time intervals
			// *Aviva* if there are less than the initial proportion of protesters, make them disagree, otherwise make them agree 
			if(i < initialProtesters*n) {
				a.belief = 0.5 * random.nextDouble(true, false); //have a value of [0.5,1), biased towards strategyB
			}
			else {
				a.belief = 0.5 + 0.5 * random.nextDouble(true, false);  //have a value of [0,0.5), biased towards strategyA
			}
			a.sensitivity = initialSensitivity + (random.nextDouble(true, false) - 0.5)/10;
		}

	}
	


	/*
	 * Modified by Aviva (05/06/2022) - moved network structuring into makeNet function
	 */
	public void start() {
		super.start();
		experimenter = (Experimenter)observer;
		spaces = Spaces.CONTINUOUS;
		this.make2DSpace(Spaces.CONTINUOUS, 1.0, gridWidth, gridHeight);
		makeAgents();
		if(useNetwork) {
			network = new NetworkStructure();
			network.addMembers(this.continuousSpace.allObjects);
			// *Aviva* actually make the networks (throws an error if an invalid network type is given)
			try {
				// *Aviva* if there's no network split (0 or total number of agents) just make a network of the designated type
				if(this.networkSplit <= 0 || this.networkSplit >= n) {
					// *Aviva* make a network of the designated type
					network.makeNet(this, network.allNodes, this.networkType);
				} else {
					// *Aviva* otherwise, randomly divide up the network into components of the designated sizes
					network.splitNetwork(this, this.networkSplit, this.networkShare, this.networkType);
				}
			} catch (RuntimeException e) {
				System.out.println("Invalid network type");
				System.exit(0);
			}
		}
		if(observer != null) observer.initialize(this.continuousSpace, spaces,scheduleTimeInterval);
	}

}
