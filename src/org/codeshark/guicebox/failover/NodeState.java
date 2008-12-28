package org.codeshark.guicebox.failover;

import java.util.logging.*;
import org.codeshark.guicebox.*;

/**
 * @author willhains
 */
interface NodeState
{
	/**
	 * Network connection unconfirmed.
	 */
	NodeState DISCONNECTED = new NodeState()
	{
		@Override public NodeState onPeerAlive(Node thisNode, Heart heart, Heartbeat heartbeat, Application app)
		{
			_log.info("Became STANDBY");
			return STANDBY.onPeerAlive(thisNode, heart, heartbeat, app);
		}
		
		@Override public NodeState onPeerDead(Heart heart, Application app)
		{
			return this;
		}
		
		@Override public NodeState onWkaAlive()
		{
			_log.info("Became STANDBY");
			return STANDBY;
		}
		
		@Override public NodeState onWkaDead(Heart heart, Application app)
		{
			return this;
		}
	};
	
	/**
	 * Network connection confirmed, waiting to become primary.
	 */
	NodeState STANDBY = new NodeState()
	{
		@Override public NodeState onPeerAlive(Node thisNode, Heart heart, Heartbeat heartbeat, Application app)
		{
			return this;
		}
		
		@Override public NodeState onPeerDead(Heart heart, Application app)
		{
			// Volunteer to take over as primary
			heart.beat();
			_log.info("Became VOLUNTEER");
			return VOLUNTEER;
		}
		
		@Override public NodeState onWkaAlive()
		{
			return this;
		}
		
		@Override public NodeState onWkaDead(Heart heart, Application app)
		{
			_log.severe("Became DISCONNECTED");
			return DISCONNECTED;
		}
	};
	
	/**
	 * Volunteering to become primary.
	 */
	NodeState VOLUNTEER = new NodeState()
	{
		@Override public NodeState onPeerAlive(Node thisNode, Heart heart, Heartbeat heartbeat, Application app)
		{
			// If this is the superior node, send a heartbeat now to stop the other node
			if(thisNode.isSuperiorTo(heartbeat.getNode()))
			{
				_log.warning("Received heartbeat from INFERIOR node: " + heartbeat);
				heart.beat();
				return this;
			}
			
			// Yield to the superior node
			_log.warning("Received heartbeat from SUPERIOR node: " + heartbeat);
			heart.stopBeating();
			_log.info("Became STANDBY");
			return STANDBY;
		}
		
		@Override public NodeState onPeerDead(Heart heart, Application app)
		{
			// Take over as primary
			_log.info("Became PRIMARY");
			app.start();
			return PRIMARY;
		}
		
		@Override public NodeState onWkaAlive()
		{
			return this;
		}
		
		@Override public NodeState onWkaDead(Heart heart, Application app)
		{
			heart.stopBeating();
			return STANDBY.onWkaDead(heart, app);
		}
	};
	
	/**
	 * Active as primary node.
	 */
	NodeState PRIMARY = new NodeState()
	{
		@Override public NodeState onPeerAlive(Node thisNode, Heart heart, Heartbeat heartbeat, Application app)
		{
			// If this is the superior node, send a heartbeat now to stop the other node
			if(thisNode.isSuperiorTo(heartbeat.getNode()))
			{
				_log.warning("Received heartbeat from INFERIOR node: " + heartbeat);
				heart.beat();
				return this;
			}
			
			// Yield to the superior node
			_log.warning("Received heartbeat from SUPERIOR node: " + heartbeat);
			heart.stopBeating();
			app.stop();
			_log.warning("Became STANDBY");
			return STANDBY;
		}
		
		@Override public NodeState onPeerDead(Heart heart, Application app)
		{
			return this;
		}
		
		@Override public NodeState onWkaAlive()
		{
			return this;
		}
		
		@Override public NodeState onWkaDead(Heart heart, Application app)
		{
			// Stop this node
			app.stop();
			heart.stopBeating();
			_log.severe("Became DISCONNECTED");
			return DISCONNECTED;
		}
	};
	
	Logger _log = Logger.getLogger(NodeState.class.getName());
	
	NodeState onPeerAlive(Node thisNode, Heart heart, Heartbeat heartbeat, Application app);
	
	NodeState onPeerDead(Heart heart, Application app);
	
	NodeState onWkaAlive();
	
	NodeState onWkaDead(Heart heart, Application app);
}
