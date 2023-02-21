///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.7.1
//DEPS org.postgresql:postgresql:42.5.4

import picocli.CommandLine;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

@CommandLine.Command(
        subcommands = {
                troublemaker.HttpPlaintext.class,
                troublemaker.HttpWithTls.class,
                troublemaker.PickRandom.class,
                troublemaker.DeadLock.class
        },
        mixinStandardHelpOptions = true)
public class troublemaker {

    public static void main(String... args) {
        System.exit(new CommandLine(troublemaker.class).execute(args));
    }

    @CommandLine.Command(name = "http-plaintext")
    public static class HttpPlaintext implements Callable<Void> {

        @Override
        public Void call() throws Exception {
            HttpResponse<Stream<String>> response =
                    HttpClient.newHttpClient().send(
                            HttpRequest.newBuilder()
                                    .GET()
                                    .uri(URI.create("http://httpbin.org/status/401"))
                                    .build(),
                            HttpResponse.BodyHandlers.ofLines());
            if (response.statusCode() != 200) {
                throw new IOException("Bad status");
            }
            response.body().forEach(System.out::println);
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


}
