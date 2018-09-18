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

public class DiffusionServeur
{
    public static void main(String[] args) throws IOException
    {
      String message = "Allo";
      byte[] tampon = new byte[256];

      // Associer un port de communication a un groupe 
      InetAddress groupe = InetAddress.getByName("228.5.6.7");
      MulticastSocket socket = new MulticastSocket(4446);
      socket.joinGroup(groupe);

      // Creation du message a diffuser aux clients
      tampon = message.getBytes();
      DatagramPacket paquet = new DatagramPacket(tampon,tampon.length,groupe,4445);

      // Envoyer le message aux membres du groupe
      socket.send(paquet);

      socket.leaveGroup(groupe);
      socket.close();
    }
}
