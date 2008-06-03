package org.codeshark.guicebox.ha;

import static org.codeshark.guicebox.Log.*;

enum NodeState
{
	/**
	 * Network connection unconfirmed.
	 */
	DISCONNECTED
	{
		@Override
		NodeState onHeartbeat(Node node, Heart heart, Heartbeat heartbeat, Runnable stopTrigger)
		{
			log.info("Became STANDBY");
			return STANDBY.onHeartbeat(node, heart, heartbeat, stopTrigger);
		}
		
		@Override
		NodeState onPeerDead(Heart heart, Runnable startTrigger)
		{
			return this;
		}
		
		@Override
		NodeState onWkaAlive()
		{
			log.info("Became STANDBY");
			return STANDBY;
		}
		
		@Override
		NodeState onWkaDead(Heart heart, Runnable stopTrigger)
		{
			return this;
		}
	},
	
	/**
	 * Network connection confirmed, waiting to become primary.
	 */
	STANDBY
	{
		@Override
		NodeState onHeartbeat(Node node, Heart heart, Heartbeat heartbeat, Runnable stopTrigger)
		{
			return this;
		}
		
		@Override
		NodeState onPeerDead(Heart heart, Runnable startTrigger)
		{
			// Volunteer to take over as primary
			heart.beat();
			log.info("Became VOLUNTEER");
			return VOLUNTEER;
		}
		
		@Override
		NodeState onWkaAlive()
		{
			return this;
		}
		
		@Override
		NodeState onWkaDead(Heart heart, Runnable stopTrigger)
		{
			log.error("Became DISCONNECTED");
			return DISCONNECTED;
		}
	},
	
	/**
	 * Volunteering to become primary.
	 */
	VOLUNTEER
	{
		@Override
		NodeState onHeartbeat(Node node, Heart heart, Heartbeat heartbeat, Runnable stopTrigger)
		{
			// If this is the inferior node, step down
			if(!node.isSuperiorTo(heartbeat.getNode()))
			{
				log.warn("Received heartbeat from SUPERIOR node:", heartbeat);
				heart.stopBeating();
				log.info("Became STANDBY");
				return STANDBY;
			}
			
			// Otherwise hold on to become primary
			return this;
		}
		
		@Override
		NodeState onPeerDead(Heart heart, Runnable startTrigger)
		{
			// Take over as primary
			log.info("Became PRIMARY");
			startTrigger.run();
			return PRIMARY;
		}
		
		@Override
		NodeState onWkaAlive()
		{
			return this;
		}
		
		@Override
		NodeState onWkaDead(Heart heart, Runnable stopTrigger)
		{
			return STANDBY.onWkaDead(heart, stopTrigger);
		}
	},
	
	/**
	 * Active as primary node.
	 */
	PRIMARY
	{
		@Override
		NodeState onHeartbeat(Node node, Heart heart, Heartbeat heartbeat, Runnable stopTrigger)
		{
			// If this is the superior node, send a heartbeat now to stop the other node
			if(node.isSuperiorTo(heartbeat.getNode()))
			{
				log.warn("Received heartbeat from INFERIOR node:", heartbeat);
				heart.beat();
				return this;
			}
			
			// Yield to the superior node
			heart.stopBeating();
			stopTrigger.run();
			log.warn("Became STANDBY");
			return STANDBY;
		}
		
		@Override
		NodeState onPeerDead(Heart heart, Runnable startTrigger)
		{
			return this;
		}
		
		@Override
		NodeState onWkaAlive()
		{
			return this;
		}
		
		@Override
		NodeState onWkaDead(Heart heart, Runnable stopTrigger)
		{
			// Stop this node
			stopTrigger.run();
			heart.stopBeating();
			log.error("Became DISCONNECTED");
			return DISCONNECTED;
		}
	};
	
	abstract NodeState onHeartbeat(Node node, Heart heart, Heartbeat heartbeat, Runnable stopTrigger);
	
	abstract NodeState onPeerDead(Heart heart, Runnable startTrigger);
	
	abstract NodeState onWkaAlive();
	
	abstract NodeState onWkaDead(Heart heart, Runnable stopTrigger);
}
