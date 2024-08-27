package com.server;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple web server that listens on a specific port and handles HTTP requests.
 * This server supports both GET and POST methods and can serve static files or process
 * custom routes defined via a REST-like interface.
 */
public class SimpleWebServer {
    private static final int PORT = 8080;
    private static boolean running = true;

    /**
     * Main method to start the web server. The server listens for client connections
     * on the specified port and handles each connection using a thread pool.
     *
     * @param args command line arguments (not used)
     * @throws IOException if an I/O error occurs when opening the socket
     */
    public static void main(String[] args) throws IOException {
        ExecutorService threadPool = Executors.newFixedThreadPool(10);
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Ready to receive on port " + PORT + "...");

        initialConfig();

        while (running) {
            Socket clientSocket = serverSocket.accept();
            threadPool.submit(new ClientHandler(clientSocket));
        }
        serverSocket.close();
        threadPool.shutdown();
    }

    /**
     * Configures the initial settings of the web server. This method sets the location
     * of static files and defines custom routes for handling HTTP GET requests.
     */
    private static void initialConfig() {
        WebServer.staticfiles("src/main/resources/webroot");

        WebServer.get("/hello", (req, resp) -> {
            String name = WebServer.queryParams(req, "name");
            
            if (!name.isEmpty()) {
                try {
                    String decodedName = URLDecoder.decode(name, StandardCharsets.UTF_8.name());
                    String plainTextResponse = "Hola, " + decodedName;
                    System.out.println("Respuesta texto plano: " + plainTextResponse);
                    return plainTextResponse;
                } catch (Exception e) {
                    return "Error al decodificar el nombre.";
                }
            } else {
                return "Error: No se proporcionó ningún nombre.";
            }
        });

        WebServer.get("/echo", (req, resp) -> {
            String message = req;
            if (!message.isEmpty()) {
                Pattern pattern = Pattern.compile("\"text\"\\s*:\\s*\"(.*?)\"");
                Matcher matcher = pattern.matcher(message);

                String text;
                if (matcher.find()) {
                    text = matcher.group(1);
                } else {
                    text = "Error: Campo 'text' no encontrado";
                }
                String plainTextResponse = "Echo: " + text;
                System.out.println("Respuesta texto plano: " + plainTextResponse);
                return plainTextResponse;
            } else {
                return "Error: No se proporcionó ningún mensaje.";
            }
        });
    }

    /**
     * Stops the web server by setting the running flag to false.
     */
    public static void stop() {
        running = false;
    }
}

/**
 * Handles client requests in a separate thread. Each instance of this class processes
 * a single client connection, handling GET and POST requests and serving static files
 * or dynamic content based on the request.
 */
class ClientHandler implements Runnable {
    private Socket clientSocket;

    /**
     * Constructs a new ClientHandler with the given client socket.
     *
     * @param socket the client socket
     */
    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    /**
     * Processes the client's request. Depending on the request method (GET or POST),
     * it either serves a file or handles dynamic content via a REST-like interface.
     */
    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
             BufferedOutputStream dataOut = new BufferedOutputStream(clientSocket.getOutputStream())) {

            String requestLine = in.readLine();
            if (requestLine == null)
                return;
                
            String[] tokens = requestLine.split(" ");
            String method = tokens[0];
            String fileRequested = tokens[1];

            printRequestLine(requestLine, in);

            if (fileRequested.startsWith("/app")) {
                handleAppRequest(fileRequested, in, out);
            } else {
                if (method.equals("GET")) {
                    handleGetRequest(fileRequested, out, dataOut);
                } else if (method.equals("POST")) {
                    handlePostRequest(fileRequested, out, dataOut);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close(); 
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Prints the request line and headers for debugging purposes.
     *
     * @param requestLine the initial request line (e.g., "GET /index.html HTTP/1.1")
     * @param in the BufferedReader used to read the client's input
     */
    private void printRequestLine(String requestLine, BufferedReader in) {
        System.out.println("Request line: " + requestLine);
        String inputLine;
        try {
            while ((inputLine = in.readLine()) != null) {
                System.out.println("Header: " + inputLine);
                if (in.ready()) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles HTTP GET requests by serving static files from the configured web root directory.
     *
     * @param fileRequested the path of the file requested by the client
     * @param out the PrintWriter to send the HTTP response headers
     * @param dataOut the BufferedOutputStream to send the file data
     * @throws IOException if an I/O error occurs while reading the file or writing the response
     */
    private void handleGetRequest(String fileRequested, PrintWriter out, BufferedOutputStream dataOut) throws IOException {
        File file = new File(WebServer.staticFilesLocation, fileRequested);
        int fileLength = (int) file.length();
        String content = getContentType(fileRequested);

        if (file.exists()) {
            byte[] fileData = readFileData(file, fileLength);
            out.println("HTTP/1.1 200 OK");
            out.println("Content-type: " + content);
            out.println("Content-length: " + fileLength);
            out.println();
            out.flush();
            dataOut.write(fileData, 0, fileLength);
            dataOut.flush();
        } else {
            out.println("HTTP/1.1 404 Not Found");
            out.println("Content-type: text/html");
            out.println();
            out.flush();
            out.println("<html><body><h1>File Not Found</h1></body></html>");
            out.flush();
        }
    }

    /**
     * Handles HTTP POST requests by reading the client's payload and returning it in an HTML response.
     *
     * @param fileRequested the path requested by the client (not used for POST processing)
     * @param out the PrintWriter to send the HTTP response headers and body
     * @param dataOut the BufferedOutputStream to send any additional data (not used for POST processing)
     * @throws IOException if an I/O error occurs while reading the input or writing the response
     */
    private void handlePostRequest(String fileRequested, PrintWriter out, BufferedOutputStream dataOut) throws IOException {
        StringBuilder payload = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                payload.append(line);
            }
        }

        out.println("HTTP/1.1 200 OK");
        out.println("Content-type: text/html");
        out.println();
        out.println("<html><body><h1>POST data received:</h1>");
        out.println("<p>" + payload.toString() + "</p>");
        out.println("</body></html>");
        out.flush();
    }

    /**
     * Handles requests that start with "/app", delegating to specific handlers based on the request path.
     *
     * @param fileRequested the path requested by the client
     * @param in the BufferedReader to read the client's input
     * @param out the PrintWriter to send the HTTP response
     */
    private void handleAppRequest(String fileRequested, BufferedReader in, PrintWriter out) {
        out.println("HTTP/1.1 200 OK");
        out.println("Content-type: text/plain");
        out.println();
        String modifiedUrl = fileRequested.replaceFirst("^/app", "");
        String response = "";

        if (modifiedUrl.startsWith("/hello")) {
            response = WebServer.services.get("/hello").getValue(fileRequested, "");
        } else if (modifiedUrl.startsWith("/echo")) {
            try {
                String line;
                int contentLength = 0;
                while ((line = in.readLine()) != null && !line.isEmpty()) {
                    if (line.startsWith("Content-Length:")) {
                        contentLength = Integer.parseInt(line.split(":")[1].trim());
                    }
                }
                char[] charArray = new char[contentLength];
                in.read(charArray, 0, contentLength);
                String requestBody = new String(charArray);
                response = WebServer.services.get("/echo").getValue(requestBody, "");
            } catch (IOException e) {
                e.printStackTrace();
                response = "Error al procesar la solicitud";
            }
        } else {
            response = "Error: Método no soportado";
        }
        out.println(response);
        out.flush();
    }

    /**
     * Determines the content type of the requested file based on its extension.
     *
     * @param fileRequested the path of the file requested by the client
     * @return the MIME type of the file
     */
    private String getContentType(String fileRequested) {
        if (fileRequested.endsWith(".html"))
            return "text/html";
        else if (fileRequested.endsWith(".css"))
            return "text/css";
        else if (fileRequested.endsWith(".js"))
            return "application/javascript";
        else if (fileRequested.endsWith(".png"))
            return "image/png";
        else if (fileRequested.endsWith(".jpg"))
            return "image/jpeg";
        return "text/plain";
    }

    /**
     * Reads the data of a file into a byte array.
     *
     * @param file the file to be read
     * @param fileLength the length of the file in bytes
     * @return a byte array containing the file data
     * @throws IOException if an I/O error occurs while reading the file
     */
    private byte[] readFileData(File file, int fileLength) throws IOException {
        try (FileInputStream fileIn = new FileInputStream(file)) {
            byte[] fileData = new byte[fileLength];
            fileIn.read(fileData);
            return fileData;
        }
    }
}
