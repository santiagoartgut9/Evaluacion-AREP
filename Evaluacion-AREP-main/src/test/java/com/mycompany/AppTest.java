package com.mycompany;

import static org.junit.Assert.*;
import static org.junit.Assert.assertTrue;

import java.io.*;
import java.net.Socket;
import org.junit.Test;

public class AppTest {

  @Test
  public void testSetAndGetKV() throws IOException {
    // SET
    String setRequest =
      "GET /setkv?key=testkey&value=testvalue HTTP/1.1\r\nHost: localhost\r\n\r\n";
    String setResponse = sendRequest(setRequest, 36000);
    assertTrue(setResponse.contains("\"status\":\"created\""));

    // GET
    String getRequest =
      "GET /getkv?key=testkey HTTP/1.1\r\nHost: localhost\r\n\r\n";
    String getResponse = sendRequest(getRequest, 36000);
    assertTrue(getResponse.contains("\"value\":\"testvalue\""));
  }

  private String sendRequest(String request, int port) throws IOException {
    try (Socket socket = new Socket("localhost", port)) {
      PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
      BufferedReader in = new BufferedReader(
        new InputStreamReader(socket.getInputStream())
      );
      out.print(request);
      out.flush();
      StringBuilder response = new StringBuilder();
      String line;
      while ((line = in.readLine()) != null) {
        response.append(line).append("\n");
      }
      return response.toString();
    }
  }
}
