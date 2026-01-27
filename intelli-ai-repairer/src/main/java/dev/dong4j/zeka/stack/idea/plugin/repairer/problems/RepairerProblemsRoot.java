package dev.dong4j.zeka.stack.idea.plugin.repairer.problems;

import com.intellij.analysis.problemsView.Problem;
import com.intellij.analysis.problemsView.toolWindow.FileNode;
import com.intellij.analysis.problemsView.toolWindow.Node;
import com.intellij.analysis.problemsView.toolWindow.ProblemsViewPanel;
import com.intellij.analysis.problemsView.toolWindow.Root;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.tree.TreePath;

import dev.dong4j.zeka.stack.idea.plugin.repairer.service.ViolationCache;
import dev.dong4j.zeka.stack.idea.plugin.repairer.service.ViolationCacheListener;
import dev.dong4j.zeka.stack.idea.plugin.repairer.violation.CodeViolation;

/**
 * Root node for the IntelliAI Repairer Problems tab.
 */
public final class RepairerProblemsRoot extends Root implements ViolationCacheListener {
    private static final String NO_MODULE = "No Module";
    private static final String DEFAULT_PACKAGE = "<default>";

    private final Project project;
    private final ViolationCache cache;
    private final RepairerProblemsProvider provider;
    private final AtomicLong version = new AtomicLong();
    private volatile ProblemsIndex cachedIndex;

    public RepairerProblemsRoot(@NotNull ProblemsViewPanel panel, @NotNull Project project) {
        super(panel);
        this.project = project;
        this.cache = ViolationCache.getInstance(project);
        this.provider = new RepairerProblemsProvider(project);
        this.cache.addListener(this);
    }

    @Override
    public @NotNull Collection<Node> getChildren() {
        return getIndex().moduleNodes;
    }

    @Override
    public @NotNull Collection<Node> getChildren(@NotNull FileNode fileNode) {
        List<Problem> problems = getIndex().problemsByFile.getOrDefault(fileNode.getVirtualFile(), List.of());
        return getNodesForProblems(fileNode, problems);
    }

    @Override
    public @NotNull Collection<Problem> getChildren(@NotNull VirtualFile file) {
        return getIndex().problemsByFile.getOrDefault(file, List.of());
    }

    @Override
    public void violationsUpdated(@NotNull List<CodeViolation> violations) {
        version.incrementAndGet();
        ApplicationManager.getApplication().invokeLater(() -> {
            if (project.isDisposed()) {
                return;
            }
            structureChanged(new TreePath(this));
        });
    }

    @Override
    public void dispose() {
        cache.removeListener(this);
        super.dispose();
    }

    private ProblemsIndex getIndex() {
        long currentVersion = version.get();
        ProblemsIndex index = cachedIndex;
        if (index != null && index.version == currentVersion) {
            return index;
        }
        ProblemsIndex rebuilt = buildIndex(currentVersion);
        cachedIndex = rebuilt;
        return rebuilt;
    }

    private ProblemsIndex buildIndex(long currentVersion) {
        Map<VirtualFile, List<Problem>> problemsByFile = new HashMap<>();
        Map<String, ModuleBucket> modules = new TreeMap<>(String::compareToIgnoreCase);

        for (CodeViolation violation : cache.getAll()) {
            if (violation.filePath == null || violation.filePath.isBlank()) {
                continue;
            }
            VirtualFile file = LocalFileSystem.getInstance().findFileByPath(violation.filePath);
            if (file == null) {
                continue;
            }
            RepairerProblem problem = new RepairerProblem(provider, violation, file);
            problemsByFile.computeIfAbsent(file, ignored -> new ArrayList<>()).add(problem);

            Module module = ModuleUtilCore.findModuleForFile(file, project);
            String moduleName = module != null ? module.getName() : NO_MODULE;
            ModuleBucket moduleBucket = modules.computeIfAbsent(moduleName, name -> new ModuleBucket(module));

            String packageName = resolvePackageName(file);
            PackageBucket packageBucket = moduleBucket.packages.computeIfAbsent(packageName, ignored -> new PackageBucket());
            packageBucket.files.computeIfAbsent(file, ignored -> new ArrayList<>()).add(problem);
        }

        for (List<Problem> problems : problemsByFile.values()) {
            problems.sort(Comparator.comparingInt(RepairerProblemsRoot::problemLine)
                                    .thenComparingInt(RepairerProblemsRoot::problemColumn));
        }

        List<Node> moduleNodes = new ArrayList<>();
        for (Map.Entry<String, ModuleBucket> moduleEntry : modules.entrySet()) {
            List<Node> packageNodes = new ArrayList<>();
            RepairerModuleNode moduleNode = new RepairerModuleNode(project, moduleEntry.getKey(),
                                                                   moduleEntry.getValue().module, packageNodes);
            moduleNodes.add(moduleNode);

            List<String> packageNames = new ArrayList<>(moduleEntry.getValue().packages.keySet());
            packageNames.sort(String::compareToIgnoreCase);
            for (String packageName : packageNames) {
                List<Node> fileNodes = new ArrayList<>();
                RepairerPackageNode packageNode = new RepairerPackageNode(moduleNode, packageName, fileNodes);
                packageNodes.add(packageNode);

                Map<VirtualFile, List<Problem>> files = moduleEntry.getValue().packages.get(packageName).files;
                List<VirtualFile> sortedFiles = new ArrayList<>(files.keySet());
                sortedFiles.sort(Comparator.comparing(VirtualFile::getPath, String::compareToIgnoreCase));
                for (VirtualFile file : sortedFiles) {
                    fileNodes.add(new FileNode(packageNode, file));
                }
            }
        }

        return new ProblemsIndex(currentVersion, moduleNodes, problemsByFile);
    }

    private String resolvePackageName(@NotNull VirtualFile file) {
        return ReadAction.compute(() -> {
            PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
            if (psiFile instanceof PsiJavaFile) {
                String packageName = ((PsiJavaFile) psiFile).getPackageName();
                return packageName == null || packageName.isBlank() ? DEFAULT_PACKAGE : packageName;
            }
            VirtualFile parent = file.getParent();
            if (parent == null) {
                return DEFAULT_PACKAGE;
            }
            return parent.getName();
        });
    }

    private static int problemLine(@NotNull Problem problem) {
        if (problem instanceof RepairerProblem) {
            return ((RepairerProblem) problem).getLine();
        }
        return 0;
    }

    private static int problemColumn(@NotNull Problem problem) {
        if (problem instanceof RepairerProblem) {
            return ((RepairerProblem) problem).getColumn();
        }
        return 0;
    }

    private static final class ModuleBucket {
        private final Module module;
        private final Map<String, PackageBucket> packages = new TreeMap<>(String::compareToIgnoreCase);

        private ModuleBucket(@Nullable Module module) {
            this.module = module;
        }
    }

    private static final class PackageBucket {
        private final Map<VirtualFile, List<Problem>> files = new HashMap<>();
    }

    private static final class ProblemsIndex {
        private final long version;
        private final List<Node> moduleNodes;
        private final Map<VirtualFile, List<Problem>> problemsByFile;

        private ProblemsIndex(long version,
                              @NotNull List<Node> moduleNodes,
                              @NotNull Map<VirtualFile, List<Problem>> problemsByFile) {
            this.version = version;
            this.moduleNodes = Collections.unmodifiableList(moduleNodes);
            this.problemsByFile = problemsByFile;
        }
    }
}
