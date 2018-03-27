# Health check for a Kafka broker

This (nano) service scans for a running Kafka broker and reports
the status. The purpose of this service is to support implementation
a health check (Bash script, Docker health check, Openshift, etc).
Note that it expects a single instance of a Kafka broker and stops
at the first found candidate.

This service does not create a topic and posts messages on it etc (as
many other health checkers do). Instead, this service uses JMX to
connect (locally) to the Kafka broker, finds a MBean on interest
and reads (and prints to stdout) the status and then terminates.

Possible status values are:
- 0: not running
- 1: starting
- 2: recovering from unclean shutdown
- 3: running
- 6: pending controlled shutdown
- 7: shutting down

See also:
- https://github.com/apache/kafka/blob/trunk/core/src/main/scala/kafka/server/BrokerStates.scala

## Usage

Example:

```
java -jar target/health-check-${VERSION}.jar
```
