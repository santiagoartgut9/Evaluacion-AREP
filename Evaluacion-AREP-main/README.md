Descripción:
Usted debe construir un "Almacenamiento llave-Valor (key, value) distribuido". La solución consta de un servidor backend que responde a solicitudes HTTP GET de la Facade, un servidor Facade que responde a solicitudes HTTP GET del cliente , y un cliente Html+JS que envía los comandos y muestra las respuestas. La api permite almacenar tuplas llave-valor (k,v), ambas de tipo string en el backend, y recuperar el valor dada una llave. 
 
API mínimo (solo GET) para el escenario con Fachada y Backend. Cubre crear/actualizar un par (k,v) y consultarlo.

Debe usar sockets solamente, no puede usar ni Spark ni Spring.
API mínima para “Almacenamiento llave-valor” (GET/POST)
Arquitectura: Cliente HTML+JS → Fachada → Backend. La Fachada valida y reenvía las solicitudes al Backend. Ambos exponen el mismo contrato mínimo usando únicamente GET y POST.

1) Cliente → Fachada
1.1) GET /setkv?key={key}&value={value}
Descripción: Crea o reemplaza el valor asociado a una llave.

Respuestas:

200 OK – reemplazado  o creado.
400 Bad Request – faltan key o value o no son string.
Respuesta (JSON, ejemplo):

{ "key": "mi_llave", "value": "mi_valor", "status": "created" }

1.2) GET /getkv?key={key}
Descripción: Obtiene el valor de una llave.

Respuestas:

200 OK

{ "key": "mi_llave", "value": "mi_valor" }

404 Not Found
{ "error": "key_not_found", "key": "mi_llave" }

2) Fachada → Backend
La Fachada reenvía al Backend el mismo contrato, propagando códigos y cuerpos de respuesta.

2.1) GET /setkv?key={key}&value={value}
Descripción: Crea o reemplaza el valor asociado a una llave.

Respuestas:

200 OK – reemplazado  o creado.
400 Bad Request – faltan key o value o no son string.
Respuesta (JSON, ejemplo):

{ "key": "mi_llave", "value": "mi_valor", "status": "created" }

2.2) GET /getkv?key={key}
Descripción: Obtiene el valor de una llave.

Respuestas:

200 OK
{ "key": "mi_llave", "value": "mi_valor" }

404 Not Found
{ "error": "key_not_found", "key": "mi_llave" }

3) Consideraciones mínimas
Contenido: usar Content-Type: application/json; charset=utf-8.
Validaciones en Fachada: tamaño máximo de key y value, recortar espacios, rechazar key == "".
Errores estándar: incluir campo error y opcionalmente message para diagnóstico.
Arquitectura
 
- La aplicación tendrá tres componentes distribuidos: Una fachada de servicios, un servicio de backend, y un cliente web (html +js).
- Los servicios de la fachada y del backend deben estar desplegados en máquinas virtuales java  diferentes.
- El cliente es un cliente web que usa html y js. Se descarga desde un servicio en la fachada (Puede entregar el cliente directamente desde un método no es necesario que lo lea desde el disco).
- La comunicación se hace usando http y las respuestas de los servicios son en formato JSON.
- Los llamados al servicio de fachada desde el cliente deben ser asíncronos usando el mínimo JS prosible. No actualice la página en cada llamado, solo el resultado.
-Los retornos deben estar  en formato JSON o TEXTO.
- El diseño de los servicios WEB debe tener en cuenta buenas prácticas de diseño OO

- Ayudas:
1. Para invocar servicios rest de forma asíncrona desde un cliente JS


<!DOCTYPE html>
<html>

<head>
    <title>Form Example</title>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
</head>

<body>
    <h1>Form with GET</h1>
    <form action="/hello">
        <label for="name">Name:</label><br>
        <input type="text" id="name" name="name" value="John"><br><br>
        <input type="button" value="Submit" onclick="loadGetMsg()">
    </form>
    <div id="getrespmsg"></div>

    <script>
        function loadGetMsg() {
            let nameVar = document.getElementById("name").value;
            const xhttp = new XMLHttpRequest();
            xhttp.onload = function () {
                document.getElementById("getrespmsg").innerHTML =
                    this.responseText;
            }
            xhttp.open("GET", "/hello?name=" + nameVar);
            xhttp.send();
        }
    </script>

</body>

</html>
    
 
2. Cómo hacer un servidor web mínimo que funcione correctamente:
    
a. Hacer que el servidor responda múltiples solicitudes

b. Asegurarse de que la salida sea una salida http válida.

Código del servidor http



import java.net.*;
import java.io.*;

public class HttpServer {
  public static void main(String[] args) throws IOException {
   ServerSocket serverSocket = null;
   try { 
      serverSocket = new ServerSocket(36000);
   } catch (IOException e) {
      System.err.println("Could not listen on port: 35000.");
      System.exit(1);
   }

   Socket clientSocket = null;
   try {
       System.out.println("Listo para recibir ...");
       clientSocket = serverSocket.accept();
   } catch (IOException e) {
       System.err.println("Accept failed.");
       System.exit(1);
   }
   PrintWriter out = new PrintWriter(
                         clientSocket.getOutputStream(), true);
   BufferedReader in = new BufferedReader(
                         new InputStreamReader(clientSocket.getInputStream()));
   String inputLine, outputLine;
   while ((inputLine = in.readLine()) != null) {
      System.out.println("Recibí: " + inputLine);
      if (!in.ready()) {break; }
   }
   outputLine = "HTTP/1.1 200 OK\r\n"
        + "Content-Type: text/html\r\n"
         + "\r\n"
         + "<!DOCTYPE html>\n"
         + "<html>\n"
         + "<head>\n"
         + "<meta charset=\"UTF-8\">\n"
         + "<title>Title of the document</title>\n"
         + "</head>\n"
         + "<body>\n"
         + "<h1>Mi propio mensaje</h1>\n"
         + "</body>\n"
         + "</html>\n";
    out.println(outputLine);
    out.close(); 
    in.close(); 
    clientSocket.close(); 
    serverSocket.close(); 
  }
}
    

3. Para invocar un servicio REST desde java:



  public class HttpConnectionExample {

    private static final String USER_AGENT = "Mozilla/5.0";
    private static final String GET_URL = "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=fb&apikey=Q1QZFVJQ21K7C6XM";

    public static void main(String[] args) throws IOException {

        URL obj = new URL(GET_URL);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", USER_AGENT);
        
        //The following invocation perform the connection implicitly before getting the code
        int responseCode = con.getResponseCode();
        System.out.println("GET Response Code :: " + responseCode);
        
        if (responseCode == HttpURLConnection.HTTP_OK) { // success
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            // print result
            System.out.println(response.toString());
        } else {
            System.out.println("GET request not worked");
        }
        System.out.println("GET DONE");
    }

} 


