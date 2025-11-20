package dev.dong4j.zeka.stack.idea.plugin.nacos.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 注册中心服务连接日志记录器
 * 用于记录本地注册中心启动和运行过程中的日志信息
 *
 * @author dong4j
 * @since 1.0.0
 */
public class RegistryLogger {
    
    private File logFile;
    private BufferedWriter writer;
    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public RegistryLogger(String file) throws IOException {
        this.logFile = new File(file);
        RegistryUtils.touch(this.logFile);
        this.writer = new BufferedWriter(new FileWriter(this.logFile, true));
    }

    public RegistryLogger() {
    }

    public void setLogFile(String file) throws IOException {
        this.logFile = new File(file);
        RegistryUtils.touch(this.logFile);
        this.writer = new BufferedWriter(new FileWriter(this.logFile, true));
    }

    public void info(String msg) {
        try {
            String date = this.simpleDateFormat.format(new Date());
            this.writer.write(date + " " + msg + RegistryUtils.getLineSeperator());
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

    /**
     * 空日志记录器
     * 用于不需要记录日志的场景
     */
    public static class EmptyLogger extends RegistryLogger {
        
        public static EmptyLogger instance = new EmptyLogger();

        public static EmptyLogger getInstance() {
            return instance;
        }

        private EmptyLogger() {
        }

        @Override
        public void setLogFile(String file) throws IOException {
        }

        @Override
        public void info(String msg) {
        }

        @Override
        public void close() {
        }

        @Override
        public void write(String s) {
        }
    }
}
