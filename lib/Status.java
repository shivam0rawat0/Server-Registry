package lib;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashSet;

public class Status implements Runnable {
    private int refreshRate;

    public Status(int refreshRate){
        this.refreshRate = refreshRate;
    }
    public void run() {
        try {
            while (true) {
                Thread.sleep(refreshRate);
                System.out.println("[Status check : runing]");
                for (int i = 0; i <= RegistryHandler.index; ++i) {
                    System.out.print("[" + i + ":" + RegistryHandler.serverList[i] + "]");
                    ArrayList<String> pr = new ArrayList<>();
                    HashSet<String> ports = RegistryHandler.serverMap.get(RegistryHandler.serverList[i]);
                    for (String pt : ports) {
                        String status = getStatus(pt);
                        if (status == null) {
                            pr.add(pt);
                        }
                    }
                    for (String px : pr) {
                        ports.remove(px);
                    }
                    if (ports.isEmpty()) {
                        RegistryHandler.serverMap.remove(RegistryHandler.serverList[i]);
                        RegistryHandler.serverList[i] = RegistryHandler.serverList[RegistryHandler.index];
                        --RegistryHandler.index;
                        --i;
                    } else {
                        RegistryHandler.serverMap.put(RegistryHandler.serverList[i], ports);
                    }
                }
                System.out.println("[Status check : done]");
            }
        } catch (Exception e) {
            System.out.println("[Status loop error]");
        }
    }

    private synchronized String getStatus(String port) {
        HttpClient httpClient = HttpClient.newHttpClient();
        String apiUrl = "http://localhost:" + port + "/healthcheck";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println(response.body());
            return response.body();
        } catch (IOException | InterruptedException e) {
            System.out.println("[removed]");
        }
        return null;
    }
}
