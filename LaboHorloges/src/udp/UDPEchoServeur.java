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


import java.net.*;
import java.io.*;

public class UDPEchoServeur
{
   public static void main(String args[]) throws IOException, SocketException
   {
      byte[] tampon = new byte[256];

      // Obtenir un socket de datagramme
      DatagramSocket socket = new DatagramSocket(4445);

      // Attendre le message du client
      DatagramPacket paquet = new DatagramPacket(tampon,tampon.length);
      socket.receive(paquet);

      String messageRecu = new String(paquet.getData());
      System.out.println("Echo: " + messageRecu);

      // Obtenir l'adresse et le port du client
      InetAddress addresseClient = paquet.getAddress();
      int portClient = paquet.getPort();

      // Reemettre le message recu
      tampon = messageRecu.getBytes();
      paquet = new DatagramPacket(tampon,tampon.length,addresseClient,portClient);
      socket.send(paquet);

      socket.close();
   }
}
