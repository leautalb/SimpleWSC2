// =============================================================================
// Project Name: OrionC2
// File: AES.java
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
package org.esisar.command_control.encrypt;

import java.nio.charset.StandardCharsets;

public class AES {

    // Table de substitution S-Box officielle d'AES
    private static final int[] S_BOX = {
        0x63, 0x7c, 0x77, 0x7b, 0xf2, 0x6b, 0x6f, 0xc5, 0x30, 0x01, 0x67, 0x2b, 0xfe, 0xd7, 0xab, 0x76,
        0xca, 0x82, 0xc9, 0x7d, 0xfa, 0x59, 0x47, 0xf0, 0xad, 0xd4, 0xa2, 0xaf, 0x9c, 0xa4, 0x72, 0xc0,
        0xb7, 0xfd, 0x93, 0x26, 0x36, 0x3f, 0xf7, 0xcc, 0x34, 0xa5, 0xe5, 0xf1, 0x71, 0xd8, 0x31, 0x15,
        0x04, 0xc7, 0x23, 0xc3, 0x18, 0x96, 0x05, 0x9a, 0x07, 0x12, 0x80, 0xe2, 0xeb, 0x27, 0xb2, 0x75,
        0x09, 0x83, 0x2c, 0x1a, 0x1b, 0x6e, 0x5a, 0xa0, 0x52, 0x3b, 0xd6, 0xb3, 0x29, 0xe3, 0x2f, 0x84,
        0x53, 0xd1, 0x00, 0xed, 0x20, 0xfc, 0xb1, 0x5b, 0x6a, 0xcb, 0xbe, 0x39, 0x4a, 0x4c, 0x58, 0xcf,
        0xd0, 0xef, 0xaa, 0xfb, 0x43, 0x4d, 0x33, 0x85, 0x45, 0xf9, 0x02, 0x7f, 0x50, 0x3c, 0x9f, 0xa8,
        0x51, 0xa3, 0x40, 0x8f, 0x92, 0x9d, 0x38, 0xf5, 0xbc, 0xb6, 0xda, 0x21, 0x10, 0xff, 0xf3, 0xd2,
        0xcd, 0x0c, 0x13, 0xec, 0x5f, 0x97, 0x44, 0x17, 0xc4, 0xa7, 0x7e, 0x3d, 0x64, 0x5d, 0x19, 0x73,
        0x60, 0x81, 0x4f, 0xdc, 0x22, 0x2a, 0x90, 0x88, 0x46, 0xee, 0xb8, 0x14, 0xde, 0x5e, 0x0b, 0xdb,
        0xe0, 0x32, 0x3a, 0x0a, 0x49, 0x06, 0x24, 0x5c, 0xc2, 0xd3, 0xac, 0x62, 0x91, 0x95, 0xe4, 0x79,
        0xe7, 0xc8, 0x37, 0x6d, 0x8d, 0xd5, 0x4e, 0xa9, 0x6c, 0x56, 0xf4, 0xea, 0x65, 0x7a, 0xae, 0x08,
        0xba, 0x78, 0x25, 0x2e, 0x1c, 0xa6, 0xb4, 0xc6, 0xe8, 0xdd, 0x74, 0x1f, 0x4b, 0xbd, 0x8b, 0x8a,
        0x70, 0x3e, 0xb5, 0x66, 0x48, 0x03, 0xf6, 0x0e, 0x61, 0x35, 0x57, 0xb9, 0x86, 0xc1, 0x1d, 0x9e,
        0xe1, 0xf8, 0x98, 0x11, 0x69, 0xd9, 0x8e, 0x94, 0x9b, 0x1e, 0x87, 0xe9, 0xce, 0x55, 0x28, 0xdf,
        0x8c, 0xa1, 0x89, 0x0d, 0xbf, 0xe6, 0x42, 0x68, 0x41, 0x99, 0x2d, 0x0f, 0xb0, 0x54, 0xbb, 0x16
    };

    // S-Box Inverse pour le déchiffrement
    private static final int[] INV_S_BOX = new int[256];

    // Constantes de tour (Rcon)
    private static final int[] RCON = {
        0x00, 0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, 0x80, 0x1b, 0x36
    };

    static {
        // Génération automatique de la S-Box inverse
        for (int i = 0; i < 256; i++) {
            INV_S_BOX[S_BOX[i]] = i;
        }
    }

    private final int[][] expandedKey; // Clés étendues (11 sous-clés de 16 octets)

    public AES(String key) {
        if (key.length() != 16) {
            throw new IllegalArgumentException("La clé doit faire exactement 16 caractères (128 bits).");
        }
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        this.expandedKey = keyExpansion(keyBytes);
    }

    // =========================================================================
    // CHIFFREMENT (ENCRYPT) ET DÉCHIFFREMENT (DECRYPT)
    // =========================================================================

    public String encrypt(String plaintext) {
        byte[] inputBytes = plaintext.getBytes(StandardCharsets.UTF_8);
        byte[] paddedBytes = addPKCS7Padding(inputBytes);

        int numBlocks = paddedBytes.length / 16;
        StringBuilder hexResult = new StringBuilder();

        for (int b = 0; b < numBlocks; b++) {
            int[][] state = bytesToState(paddedBytes, b * 16);

            // Tour initial
            addRoundKey(state, 0);

            // 9 tours principaux
            for (int round = 1; round <= 9; round++) {
                subBytes(state);
                shiftRows(state);
                mixColumns(state);
                addRoundKey(state, round);
            }

            // Tour final (sans MixColumns)
            subBytes(state);
            shiftRows(state);
            addRoundKey(state, 10);

            byte[] encryptedBlock = stateToBytes(state);
            for (byte item : encryptedBlock) {
                hexResult.append(String.format("%02x", item & 0xFF));
            }
        }

        return hexResult.toString();
    }

    public String decrypt(String hexCiphertext) {
        byte[] cipherBytes = hexStringToByteArray(hexCiphertext);
        int numBlocks = cipherBytes.length / 16;
        byte[] decryptedBytes = new byte[cipherBytes.length];

        for (int b = 0; b < numBlocks; b++) {
            int[][] state = bytesToState(cipherBytes, b * 16);

            // Tour 10 (inverse)
            addRoundKey(state, 10);
            invShiftRows(state);
            invSubBytes(state);

            // Tours 9 à 1 (inverse)
            for (int round = 9; round >= 1; round--) {
                addRoundKey(state, round);
                invMixColumns(state);
                invShiftRows(state);
                invSubBytes(state);
            }

            // Tour initial (inverse)
            addRoundKey(state, 0);

            byte[] block = stateToBytes(state);
            System.arraycopy(block, 0, decryptedBytes, b * 16, 16);
        }

        byte[] unpaddedBytes = removePKCS7Padding(decryptedBytes);
        return new String(unpaddedBytes, StandardCharsets.UTF_8);
    }

    // =========================================================================
    // TRANSFORMATIONS AES (TRANSFORMATIONS D'ÉTAT)
    // =========================================================================

    private void subBytes(int[][] state) {
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                state[r][c] = S_BOX[state[r][c]];
            }
        }
    }

    private void invSubBytes(int[][] state) {
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                state[r][c] = INV_S_BOX[state[r][c]];
            }
        }
    }

    private void shiftRows(int[][] state) {
        for (int r = 1; r < 4; r++) {
            int[] temp = new int[4];
            for (int c = 0; c < 4; c++) {
                temp[c] = state[r][(c + r) % 4];
            }
            state[r] = temp;
        }
    }

    private void invShiftRows(int[][] state) {
        for (int r = 1; r < 4; r++) {
            int[] temp = new int[4];
            for (int c = 0; c < 4; c++) {
                temp[c] = state[r][(c - r + 4) % 4];
            }
            state[r] = temp;
        }
    }

    private void mixColumns(int[][] s) {
        for (int c = 0; c < 4; c++) {
            int s0 = s[0][c], s1 = s[1][c], s2 = s[2][c], s3 = s[3][c];
            s[0][c] = galoisMult(0x02, s0) ^ galoisMult(0x03, s1) ^ s2 ^ s3;
            s[1][c] = s0 ^ galoisMult(0x02, s1) ^ galoisMult(0x03, s2) ^ s3;
            s[2][c] = s0 ^ s1 ^ galoisMult(0x02, s2) ^ galoisMult(0x03, s3);
            s[3][c] = galoisMult(0x03, s0) ^ s1 ^ s2 ^ galoisMult(0x02, s3);
        }
    }

    private void invMixColumns(int[][] s) {
        for (int c = 0; c < 4; c++) {
            int s0 = s[0][c], s1 = s[1][c], s2 = s[2][c], s3 = s[3][c];
            s[0][c] = galoisMult(0x0e, s0) ^ galoisMult(0x0b, s1) ^ galoisMult(0x0d, s2) ^ galoisMult(0x09, s3);
            s[1][c] = galoisMult(0x09, s0) ^ galoisMult(0x0e, s1) ^ galoisMult(0x0b, s2) ^ galoisMult(0x0d, s3);
            s[2][c] = galoisMult(0x0d, s0) ^ galoisMult(0x09, s1) ^ galoisMult(0x0e, s2) ^ galoisMult(0x0b, s3);
            s[3][c] = galoisMult(0x0b, s0) ^ galoisMult(0x0d, s1) ^ galoisMult(0x09, s2) ^ galoisMult(0x0e, s3);
        }
    }

    private void addRoundKey(int[][] state, int round) {
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                state[r][c] ^= expandedKey[round * 4 + c][r];
            }
        }
    }

    // =========================================================================
    // MULTIPLICATION DE GALOIS & KEY EXPANSION
    // =========================================================================

    private int galoisMult(int a, int b) {
        int p = 0;
        for (int counter = 0; counter < 8; counter++) {
            if ((b & 1) != 0) {
                p ^= a;
            }
            boolean hiBitSet = (a & 0x80) != 0;
            a = (a << 1) & 0xFF;
            if (hiBitSet) {
                a ^= 0x1b; // Polynôme irréductible AES
            }
            b >>= 1;
        }
        return p;
    }

    private int[][] keyExpansion(byte[] key) {
        int[][] w = new int[44][4];

        for (int i = 0; i < 4; i++) {
            w[i][0] = key[4 * i] & 0xFF;
            w[i][1] = key[4 * i + 1] & 0xFF;
            w[i][2] = key[4 * i + 2] & 0xFF;
            w[i][3] = key[4 * i + 3] & 0xFF;
        }

        for (int i = 4; i < 44; i++) {
            int[] temp = w[i - 1].clone();
            if (i % 4 == 0) {
                // RotWord
                int k = temp[0];
                temp[0] = temp[1];
                temp[1] = temp[2];
                temp[2] = temp[3];
                temp[3] = k;

                // SubWord
                for (int j = 0; j < 4; j++) {
                    temp[j] = S_BOX[temp[j]];
                }

                // XOR avec Rcon
                temp[0] ^= RCON[i / 4];
            }
            for (int j = 0; j < 4; j++) {
                w[i][j] = w[i - 4][j] ^ temp[j];
            }
        }
        return w;
    }

    // =========================================================================
    // OUTILS D'ALIGNEMENT ET PADDING (PKCS#7)
    // =========================================================================

    private int[][] bytesToState(byte[] bytes, int offset) {
        int[][] state = new int[4][4];
        for (int c = 0; c < 4; c++) {
            for (int r = 0; r < 4; r++) {
                state[r][c] = bytes[offset + c * 4 + r] & 0xFF;
            }
        }
        return state;
    }

    private byte[] stateToBytes(int[][] state) {
        byte[] bytes = new byte[16];
        for (int c = 0; c < 4; c++) {
            for (int r = 0; r < 4; r++) {
                bytes[c * 4 + r] = (byte) state[r][c];
            }
        }
        return bytes;
    }

    private byte[] addPKCS7Padding(byte[] input) {
        int paddingLen = 16 - (input.length % 16);
        byte[] padded = new byte[input.length + paddingLen];
        System.arraycopy(input, 0, padded, 0, input.length);
        for (int i = input.length; i < padded.length; i++) {
            padded[i] = (byte) paddingLen;
        }
        return padded;
    }

    private byte[] removePKCS7Padding(byte[] input) {
        int paddingLen = input[input.length - 1] & 0xFF;
        if (paddingLen <= 0 || paddingLen > 16) {
            return input;
        }
        byte[] unpadded = new byte[input.length - paddingLen];
        System.arraycopy(input, 0, unpadded, 0, unpadded.length);
        return unpadded;
    }

    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                 + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}