package com.mycompany.Framework;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class BackendServer {

  private static final int PORT = 35000;
  private static final Map<String, String> store = new HashMap<>();
  private static final int MAX_LENGTH = 100;

  public static void main(String[] args) throws IOException {
    try (ServerSocket serverSocket = new ServerSocket(PORT)) {
      System.out.println("Backend listo en puerto " + PORT);
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
    String response = processRequest(requestLine);
    out.println(response);
    out.close();
    in.close();
  }

  private static String processRequest(String requestLine) {
    if (requestLine.startsWith("GET /setkv?")) {
      return handleSetKV(requestLine);
    } else if (requestLine.startsWith("GET /getkv?")) {
      return handleGetKV(requestLine);
    } else {
      return httpResponse(
        400,
        "{\"error\":\"bad_request\",\"message\":\"Ruta no soportada\"}"
      );
    }
  }

  private static String handleSetKV(String requestLine) {
    Map<String, String> params = parseParams(requestLine);
    String key = trim(params.get("key"));
    String value = trim(params.get("value"));
    if (
      key == null ||
      value == null ||
      key.isEmpty() ||
      value.isEmpty() ||
      key.length() > MAX_LENGTH ||
      value.length() > MAX_LENGTH
    ) {
      return httpResponse(
        400,
        "{\"error\":\"bad_request\",\"message\":\"Faltan key/value o exceden tamaño\"}"
      );
    }
    store.put(key, value);
    return httpResponse(
      200,
      String.format(
        "{\"key\":\"%s\",\"value\":\"%s\",\"status\":\"created\"}",
        key,
        value
      )
    );
  }

  private static String handleGetKV(String requestLine) {
    Map<String, String> params = parseParams(requestLine);
    String key = trim(params.get("key"));
    if (key == null || key.isEmpty() || key.length() > MAX_LENGTH) {
      return httpResponse(
        400,
        "{\"error\":\"bad_request\",\"message\":\"Falta key o excede tamaño\"}"
      );
    }
    if (!store.containsKey(key)) {
      return httpResponse(
        404,
        String.format("{\"error\":\"key_not_found\",\"key\":\"%s\"}", key)
      );
    }
    String value = store.get(key);
    return httpResponse(
      200,
      String.format("{\"key\":\"%s\",\"value\":\"%s\"}", key, value)
    );
  }

  private static Map<String, String> parseParams(String requestLine) {
    Map<String, String> params = new HashMap<>();
    int idx = requestLine.indexOf('?');
    if (idx < 0) return params;
    String[] parts = requestLine.substring(idx + 1).split(" ")[0].split("&");
    for (String part : parts) {
      String[] kv = part.split("=");
      if (kv.length == 2) {
        params.put(kv[0], kv[1]);
      }
    }
    return params;
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
