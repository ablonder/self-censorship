package structures;

import java.awt.Color;

import randomWalker.RandomWalkerContinuousAbstract;
import sim.engine.SimState;
import sim.engine.Stoppable;
import sim.util.Bag;
import sim.util.Double2D;
//import structures.NetworkStructure;


public class Agent extends RandomWalkerContinuousAbstract {
	protected Environment state;
	protected Stoppable event;
	public double belief; //initial belief on government policy range from 0 to 1
	public double sensitivity; //political sensitivity range from 0 to 1
	public double positive_peers; //number of peers that signal for the government policy
	public double negative_peers; //number of peers that signal against the government policy
	public double silent_peers;//number of peers that do not signal
	public double signal = 0; //action: signal pro-message=1; signal con message = -1; no signal = 0
	// *Aviva* store agent color for visualization
	public Color color;


	public Agent(Environment state, double x, double y, double stepSize) {
		this.x = x;
		this.y = y;
		this.stepSize = stepSize;
		this.state = state;
		double randomAngle = state.random.nextDouble() * ((state.random.nextBoolean())?PI:-PI); //360 random angle
		xdir = Math.cos(randomAngle) * stepSize;
		ydir = Math.sin(randomAngle) * stepSize;
		//assign internal values

	}

	/*
	 * Modified by Aviva (05/06/2022) - fixed calculations for determining when to signal, and checked math for learning
	 */
	public void play(Environment state){
		this.positive_peers = 0;
		this.negative_peers = 0;
		this.silent_peers = 0;
		//make decisions on whether to signal
		// *Aviva* if pro-government (belief [0,.5)), signal with probability proportional to strength of belief 
		if(this.belief < 0.5) {
			if(state.random.nextBoolean((1-2*this.belief))) {
				this.signal = 1;
			}
			else {
				this.signal = 0;
			}
		}
		// *Aviva* if dissident (belief (.5,1]), signal with probability of inverse sensitivity times strength of belief
		else {
			if(state.random.nextBoolean((1-this.sensitivity)*(2*this.belief-1))) {
				this.signal = -1;
			}
			else {
				this.signal = 0;
			}
		}
		// *Aviva* set color variable based on signal
		if(this.signal == 1) { 
			this.color = new Color((float)1,(float)0, (float)0, (float)1);//red for pro-government signal
		}
		if(this.signal == -1) {
			this.color = new Color((float)0,(float)0, (float)1, (float)1);//blue for anti-government signal
		}
		if(this.signal == 0) {
			this.color = new Color((float)0.75, (float)0.75, (float)0.75,(float)1); //grey for no signaling
		}

		//update info
		//Bag neighbors = state.continuousSpace.getNeighborsExactlyWithinDistance(new Double2D(x,y), state.distance, true);//get neighbors within reachable distance
		Bag neighbors = state.network.UndirNeighbors(this); 
		//neighbors.remove(this);//remove the agent itself (can infect itself)
		if(neighbors.numObjs == 0) {//If there are no neighbors
			return; //nothing more to do, can't get infected
		}
		for(int i=0;i< neighbors.numObjs;i++) {//go through each neighbor
			Agent a = (Agent)neighbors.objs[i];
			if(a.signal == 0) {//Is it silent?
				this.silent_peers += 1;
			}
			if(a.signal == 1) {
				this.positive_peers += 1;
			}
			if(a.signal == -1) {
				this.negative_peers += 1;
			}
		} 
		//update political sensitivity according to peer information, if negative_peers = 0, increase political sensitivity largely
		//TODO 1. -Aviva- math looks okay; 2.updating system (find some literature)
		// if no one expresses dissident opinions, increase sensitivity (up to a max of 1)
		if(this.negative_peers == 0) {
			this.sensitivity += this.sensitivity * 0.01;
			if(this .sensitivity > 1) {
				this.sensitivity = 1;
			}
		}
		// otherwise, decrease sensitivity according to proportion of neighbors that express dissident opinions
		else {
			this.sensitivity -= this.sensitivity * 0.01 * this.negative_peers/neighbors.numObjs;
			if(this.sensitivity < 0) {
				this.sensitivity = 0;
			}
		}



	}

	/*
	 * Modified by Aviva (06/05/2022) - commented out movement
	 */
	public void step(SimState state) {
//		Double2D newLocation;
//		if(state.random.nextBoolean(((Environment)state).active)) {
//			if(this.state.gaussian) {
//				randomOrientedGaussianStep(state,this.state.gaussanStandardDeviation,this.state.rotation);
//			}
//			else {
//				randomOrientedUniformStep(state, this.state.rotation);
//			}
//
//			x=  this.state.continuousSpace.stx(x+xdir);
//			y = this.state.continuousSpace.sty(y+ydir);
//			newLocation = new Double2D(x,y);
//			this.state.continuousSpace.setObjectLocation(this, newLocation);
//		}
		//play
		play((Environment) state);	//learners are not strategic
		//System.out.println(this.sensitivity);


	}
}
