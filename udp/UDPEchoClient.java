/*

Compilation sous Cygwin:

1. s'assurer qu'un JDK (pas seulement JRE) soit compris dans la 
variable PATH, par 
    echo $PATH
S'il n'y a aucune mention de 
"/cygdrive/c/j2sdk1.4.1_02/bin" (pour autant qu'un JDK soit installé
en c:\j2sdk1.4.1_02), il faut le rajouter par la commande 
    export PATH=/cygdrive/c/j2sdk1.4.1_02/bin:$PATH
A partir de cet instant, vous pouvez utiliser votre JDK dans la console
actuelle, et cette opération est à effectuer dans toute nouvelle 
console destinée à la compilation d'un programme Java. Vous pourrez par
la suite intégrer la commande dans un des fichiers de configuration, comme
"~/.profile".

2. compiler le programme par
    javac MonProg.java

3. lancer l'interprétation du programme par
    java -cp . MonProg

*/


import java.io.*;
import java.net.*;

public class UDPEchoClient
{
    public static void main(String[] args) throws IOException
    {
      String message = "Allo";
      byte[] tampon = new byte[256];

      // Creation du message a envoyer un message au serveur
      tampon = message.getBytes();
      InetAddress address = InetAddress.getByName("localhost");
      DatagramPacket paquet = new DatagramPacket(tampon,tampon.length,address,4445);

      // Obtenir un socket de datagramme
      DatagramSocket socket = new DatagramSocket();
      // Envoyer le message
      socket.send(paquet);

      // Obtenir l'echo
      DatagramPacket packet = new DatagramPacket(tampon,tampon.length,address,4445);
      socket.receive(packet);
      message = new String(packet.getData());
      System.out.println("Echo: " + message);

      socket.close();
    }
}
