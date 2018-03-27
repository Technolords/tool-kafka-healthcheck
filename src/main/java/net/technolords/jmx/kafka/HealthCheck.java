package net.technolords.jmx.kafka;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yammer.metrics.reporting.JmxReporter;

import sun.management.ConnectorAddressLink;

public class HealthCheck {
    private final Logger LOGGER = LoggerFactory.getLogger(getClass());
    private static final String KAFKA_PROCESS = "kafka.Kafka";
    private static final String KAFKA_MBEAN_BROKERSTATE = "kafka.server:type=KafkaServer,name=BrokerState";
    private static final String EXEC_COMMAND = "jps -l";
    private static final int EXEC_THRESHOLD = 5;

    public void reportBrokerStatus() {
        try {
            int brokerStatus = this.findKafkaBrokerStatus(this.findProcessIdForKafka());
            System.out.println(brokerStatus);
        } catch (IOException | InterruptedException | MalformedObjectNameException e) {
            LOGGER.error(e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Auxiliary method to find the status of the BrokerState. The BrokerState represents the internal state
     * of the broker. A trick is used to find/connect to the Java process (by process id) in order to connect
     * to the MBean server. From there, the Mbean is searched by means of Object name. Once found, a proxy to
     * the Object is created in order to fetch the state.
     *
     * @param processId
     *  The process id associated with the Kafka executable.
     * @return
     *  The broker state associated with the Kafka broker.
     *
     * @throws IOException
     *  When no connection could be established to the running JVM.
     * @throws MalformedObjectNameException
     *  When the JMX Object reference to the BrokerState fails.
     */
    protected int findKafkaBrokerStatus(int processId) throws IOException, MalformedObjectNameException {
        String localJmxAddress = ConnectorAddressLink.importFrom(processId);
        LOGGER.debug("Found localJmxAddress: {}", localJmxAddress);
        JMXServiceURL jmxServiceURL = new JMXServiceURL(localJmxAddress);
        JMXConnector jmxConnector = JMXConnectorFactory.connect(jmxServiceURL, null);
        MBeanServerConnection mBeanServerConnection = jmxConnector.getMBeanServerConnection();
        // Find MBean
        ObjectName mbeanName = new ObjectName(KAFKA_MBEAN_BROKERSTATE);
        // Instantiate proxy to MBean
        JmxReporter.GaugeMBean gaugeMBean = JMX.newMBeanProxy(mBeanServerConnection, mbeanName, JmxReporter.GaugeMBean.class, true);
        return (int) gaugeMBean.getValue();
    }

    /**
     * Auxiliary method to execute a sub process to extract all Java processes:
     *
     *  jps -l
     *
     * Expected result:
     *  2544 org.jetbrains.idea.maven.server.RemoteMavenServer
     *  3522 kafka.Kafka
     *  3955 sun.tools.jps.Jps
     *  3892 org.jetbrains.jps.cmdline.Launcher
     *  2423 com.intellij.idea.Main
     *  3227 org.apache.zookeeper.server.quorum.QuorumPeerMain
     *
     * @return The process Id associated with the Kafka broker.
     *
     * @throws IOException
     *  When processing the InputStream associated with the sub process fails.
     * @throws InterruptedException
     *  When executing the sub process fails.
     */
    protected int findProcessIdForKafka() throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec(EXEC_COMMAND);
        boolean success = process.waitFor(EXEC_THRESHOLD, TimeUnit.SECONDS);
        if (!success) {
            throw new IllegalStateException(String.format("Query for Java processes did not finish in time ({}), aborting", EXEC_THRESHOLD));
        }
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            LOGGER.debug("Got line: {}", line);
            // Got line: 3522 kafka.Kafka
            if (line.contains(KAFKA_PROCESS)) {
                return Integer.valueOf(line.split(" ")[0]);
            }
        }
        throw new IllegalStateException("No running Java processes found (associated with Kafka)...");
    }

    /**
     * The standard java main executable.
     *
     * @param args
     *  The arguments passed to the JVM.
     */
    public static void main(String[] args) {
        HealthCheck healthCheck = new HealthCheck();
        healthCheck.reportBrokerStatus();
    }
}
