# Introduction #

GuiceBox is an open source **lifecycle injection** framework built on top of [Guice](http://code.google.com/p/google-guice) to manage the life cycle of Java applications, and provide a simple but effective high availability (HA) solution.

# Lifecycle Injection #

Java applications with more than a few classes tend to evolve fragile, often complicated initialisation and start-up dependencies. Worse, most applications _should_ be cleaning up resources as they shut down, but too often don't. GuiceBox aims to solve these issues using a modular approach that keeps your application elegant and safe.

Like dependency injection, "lifecycle injection" (cute name, huh?) means you can leave the tedium of managing application lifecycle to the framework and concentrate on writing code that does the important stuff.

Like Guice, GuiceBox uses annotations to control how it works at runtime. All you have to do is annotate your methods and Runnable fields with `@Start`, `@Stop` and `@Kill` and GuiceBox will use these hints to manage initialisation, start, pause, and shutdown of your application. Also like Guice, the benefit of using annotations for this is that your test code can completely ignore them, making unit testing much easier.

![http://guicebox.googlecode.com/svn/wiki/images/guicebox_states.png](http://guicebox.googlecode.com/svn/wiki/images/guicebox_states.png)

GuiceBox manages your application's overall state, which can be either **STARTED** or **STOPPED**. You can move back and forth between STARTED and STOPPED as many times as you like. (There are more states when you use HotFailover.)

# Failover #

Another key feature of GuiceBox is HotFailover. You can run two (or more) instances of your application in an active-passive cluster. GuiceBox will automatically detect the failure of the primary node and start up one of the back-up nodes. This is done peer-to-peer with no single point of failure.

# Properties #

GuiceBox makes it easy to bind large numbers of constants to Guice through the PropertiesModule and CommandLineModule.

# Getting Started #

If you haven't already, [go and learn about Guice](http://code.google.com/p/google-guice). Everything on this site assumes you have! :-)

The below is a quick overview of what's involved in using GuiceBox in your application. **For a step-by-step guide on getting started, see GettingStarted.**

To start using GuiceBox, first create a class with a `main` method that will launch your application. The `main` method should look something like this:
```
public static void main(String[] args)
{
    final Injector injector = Guice.createInjector(
        new CommandLineModule(args), // command-line and properties files
        new UdpFailoverModule("MyApp"), // hot failover
        ...etc );
    final GuiceBox guicebox = injector.getInstance(GuiceBox.class); // Initiates your app
    guicebox.registerJMX(); // if you want to control GuiceBox via JMX
    guicebox.start(); // starts everything bound by your modules that has @Start
}
```
`UdpFailoverModule` and `CommandLineModule` are optional modules provided by GuiceBox.

The next thing to do is add `@Start`, `@Stop` and `@Kill` annotations to your methods and `Runnable` fields. When you call `guicebox.start()`, all of the methods you have annotated with `@Start` will be called, and threads will be created and started based on your `Runnable` fields annotated with `@Start`.

When something in your application calls `guicebox.stop()` or `guicebox.kill()`, all of the threads created during the start phase will be **interrupted**, and all your methods annotated with `@Stop` will be called.

Your application may be started and stopped multiple times. To shut the application down, call `guicebox.kill()`. This will call all your methods annotated with `@Kill` and shut down GuiceBox's internal threads, causing the JVM to exit.

The `GuiceBox` class is a `@Singleton` and so can be injected into your classes. You will normally need to do this so that something other than `main` can call `guicebox.kill()`. Alternatively, you can just call `System.exit(0)` as there is a shutdown hook that will make sure GuiceBox goes through `stop()` and `kill()` before the JVM croaks.