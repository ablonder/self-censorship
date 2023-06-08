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
	public boolean con; // whether con (hi) or pro (low)
	public double signalProb;// probability of signaling
	public int signal = 0; // no signal = 0; signal high = 1; signal low = -1
	public double fitness = 0;//accumulated fitness
	public double pcCon; //perceived proportion of high (con) signalers
	public double pcGovPunish;//perceived probability of government punishment
	public int step;
	public double lastfitness;
	public int IDnum;
	public double obsvarPay; // *Aviva* - observed variance in payoff for all individuals who signaled the same
	//public Color color;

	public Agent(Environment state) {
		this.state = state;
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
		if (this.con) {
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
	public Agent interact(Environment state) {
		// choose an interaction partner with a potential bias based on homophily
		Bag others = new Bag();
		if(state.random.nextBoolean(state.homophily)) {
			if(this.con) {
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
		// calculate payoff for both this agent and its partner
		payoff(state, other);
		other.payoff(state, this);
		return other;
	}
	
	/*
	 * @author Aviva
	 * Calculate payoff for agents in an interaction (and do Bayesian learning)
	 */
	public void payoff(Environment state, Agent other) {
		// store the latest fitness before calculating the new fitness
		this.lastfitness = this.fitness;
		// calculate coordination payoff based on this agent's signal and their partner's beliefs
		if (this.signal == 1) {
			if(other.con) {
				this.fitness += state.peerBenefit;
			} else {
				this.fitness -= state.peerCost;
			}
		} else if (this.signal == -1) {
			if(other.con) {
				this.fitness -= state.peerCost;
			} else {
				this.fitness += state.peerBenefit;
			}
		}
		// the partner agent can then update their perceived con based on this agent's signal
		if(state.intlearn && this.signal != 0) {
			other.pcCon = (1-state.priorweight)*(this.signal+1)/2 + state.priorweight*other.pcCon;
		}
		
		// government censorship
		int censor = 0;
		// *Aviva* - modified so censorship occurs with some probability
		if(this.signal == 1) {
			if (state.random.nextBoolean(state.punishCon)) {
				this.fitness -= state.censorCost;
				censor = 1;
			}
			// if agents learn on interaction, con agents also update their perceived censorship here
			if(state.intlearn) {
				this.pcGovPunish = (1-state.priorweight)*censor + state.priorweight*this.pcGovPunish;
				if(other.con) {
					other.pcGovPunish = (1-state.priorweight)*censor + state.priorweight*other.pcGovPunish;
				}
			}
		}
		// *Aviva* - finally remove the old fitness and add the new fitness to the population count
		// and update the weighted estimate (which is also by fitness)
		if(this.con) {
			state.exppayCon -= Math.exp(this.lastfitness);
			state.exppayCon += Math.exp(this.fitness);
			state.estimateCon -= Math.exp(this.lastfitness)*this.pcCon;
			state.estimateCon += Math.exp(this.fitness)*this.pcCon;
		} else {
			state.exppayPro -= Math.exp(this.lastfitness);
			state.exppayPro += Math.exp(this.fitness);
			state.estimatePro -= Math.exp(this.lastfitness)*this.pcCon;
			state.estimatePro += Math.exp(this.fitness)*this.pcCon;
		}
	}
	
	/*
	 * Evolutionary learning model - adjust signal probability based on the success of like neighbors
	 */
	public void evolLearn(Environment state, Agent other) {
		//step4: learning
		if (this.step == other.step) {
			if (this.con == other.con) {
				double learningifromj = 1/(1 + Math.exp(- state.learningBeta*(other.fitness - this.fitness)));
				if (state.random.nextBoolean(learningifromj)) {
					this.signalProb = other.signalProb;
				}
			}
			else {
				if (this.con == other.con) {
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
		double stratest = 0;
		double stratfit = 0;
		if(this.con) {
			stratest = state.estimateCon;
			stratfit = state.exppayCon;
			state.estimateCon -= this.pcCon*Math.exp(this.fitness);
		} else {
			stratest = state.estimatePro;
			stratfit = state.exppayPro;
			state.estimatePro -= this.pcCon*Math.exp(this.fitness);
		}
		// option to learn from network neighbors rather than the whole population
		if(state.socnet && state.socweight > 0) {
			// loop through all the neighbors who this agent learns from (with edges in to it)
			Bag neighbors = state.net.getEdgesIn(this);
			for(Object o : neighbors) {
				// grab the agent and cast it as an agent
				Agent a = (Agent) ((Edge)o).getFrom();
				// if the other agent has the same strategy as this agent, add it to the counts
				if (a.con == this.con) {
					// add the agnet's weighted estimate to the total estimate
					stratest += a.pcCon*Math.exp(a.fitness);
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
				// and learn from it
				if(a.signal == 1) {
					indstrat++;
				}
				if(a.signal != 0) {
					indtotal++;
				}
				// also grab agents that signaled the same
				if((this.con && a.signal == 1) || (!this.con && a.signal == -1)) {
					signalers.add(a);
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
					// and learn from this one
					if(a.signal == 1) {
						indstrat++;
					}
					if(a.signal != 0) {
						indtotal++;
					}
					// also grab agents that signaled the same
					if((this.con && a.signal == 1) || (!this.con && a.signal == -1)) {
						signalers.add(a);
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
		this.pcCon = (1-perweight)*socweight*socval + (1-perweight)*(1-socweight)*indval
				+ perweight*this.pcCon;
		// and then add it to the population counts
		if(this.con) {
			state.estimateCon += this.pcCon*Math.exp(this.fitness);
		} else {
			state.estimatePro += this.pcCon*Math.exp(this.fitness);
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
		if(this.con) {
			// dissidents also face a censorship cost (with some probability)
			predpayoff = state.peerBenefit*this.pcCon - state.peerCost*(1-this.pcCon)
					- this.pcGovPunish*state.censorCost;
		} else {
			predpayoff = state.peerBenefit*(1-this.pcCon) - state.peerCost*this.pcCon;
		}
		// the expected payoff is converted into a probability using a linear probability model
		// with B0 = (peerCost + censorCost)/(peerBenefit + peerCost + censorCost)
		// and B1 = 1/(peerBenefit + peerCost + censorCost)
		// TODO - turn this into a meaningfully scaled sigmoid
		this.signalProb = (predpayoff + state.peerCost + state.censorCost)/(state.peerBenefit + state.peerCost + state.censorCost);
	}

	/*
	 * Modified by Aviva (01/27/2023) - removed movement, added play functions and toggle between learning models
	 */
	public void step(SimState state) {
		// cast the environment as a state since I'll be using it a few times
		Environment env = (Environment) state;
		// if agents act optimally based on learned estimates, start by calculating the probability of speaking up
		if(!env.evol) {
			speakOptimal(env);
		}
		// in any case, signal according to the agent's stored probability of speaking up
		signal(env);
		// then adjust this agent's fitness accordingly and grab its partner
		Agent partner = interact(env);
		// then learn based on the learning model for this simulation if not updating during interaction
		if(!env.intlearn) {
			if(env.evol) {
				evolLearn(env, partner);
			} else {
				socLearn(env);
			}
		}
	}
}
