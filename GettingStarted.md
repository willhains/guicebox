# Introduction #

The following steps are all you need to start using GuiceBox in your application.

# Step 1: Required Libraries #

Download the following libraries and add them to your project's classpath.
  * latest version of GuiceBox from [here](http://code.google.com/p/guicebox/downloads/list)
  * latest version of Guice from [here](http://code.google.com/p/google-guice/downloads/list)

# Step 2: Minimal Configuration for HotFailover #

Although using failover is optional for GuiceBox-based applications, you should set it up and test it first anyway to make sure GuiceBox is working.

HotFailover requires, at a minimum, the following properties, which need to be in a file with the `.properties` extension, located in a directory named `properties`. The `properties` directory should be in the current working directory when your application runs.
```
org.guicebox.failover.Environment=TEST
org.guicebox.failover.WellKnownAddress=127.0.0.1
org.guicebox.failover.udp.GroupAddress=239.192.169.151
```
(download a sample file [here](http://guicebox.googlecode.com/files/failover.properties))

The values of these properties are examples. You should change them to match your environment. A WellKnownAddress is any address which is available on your network. If you're not connected to a network, or not sure what to use, just leave it as it is above. A GroupAddress is a multicast group IP address. If you're not sure what to use, ask your network administrator, or just leave it as it is above.

# Step 3: Set up Logging Configuration #

By default, the Java Logging system logs only at WARN and above. If GuiceBox is working correctly, it logs at INFO and below, so to check this you need to configure the logger. Also, GuiceBox includes an alternative log formatter class which you may prefer to use over the default one which comes with Java.

To set this up, create a file named `logging.properties` in the `properties` directory, with the following contents:
```
handlers=java.util.logging.ConsoleHandler
.level=INFO
java.util.logging.ConsoleHandler.formatter=org.guicebox.BetterFormatter
```
(download a sample file [here](http://guicebox.googlecode.com/files/logging.properties))

You can set the `.level` property to `FINE` or `FINEST` to really see what's going on in all its gory detail.

# Step 4: Run the Sample #

GuiceBox includes a tiny sample application to test that you have the environment working correctly, and to demonstrate HotFailover. Run it as follows:
  * Working directory must be the one that contains the `properties` directory.
  * Classpath must include the `guicebox-X.x.jar` and `guice-X.x.jar` files.
  * Main class is `org.guicebox.sample.EmptyGuiceBox` and requires no command line arguments.

You should see log output something like the following.
```
2009-05-03 19:45:31.954 (PropertiesModule.java:177) [main] 
    .level                                                      = INFO
    java.util.logging.ConsoleHandler.formatter                  = org.guicebox.BetterFormatter
    handlers                                                    = java.util.logging.ConsoleHandler
   @org.guicebox.failover.WellKnownAddress                      = 192.168.1.100
   @org.guicebox.failover.Environment                           = TEST
   @org.guicebox.failover.udp.GroupAddress                          = 239.192.169.151

2009-05-03 19:45:32.220 (NodeState.java:29) [JavaPing: 192.168.1.100] Became STANDBY
2009-05-03 19:45:54.478 (NodeState.java:53) [Heartbeat listener] Became VOLUNTEER
2009-05-03 19:45:59.485 (NodeState.java:94) [Heartbeat listener] Became PRIMARY
```

## Step 4.1: Test Failover ##

Note that the last few lines of log will take some time to appear. This is the HotFailover mechanism making sure there are no other PRIMARY nodes on  your network, then volunteering to become the PRIMARY, waiting for objections, and then doing so.

Now go ahead and start another `EmptyGuiceBox`, without killing the previous one. It should look the same, except that it never becomes a VOLUNTEER or PRIMARY - it just stays in STANDBY mode.

Now, if you kill the first instance, the second one should finally log out the last two lines of the log, as it takes over as PRIMARY.

You can run as many instances as you like, on any machine within your local network. Only one should ever assume the role of PRIMARY at a time.

# Step 5: Start using GuiceBox in Your Application #

Create a main class with the following code:
```
public static void main(String[] args)
{
	final Injector injector = Guice.createInjector(new CommandLineModule(), new UdpFailoverModule("my.app"));
	final GuiceBox guicebox = injector.getInstance(GuiceBox.class);
	guicebox.start();
}
```
_If you don't want to use the HotFailover feature in your application, just remove the `new UdpFailoverModule("my.app")` part._

Try running this class to make sure you get similar output to the previous sample program.

Finally, it's time to start adding `@Start`, `@Stop` and `@Kill` annotations to your code. Please see the documentation for GuiceBox to find out how to do this.

# Step 6: (Optional) Additional Configuration #

If you want, you can create a custom header for your log output. Just create a file called `loghead.txt` and put it in the working directory of your application. The contents of the file will be printed at the top of your logs.

The instructions above cover the bare minimum required to get started with GuiceBox in your application. There are several other configuration options available, which have reasonable default values if you don't specify them. To find out how you can tweak GuiceBox further, see the [Wiki](http://code.google.com/p/guicebox/w/list) and [JavaDocs](http://guicebox.googlecode.com/svn/trunk/javadoc/index.html).