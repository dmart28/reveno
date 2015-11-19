# Reveno - Event-Sourced Transaction Processing Framework
Reveno is thoroughgoing lightning-fast, durable and yet simple async transaction processing JVM based framework made to fit your domain in first place. It's highly influented by things like CQRS, ES, Zero-Copy, Mechanical Symphaty.

### High performance with low latency
Able to process millions of transactions per second with mean latency measured by tens of microseconds on an average hardware, thus delivering the result with the speed of lightning.

### Transactions durability
A rich set of configurations for journaling, in-memory model snapshotting and clustered failover replication makes the system totally reliable, so you make sure no single bit of data is lost.

### Easy to code, fluent API
We kept simplicity at heart of the project so that you can concentrate only on a domain model and transactional business logic, and let Reveno do the rest dirty work for you.

Most of todays solutions are suffering from excessively complex architecture and hard maintainable infrastructure, not to mention an overall maintenance cost of them.

The purpose of Reveno is to give an easy domain-oriented development tool with simple and transparent infrastructure, with perfectly fitted components for max performance. But easy doesn't mean simplistic. Instead, we are different in intention to give you as many options as possible, so you can choose the best one for you.

### Few highlights:
* Reveno is based on JVM and written fully on Java.

* Reveno is fast - Able to process millions of transaction per second with microseconds latency.

* Reveno is domain oriented - your primary focus will be on the core domain and domain logic only.

* Reveno is an in-memory transactional event-driven framework with separated command and query sides and event sourcing intruded. See our Architecture overview

* Reveno is modular - use only components you really need to.

* Reveno is robust - we have much durability options as well as failover replication among cluster, pre-allocated volumes and much more.

* Reveno is lightweight. The core is about 300kb only.

# Installation

## Maven repository
Currently, Reveno is not part of Maven Central, by this reason we provide our own repository.

Maven
```xml
<repositories>
    <repository>
        <id>reveno</id>
        <url>http://mvn.reveno.org</url>
    </repository>
</repositories>
```

Gradle
```groovy
repositories {
    maven {
        url "http://mvn.reveno.org"
    }
}
```

The current list of available artifacts consists of:

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
        <version>1.13</version>
    </dependency>
    <dependency>
        <groupId>org.reveno</groupId>
        <artifactId>reveno-cluster</artifactId>
        <version>1.13</version>
    </dependency>
</dependencies>
```

Gradle
```groovy
dependencies {
    compile 'org.reveno:reveno-core:1.13'
    compile 'org.reveno:reveno-cluster:1.13'
}
```

# Quick start guide

For the quick start guide and other very useful documentation, go to our page [http://reveno.org](http://reveno.org/quickstart-guide/)

# Support
[Google Groups](https://groups.google.com/forum/#!forum/reveno-dev)
[Support mail](mailto:support@reveno.org)
[Issues](https://github.com/dmart28/reveno/issues)
