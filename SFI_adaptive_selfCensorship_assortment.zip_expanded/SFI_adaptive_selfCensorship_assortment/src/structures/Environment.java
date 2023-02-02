package structures;

//import continuous_networked_agents.Agent;
//import groups.NetworkGroup;
import model.Model;
import sim.util.Bag;

public class Environment extends Model {
	boolean gaussian = false;
	double gaussanStandardDeviation = 1.0;
	public double homophily = 0.0;
	boolean SignalAssortment = false;
	public boolean soclearn; // *Aviva* - toggles between social learning and evolutionary learning models
	Bag agentPro;
	Bag agentCon;
	public double punishHi = 0.5; //probability for government to punish high signals
	//public double signalerRatio = 0.5; //the percentage of people who signals
	public double peerCost = 0.1;// neighbor punishment cost
	public double peerBenefit = 0.2;// neighbor coordination benefit
	public double censorCost = 0.2; //probability for censorship cost
	public double highRatio = 0.6; //proportion of high signalers
	public double learningBeta = 10.0;
    //public double signalProb = 0.1; //the probability to signal
	// *Aviva* - social learning model parameters
	// how much weight is put on individual learning and social learning (and the rest (to 1) is persistence)
	public double indweight;
	public double socweight;

	public int n = 50; //the number of agents
	
	// *Aviva* - some running totals for data collection and learning
	// number of agents that are signaling pro and con on the latest step
	public double signalPro;
	public double signalCon;
	// weighted total perceived number of dissidents (for each opinion)
	public double estimateCon;
	public double estimatePro;
	// total payoff by opinion (for dividing)
	public double payoffCon;
	public double payoffPro;
	
	public double getPunishHi() {
		return punishHi;
	}

	public void setPunishHi(double punishHi) {
		this.punishHi = punishHi;
	}


	public double getPeerCost() {
		return peerCost;
	}

	public void setPeerCost(double peerCost) {
		this.peerCost = peerCost;
	}

	public double getPeerBenefit() {
		return peerBenefit;
	}

	public void setPeerBenefit(double peerBenefit) {
		this.peerBenefit = peerBenefit;
	}

	public double getCensorCost() {
		return censorCost;
	}

	public void setCensorCost(double censorCost) {
		this.censorCost = censorCost;
	}

	public double getHighRatio() {
		return highRatio;
	}

	public void setHighRatio(double highRatio) {
		this.highRatio = highRatio;
	}

	public boolean isGaussian() {
		return gaussian;
	}

	public void setGaussian(boolean gaussian) {
		this.gaussian = gaussian;
	}


	public double getGaussanStandardDeviation() {
		return gaussanStandardDeviation;
	}

	public void setGaussanStandardDeviation(double gaussanStandardDeviation) {
		this.gaussanStandardDeviation = gaussanStandardDeviation;
	}

	public int getN() {
		return n;
	}

	public void setN(int n) {
		this.n = n;
	}

	//public NetworkGroup getNetwork() {
	//	return network;
	//}

	//public void setNetwork(NetworkStructure network) {
	//		this.network = network;
	//	}


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
	@Override
	public void setClasses() {
		// the class that runs the simulation and handles most parameters
		this.subclass = Environment.class;
		// the agent class for gathering agent-level data
		this.agentclass = Agent.class;
	}

	/*
	 * Modified by Aviva (05/06/2022) - made initialProtesters a starting proportion rather than a probability
	 */
	public void makeAgents() {
		// *Aviva* Initialize list of agents for gathering data
		this.agents = new Object[n];
		this.agentPro = new Bag();
		this.agentCon = new Bag();
		for(int i=0;i<n;i++) {
			Agent a = new Agent(this);
			a.event = schedule.scheduleRepeating(a);
			//add this agent to the list for data gathering
			this.agents[i] = a;
			a.IDnum = i;
			//a.event = schedule.scheduleRepeating(1.0,a, scheduleTimeInterval); //this allows us to schedule for explicit time intervals
			//set a random value for productivity
			//a.countSignal(this, 1);
			if(random.nextBoolean(highRatio)) {
				a.ValueHi = true;
				agentCon.add(a);
				// *Aviva* - also add all this agent's values to the population counts (by opinion)
				this.payoffCon += Math.exp(a.fitness);
				this.estimateCon += a.pcHi*Math.exp(a.fitness);
			}
			else {
				a.ValueHi = false;
				agentPro.add(a);
				// *Aviva* - and add this agent's values to the population counts (by opinion)
				this.payoffPro += Math.exp(a.fitness);
				this.estimatePro += a.pcHi*Math.exp(a.fitness);
			}
			a.signalProb = random.nextDouble(true, true);
			a.step = 1;
		}
	}


	/*
	 * Makes agents and resets counts to 0
	 */
	public void start() {
		super.start();
		// *Aviva* - reset population-wide counts to 0
		this.signalCon = 0;
		this.signalPro = 0;
		this.estimateCon = 0;
		this.estimatePro = 0;
		this.payoffCon = 0;
		this.payoffPro = 0;
		makeAgents();
	}

	/*
	 * @author Aviva
	 * Sets the values of parameters that the superclass can't set automatically (e.g. arrays)
	 */
	@Override
	public void setParamVal(Class c, String pname, String pval) {
		// so far, this is just for converting a list of ints (in the form 1,2,3) into an array of splits
		if(c == this.subclass && pname.contentEquals("networkSplit")) {
			// start by splitting the provided value into an array of Strings
			String[] splits = pval.split(",");
			// this can be used to initialize the array of splits to the right length
			// this.networkSplit = new int[splits.length];
			// then I can loop through the strings, convert them to ints, and add them to the array
			//for(int i = 0; i < splits.length; i++) {
			//	this.networkSplit[i] = Integer.parseInt(splits[i]);
			//}
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
		Environment env = new Environment("learnTest.txt");
	}

}

