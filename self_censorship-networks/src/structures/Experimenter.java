//package structures;
//
//import observer.Observer;
//import sim.engine.SimState;
//import sim.util.Bag;
//import sim.util.Double2D;
//import sweep.ParameterSweeper;
//import sweep.SimStateSweep;
//
//public class Experimenter extends Observer {
//	public Environment estate;
//	
//	public Experimenter(String fileName, String folderName, SimStateSweep state, ParameterSweeper sweeper,
//			String precision, String[] headers) {
//		super(fileName, folderName, state, sweeper, precision, headers);
//		estate = (Environment)state;
//	}
//	
//	/**
//	 * This method examines each agent and determines exactly how many other agents are within its distancing radius.  I then calculates
//	 * the average number of agents within agents social distancing radius.  For randomly moving agents, means will increase as the
//	 * density of agents increases.
//	 * @param state
//	 */
//	
//	public void signals(Environment state) {
//		double pro = 0;
//		double con = 0;
//		double silent = 0;
//		double avgSensitivity =  0;
//		//double internalized = 0;
//		Bag agents = state.continuousSpace.getAllObjects();
//		final double n = (double)agents.numObjs;
//		for(int i=0;i<n;i++) {
//			Agent a = (Agent)agents.objs[i];
//			avgSensitivity += a.sensitivity;
//			if(a.signal == 0) {//Is it silent?
//				silent += 1;
//				}
//			if(a.signal == 1) {//Is it silent?
//				pro += 1;
//				}
//			if(a.signal == -1) {//Is it silent?
//				con += 1;
//				}
//	  	}
//		
//		//TODO:which is the information to collect that captures UNCERTAINTY
//		data.add(pro);
//		data.add(con);
//		data.add(silent);
//		data.add(avgSensitivity/1000.0);
//		double time = (double)state.schedule.getTime();
//		this.upDateTimeChart(time, con, true, 1000);
//	}
//	
//	
//	
//	
//	public void step (SimState state) {
//		super.step(state);
//		final long step = (long)state.schedule.getTime();
//		if(step %estate.dataSamplingInterval == 0) {
//			signals(estate);
//			//get(estate);
//			}
//	}
//
//}
