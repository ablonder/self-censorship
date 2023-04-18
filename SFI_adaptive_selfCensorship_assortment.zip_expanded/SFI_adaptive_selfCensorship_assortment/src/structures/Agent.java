package structures;

//import continuous_networked_agents.Agent;
import sim.engine.SimState;
import sim.engine.Stoppable;
import sim.field.network.Edge;
import sim.util.Bag;
import sim.engine.Steppable;


public class Agent implements Steppable {
	protected Environment state;
	protected Stoppable event;
	public boolean ValueHi; // whether con (hi) or pro (low)
	public double signalProb;// probability of signaling
	public int signal = 0; // no signal = 0; signal high = 1; signal low = -1
	public double fitness = 0;//accumulated fitness
	public double pcHi; //perceived proportion of high (con) signalers
	public double pcGovPunish;//perceived probability of government punishment
	public int step;
	public double lastfitness;
	public int IDnum;
	public double obsvarPay; // *Aviva* - observed variance in payoff for all individuals who signaled the same
	//public Color color;

	public Agent(Environment state) {
		this.state = state;
		if (state.soclearn){
			// if using the social learning algorithm, start with a uniformly distributed perceived proportion of dissidents
			this.pcHi = state.random.nextDouble();
		}
	}

	
	/*
	 * Changes the agents signal based on their probability parameter
	 * *Aviva* - split play into separate functions (01/27/2023)
	 */
	public void signal(Environment state){
		// *Aviva* - start by subtracting it's old signal from the population count
		if(this.signal == 1) {
			state.signalCon--;
		} else if(this.signal == -1) {
			state.signalPro--;
		}
		// then determine the new signal
		if (this.ValueHi) {
			if(state.random.nextBoolean(this.signalProb)) {
				this.signal = 1;
				// *Aviva* - and add it to the population con signal count
				state.signalCon++;
			} else {
				this.signal = 0;
			}}
		else {
			if(state.random.nextBoolean(this.signalProb)) {
				this.signal = -1;
				// *Aviva* - or add it to the population pro signal count
				state.signalPro++;
			} else {
				this.signal = 0;
			}
		}
		this.step += 1;
	}
	
	/*
	 * Calculates the agent's new fitness based their signal and a randomly drawn peer (with some homophily)
	 * @return - the other agent they played with
	 */
	public Agent payoff(Environment state) {
		// store the latest fitness before calculating the fitness on this step
		this.lastfitness = this.fitness;
		//step2: get cost and benefit
		//*get a random partner vary with homophily
		//Bag players = state.continuousSpace.getNeighborsExactlyWithinDistance(new Double2D(x,y), 100, true);
		//players.remove(this);
		Bag others = new Bag();
		if(state.random.nextBoolean(state.homophily)) {
			if(this.ValueHi) {
				others.addAll(state.agentCon);
			} else {
				others.addAll(state.agentPro);
			}
		} else {
			others.addAll(state.agents);
		}
		others.remove(this);
		int randomAgent = state.random.nextInt(others.numObjs); //random agent
		Agent other = (Agent)others.objs[randomAgent];
		if (this.signal == 1) {
			if(other.ValueHi) {
				this.fitness += state.peerBenefit;
			} else {
				this.fitness -= state.peerCost;
			}
		} else {
			if (this.signal == -1) {
				if(other.ValueHi) {
					this.fitness -= state.peerCost;
				} else {
					this.fitness += state.peerBenefit;
				}
			}
		}
		//step3: government censorship
		// *Aviva* - modified to .5 probability of censorship
		if (state.random.nextBoolean(state.punishHi) & this.signal == 1) {
			this.fitness -= state.censorCost;
		}
		// *Aviva* - finally remove the old fitness and add the new fitness to the population count
		// and update the weighted estimate (which is also by fitness)
		if(this.ValueHi) {
			state.exppayCon -= Math.exp(this.lastfitness);
			state.exppayCon += Math.exp(this.fitness);
			state.estimateCon -= Math.exp(this.lastfitness)*this.pcHi;
			state.estimateCon += Math.exp(this.fitness)*this.pcHi;
		} else {
			state.exppayPro -= Math.exp(this.lastfitness);
			state.exppayPro += Math.exp(this.fitness);
			state.estimatePro -= Math.exp(this.lastfitness)*this.pcHi;
			state.estimatePro += Math.exp(this.fitness)*this.pcHi;
		}
		return other;
	}
	
	/*
	 * Evolutionary learning model - adjust signal probability based on the success of like neighbors
	 */
	public void evolLearn(Environment state, Agent other) {
		//step4: learning
		if (this.step == other.step) {
			if (this.ValueHi == other.ValueHi) {
				double learningifromj = 1/(1 + Math.exp(- state.learningBeta*(other.fitness - this.fitness)));
				if (state.random.nextBoolean(learningifromj)) {
					this.signalProb = other.signalProb;
				}
			}
			else {
				if (this.ValueHi == other.ValueHi) {
					double learningifromj = 1/(1 + Math.exp(- state.learningBeta*(other.fitness - this.lastfitness)));
					if (state.random.nextBoolean(learningifromj)) {
						this.signalProb = other.signalProb;
					}
				}
			}
		}
	}
	
	
	/*
	 * @author Aviva
	 * Social learning model - estimate proportion of dissidents based on observed behavior and others' estimates
	 */
	public void socLearn(Environment state) {
		// grab population counts based on this agent's strategy (and then remove this agent's weighted estimate)
		double stratest;
		double stratfit;
		if(this.ValueHi) {
			stratest = state.estimateCon;
			stratfit = state.exppayCon;
			state.estimateCon -= this.pcHi*Math.exp(this.fitness);
		} else {
			stratest = state.estimatePro;
			stratfit = state.exppayPro;
			state.estimatePro -= this.pcHi*Math.exp(this.fitness);
		}
		// option to learn from network neighbors rather than the whole population
		if(state.socnet) {
			stratest = 0;
			stratfit = 0;
			// loop through all the neighbors who this agent learns from (with edges in to it)
			Bag neighbors = state.net.getEdgesIn(this);
			for(Object o : neighbors) {
				// grab the agent and cast it as an agent
				Agent a = (Agent) ((Edge)o).getFrom();
				// if the other agent has the same strategy as this agent, add it to the counts
				if (a.ValueHi == this.ValueHi) {
					// add the agnet's weighted estimate to the total estimate
					stratest += a.pcHi*Math.exp(a.fitness);
					// and add its exp(fitness) to the total fitness for dividing
					stratfit += Math.exp(a.fitness);
				}
			}
		}
		// social learning is based on like-minded agents' average estimate weighted by payoff (unless payoff is 0)
		double socval = 0;
		if (stratfit != 0) {
			socval = stratest/stratfit;
		}
		// individual learning is based on what proportion of signaling individuals signaled in agreement with this agent
		// can be from the whole population, this agent's neighbors on the network, or randomly selected
		double indval = 0;
		double indstrat = 0;
		double indtotal = 0;
		// also creating a bag to hold all individuals who signaled in agreement to get variance in payoff
		Bag signalers = new Bag();
		if (state.indpool == 'a') {
			// if learning from all individuals, just use the population totals
			indstrat = state.signalCon;
			indtotal = state.signalCon + state.signalPro;
		} else if(state.indpool == 'n'){
			// if learning from network neighbors, loop through them and count up the signals
			for(Object o : state.net.getEdgesIn(this)) {
				// cast each agent as an agent
				Agent a = (Agent) ((Edge)o).getFrom();
				if((this.ValueHi && a.signal == 1) || (!this.ValueHi && a.signal == -1)) {
					indstrat++;
					indtotal++;
					signalers.add(a);
				} else if(a.signal != 0) {
					indtotal++;
				}
			}
		} else if(state.indpool == 'r') {
			// if learning from random individuals, draw up to this agent's total number of in edges
			int count = state.net.getEdgesIn(this).numObjs;
			while(count > 0) {
				// grab a random agent
				Agent a = (Agent) state.agents[state.random.nextInt(state.agents.length)];
				// make sure it really exists and isn't this agent
				if(a != null && a != this) {
					// if so, decrement the number of agents left to learn from
					count--;
					// and learn from it
					if((this.ValueHi && a.signal == 1) || (!this.ValueHi && a.signal == -1)) {
						indstrat++;
						indtotal++;
						signalers.add(a);
					} else if(a.signal != 0) {
						indtotal++;
					}
				}
			}
		}
		double socweight = state.socweight;
		double perweight = state.persistweight;
		if(indtotal != 0) {
			indval = indstrat/indtotal;
		} else if (socval == 0){
			perweight = 1;
		} else {
			socweight = 1;
		}
		// I'm also going to calculate the variance in payoff of those that signaled in agreement (for possible use later)
		if(signalers.numObjs > 0) {
			// first I have to loop through and get the mean payoff
			double mean = 0;
			for(Object o : signalers) {
				mean += ((Agent) o).fitness;
			}
			// then loop through again to get the summed distance from the mean
			this.obsvarPay = 0;
			for(Object o : signalers) {
				this.obsvarPay += Math.pow(((Agent) o).fitness - mean/signalers.numObjs, 2)/signalers.numObjs;
			}
		}
		// TODO - incorporate variance (uncertainty)
		// the actual updated value is the weighted average of social and individual learning and the previous estimate
		this.pcHi = (1-perweight)*socweight*socval + (1-perweight)*(1-socweight)*indval
				+ perweight*this.pcHi;
		// and then add it to the population counts
		if(this.ValueHi) {
			state.estimateCon += this.pcHi*Math.exp(this.fitness);
		} else {
			state.estimatePro += this.pcHi*Math.exp(this.fitness);
		}
	}
	
	/*
	 * @author Aviva
	 * Adjust probability of speaking up based on the perceived proportion of dissidents (for social learning model)
	 */
	public void speakOptimal(Environment state) {
		// agents speak up when the estimated coordination benefits outweigh the costs based on their opinion
		double predpayoff;
		// TODO - add confidence parameter*variance in payoff as exponent on predicted proportion of agreeing signalers
		if(this.ValueHi) {
			// dissidents also face a censorship cost (with some probability)
			predpayoff = (state.peerBenefit-state.punishHi*state.censorCost)*this.pcHi +
					(-state.peerCost-state.punishHi*state.censorCost)*(1-this.pcHi);
		} else {
			predpayoff = state.peerBenefit*(1-this.pcHi) - state.peerCost*this.pcHi;
		}
		// for now, agents will just deterministically speak up when the expected payoff is positive
		if(predpayoff > 0) {
			this.signalProb = 1;
		} else {
			this.signalProb = 0;
		}
	}

	/*
	 * Modified by Aviva (01/27/2023) - removed movement, added play functions and toggle between learning models
	 */
	public void step(SimState state) {
		// cast the environment as a state since I'll be using it a few times
		Environment env = (Environment) state;
		// if using the social learning algorithm, start by calculating the probability of speaking up
		if(env.soclearn) {
			speakOptimal(env);
		}
		// in any case, signal according to the agent's stored probability of speaking up
		signal(env);
		// then adjust this agent's fitness accordingly and grab its partner
		Agent partner = payoff(env);
		// then learn based on the learning model for this simulation
		if(env.soclearn) {
			socLearn(env);
		} else {
			evolLearn(env, partner);
		}
	}
}
