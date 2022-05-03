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

	public void makeAgents() {
		for(int i=0;i<n;i++) {
			double x = random.nextDouble()*gridWidth;
			double y = random.nextDouble()*gridHeight;
			Agent a = new Agent(this,x,y,stepSize);
			this.continuousSpace.setObjectLocation(a, new Double2D(x,y));
			a.event = schedule.scheduleRepeating(a);
			//a.event = schedule.scheduleRepeating(1.0,a, scheduleTimeInterval); //this allows us to schedule for explicit time intervals
			//TODO: change probability to proportion
			if(random.nextBoolean(initialProtesters)) {
				a.belief = 0.5 * random.nextDouble(true, false); //have a value of [0.5,1), biased towards strategyB
			}
			else {
				a.belief = 0.5 + 0.5 * random.nextDouble(true, false);  //have a value of [0,0.5), biased towards strategyA
			}
			a.sensitivity = initialSensitivity + (random.nextDouble(true, false) - 0.5)/10;
		}
		
	}

	
	
	public void start() {
		super.start();
		experimenter = (Experimenter)observer;
		spaces = Spaces.CONTINUOUS;
		this.make2DSpace(Spaces.CONTINUOUS, 1.0, gridWidth, gridHeight);
		makeAgents();
		if(useNetwork) {
			network = new NetworkStructure();
			network.addMembers(this.continuousSpace.allObject
			// TODO - create option to make 2 separate networks with n shared nodes
			//network.randomNetworkMeanK(this, network.allNodes, 2, null);//random network
			network.preferentialNetwork(this, network.allNodes,1.2, null); //preferential attachment network
			//network.preferentialNetworkLinear(this, network.allNodes, null);
		}
		if(observer != null) observer.initialize(this.continuousSpace, spaces,scheduleTimeInterval);
	}

}
