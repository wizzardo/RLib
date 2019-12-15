package com.ss.rlib.network.test;

import static com.ss.rlib.network.NetworkFactory.*;
import com.ss.rlib.common.util.StringUtils;
import com.ss.rlib.common.util.Utils;
import com.ss.rlib.logger.api.Logger;
import com.ss.rlib.logger.api.LoggerLevel;
import com.ss.rlib.logger.api.LoggerManager;
import com.ss.rlib.network.Connection;
import com.ss.rlib.network.NetworkConfig;
import com.ss.rlib.network.ServerNetworkConfig;
import com.ss.rlib.network.impl.DefaultBufferAllocator;
import com.ss.rlib.network.packet.impl.*;
import com.ss.rlib.network.util.NetworkUtils;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.net.ssl.*;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * The tests of string based network.
 *
 * @author JavaSaBr
 */
public class StringSSLNetworkTest extends BaseNetworkTest {

    private static final Logger LOGGER = LoggerManager.getLogger(StringSSLNetworkTest.class);

    @Test
    @SneakyThrows
    void certificatesTest() {

        //System.setProperty("javax.net.debug", "all");

        var keystoreFile = StringSSLNetworkTest.class.getResourceAsStream("/ssl/rlib_test_cert.p12");
        var sslContext = NetworkUtils.createSslContext(keystoreFile, "test");
        var clientSSLContext = NetworkUtils.createAllTrustedClientSslContext();

        var serverPort = NetworkUtils.getAvailablePort(10000);

        var serverSocketFactory = sslContext.getServerSocketFactory();
        var serverSocket = serverSocketFactory.createServerSocket(serverPort);

        var clientSocketFactory = clientSSLContext.getSocketFactory();
        var clientSocket = (SSLSocket) clientSocketFactory.createSocket("localhost", serverPort);

        var clientSocketOnServer = serverSocket.accept();

        new Thread(() -> Utils.unchecked(() -> {
            var clientOutStream = new PrintWriter(clientSocket.getOutputStream());
            clientOutStream.println("Hello SSL");
            clientOutStream.flush();
        })).start();

        var serverIn = new Scanner(clientSocketOnServer.getInputStream());
        var receivedOnServer = serverIn.next() + " " + serverIn.next();

        Assertions.assertEquals("Hello SSL", receivedOnServer);
    }

    @Test
    @SneakyThrows
    void serverSSLNetworkTest() {

        //System.setProperty("javax.net.debug", "all");

        //LoggerManager.getLogger(AbstractPacketWriter.class).setEnabled(LoggerLevel.DEBUG, true);
        //LoggerManager.getLogger(AbstractSSLPacketWriter.class).setEnabled(LoggerLevel.DEBUG, true);
        //LoggerManager.getLogger(AbstractSSLPacketReader.class).setEnabled(LoggerLevel.DEBUG, true);

        var keystoreFile = StringSSLNetworkTest.class.getResourceAsStream("/ssl/rlib_test_cert.p12");
        var sslContext = NetworkUtils.createSslContext(keystoreFile, "test");

        var serverNetwork = newStringDataSSLServerNetwork(
            ServerNetworkConfig.DEFAULT_SERVER,
            new DefaultBufferAllocator(ServerNetworkConfig.DEFAULT_CLIENT),
            sslContext
        );

        var serverAddress = serverNetwork.start();

        serverNetwork.accepted()
            .flatMap(Connection::receivedEvents)
            .subscribe(event -> {
                var message = event.packet.getData();
                LOGGER.info("Received from client: " + message);
                event.connection.send(new StringWritablePacket("Echo: " + message));
            });

        var clientSslContext = NetworkUtils.createAllTrustedClientSslContext();
        var sslSocketFactory = clientSslContext.getSocketFactory();
        var sslSocket = (SSLSocket) sslSocketFactory.createSocket(serverAddress.getHostName(), serverAddress.getPort());

        var buffer = ByteBuffer.allocate(1024);
        buffer.position(2);

        new StringWritablePacket("Hello SSL").write(buffer);

        buffer.putShort(0, (short) buffer.position());
        buffer.flip();

        var out = sslSocket.getOutputStream();
        out.write(buffer.array(), 0, buffer.limit());
        out.flush();

        buffer.clear();

        var in = sslSocket.getInputStream();
        var readBytes = in.read(buffer.array());

        buffer.position(readBytes).flip();
        var packetLength = buffer.getShort();

        var response = new StringReadablePacket();
        response.read(null, buffer, packetLength - 2);

        LOGGER.info("Response: " + response.getData());

        serverNetwork.shutdown();

        //LoggerManager.getLogger(AbstractSSLPacketWriter.class).setEnabled(LoggerLevel.DEBUG, false);
        //LoggerManager.getLogger(AbstractSSLPacketReader.class).setEnabled(LoggerLevel.DEBUG, false);
        //LoggerManager.getLogger(AbstractPacketWriter.class).setEnabled(LoggerLevel.DEBUG, false);
    }

    @Test
    @SneakyThrows
    void clientSSLNetworkTest() {

        //System.setProperty("javax.net.debug", "all");

        //LoggerManager.getLogger(AbstractPacketWriter.class).setEnabled(LoggerLevel.DEBUG, true);
        //LoggerManager.getLogger(AbstractSSLPacketWriter.class).setEnabled(LoggerLevel.DEBUG, true);
        //LoggerManager.getLogger(AbstractSSLPacketReader.class).setEnabled(LoggerLevel.DEBUG, true);

        var keystoreFile = StringSSLNetworkTest.class.getResourceAsStream("/ssl/rlib_test_cert.p12");
        var sslContext = NetworkUtils.createSslContext(keystoreFile, "test");

        var serverPort = NetworkUtils.getAvailablePort(1000);

        var serverSocketFactory = sslContext.getServerSocketFactory();
        var serverSocket = serverSocketFactory.createServerSocket(serverPort);
        var counter = new CountDownLatch(1);

        var clientSslContext = NetworkUtils.createAllTrustedClientSslContext();
        var clientNetwork = newStringDataSSLClientNetwork(
            NetworkConfig.DEFAULT_CLIENT,
            new DefaultBufferAllocator(NetworkConfig.DEFAULT_CLIENT),
            clientSslContext
        );

        clientNetwork.connected(new InetSocketAddress("localhost", serverPort))
            .doOnNext(connection -> connection.send(new StringWritablePacket("Hello SSL")))
            .doOnError(Throwable::printStackTrace)
            .flatMapMany(Connection::receivedEvents)
            .subscribe(event -> {
                LOGGER.info("Received from server: " + event.packet.getData());
                counter.countDown();
            });

        var acceptedClientSocket = serverSocket.accept();

        var buffer = ByteBuffer.allocate(512);

        var clientIn = acceptedClientSocket.getInputStream();
        var readBytes = clientIn.read(buffer.array());

        buffer.position(readBytes).flip();

        var dataLength = buffer.getShort();

        var receivedPacket = new StringReadablePacket();
        receivedPacket.read(null, buffer, dataLength);

        Assertions.assertEquals("Hello SSL", receivedPacket.getData());

        LOGGER.info("Received from client: " + receivedPacket.getData());

        buffer.clear();
        buffer.position(2);

        new StringWritablePacket("Echo: Hello SSL").write(buffer);

        buffer.putShort(0, (short) buffer.position());
        buffer.flip();

        var out = acceptedClientSocket.getOutputStream();
        out.write(buffer.array(), 0, buffer.limit());
        out.flush();

        buffer.clear();

        Assertions.assertTrue(
            counter.await(1000, TimeUnit.MILLISECONDS),
            "Still wait for " + counter.getCount() + " packets..."
        );

        clientNetwork.shutdown();
        serverSocket.close();

        //LoggerManager.getLogger(AbstractSSLPacketWriter.class).setEnabled(LoggerLevel.DEBUG, false);
        //LoggerManager.getLogger(AbstractSSLPacketReader.class).setEnabled(LoggerLevel.DEBUG, false);
        //LoggerManager.getLogger(AbstractPacketWriter.class).setEnabled(LoggerLevel.DEBUG, false);
    }

    @Test
    @SneakyThrows
    void echoNetworkTest() {

        //System.setProperty("javax.net.debug", "all");

        //LoggerManager.getLogger(AbstractPacketWriter.class).setEnabled(LoggerLevel.DEBUG, true);
        LoggerManager.getLogger(AbstractSSLPacketWriter.class).setEnabled(LoggerLevel.DEBUG, true);
        LoggerManager.getLogger(AbstractSSLPacketReader.class).setEnabled(LoggerLevel.DEBUG, true);

        var keystoreFile = StringSSLNetworkTest.class.getResourceAsStream("/ssl/rlib_test_cert.p12");
        var sslContext = NetworkUtils.createSslContext(keystoreFile, "test");

        var serverNetwork = newStringDataSSLServerNetwork(
            ServerNetworkConfig.DEFAULT_SERVER,
            new DefaultBufferAllocator(ServerNetworkConfig.DEFAULT_CLIENT),
            sslContext
        );

        var serverAddress = serverNetwork.start();
        var counter = new CountDownLatch(90);

        serverNetwork.accepted()
            .flatMap(Connection::receivedEvents)
            .subscribe(event -> {
                var message = event.packet.getData();
                LOGGER.info("Received from client: " + message);
                event.connection.send(new StringWritablePacket("Echo: " + message));
            });

        var clientSslContext = NetworkUtils.createAllTrustedClientSslContext();
        var clientNetwork = newStringDataSSLClientNetwork(
            NetworkConfig.DEFAULT_CLIENT,
            new DefaultBufferAllocator(NetworkConfig.DEFAULT_CLIENT),
            clientSslContext
        );

        clientNetwork.connected(serverAddress)
            .doOnNext(connection -> IntStream.range(0, 1)
                .forEach(length -> connection.send(new StringWritablePacket(StringUtils.generate(length)))))
            .doOnError(Throwable::printStackTrace)
            .flatMapMany(Connection::receivedEvents)
            .subscribe(event -> {
                LOGGER.info("Received from server: " + event.packet.getData());
                counter.countDown();
            });

        Assertions.assertTrue(
            counter.await(10000000, TimeUnit.MILLISECONDS),
            "Still wait for " + counter.getCount() + " packets..."
        );

        serverNetwork.shutdown();
        clientNetwork.shutdown();

        LoggerManager.getLogger(AbstractSSLPacketWriter.class).setEnabled(LoggerLevel.DEBUG, false);
        LoggerManager.getLogger(AbstractSSLPacketReader.class).setEnabled(LoggerLevel.DEBUG, false);
        //LoggerManager.getLogger(AbstractPacketWriter.class).setEnabled(LoggerLevel.DEBUG, false);
    }

    private static @NotNull StringWritablePacket newMessage(int minMessageLength, int maxMessageLength) {
        return new StringWritablePacket(StringUtils.generate(minMessageLength, maxMessageLength));
    }
}
