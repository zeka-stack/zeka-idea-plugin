create table if not exists event (
    id               bigint unsigned not null auto_increment comment '主键',
    device_id        varchar(64)     not null comment '设备 ID',
    client_timestamp bigint          not null comment '客户端上报时间戳(毫秒)',
    project_name     varchar(255)             default '' comment '项目名称',
    plugin_id        varchar(32)     not null comment '插件标识',
    event_type       varchar(64)     not null comment '事件类型',
    provider         varchar(64)              default '' comment 'AI 服务商',
    model            varchar(128)             default '' comment '模型名称',
    token_count      bigint          not null default 0 comment '总 token 数',
    result_status    varchar(32)              default '' comment '结果状态',
    latency_ms       bigint          not null default 0 comment '耗时(毫秒)',
    input_token      bigint          not null default 0 comment '输入 token 数',
    output_token     bigint          not null default 0 comment '输出 token 数',
    user_action      varchar(64)              default null comment '触发入口',
    create_time      timestamp       not null default current_timestamp comment '创建时间 (公共字段)',
    received_time    timestamp       not null default current_timestamp comment '服务端接收时间',
    primary key (id),
    key idx_device_id (device_id),
    key idx_client_timestamp (client_timestamp),
    key idx_event_type (event_type),
    key idx_provider (provider),
    key idx_project_name (project_name),
    key idx_created_at (create_time)
) engine = InnoDB
    default charset = utf8mb4 comment '统计事件表';


create table if not exists event_stat_30m (
    id                 bigint unsigned auto_increment comment '主键' primary key,
    bucket_start       datetime    not null comment '统计窗口开始时间（30分钟）',
    bucket_end         datetime    not null comment '统计窗口结束时间（30分钟）',

    device_id          varchar(64) not null comment '设备 ID',
    project_name       varchar(255)         default '' null comment '项目名称',
    plugin_id          varchar(32) not null comment '插件标识',
    event_type         varchar(64) not null comment '事件类型',
    provider           varchar(64)          default '' null comment 'AI 服务商',
    model              varchar(128)         default '' null comment '模型名称',
    user_action        varchar(64)          default '' null comment '触发入口',

    total_count        bigint      not null default 0 comment '总次数',
    success_count      bigint      not null default 0 comment '成功次数',
    failed_count       bigint      not null default 0 comment '失败次数',

    token_total        bigint      not null default 0 comment '总 token 数',
    input_token_total  bigint      not null default 0 comment '输入 token 总数',
    output_token_total bigint      not null default 0 comment '输出 token 总数',

    latency_total_ms   bigint      not null default 0 comment '总耗时(毫秒)',
    latency_avg_ms     bigint      not null default 0 comment '平均耗时(毫秒)',
    latency_max_ms     bigint      not null default 0 comment '最大耗时(毫秒)',
    latency_min_ms     bigint      not null default 0 comment '最小耗时(毫秒)',

    create_time        timestamp            default current_timestamp not null comment '创建时间 (公共字段)',
    update_time        timestamp            default current_timestamp not null on update current_timestamp comment '更新时间',

    unique key uq_bucket_dim (bucket_start, bucket_end, device_id, project_name, plugin_id, event_type, provider, model, user_action
        ),
    key idx_bucket (bucket_start),
    key idx_device (device_id),
    key idx_project (project_name),
    key idx_event_type (event_type),
    key idx_provider (provider),
    key idx_user_action (user_action)
) comment '统计事件 30 分钟聚合表' charset = utf8mb4;


create table if not exists user_account (
    id              bigint unsigned not null auto_increment comment '主键',
    github_id       bigint unsigned not null comment 'GitHub 用户 ID',
    github_login    varchar(64)     not null comment 'GitHub 登录名',
    github_name     varchar(128)             default '' comment 'GitHub 显示名',
    avatar_url      varchar(512)             default '' comment '头像 URL',
    email           varchar(255)             default '' comment '邮箱(可选)',
    device_id       varchar(64)     not null comment '设备 ID',
    last_login_time timestamp                default current_timestamp comment '最后登录时间',
    create_time     timestamp       not null default current_timestamp comment '创建时间',
    update_time     timestamp       not null default current_timestamp on update current_timestamp comment '更新时间',
    primary key (id),
    unique key uq_github_id (github_id),
    unique key uq_github_login (github_login),
    unique key uq_device_id (device_id),
    key idx_device_id (device_id)
) engine = InnoDB
    default charset = utf8mb4 comment 'GitHub 账号绑定表';

alter table user_account
    add role varchar(8) default '' null after email;

-- auto-generated definition
create table user_session (
    id            bigint unsigned auto_increment comment '主键' primary key,
    user_id       bigint unsigned                     not null comment '用户 ID',
    session_token varchar(64)                         not null comment '会话 token',
    expires_at    timestamp                           not null comment '过期时间',
    create_time   timestamp default current_timestamp not null comment '创建时间',
    update_time   timestamp default current_timestamp not null on update current_timestamp comment '更新时间',
    constraint uq_session_token unique (session_token)
) comment '登录会话表' charset = utf8mb4;

create index idx_expires_at on user_session (expires_at);

create index idx_user_id on user_session (user_id);

-- 1. 项目/插件表 (用于前端下拉选择)
create table if not exists project (
    id          bigint unsigned auto_increment primary key comment '主键',
    `key`       varchar(64)                            not null comment '项目唯一标识 (如 zeka-idea-plugin)',
    name        varchar(128)                           not null comment '项目显示名称',
    description varchar(512) default '' comment '项目描述',
    icon        varchar(255) default '' comment '图标标识(前端映射 lucide 图标)',
    sort_order  int          default 0 comment '排序权重',
    status      tinyint      default 1 comment '状态: 1-启用, 0-禁用',
    create_time timestamp    default current_timestamp not null comment '创建时间',
    update_time timestamp    default current_timestamp not null on update current_timestamp comment '更新时间',
    unique key uq_project_key (`key`)
) comment '项目表' charset = utf8mb4;

-- 2. 需求反馈表 (匿名提交)
create table if not exists feedback (
    id            bigint unsigned auto_increment primary key comment '主键',
    project_id    bigint unsigned                       not null comment '关联的项目ID',
    title         varchar(255)                          not null comment '标题',
    description   text                                  null comment '详细描述',
    status        varchar(32) default 'Open' comment '状态: Open, In Progress, Complete, Planned, Under Review',
    priority      varchar(32) default 'Medium' comment '优先级: Low, Medium, High',
    vote_count    int         default 0 comment '点赞数',
    comment_count int         default 0 comment '评论数',
    create_time   timestamp   default current_timestamp not null comment '创建时间',
    update_time   timestamp   default current_timestamp not null on update current_timestamp comment '更新时间',
    key idx_project_status (project_id, status),
    key idx_project_title (project_id, title)
) comment '反馈表' charset = utf8mb4;

alter table feedback
    add issues_url varchar(128) default '' null comment 'github issues url' after description;

alter table feedback
    add issues_id bigint unsigned default null comment 'github issues id' after issues_url;

alter table project
    add repos varchar(512) default '' null comment 'GitHub 仓库 URL (如 https://github.com/zeka-stack/zeka-idea-plugin)' after description;

-- 3. 需求评论表 (匿名评论)
create table if not exists feedback_comment (
    id          bigint unsigned auto_increment primary key comment '主键',
    feedback_id bigint unsigned                     not null comment '关联的需求ID',
    content     text                                not null comment '评论内容',
    create_time timestamp default current_timestamp not null comment '创建时间',
    key idx_feedback_id (feedback_id)
) comment '反馈评论表' charset = utf8mb4;

-- 初始化数据
insert into project (`key`, name, description, icon, sort_order)
values ('zeka-idea-plugin', 'Zeka Idea Plugin', 'ZekaStack 框架支撑插件', 'Bot', 10),
       ('blen-kernel', '核心工具包', 'Common utilities and tools', 'FileText', 20);





