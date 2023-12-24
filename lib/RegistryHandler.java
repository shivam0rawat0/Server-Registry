package lib;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.Headers;

import java.nio.charset.StandardCharsets;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

public class RegistryHandler implements HttpHandler {

    public static int index;
    public static Map<String, HashSet<String>> serverMap;
    public static String[] serverList;

    private String apikey;
    private String user;
    private String pass;
    private String context;

    public RegistryHandler(String apikey, String user, String pass, String context,int refreshRate) {
        this.apikey = apikey;
        this.user = user;
        this.pass = pass;
        this.context = context;
        index = -1;
        serverMap = new HashMap<>();
        serverList = new String[65537];

        Status status = new Status(refreshRate);
        Thread t1 = new Thread(status);
        t1.start();
    }

    @Override
    public void handle(HttpExchange exchange) {
        try {
            String type = exchange.getRequestURI().toString();
            String method = exchange.getRequestMethod().toUpperCase();
            System.out.println("[" + type + "]");

            Headers RQheaders = exchange.getRequestHeaders();
            String hkey = RQheaders.getFirst("API-Key");
            String ckey = getCookies("apikey", RQheaders.get("Cookie"));

            Headers RSheaders = exchange.getResponseHeaders();
            RSheaders.add("Access-Control-Allow-Origin", "*");

            String response = "";
            boolean access = (hkey != null && hkey.equals(apikey)) ||
                    (ckey != null && ckey.equals(apikey));

            if (type.contains(context)) {
                type = type.replace(context, "");
                switch (method) {
                    case "GET":
                        switch (type) {
                            case "getList":
                                if (access) {
                                    for (int i = 0; i <= index; ++i) {
                                        Set<String> ports = serverMap.get(serverList[i]);
                                        for (String pt : ports) {
                                            response = response + "<div class=\"server\" >" +
                                                    serverList[i] + "  <div class=\"port\" >" +
                                                    pt + "</div></div>";
                                        }
                                    }
                                    response = getTemplate("app", new String[] { response }, true);
                                } else {
                                    response = getTemplate("login", null, false);
                                }
                                break;
                            default:
                                if (access && type.contains(":") && addToList(type))
                                    response = "ok";
                                else
                                    response = "nok";
                        }
                        exchange.sendResponseHeaders(200, response.length());
                        break;

                    case "POST":
                        switch (type) {
                            case "login":
                                if (!access) {
                                    Map<String, String> vars = parsePostVariables(
                                            parseRequestBody(exchange.getRequestBody()));
                                    String ruser = vars.get("user");
                                    String rpass = vars.get("pass");
                                    if (ruser.equals(user) && rpass.equals(pass)) {
                                        String cookie = "apikey=" + apikey +
                                                "; Path=/; HttpOnly=false; Secure=false; Max-Age=2592000";
                                        RSheaders.add("Set-Cookie", cookie);
                                    }
                                }
                                break;
                            default:
                                if (access && type.contains(":") && addToList(type))
                                    response = "ok";
                                else
                                    response = "nok";
                        }
                        exchange.sendResponseHeaders(200, response.length());
                        break;

                    default:
                        response = "<html>API [HTTP Verb Error]</html>";
                        exchange.sendResponseHeaders(401, response.length());
                }
            } else {
                response = getTemplate("error", null, false);
                exchange.sendResponseHeaders(400, response.length());
            }
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private synchronized boolean addToList(String serverID) {
        String[] appData = serverID.split(":");
        if (serverMap.containsKey(appData[0])) {
            HashSet<String> ports = serverMap.get(appData[0]);
            if (ports.add(appData[1]))
                serverMap.put(appData[0], ports);
            return true;
        } else {
            HashSet<String> ports = new HashSet<>();
            ports.add(appData[1]);
            serverMap.put(appData[0], ports);
            serverList[++index] = appData[0];
            return true;
        }
    }

    //////////////////////////////////////////// [Utility
    //////////////////////////////////////////// Funtions]/////////////////////////////////////////////////
    private String getCookies(String key, List<String> RQCookies) {
        if (RQCookies != null) {
            String cookies[] = RQCookies.get(0).split(";");
            for (String cookie : cookies) {
                String pair[] = cookie.split("=");
                if (pair[0].trim().equals(key)) {
                    return pair[1];
                }
            }
        }
        return null;
    }

    private String parseRequestBody(InputStream inputStream) throws Exception {
        byte[] buffer = new byte[1024];
        int bytesRead;
        StringBuilder stringBuilder = new StringBuilder();
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            stringBuilder.append(new String(buffer, 0,
                    bytesRead, StandardCharsets.UTF_8));
        }
        return stringBuilder.toString();
    }

    private Map<String, String> parsePostVariables(String requestBody) {
        Map<String, String> variables = new HashMap<>();
        String[] pairs = requestBody.split("\r\n");
        boolean key = true;
        for (int i = 1; i < pairs.length; i = i + 2) {
            if (key) {
                key = false;
                pairs[i] = pairs[i].replace("Content-Disposition: form-data; name=\"", "")
                        .replace("\"", "");
            } else {
                key = true;
                variables.put(pairs[i - 2], pairs[i]);
            }
        }
        return variables;
    }

    public static String getTemplate(String file, String[] data, boolean isSegmented) {
        String res = "";
        try {
            File efile = new File("lib/template/" + file + ".html");
            Scanner sc = new Scanner(efile);
            while (sc.hasNextLine()) {
                res = res + sc.nextLine();
            }
            sc.close();
            if (isSegmented) {
                String[] seg = res.split("<@segment>");
                res = seg[0];
                for (int i = 0; i < data.length; ++i)
                    res = res + data[i] + seg[i + 1];
            }
            return res;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "<html>Template Undefined</html>";
    }
}