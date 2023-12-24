import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import lib.RegistryHandler;

public class ServerRegistry {
    private static Map<String, String> configMap;

    public static void main(String[] args) throws IOException {
        readConfig(args[0]);
        int port = Integer.parseInt(configMap.get("port"));

        String apikey = configMap.get("api-key");
        String context = configMap.get("context");
        String user = configMap.get("username");
        String pass = configMap.get("password");
        int refreshRate = Integer.parseInt(configMap.get("refresh-rate"));

        try {
            HttpHandler handler = new RegistryHandler(apikey, user, pass, context, refreshRate);
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/", handler);
            server.start();
            System.out.println("[INFO] registry stared at http://localhost:" + port + context);
        } catch (Exception e) {
            System.out.println("[ERROR] [" + port + "] port in use");
        }
    }

    public static void readConfig(String propertyFile) {
        try {
            configMap = new HashMap<>();
            File file = new File(propertyFile);
            Scanner sc = new Scanner(file);
            while (sc.hasNextLine()) {
                String[] data = sc.nextLine().split("=");
                configMap.put(data[0], data[1]);
            }
            sc.close();
        } catch (Exception e) {
            System.out.println("[ERROR] [" + propertyFile + "] config file not found");
        }
    }
}