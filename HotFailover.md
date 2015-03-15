# Introduction #

When you use GuiceBox lifecycle injection to manage the initialisation, start-up and shutdown of your application, you can easily drop in _active-passive clustering_ to increase uptime by running your application on multiple machines.

All you have to do is configure GuiceBox with a `FailoverModule`, and provide a couple of IP addresses as config.

# How it Works #

The active-passive cluster contains _at most_ one **primary** node, and zero or more **standby** nodes. These are instances of your application running on separate physical machines. Your application initialises in both places, but only the primary node is _started_ (see [GuiceBox states](GuiceBox.md)). If the primary node dies, a standby takes over. This mechanism is called "_hot_ failover" because each standby node's JVM is started and all your objects are initialised & ready to go.

![http://guicebox.googlecode.com/svn/wiki/images/active_passive.png](http://guicebox.googlecode.com/svn/wiki/images/active_passive.png)

The primary node broadcasts regular **heartbeats** to the network, letting other nodes know that it exists and is active as the primary. The standby nodes listen for these heartbeats. All nodes in the cluster continuously ping a **well-known address (WKA)**, to determine whether they have network connectivity.

When the primary node suffers hardware or network failure, or if your application process is killed, the standby node detects the lack of heartbeats and becomes a **volunteer**. Volunteers send out heartbeats, but wait for a short time to make sure there are no other volunteers before _starting_ and becoming the primary.

If another standby node has also become a volunteer at the same time, both will receive each other's heartbeats. Every heartbeat contains information about the node that sent it, so that in this situation the volunteers can compare their IP addresses to determine which volunteer should back down and which volunteer should become primary - the lowest IP address wins. (If both nodes are running on the same machine, they compare their process IDs.)

# What You Need #

Adding failover to your GuiceBox application is very easy.

## Step 1: GuiceBox Right ##

The intention of `@Start` and `@Stop` in GuiceBox is to separate your application's _initialisation_ code from its _start-up_ code. You shouldn't be doing any work before your `@Start` methods are called, or after your `@Stop` methods are called. You _should_ however do as much initialisation as you can in your constructors and `@Inject` methods, so that when `@Start` methods _are_ called, there is nothing left to do but start working.

Failover depends on cleanly separated initialisation and start-up code. If you are doing work before `@Start`, **your standby nodes will do it too**.

## Step 2: Wire `UdpFailoverModule` ##

Just add `UdpFailoverModule` to the `GuiceBox.init()` call in your main class:
```
public static void main(String[] args)
{
    final Injector injector = Guice.createInjector(
        new CommandLineModule(args), // command-line and properties files
        new UdpFailoverModule("MyApp"), // hot failover
        ...etc );
    final GuiceBox guicebox = injector.getInstance(GuiceBox.class); // Initialises your app
    guicebox.start(); // starts everything bound by your modules that has @Start
}
```
_Note: `"MyApp"` is the name of your application. It is used by the nodes of the same cluster to recognise one other.  This allows multiple GuiceBox applications to coexist on the same network._

## Step 3: Configuration ##

At a minimum, you need to configure the following:
  1. **`@WellKnownAddress`** - ideally, use a virtual IP, but if you don't have one on your network, just use an IP that is not part of your cluster.
  1. **`@GroupAddress`** - the multicast address for broadcasting UDP heartbeats. Talk to your network administrator if you're not sure what value to use.

These values must be bound in Guice as constants. The easiest way to do that is either PropertiesModule or CommandLineModule, with the following (sample) in `./properties/username/failover.properties`:
```
# failover.properties
org.guicebox.failover.WellKnownAddress=192.168.1.100
org.guicebox.failover.udp.GroupAddress=239.192.169.151
```

### Optional Configuration ###
There are several optional configuration options available. Here they are, with their default values:
```
# Source and Destination ports for multicast heartbeats
org.guicebox.failover.udp.SourcePort=7979
org.guicebox.failover.udp.DestinationPort=9797

# Number of router hops allowed for heartbeats
org.guicebox.failover.udp.TimeToLive=16

# Interval between heartbeats (seconds or milliseconds) and number permissible to miss
org.guicebox.failover.HeartbeatInterval=1000
org.guicebox.failover.HeartbeatTolerance=5

# Interval between pings (seconds or milliseconds) and number permissible to miss
org.guicebox.failover.PingInterval=1500
org.guicebox.failover.PingTolerance=3
```

# Node States vs GuiceBox States #

With `FailoverModule`, when you call `guicebox.start()` in your main class, GuiceBox doesn't `@Start` your application right away - instead it _joins the cluster_, and your application takes on a node state of **standby**. If your node becomes **volunteer** and is unchallenged, your `@Start` methods/threads will be invoked and the node becomes **primary**.

If you explicitly call `guicebox.stop()` or `guicebox.kill()` in your code, or you shutdown the node using [JMX](JmxIntegration.md), your `@Stop` methods will be invoked, application threads interrupted, and your node _leaves the cluster_.

The following timelines may help visualise the relationship between GuiceBox state and node state.

**Without `FailoverModule`:**
```
Main class:     init()      start()     stop()
GuiceBox state: STOPPED --> STARTED --> STOPPED
```

**With `FailoverModule`:**
```
Main class:     init()     start()                               stop()
GuiceBox state: STOPPED ---------------------------> STARTED --> STOPPED
Node state:     (none) --> STANDBY --> VOLUNTEER --> PRIMARY --> (none)
```