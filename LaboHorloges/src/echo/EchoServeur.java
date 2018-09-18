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

public class EchoServeur
{
   public static void main(String args[])
   {
      try {
         // Creation du serveur
         ServerSocket echo = new ServerSocket(13267);

         // Attente de la connexion d'un client
         Socket socket = echo.accept();

         // Recupuration du flux d'entree et de sortie
         BufferedReader fluxEntree = new BufferedReader(new InputStreamReader(
				       socket.getInputStream()));
         PrintWriter fluxSortie = new PrintWriter(socket.getOutputStream(),true);

         String chaineLue;
         // Renvoie au client de chaque chaine de caracteres lu
         // jusqu'a ce que ce caractere soit un retour chariot
         while ((chaineLue = fluxEntree.readLine()) != null)
             fluxSortie.println(chaineLue);
	 fluxEntree.close();
	 fluxSortie.close();
	 socket.close();
         echo.close();
      }
      catch (IOException e) {
         System.out.println(e);
      }
   }
}
