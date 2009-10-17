package sample.demo;

import static java.awt.Color.*;
import static org.guicebox.failover.NodeState.Impl.*;

import com.google.inject.*;
import com.willhains.swingutil.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import org.guicebox.*;
import org.guicebox.failover.*;

/**
 * A simple test GUI to demonstrate the interactions between GuiceBox state and Failover node state.
 * 
 * @author willhains
 */
@SuppressWarnings("serial") public class FailoverFrame extends JFrame implements ClusterListener
{
	private final GuiceBox _guicebox;
	
	@Inject FailoverFrame(GuiceBox guicebox, Cluster cluster)
	{
		_guicebox = guicebox;
		cluster.addListener(this);
		
		final Container content = getContentPane();
		content.setLayout(new StretchLayout(0, 0, new String[][] {
			{ "instructions", "instructions" },
			{ "guiceboxstate", "start" },
			{ "nodestate", "stop" },
			{ "", "kill" } }));
		content.add(_labelInstructions, "instructions");
		content.add(_labelGuiceBoxState, "guiceboxstate");
		content.add(_labelNodeState, "nodestate");
		content.add(_buttonStart, "start");
		content.add(_buttonStop, "stop");
		content.add(_buttonKill, "kill");
		
		setBounds(100, 100, 300, 200);
		setBackground(Color.WHITE);
		pack();
		setVisible(true);
	}
	
	private final JLabel _labelInstructions = new JLabel("<html>"
		+ "Use the <b>Start</b> and <b>Stop</b> buttons to control the GuiceBox.<br/>"
		+ "Start multiple instances to explore failover clustering.</html>");
	private final JLabel _labelGuiceBoxState = new JLabel("INJECTED", SwingConstants.CENTER);
	private final JLabel _labelNodeState = new JLabel("-", SwingConstants.CENTER);
	private final JButton _buttonStart = new JButton(new AbstractAction("Start")
	{
		public void actionPerformed(ActionEvent e)
		{
			_guicebox.start();
		}
	});
	private final JButton _buttonStop = new JButton(new AbstractAction("Stop")
	{
		public void actionPerformed(ActionEvent e)
		{
			_guicebox.stop();
		}
	});
	private final JButton _buttonKill = new JButton(new AbstractAction("Kill")
	{
		public void actionPerformed(ActionEvent e)
		{
			_guicebox.kill();
		}
	});
	
	@Start void onGuiceBoxStart()
	{
		_labelGuiceBoxState.setText("STARTED");
		_labelGuiceBoxState.setOpaque(true);
	}
	
	@Stop void onGuiceBoxStop()
	{
		_labelGuiceBoxState.setText("STOPPED");
		_labelGuiceBoxState.setOpaque(false);
	}
	
	@Kill void onGuiceBoxKill()
	{
		_labelGuiceBoxState.setText("DEAD");
		_labelGuiceBoxState.setOpaque(false);
		dispose();
	}
	
	@Start(value = "Label Pulse", repeat = 42) final Runnable pulseStateLabels = new Runnable()
	{
		private final long _now = System.currentTimeMillis();
		
		public void run()
		{
			final float cycleTime = (System.currentTimeMillis() - _now) / 200F;
			final float whiteness = (float)((Math.cos(cycleTime) + 1) / 2);
			_labelGuiceBoxState.setBackground(new Color(whiteness, 1F, whiteness));
		}
	};
	
	private final Map<NodeState.Impl, Color> _failoverColours = new EnumMap<NodeState.Impl, Color>(NodeState.Impl.class)
	{
		{
			put(STANDBY, CYAN);
			put(VOLUNTEER, YELLOW);
			put(PRIMARY, GREEN);
		}
	};
	
	public void onClusterChange(String newStateName)
	{
		final NodeState.Impl newState = newStateName == null ? null : NodeState.Impl.valueOf(newStateName);
		_labelNodeState.setOpaque(_failoverColours.containsKey(newState));
		if(newState == null) _labelNodeState.setText("-");
		else
		{
			_labelNodeState.setText(newState.toString());
			_labelNodeState.setBackground(_failoverColours.get(newState));
		}
	}
}
