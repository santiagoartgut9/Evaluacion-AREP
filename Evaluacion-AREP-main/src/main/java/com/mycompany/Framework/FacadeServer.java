package com.mycompany.Framework;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class FacadeServer {

  private static final int PORT = 36000;
  private static final String BACKEND_HOST = "localhost";
  private static final int BACKEND_PORT = 35000;
  private static final int MAX_LENGTH = 100;

  public static void main(String[] args) throws IOException {
    try (ServerSocket serverSocket = new ServerSocket(PORT)) {
      System.out.println("Fachada lista en puerto " + PORT);
      while (true) {
        try (Socket clientSocket = serverSocket.accept()) {
          handleClient(clientSocket);
        }
      }
    }
  }

  private static void handleClient(Socket clientSocket) throws IOException {
    BufferedReader in = new BufferedReader(
      new InputStreamReader(clientSocket.getInputStream())
    );
    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
    String inputLine;
    StringBuilder request = new StringBuilder();
    while ((inputLine = in.readLine()) != null && !inputLine.isEmpty()) {
      request.append(inputLine).append("\n");
    }
    String requestLine = request.toString().split("\n")[0];
    String response;
    if (
      requestLine.startsWith("GET / ") || requestLine.startsWith("GET /HTTP")
    ) {
      response = serveIndexHtml();
    } else {
      response = processRequest(requestLine);
    }
    out.println(response);
    out.close();
    in.close();
  }

  private static String serveIndexHtml() {
    StringBuilder html = new StringBuilder();
    html.append("HTTP/1.1 200 OK\r\n");
    html.append("Content-Type: text/html; charset=utf-8\r\n\r\n");
    html.append(
      "<!DOCTYPE html><html><head><title>Key-Value Store Cliente</title><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"></head><body><h1>Almacenamiento llave-valor distribuido</h1><div><h2>Guardar (setkv)</h2><label for=\"setkey\">Key:</label><input type=\"text\" id=\"setkey\" maxlength=\"100\"><br><label for=\"setvalue\">Value:</label><input type=\"text\" id=\"setvalue\" maxlength=\"100\"><br><button onclick=\"setKV()\">Guardar</button><div id=\"setrespmsg\"></div></div><hr><div><h2>Consultar (getkv)</h2><label for=\"getkey\">Key:</label><input type=\"text\" id=\"getkey\" maxlength=\"100\"><button onclick=\"getKV()\">Consultar</button><div id=\"getrespmsg\"></div></div><script>function setKV(){let key=document.getElementById('setkey').value.trim();let value=document.getElementById('setvalue').value.trim();if(!key||!value){document.getElementById('setrespmsg').innerHTML='<span style=\'color:red\'>Key y Value son requeridos</span>';return;}const xhttp=new XMLHttpRequest();xhttp.onload=function(){document.getElementById('setrespmsg').innerHTML=`<pre>${this.responseText}</pre>`};xhttp.open('GET',`/setkv?key=${encodeURIComponent(key)}&value=${encodeURIComponent(value)}`);xhttp.setRequestHeader('Accept','application/json');xhttp.send();}function getKV(){let key=document.getElementById('getkey').value.trim();if(!key){document.getElementById('getrespmsg').innerHTML='<span style=\'color:red\'>Key es requerida</span>';return;}const xhttp=new XMLHttpRequest();xhttp.onload=function(){document.getElementById('getrespmsg').innerHTML=`<pre>${this.responseText}</pre>`};xhttp.open('GET',`/getkv?key=${encodeURIComponent(key)}`);xhttp.setRequestHeader('Accept','application/json');xhttp.send();}</script></body></html>"
    );
    return html.toString();
  }

  private static String processRequest(String requestLine) {
    if (requestLine.startsWith("GET /setkv?")) {
      return validateAndForward(requestLine, true);
    } else if (requestLine.startsWith("GET /getkv?")) {
      return validateAndForward(requestLine, false);
    } else {
      return httpResponse(
        400,
        "{\"error\":\"bad_request\",\"message\":\"Ruta no soportada\"}"
      );
    }
  }

  private static String validateAndForward(String requestLine, boolean isSet) {
    String key = getParam(requestLine, "key");
    String value = isSet ? getParam(requestLine, "value") : null;
    key = trim(key);
    if (key == null || key.isEmpty() || key.length() > MAX_LENGTH) {
      return httpResponse(
        400,
        "{\"error\":\"bad_request\",\"message\":\"Key inválida\"}"
      );
    }
    if (isSet) {
      value = trim(value);
      if (value == null || value.isEmpty() || value.length() > MAX_LENGTH) {
        return httpResponse(
          400,
          "{\"error\":\"bad_request\",\"message\":\"Value inválido\"}"
        );
      }
    }
    // Forward to backend
    return forwardToBackend(requestLine);
  }

  private static String forwardToBackend(String requestLine) {
    try (Socket backendSocket = new Socket(BACKEND_HOST, BACKEND_PORT)) {
      PrintWriter out = new PrintWriter(backendSocket.getOutputStream(), true);
      BufferedReader in = new BufferedReader(
        new InputStreamReader(backendSocket.getInputStream())
      );
      out.println(requestLine);
      out.println(); // End of request
      StringBuilder response = new StringBuilder();
      String line;
      while ((line = in.readLine()) != null) {
        response.append(line).append("\n");
      }
      return response.toString().trim();
    } catch (IOException e) {
      return httpResponse(
        500,
        "{\"error\":\"backend_unreachable\",\"message\":\"No se pudo conectar al backend\"}"
      );
    }
  }

  private static String getParam(String requestLine, String param) {
    int idx = requestLine.indexOf('?');
    if (idx < 0) return null;
    String[] parts = requestLine.substring(idx + 1).split(" ")[0].split("&");
    for (String part : parts) {
      String[] kv = part.split("=");
      if (kv.length == 2 && kv[0].equals(param)) {
        return kv[1];
      }
    }
    return null;
  }

  private static String trim(String s) {
    return s == null ? null : s.trim();
  }

  private static String httpResponse(int code, String body) {
    String status = code == 200
      ? "OK"
      : code == 400 ? "Bad Request" : code == 404 ? "Not Found" : "Error";
    return (
      "HTTP/1.1 " +
      code +
      " " +
      status +
      "\r\n" +
      "Content-Type: application/json; charset=utf-8\r\n" +
      "\r\n" +
      body
    );
  }
}
