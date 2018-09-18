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

public class DiffusionClient
{
   public static void main(String args[]) throws IOException
   {
      byte[] tampon = new byte[256];

      // Joindre le groupe pour recevoir le message diffuse
      MulticastSocket socket = new MulticastSocket(4445);
      InetAddress groupe = InetAddress.getByName("228.5.6.7");
      socket.joinGroup(groupe);

      // Attendre le message du serveur
      DatagramPacket paquet = new DatagramPacket(tampon,tampon.length);
      socket.receive(paquet);

      String messageRecu = new String(paquet.getData(),0);
      System.out.println("DiffusionClient: Message recu: " + messageRecu);

      socket.leaveGroup(groupe);
      socket.close();
   }
}
