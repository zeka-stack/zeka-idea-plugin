package com.alibabacloud.intellij.service.edas.registry;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class EdasServiceConnectLogger {
    private File logFile;
    private BufferedWriter writer;
    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public EdasServiceConnectLogger(String file) throws IOException {
        this.logFile = new File(file);
        EdasServiceConnectUtils.touch(this.logFile);
        this.writer = new BufferedWriter(new FileWriter(this.logFile, true));
    }

    public EdasServiceConnectLogger() {
    }

    public void setLogFile(String file) throws IOException {
        this.logFile = new File(file);
        EdasServiceConnectUtils.touch(this.logFile);
        this.writer = new BufferedWriter(new FileWriter(this.logFile, true));
    }

    public void info(String msg) {
        try {
            String date = this.simpleDateFormat.format(new Date());
            this.writer.write(date + " " + msg + EdasServiceConnectUtils.getLineSeperator());
            this.writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void write(String s) {
        try {
            this.writer.write(s);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void close() {
        try {
            this.writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static class EmptyLogger extends EdasServiceConnectLogger {
        public static EmptyLogger instance = new EmptyLogger();

        public static EmptyLogger getInstance() {
            return instance;
        }

        private EmptyLogger() {
        }

        public void setLogFile(String file) throws IOException {
        }

        public void info(String msg) {
        }

        public void close() {
        }

        public void write(String s) {
        }
    }
}
