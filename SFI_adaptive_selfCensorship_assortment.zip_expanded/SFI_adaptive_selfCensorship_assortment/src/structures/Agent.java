package structures;

//import continuous_networked_agents.Agent;
import sim.engine.SimState;
import sim.engine.Stoppable;
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
			state.payoffCon -= Math.exp(this.lastfitness);
			state.payoffCon += Math.exp(this.fitness);
			state.estimateCon -= Math.exp(this.lastfitness)*this.pcHi;
			state.estimateCon += Math.exp(this.fitness)*this.pcHi;
		} else {
			state.payoffPro -= Math.exp(this.lastfitness);
			state.payoffPro += Math.exp(this.fitness);
			state.estimatePro -= Math.exp(this.lastfitness)*this.pcHi;
			state.estimatePro += Math.exp(this.lastfitness)*this.pcHi;
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
		double stratsig;
		if(this.ValueHi) {
			stratest = state.estimateCon;
			stratfit = state.payoffCon;
			state.estimateCon -= this.pcHi*Math.exp(this.fitness);
		} else {
			stratest = state.estimatePro;
			stratfit = state.payoffPro;
			state.estimatePro -= this.pcHi*Math.exp(this.fitness);
		}
		// new estimate is based on the sum of like-minded agents' weighted average estimate and observed signals
		this.pcHi = state.socweight*stratest/stratfit +
				state.indweight*state.signalCon/(state.signalCon+state.signalPro) +
				(1-state.socweight-state.indweight)*this.pcHi;
		// and then add it to the counts
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
