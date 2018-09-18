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

public class EchoClient
{
   public static void main(String args[]) throws IOException
   {
      Socket echoSocket = null;
      BufferedReader fluxEntree = null;
      PrintWriter fluxSortie = null;
      String serveur = "localhost";

      try {
         // Ouverture d'une connexion sur le port 13267 du serveur
         echoSocket = new Socket(serveur,13267);
         // Recupuration du flux d'entree et de sortie
         fluxEntree = new BufferedReader(new InputStreamReader(
			     echoSocket.getInputStream()));
         fluxSortie = new PrintWriter(echoSocket.getOutputStream(),true);
      }
      catch (UnknownHostException e) {
         System.err.println("Serveur hote " + serveur + "inconnu");
         System.exit(1);
      }
      catch (IOException e) {
         System.err.println("Problemes d'ouverture des flux d'E/S");
         System.exit(1);
      }

      String echoChaine = "Allo?";
      fluxSortie.println(echoChaine);
      System.out.println("Message recu:" + fluxEntree.readLine());

      fluxEntree.close();
      fluxSortie.close();
      echoSocket.close();
   }
}
