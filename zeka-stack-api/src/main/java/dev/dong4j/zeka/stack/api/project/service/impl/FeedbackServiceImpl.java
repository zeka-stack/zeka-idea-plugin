package dev.dong4j.zeka.stack.api.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.dong4j.zeka.kernel.common.api.BaseCodes;
import dev.dong4j.zeka.stack.api.manager.github.client.GitHubIssueClient;
import dev.dong4j.zeka.stack.api.plugin.feedback.dto.FeedbackRequest;
import dev.dong4j.zeka.stack.api.project.dao.FeedbackMapper;
import dev.dong4j.zeka.stack.api.project.entity.converter.FeedbackConverter;
import dev.dong4j.zeka.stack.api.project.entity.dto.FeedbackDTO;
import dev.dong4j.zeka.stack.api.project.entity.form.FeedbackForm;
import dev.dong4j.zeka.stack.api.project.entity.po.Feedback;
import dev.dong4j.zeka.stack.api.project.entity.po.Project;
import dev.dong4j.zeka.stack.api.project.service.FeedbackService;
import dev.dong4j.zeka.stack.api.project.service.ProjectService;
import dev.dong4j.zeka.starter.mybatis.service.impl.BaseServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * <p> 反馈表 服务接口实现类 </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2026.01.22 20:02
 * @since 1.0.0
 */
@Slf4j
@Service
@AllArgsConstructor
public class FeedbackServiceImpl extends BaseServiceImpl<FeedbackMapper, Feedback> implements FeedbackService {
    /** Plugin feedback service for creating GitHub issues */
    private final dev.dong4j.zeka.stack.api.manager.github.FeedbackService pluginFeedbackService;
    /** GitHub Issue client for updating issue state */
    private final GitHubIssueClient githubIssueClient;
    /** Project service for getting project information */
    private final ProjectService projectService;

    /**
     * 根据 ID 获取详细信息
     *
     * @param id 主键
     * @return 实体对象
     * @since 1.0.0
     */
    @Override
    public FeedbackDTO detail(Long id) {
        final Feedback po = this.baseMapper.selectById(id);
        BaseCodes.DATA_ERROR.notNull(po);
        return FeedbackConverter.INSTANCE.p2d(po);
    }

    /**
     * 新增数据
     *
     * @param form 参数实体
     * @since 1.0.0
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(FeedbackForm form) {
        final Feedback po = FeedbackConverter.INSTANCE.f2p(form);
        final int savedCount = this.baseMapper.insertIgnore(po);
        BaseCodes.OPTION_FAILURE.isTrue(savedCount == 1);

        final Long feedbackId = po.getId();

        // 异步调用 GitHub 创建 Issue
        CompletableFuture.runAsync(() -> {
            try {
                FeedbackRequest request = convertToFeedbackRequest(form);
                dev.dong4j.zeka.stack.api.plugin.feedback.dto.FeedbackResponse response = pluginFeedbackService.submitIssue(request);

                // 如果成功创建 Issue，更新 feedback 表的 issues_url 和 issues_id
                if (response.getSuccess() && response.getIssue() != null && response.getIssue().getUrl() != null) {
                    String issuesUrl = response.getIssue().getUrl();
                    Long issuesId = response.getIssue().getId() != null
                                    ? Long.parseLong(response.getIssue().getId())
                                    : null;

                    LambdaUpdateWrapper<Feedback> updateWrapper = new LambdaUpdateWrapper<Feedback>()
                        .set(Feedback::getIssuesUrl, issuesUrl)
                        .eq(Feedback::getId, feedbackId);

                    if (issuesId != null) {
                        updateWrapper.set(Feedback::getIssuesId, issuesId);
                    }

                    this.update(updateWrapper);
                    log.debug("Successfully created GitHub issue for feedback: {}, URL: {}, ID: {}",
                              form.getTitle(), issuesUrl, issuesId);
                } else {
                    log.warn("Failed to create GitHub issue for feedback: {}, response: {}", form.getTitle(), response);
                }
            } catch (Exception e) {
                log.warn("Failed to create GitHub issue for feedback: {}", form.getTitle(), e);
            }
        });
    }

    /**
     * 将 FeedbackForm 转换为 FeedbackRequest
     * <p> 目前先写死一些数据，后续可以根据实际需求调整
     *
     * @param form 反馈表单
     * @return FeedbackRequest
     */
    private FeedbackRequest convertToFeedbackRequest(FeedbackForm form) {
        FeedbackRequest request = new FeedbackRequest();
        request.setTitle(form.getTitle());
        request.setContent(form.getDescription() != null ? form.getDescription() : "");
        // 默认设置为功能建议类型
        request.setType(FeedbackRequest.FeedbackType.FEATURE);

        // 构建用户信息（目前写死）
        FeedbackRequest.UserInfo userInfo = new FeedbackRequest.UserInfo();
        userInfo.setPluginName("IntelliAI WebUI");
        userInfo.setGithubUsername(""); // 可以从认证信息中获取，目前先写死
        request.setUserInfo(userInfo);

        // 构建元数据
        FeedbackRequest.Metadata metadata = new FeedbackRequest.Metadata();
        metadata.setClientId("zeka-idea-webui");
        metadata.setTimestamp(System.currentTimeMillis());
        request.setMetadata(metadata);

        return request;
    }


    /**
     * 更新数据
     *
     * @param form 参数实体
     * @since 1.0.0
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void edit(FeedbackForm form) {
        // 获取旧数据，用于检查状态是否改变
        Feedback oldFeedback = this.getById(form.getId());
        if (oldFeedback == null) {
            BaseCodes.DATA_ERROR.notNull(null);
            return;
        }

        String oldStatus = oldFeedback.getStatus();
        String newStatus = form.getStatus();

        // 更新数据
        final int updatedCount = this.baseMapper.updateById(FeedbackConverter.INSTANCE.f2p(form));
        BaseCodes.OPTION_FAILURE.isTrue(updatedCount == 1);

        // 如果状态改变，且不是新建操作（新建时 issues_url 为空），则同步更新 GitHub Issue 状态
        if (oldStatus != null && !oldStatus.equals(newStatus) && oldFeedback.getIssuesUrl() != null && !oldFeedback.getIssuesUrl().isBlank()) {
            // 异步更新 GitHub Issue 状态
            CompletableFuture.runAsync(() -> {
                try {
                    updateGitHubIssueState(oldFeedback, newStatus);
                } catch (Exception e) {
                    log.warn("Failed to update GitHub issue state for feedback: {}", form.getId(), e);
                }
            });
        }
    }

    /**
     * 更新 GitHub Issue 状态
     * <p>根据 feedback 的状态映射到 GitHub Issue 的状态并更新</p>
     *
     * @param feedback  反馈对象
     * @param newStatus 新状态
     */
    private void updateGitHubIssueState(Feedback feedback, String newStatus) {
        // 容错处理：检查 issues_url 是否为空
        if (feedback.getIssuesUrl() == null || feedback.getIssuesUrl().isBlank()) {
            log.debug("Issues URL is empty for feedback: {}, skip updating GitHub issue", feedback.getId());
            return;
        }

        // 容错处理：检查 project_id 是否为空
        if (feedback.getProjectId() == null) {
            log.warn("Project ID is null for feedback: {}, skip updating GitHub issue", feedback.getId());
            return;
        }

        // 获取项目信息
        Project project = projectService.getById(feedback.getProjectId());
        if (project == null) {
            log.warn("Project not found for projectId: {}, skip updating GitHub issue", feedback.getProjectId());
            return;
        }

        // 容错处理：检查 repos 是否为空
        if (project.getRepos() == null || project.getRepos().isBlank()) {
            log.debug("Repos is empty for project: {}, skip updating GitHub issue", project.getKey());
            return;
        }

        try {
            // 解析 repos URL 获取 owner 和 repo
            RepoInfo repoInfo = parseRepoUrl(project.getRepos());
            if (repoInfo == null) {
                log.warn("Failed to parse repos URL: {}, skip updating GitHub issue", project.getRepos());
                return;
            }

            // 解析 issues_url 获取 issue_number
            Integer issueNumber = parseIssueNumber(feedback.getIssuesUrl());
            if (issueNumber == null) {
                log.warn("Failed to parse issue number from URL: {}, skip updating GitHub issue", feedback.getIssuesUrl());
                return;
            }

            // 映射状态到 GitHub Issue 状态
            String githubState = mapStatusToGitHubState(newStatus);
            if (githubState == null) {
                log.debug("Status {} does not need to update GitHub issue state, skip", newStatus);
                return;
            }

            // 调用 GitHub API 更新 Issue 状态
            githubIssueClient.updateIssueState(repoInfo.owner, repoInfo.repo, issueNumber, githubState);
            log.info("Successfully updated GitHub issue {}/{}#{} to state: {}", repoInfo.owner, repoInfo.repo, issueNumber, githubState);

        } catch (Exception e) {
            log.error("Failed to update GitHub issue state for feedback: {}", feedback.getId(), e);
        }
    }

    /**
     * 解析仓库 URL 获取 owner 和 repo
     * <p>支持格式：https://github.com/owner/repo</p>
     *
     * @param repoUrl 仓库 URL
     * @return RepoInfo 对象，解析失败返回 null
     */
    private RepoInfo parseRepoUrl(String repoUrl) {
        if (repoUrl == null || repoUrl.isBlank()) {
            return null;
        }

        try {
            URI uri = new URI(repoUrl);
            String path = uri.getPath();
            if (path == null || path.isBlank()) {
                return null;
            }

            // 移除开头的斜杠并分割路径
            path = path.startsWith("/") ? path.substring(1) : path;
            String[] parts = path.split("/");
            if (parts.length >= 2) {
                return new RepoInfo(parts[0], parts[1]);
            }
        } catch (Exception e) {
            log.warn("Failed to parse repo URL: {}", repoUrl, e);
        }

        return null;
    }

    /**
     * 解析 issues_url 获取 issue_number
     * <p>支持格式：https://github.com/owner/repo/issues/123</p>
     *
     * @param issuesUrl Issues URL
     * @return Issue 编号，解析失败返回 null
     */
    private Integer parseIssueNumber(String issuesUrl) {
        if (issuesUrl == null || issuesUrl.isBlank()) {
            return null;
        }

        try {
            // 使用正则表达式提取 issue 编号
            // 匹配格式：/issues/123 或 /issues/123#comment-xxx
            Pattern pattern = Pattern.compile("/issues/(\\d+)");
            Matcher matcher = pattern.matcher(issuesUrl);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
        } catch (Exception e) {
            log.warn("Failed to parse issue number from URL: {}", issuesUrl, e);
        }

        return null;
    }

    /**
     * 映射状态到 GitHub Issue 状态
     * <p>状态映射规则：
     * <ul>
     *   <li>Complete -> closed</li>
     *   <li>Open -> open (新建时不处理，因为已经调用 GitHub 的 issues 创建接口了，默认就是开启状态)</li>
     *   <li>In Progress -> open</li>
     *   <li>Planned -> closed</li>
     *   <li>Under Review -> open</li>
     * </ul>
     * </p>
     *
     * @param status 反馈状态
     * @return GitHub Issue 状态（"open" 或 "closed"），如果不需要更新则返回 null
     */
    private String mapStatusToGitHubState(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        return switch (status) {
            case "Complete", "Planned" -> "closed";
            case "Open", "In Progress", "Under Review" -> "open";
            default -> {
                log.debug("Unknown status: {}, skip mapping", status);
                yield null;
            }
        };
    }

    /**
     * 仓库信息内部类
     *
     * @param owner 仓库所有者用户名
     * @param repo  仓库名称
     */
        private record RepoInfo(String owner, String repo) {
        /**
         * 初始化仓库信息对象
         * <p> 通过传入的所有者名称和仓库名称来初始化仓库信息对象
         *
         * @param owner 所有者名称
         * @param repo  仓库名称
         */
        private RepoInfo {
        }
        }

    /**
     * 批量删除数据
     * <p>在删除前，会先关闭对应的 GitHub Issue（如果存在）</p>
     *
     * @param idList 主键集合
     * @since 1.0.0
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeByIds(Collection<?> idList) {
        if (idList == null || idList.isEmpty()) {
            return false;
        }

        // 在删除前，获取所有要删除的 feedback 记录
        @SuppressWarnings("unchecked") List<Feedback> feedbacksToDelete = this.listByIds((Collection<? extends Serializable>) idList);
        if (feedbacksToDelete == null || feedbacksToDelete.isEmpty()) {
            return false;
        }

        // 异步关闭对应的 GitHub Issue
        CompletableFuture.runAsync(() -> {
            for (Feedback feedback : feedbacksToDelete) {
                try {
                    closeGitHubIssue(feedback);
                } catch (Exception e) {
                    log.warn("Failed to close GitHub issue for feedback: {}", feedback.getId(), e);
                }
            }
        });

        // 执行删除操作
        return super.removeByIds(idList);
    }

    /**
     * 关闭 GitHub Issue
     * <p>在删除 feedback 时，关闭对应的 GitHub Issue</p>
     *
     * @param feedback 反馈对象
     */
    private void closeGitHubIssue(Feedback feedback) {
        // 容错处理：检查 issues_url 是否为空
        if (feedback.getIssuesUrl() == null || feedback.getIssuesUrl().isBlank()) {
            log.debug("Issues URL is empty for feedback: {}, skip closing GitHub issue", feedback.getId());
            return;
        }

        // 容错处理：检查 project_id 是否为空
        if (feedback.getProjectId() == null) {
            log.warn("Project ID is null for feedback: {}, skip closing GitHub issue", feedback.getId());
            return;
        }

        // 获取项目信息
        Project project = projectService.getById(feedback.getProjectId());
        if (project == null) {
            log.warn("Project not found for projectId: {}, skip closing GitHub issue", feedback.getProjectId());
            return;
        }

        // 容错处理：检查 repos 是否为空
        if (project.getRepos() == null || project.getRepos().isBlank()) {
            log.debug("Repos is empty for project: {}, skip closing GitHub issue", project.getKey());
            return;
        }

        try {
            // 解析 repos URL 获取 owner 和 repo
            RepoInfo repoInfo = parseRepoUrl(project.getRepos());
            if (repoInfo == null) {
                log.warn("Failed to parse repos URL: {}, skip closing GitHub issue", project.getRepos());
                return;
            }

            // 解析 issues_url 获取 issue_number
            Integer issueNumber = parseIssueNumber(feedback.getIssuesUrl());
            if (issueNumber == null) {
                log.warn("Failed to parse issue number from URL: {}, skip closing GitHub issue", feedback.getIssuesUrl());
                return;
            }

            // 调用 GitHub API 关闭 Issue
            githubIssueClient.updateIssueState(repoInfo.owner, repoInfo.repo, issueNumber, "closed");
            log.info("Successfully closed GitHub issue {}/{}#{} for deleted feedback: {}",
                     repoInfo.owner, repoInfo.repo, issueNumber, feedback.getId());

        } catch (Exception e) {
            log.error("Failed to close GitHub issue for feedback: {}", feedback.getId(), e);
        }
    }

    /**
     * 点赞
     *
     * @param id 主键
     * @since 1.0.0
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void vote(Long id) {
        this.update(new LambdaUpdateWrapper<Feedback>()
                        .setSql("vote_count = vote_count + 1")
                        .eq(Feedback::getId, id));
    }
}


