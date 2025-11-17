package dev.dong4j.zeka.stack.idea.plugin.workflow.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 类信息
 *
 * @author dong4j
 * @version 1.0.0
 */
public class ClassInfo {
    /** 类名 */
    @NotNull
    public String name = "";
    /** 完整限定类名 */
    @NotNull
    public String qualifiedName = "";
    /** 包名 */
    @NotNull
    public String packageName = "";
    /** 注解列表 */
    @NotNull
    public List<String> annotations = new ArrayList<>();
    /** 文档注释 */
    @Nullable
    public String docComment;
}

