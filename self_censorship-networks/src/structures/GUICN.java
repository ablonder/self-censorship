package structures;

import java.awt.Color;
import sim.display.Controller;
import sim.portrayal.network.NetworkPortrayal2D;
import sim.portrayal.network.SimpleEdgePortrayal2D;
import sim.portrayal.network.SpatialNetwork2D;
import sim.portrayal.simple.OvalPortrayal2D;
import sweep.GUIStateSweep;
import sweep.SimStateSweep;

public class GUICN extends GUIStateSweep {
	NetworkPortrayal2D edgePortrayal = new NetworkPortrayal2D();

	public GUICN(SimStateSweep state, int gridWidth, int gridHeight, Color backdrop, Color agentDefaultColor,
			boolean agentPortrayal) {
		super(state, gridWidth, gridHeight, backdrop, agentDefaultColor, agentPortrayal);
		// TODO Auto-generated constructor stub
	}

	public void setupPortrayals()
	{
		super.setupPortrayals();
		
		if(agentPortrayal){
			OvalPortrayal2D o = new OvalPortrayal2D(agentDefaultColor);  
			agentsPortrayalContnuous.setPortrayalForAll(o);  //sets all the agents to red by default
		}
		if(((Environment)state).useNetwork) {
			// tell the portrayals what to portray and how to portray them
			edgePortrayal.setField( new SpatialNetwork2D(sweepState.continuousSpace, ((Environment)state).network ) );
			
			agentsPortrayalContnuous.setField(sweepState.continuousSpace);  /** <---- Change to your extended SimState class   */
			SimpleEdgePortrayal2D p = new SimpleEdgePortrayal2D(Color.black, Color.black,Color.black);
			p.setLabelScaling(1);
			p.setShape(SimpleEdgePortrayal2D.SHAPE_THIN_LINE);
			//p.setBaseWidth(1);
			edgePortrayal.setPortrayalForAll(p);
		}
		

		// Set the nodes in the node portrayal to show a 20-pixel non-scaling 
		// circle around them only when they're being selected (the 'true').
		// the 'null' means "Assume the underlying object is its own portrayal". 


		// reschedule the displayer
	
		display.reset();
		display.setBackdrop(Color.white);
	
		// redraw the display
		display.repaint();
	}

	public void init(Controller c){
		super.init(c);  // use the predefined method to initialize the
		display.attach( edgePortrayal, "Edges" );
			
	}


	public static void main(String[] args) {
		//GUICN.initializeTimeSeriesChart( "Con-government signalers", "Time", "Number of people who signal agains goverment policy");
		//GUICN.initializeHistogramChart( "Phases", "Phases", "# Agents",5);
		GUICN.initialize(Environment.class, Experimenter.class, GUICN.class, 600, 600, Color.WHITE, Color.BLUE, false, spaces.CONTINUOUS);

	}

}
