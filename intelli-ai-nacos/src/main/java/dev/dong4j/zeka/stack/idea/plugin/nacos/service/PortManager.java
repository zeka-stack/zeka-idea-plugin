package dev.dong4j.zeka.stack.idea.plugin.nacos.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 端口管理器
 * 用于检测系统端口占用情况
 *
 * @author dong4j
 * @since 1.0.0
 */
public class PortManager {

    /**
     * 获取 Windows 系统 TCP 端口占用情况
     *
     * @return 已占用的端口集合
     */
    public static Set<Integer> getPortInUseOnWindowsForTcp() {
        BufferedReader reader = null;

        Set<Integer> process;
        try {
            ProcessBuilder pb = new ProcessBuilder("CMD", "/C", "netstat", "-an");
            pb.directory(new File("C:/Windows/System32/"));
            Process proces = pb.start();
            Set<Integer> portInUse = new HashSet();
            reader = new BufferedReader(new InputStreamReader(proces.getInputStream()));
            Pattern pattern = Pattern.compile("TCP\\s+[^\\s]+:([\\d]+)\\s+[^\\s]+\\s+LISTENING\\s*");

            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    portInUse.add(Integer.valueOf(matcher.group(1)));
                }
            }

            Object var13 = portInUse;
            return (Set<Integer>) var13;
        } catch (Exception e) {
            e.printStackTrace();
            process = Collections.emptySet();
        } finally {
            RegistryUtils.close(reader);
        }

        return process;
    }

    /**
     * 检测 Mac 系统 TCP 端口是否可用
     *
     * @param port 端口号
     * @return true 如果端口可用
     */
    public static boolean isPortAvailableOnMacForTcp(int port) {
        try {
            String[] cmd = new String[] {"/bin/sh", "-c", "lsof -i:" + port + " | grep LISTEN"};
            Process process = Runtime.getRuntime().exec(cmd);
            return process.waitFor() == 1;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
