import java.io.IOException;
import java.net.*;
import java.util.LinkedList;

public class TokenRing {

    private static void removeNode(Token token, Token.Endpoint node) {
        token.getRing().remove(node);
    }

    private static boolean isReachable(String ip, int port) {
        // TODO
        return false;
    }

    private static void loop(DatagramSocket socket, String ip, int port, boolean first) {
        LinkedList<Token.Endpoint> candidates = new LinkedList<>();
        if (first) {
            candidates.add(new Token.Endpoint(ip, port));
        }
        while (true) {
            try {
                Token rc = Token.receive(socket);
                System.out.printf("Token: seq=%d, #members=%d", rc.getSequence(), rc.length());
                for (Token.Endpoint endpoint : rc.getRing()) {
                    System.out.printf(" (%s, %d)", endpoint.ip(), endpoint.port());
                }
                System.out.println();
                if (rc.length() == 1) {
                    candidates.add(rc.poll());
                    if (!first) {
                        continue;
                    }
                }
                first = false;
                for (Token.Endpoint candidate : candidates) {
                    rc.append(candidate);
                }
                candidates.clear();
                Token.Endpoint next = rc.poll();
                if (rc.length() > 1 && !isReachable(next.ip(), next.port())) {
                    System.out.printf("Node %s:%d is unreachable%n", next.ip(), next.port());
                    removeNode(rc, next);
                    next = rc.poll();
                }
                rc.append(next);
                rc.incrementSequence();
                Thread.sleep(1000);
                rc.send(socket, next);
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            String ip = socket.getLocalAddress().getHostAddress();
            socket.disconnect();
            int port = socket.getLocalPort();
            System.out.printf("UDP endpoint is (%s %d)\n", ip, port);
            if (args.length == 0) {
                loop(socket, ip, port, true);
            } else if (args.length == 2) {
                Token rc = new Token().append(ip, port);
                rc.send(socket, args[0], Integer.parseInt(args[1]));
                loop(socket, ip, port, false);
            } else {
                System.out.println("Usage: \"java TokenRing\" or \"java TokenRing <ip> <port>\"");
            }
        } catch (SocketException e) {
            System.out.println("Error creating socket: " + e.getMessage());
        } catch (UnknownHostException e) {
            System.out.println("Error while determining IP address: " + e.getMessage());
        }
    }
}