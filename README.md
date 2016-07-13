[![Gitter](https://badges.gitter.im/dmart28/reveno.svg)](https://gitter.im/dmart28/reveno?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=body_badge) [![Build Status](http://212.47.227.53:8080/buildStatus/icon?job=reveno-master)](http://212.47.227.53:8080/job/reveno-master/) [ ![Download](https://api.bintray.com/packages/bintray/jcenter/org.reveno%3Areveno-metrics/images/download.svg) ](https://bintray.com/bintray/jcenter/org.reveno%3Areveno-metrics/_latestVersion)

# Reveno | Event-Sourcing CQRS framework
Reveno is thoroughgoing lightning-fast, durable and yet simple async transaction processing JVM based framework made to fit your domain in first place. It's highly influenced by patterns/approaches like Event Sourcing, CQRS, Zero-Copy, DDD, Mechanical Symphaty.

### High performance with low latency
Able to process millions of transactions per second with mean latency measured by tens of microseconds on an average hardware, thus delivering the result with the speed of lightning.

### Durability
A rich set of configurations for journaling, in-memory model snapshotting and clustered failover replication makes the system totally reliable, so you make sure no single bit of data is lost.

### Easy to code, fluent API
We kept simplicity at heart of the project so that you can concentrate only on a domain model and transactional business logic, and let Reveno do the rest dirty work for you.

Most of todays solutions are suffering from excessively complex architecture and hard maintainable infrastructure, not to mention an overall maintenance cost of them.

The purpose of Reveno is to give an easy domain-oriented development tool with simple and transparent infrastructure, with perfectly fitted components for max performance. But easy doesn't mean simplistic. Instead, we are different in intention to give you as many options as possible, so you can choose the best one for you.

### Few highlights:
* Reveno is an in-memory transactional event-driven framework with CQRS and Event Sourcing intruded. See our Architecture overview
* Reveno is based on JVM and written fully in Java.
* Reveno is fast - Able to process millions of transaction per second with microseconds latency.
* Reveno is domain oriented - your primary focus will be on the core domain and domain logic only.
* Reveno is modular - use only components you really need to.
* Reveno is GC-friendly - despite it is general purpose framework, we minize the costs as much as possible.
* Reveno is robust - we have much durability options as well as failover replication among cluster, pre-allocated volumes and much more.
* Reveno is lightweight. The core is about 300kb only.

# Installation

## Maven repository
The current list of available artifacts in Maven consists of:

* reveno-core – includes all Reveno core packages, responsible for engine initialization, transaction processing, etc.
* reveno-metrics – includes packages, responsible for gathering metrics from working engine, and sending them to Graphite, Slf4j, etc.
* reveno-cluster – makes it possible to run Reveno in cluster with Master-Slave architecture, thus providing decent failover ability.

## Importing last version

Maven
```xml
<dependencies>
    <dependency>
        <groupId>org.reveno</groupId>
        <artifactId>reveno-core</artifactId>
        <version>1.23</version>
    </dependency>
    <dependency>
        <groupId>org.reveno</groupId>
        <artifactId>reveno-cluster</artifactId>
        <version>1.23</version>
    </dependency>
</dependencies>
```

Gradle
```groovy
dependencies {
    compile 'org.reveno:reveno-core:1.23'
    compile 'org.reveno:reveno-cluster:1.23'
}
```

# Sample usage
This example is not a quite useful in reality, but can give you a very good look at how easily relatively complex things can be done. For more real case example, we strongly encourage you to look at our [examples](https://github.com/dmart28/reveno/tree/master/examples).

```java
Reveno reveno = new Engine("/tmp/reveno-sample");
reveno.config().mutableModel();

reveno.domain()
	.transaction("createAccount", (t,c) ->
		c.repo().store(t.id(), new Account(t.arg())))
	.uniqueIdFor(Account.class).command();

reveno.domain()
	.transaction("changeBalance", (t,c) -> 
		c.repo().get(Account.class, t.arg()).balance += t.intArg("inc"))
	.command();

reveno.startup();

long accountId = reveno.executeSync("createAccount", map("name", "John"));
reveno.executeSync("changeBalance", map("id", accountId, "inc", 10_000));

reveno.shutdown();
```

# Quick start guide

For the quick start guide and other very useful documentation, go to our page [http://reveno.org](http://reveno.org/quickstart-guide/)

# Javadoc

Our javadoc page can be found [here](http://javadoc.reveno.org)

# Support
[Google Groups](https://groups.google.com/forum/#!forum/reveno-dev) | 
[Email support](mailto:support@reveno.org) | 
[Issues](https://github.com/dmart28/reveno/issues)
