package structures;

import groups.NetworkGroup;
//import continuous_networked_agents.Agent;
import model.SimDataCollection;
import sim.field.network.Network;
import sim.util.Bag;

public class Environment extends SimDataCollection {
	
	public int n = 50; //the number of agents
	public double conRatio = 0.6; //proportion of con agents
	
	// payoffs
	public double punishCon = 0.5; //probability for government to punish con signals
	public double peerCost = 0.1;// neighbor punishment cost
	public double peerBenefit = 0.2;// neighbor coordination benefit
	public double censorCost = 0.2; //probability for censorship cost
	
	// learning parameters
	// who to learn from
	public boolean intlearn; // whether agents update their estimates based on interactions versus neighbors
	public char indpool; // whether to individually learn from network neighbors ('n'), random individuals ('r') or everyone ('a')
	public boolean socnet; // whether a network is used for social learning
	// evolutionary learning model
	public boolean evol; // whether agents use the evolutionary learning model or behave optimally based on estimates
    public double learningBeta = 10.0;
	// social learning model parameters
	public double persistweight;
	public double socweight;
	// bayesian updating parameters
	public double priorweight;
	public double priorCon;
	public double priorPunish;
	
	// social network parameters
	public NetworkGroup net;
	public double meank;
	public double homophily = 0.0;
	
	// *Aviva* - some running totals for data collection and learning
	Bag agentPro;
	Bag agentCon;
	// number of agents that are signaling pro and con on the latest step
	public double signalPro;
	public double signalCon;
	// weighted total perceived number of dissidents by opinion (for social learning)
	public double estimateCon;
	public double estimatePro;
	// total e^payoff by opinion (for dividing in social learning)
	public double exppayCon;
	public double exppayPro;
	

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
			//add this agent to the list of agents
			this.agents[i] = a;
			a.IDnum = i;
			// *Aviva* - set the right proportion of agents to con and the rest to pro
			if(i < n*conRatio) {
				a.con = true;
				agentCon.add(a);
				// *Aviva* - also add all this agent's values to the population counts (by opinion)
				this.exppayCon += Math.exp(a.fitness);
				this.estimateCon += a.pcCon*Math.exp(a.fitness);
			} else {
				a.con = false;
				agentPro.add(a);
				// *Aviva* - and add this agent's values to the population counts (by opinion)
				this.exppayPro += Math.exp(a.fitness);
				this.estimatePro += a.pcCon*Math.exp(a.fitness);
			}
			// *Aviva* - and finally update this individuals estimates
			a.pcCon = this.priorCon;
			a.pcGovPunish = this.priorPunish;
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
		this.exppayCon = 0;
		this.exppayPro = 0;
		makeAgents();
		// *Aviva* - if using a network for social or individual learning, create a mean k random network
		if (this.socnet || this.indpool == 'n'){
			this.net = new NetworkGroup();
			this.net.randomNetworkMeanK(this, new Bag(this.agents), this.meank, null);
		}
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
		Environment env = new Environment("testrun.txt");
	}

}

