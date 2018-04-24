import com.ibm.mq.jms.MQQueue;
import com.ibm.mq.jms.MQQueueConnectionFactory;
import com.ibm.msg.client.wmq.WMQConstants;

import javax.jms.*;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;

/**
 * Java sample client to send / receive messages to IBM MQ queue via SSL.
 */
public class QueuePublisherSSL {

    private static String HOSTNAME = "localhost";
    private static String QMGRNAME = "testmanager";
    private static String CHANNEL = "channel1";
    private static String QUEUE_NAME = "testqueue";
    private static String SSLCIPHERSUITE = "TLS_RSA_WITH_AES_256_CBC_SHA256";

    public static void main(String[] args) {

        // Disabling IBM cipher suite mapping
        System.setProperty("com.ibm.mq.cfg.useIBMCipherMappings", "False");
        // Enabling SSL debug to view the communication
        System.setProperty("javax.net.debug", "ssl:handshake");

        try {

            Class.forName("com.sun.net.ssl.internal.ssl.Provider");
            System.out.println("JSSE is installed correctly!");

            // keystore password (change appropriately )
            char[] KSPW = "password".toCharArray();

            // Loading the keystore
            KeyStore keyStore = KeyStore.getInstance("JKS");
            // change the path to keystore appropriately
            FileInputStream ins = new FileInputStream("/home/lahiru/Documents/Blog/keys/javaClientKey.jks");
            keyStore.load(ins, KSPW);
            System.out.println("Number of keys on keystore: " + Integer.toString(keyStore.size()));

            // Loading truststore
            KeyStore trustStore = KeyStore.getInstance("JKS");
            // change the path to keystore appropriately
            trustStore.load(new FileInputStream("/home/lahiru/Documents/Blog/keys/JavaClientTrust.jks"), KSPW);
            System.out.println("Number of keys on trustStore: " + Integer.toString(trustStore.size()));

            // Create a default trust and key manager
            TrustManagerFactory trustManagerFactory =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            KeyManagerFactory keyManagerFactory =
                    KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());

            // Initialise the managers
            trustManagerFactory.init(trustStore);
            keyManagerFactory.init(keyStore, KSPW);
            SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            System.out.println("SSLContext provider: " + sslContext.getProvider().toString());

            // Initialise our SSL context from the key/trust managers
            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

            // Get an SSLSocketFactory to pass to WMQ
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            // Create default MQ connection factory
            MQQueueConnectionFactory factory = new MQQueueConnectionFactory();

            // Customize the factory
            factory.setSSLSocketFactory(sslSocketFactory);
            // Using MQ-Client transport type
            factory.setTransportType(WMQConstants.WMQ_CM_CLIENT);
            factory.setQueueManager(QMGRNAME);
            factory.setHostName(HOSTNAME);
            factory.setChannel(CHANNEL);
            factory.setPort(1414);
            factory.setLocalAddress(HOSTNAME);
            // Disabling FIPS
            factory.setSSLFipsRequired(false);
            factory.setSSLCipherSuite(SSLCIPHERSUITE);

            // Creating a QueueConnection
            QueueConnection connection = factory.createQueueConnection("mqm", "mqm");
            connection.start();
            System.out.println("JMS SSL client connection started!");

            // Create a queue and a session
            Queue queue = new MQQueue(QUEUE_NAME);
            QueueSession queueSession = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

            // Create and send a TextMessage
            QueueSender queueSender = queueSession.createSender(queue);
            Message m = queueSession.createTextMessage("Hello, World!");
            queueSender.send(m);

            // Create a QueueReceiver and wait for one message to be delivered
            QueueReceiver queueReceiver = queueSession.createReceiver(queue);
            Message response = queueReceiver.receive();

            System.out.println("Received message: " + response);
            System.out.println("--------------------------------------------------------------------------");

            queueSession.close();
            connection.close();

        } catch (JMSException ex) {
            ex.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
