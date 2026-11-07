// =============================================================================
// Project Name: OrionC2
// File: Download.java
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
package org.esisar.command_control.download;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;

public class Download {

    public static void main(String fichier) throws Exception {
        // 1. On crée le verrou ici pour qu'il soit neuf à chaque nouveau téléchargement
        CountDownLatch latch = new CountDownLatch(1);

        HttpClient client = HttpClient.newHttpClient();

        // 2. On passe 'fichier' et 'latch' lors de la création du Listener
        WebSocket webSocket = client.newWebSocketBuilder()
                .buildAsync(URI.create("ws://localhost:7000/download"), new ClientWebSocketListener(fichier, latch))
                .join();

        System.out.println("Connecté au serveur en attente du fichier : " + fichier);

        // 3. On bloque ce thread spécifique jusqu'à ce que le verrou atteigne 0
        latch.await();

        System.out.println("Fermeture du thread de téléchargement.");
    }

    private static class ClientWebSocketListener implements WebSocket.Listener {

        private final Path cheminDestination;
        private final CountDownLatch latch;
        private FileChannel channel; // Variable pour garder le fichier ouvert

        public ClientWebSocketListener(String nomFichier, CountDownLatch latch) {
            this.cheminDestination = Path.of(nomFichier);
            this.latch = latch;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            try {
                // 1. On ouvre le fichier au moment de la connexion
                // Utilisation de TRUNCATE_EXISTING pour écraser le fichier s'il existe déjà
                this.channel = FileChannel.open(cheminDestination,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.TRUNCATE_EXISTING);

                WebSocket.Listener.super.onOpen(webSocket);
                webSocket.request(1);
            } catch (Exception e) {
                System.err.println("Erreur lors de l'ouverture du fichier : " + e.getMessage());
                latch.countDown(); // Libérer le verrou en cas d'échec initial
            }
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            try {
                // 2. On écrit directement dans le canal déjà ouvert
                while (data.hasRemaining()) {
                    channel.write(data);
                }

                if (last) {
                    System.out.println("Téléchargement terminé avec succès sous : " + cheminDestination.toString());
                    fermerCanal();
                    latch.countDown();
                } else {
                    // Demande le prochain message seulement si ce n'est pas le dernier
                    webSocket.request(1);
                }
            } catch (Exception e) {
                e.printStackTrace();
                fermerCanal();
                latch.countDown();
            }

            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            System.err.println("Erreur WebSocket : " + error.getMessage());
            fermerCanal(); // Penser à fermer en cas d'erreur réseau
            latch.countDown();
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            fermerCanal(); // Sécurité au cas où la connexion se ferme inopinément
            latch.countDown();
            return null;
        }

        // Méthode utilitaire pour fermer proprement la ressource
        private void fermerCanal() {
            if (channel != null && channel.isOpen()) {
                try {
                    channel.close();
                } catch (Exception e) {
                    System.err.println("Erreur lors de la fermeture du fichier : " + e.getMessage());
                }
            }
        }
    }
}