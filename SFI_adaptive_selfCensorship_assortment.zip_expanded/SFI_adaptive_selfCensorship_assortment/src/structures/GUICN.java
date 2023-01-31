package structures;

import java.awt.Color;
import java.awt.Graphics2D;

import javax.swing.JFrame;

import sim.display.Console;
import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.engine.SimState;
import sim.portrayal.DrawInfo2D;
import sim.portrayal.continuous.ContinuousPortrayal2D;
import sim.portrayal.network.NetworkPortrayal2D;
import sim.portrayal.network.SimpleEdgePortrayal2D;
import sim.portrayal.network.SpatialNetwork2D;
import sim.portrayal.simple.CircledPortrayal2D;
import sim.portrayal.simple.OvalPortrayal2D;

public class GUICN extends GUIState {
	NetworkPortrayal2D edgePortrayal = new NetworkPortrayal2D();
	// *Aviva* portrayal to hold the agents and their locations
	public ContinuousPortrayal2D agentPortrayal = new ContinuousPortrayal2D();
	// *Aviva* other necessary fields for visualization
	public Display2D display;
	public JFrame frame;

	/*
	 * Modified by Aviva (05/13/2022) - removed arguments to match GUIState rather than GUIStateSweep
	 */
	public GUICN(SimState state) {
		super(state);
	}

	/*
	 * Modified by Aviva (05/13/2022) - set up portrayals manually since we're not using Jeff's framework
	 */
	public void setupPortrayals()	{
		// *Aviva* cast state class variable as Environment for use
		Environment env = (Environment) state;
		// *Aviva* set up space portrayal
		agentPortrayal.setField(env.continuousSpace);
		// *Aviva* set up agent portrayals
		agentPortrayal.setPortrayalForAll(new CircledPortrayal2D(new OvalPortrayal2D() {
			public void draw(Object object, Graphics2D graphics, DrawInfo2D info) {
				// cast the object as an agent
				Agent agent = (Agent) object;
				// grab the agent's color
				//paint = agent.color;
				// draw it
				super.draw(object, graphics, info);
				}}, 0, 1, Color.gray, false));
		//if(env.useNetwork) {
			// *Aviva* tell the portrayals what to portray and how to portray them
		//	edgePortrayal.setField(new SpatialNetwork2D(env.continuousSpace, env.network ));
		//	agentPortrayal.setField(new SpatialNetwork2D(env.continuousSpace, env.network));
			SimpleEdgePortrayal2D p = new SimpleEdgePortrayal2D(Color.black, Color.black,Color.black);
			p.setLabelScaling(1);
			p.setShape(SimpleEdgePortrayal2D.SHAPE_THIN_LINE);
			//p.setBaseWidth(1);
		//	edgePortrayal.setPortrayalForAll(p);
		}

		// Set the nodes in the node portrayal to show a 20-pixel non-scaling
		// circle around them only when they're being selected (the 'true').
		// the 'null' means "Assume the underlying object is its own portrayal".

		// reschedule the displayer
		//display.reset();
		//display.setBackdrop(Color.white);

		// redraw the display
		//display.repaint();

	/*
	 * Modified by Aviva (05/13/2022) - added code to manually set up the display
	 */
	public void init(Controller c){
		super.init(c);  // use the predefined method to initialize the
		// *Aviva* sets up the display
		display = new Display2D(600, 600, this);
		display.setClipping(false);

		frame = display.createFrame();
		frame.setTitle("Familiarity Display");
		c.registerFrame(frame);
		frame.setVisible(true);
		display.attach(agentPortrayal, "Agents");
		display.attach( edgePortrayal, "Edges" );

	}


	/*
	 * Modified by Aviva (05/13/2022) - mannually sets up the environment an UI
	 */
	public static void main(String[] args) {
		// *Aviva* first create a model, make sure gui is set to true in the input file or it'll just run normally
		Environment env = new Environment();
		GUICN vid = new GUICN(env);
		Console c = new Console(vid);
		c.setVisible(true);
	}

}