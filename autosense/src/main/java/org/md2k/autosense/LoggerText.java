package org.md2k.autosense;

import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * Copyright (c) 2015, The University of Memphis, MD2K Center
 * - Syed Monowar Hossain <monowar.hossain@gmail.com>
 * All rights reserved.
 * <p/>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * <p/>
 * * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * <p/>
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * <p/>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
public class LoggerText {
    private static LoggerText instance = null;
    OutputStreamWriter outputStreamWriter;
    FileOutputStream fos;

    public static LoggerText getInstance() {
        if (instance == null) instance = new LoggerText();
        return instance;
    }

    void createFile() {
        String filename = "logger.csv";
        try {
            File file = new File(Environment.getExternalStorageDirectory(), filename);
            file.createNewFile();
            fos = new FileOutputStream(file);
            outputStreamWriter = new OutputStreamWriter(fos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private LoggerText() {
        createFile();
    }

    public void saveDataToTextFile(String data) {
        try {
                outputStreamWriter.append(data);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void closeFile() {
        try {
            outputStreamWriter.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void close() {
        closeFile();
        instance = null;
    }

}
