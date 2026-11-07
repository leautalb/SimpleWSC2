// =============================================================================
// Project Name: OrionC2
// File: CheckAuth.java
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
package org.esisar.command_control.auth;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.UnauthorizedResponse;

import java.util.HashMap;

//Permet de vérifier que le client n'est pas un "gentil" qui essaye de comprendre comment le C2 fonctionne.
public class CheckAuth implements Handler {
    private HashMap<String, String> ids = new HashMap<>();

    public void handle(Context ctx) {
        if(ctx.path().equals("/auth")){
            return;
        }
        else{
            if(ids.containsKey(ctx.queryParam("id"))){
                return;
            }
            else{
                ctx.status(401);
                System.out.println("erreur d'authentification");
                throw new UnauthorizedResponse("Vous n'êtes pas autorisé à effectuer cette action");
            }
        }
    }
}