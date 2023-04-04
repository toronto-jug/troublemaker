///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.7.1
//DEPS org.postgresql:postgresql:42.5.4

import picocli.CommandLine;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

@CommandLine.Command(
        subcommands = {
                troublemaker.Http.class,
                troublemaker.HttpWithTls.class,
                troublemaker.PickRandom.class,
                troublemaker.Listen.class,
                troublemaker.OpenFile.class,
                troublemaker.DeadLock.class
        },
        mixinStandardHelpOptions = true)
public class troublemaker {

    public static class ResponseBody {
        public final List<String> body;

        public ResponseBody(List<String> body) {
            this.body = body;
        }
    }

    public static void main(String... args) {
        System.exit(new CommandLine(troublemaker.class).execute(args));
    }

    @CommandLine.Command(name = "http")
    public static class Http implements Callable<Void> {

        private static List<ResponseBody> responses = new ArrayList<>();

        @CommandLine.Option(names = "--loop")
        boolean loop = false;

        @CommandLine.Option(names = "--leak")
        boolean leak = false;

        @CommandLine.Option(names = "--url")
        String url = "http://httpbin.org/status/401";

        @Override
        public Void call() throws Exception {
            do {
                HttpResponse<Stream<String>> response =
                        HttpClient.newHttpClient().send(
                                HttpRequest.newBuilder()
                                        .GET()
                                        .uri(URI.create(url))
                                        .build(),
                                HttpResponse.BodyHandlers.ofLines());
                if (response.statusCode() != 200) {
                    throw new IOException("Bad status");
                }
                List<String> responseBody = response.body().toList();
                responseBody.forEach(System.out::println);
                if(leak) {
                    responses.add(new ResponseBody(responseBody));
                }
            } while (loop);
            return null;
        }
    }

    @CommandLine.Command(name = "https")
    public static class HttpWithTls implements Callable<Void> {

        @CommandLine.Option(names = "--version")
        HttpClient.Version version = HttpClient.Version.HTTP_2;

        @Override
        public Void call() throws Exception {
            HttpResponse<Stream<String>> response =
                    HttpClient.newBuilder()
                            .version(version)
                            .build()
                            .send(HttpRequest.newBuilder()
                                            .GET()
                                            .uri(URI.create("https://httpbin.org/status/401"))
                                            .build(),
                                    HttpResponse.BodyHandlers.ofLines());
            if (response.statusCode() != 200) {
                throw new IOException("Bad status");
            }
            response.body().forEach(System.out::println);
            return null;
        }
    }

    @CommandLine.Command(name = "pick-random")
    public static class PickRandom implements Callable<Void> {

        @Override
        public Void call() throws Exception {
            Random rand = new SecureRandom();
            boolean cont = true;
            while (cont) {
                int i = rand.nextInt();
                System.out.println(i);
                Thread.sleep(1000);
            }
            return null;
        }
    }

    @CommandLine.Command(name = "dead-lock")
    public static class DeadLock implements Callable<Void> {

        @Override
        public Void call() throws Exception {
            Object lockA = new Object();
            Object lockB = new Object();

            Thread threadA = new Thread(() -> {
                while (true) {
                    synchronized (lockA) {
                        try {
                            Thread.sleep(1000);
                            synchronized (lockB) {
                                System.out.println("I win");
                            }
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }, "Player 1");

            System.out.println("Starting Player 1");
            threadA.start();

            Thread threadB = new Thread(() -> {
                while (true) {
                    synchronized (lockB) {
                        try {
                            Thread.sleep(1000);
                            synchronized (lockA) {
                                System.out.println("No, I win");
                            }
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }, "Player 2");

            System.out.println("Starting Player 2");
            threadB.start();

            threadA.join();
            threadB.join();

            return null;
        }
    }

    @CommandLine.Command(name = "listen")
    public static class Listen implements Callable<Void> {

        @CommandLine.Option(names = "--port")
        int port = 8080;

        @Override
        public Void call() throws Exception {
            try (ServerSocket socket = new ServerSocket(port)) {
                System.out.println("Trying to listen on a port");
                socket.accept();
            }
            return null;
        }
    }

    @CommandLine.Command(name = "open")
    public static class OpenFile implements Callable<Void> {

        @CommandLine.Option(names = "--path")
        String path = "/not/a/file";

        @Override
        public Void call() throws Exception {

            try (InputStream fis = new FileInputStream(path)) {
                System.out.println("Opened file " + path);
            } catch (Exception e) {
                System.out.println("File not found");
            }
            return null;
        }
    }
}
