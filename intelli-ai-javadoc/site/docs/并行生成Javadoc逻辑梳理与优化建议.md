# 并行生成 JavaDoc 逻辑梳理与优化建议

## 架构速览（dev.dong4j.zeka.stack.idea.plugin.task.parallel）

- `ParallelTaskExecutor`: 按任务/服务商数量分配线程池，启动 `ParallelTaskWorker` 并等待收尾。
- `TaskDispatcher`: 轮询按文件分组的 `FileTaskQueue`，文件内串行、文件间并行；文件为空后退化为 `RetryQueue`。
- `ParallelTaskWorker`: 拉取任务、构造 AI 请求、写回文档，处理超时/异常/重试/进度统计。
- `ProviderManager`: 维护服务商状态与其线程池，遇到 429 时销毁对应线程池。
- `RetryQueue`/`RetryableTask`: 保存失败任务及重试计数（上限 3）。
- `DocumentationInserter`: 在 EDT 写回文档的适配接口。

## 现有流程要点

1. `TaskDispatcher` 将任务按 `filePath` 分组，文件内任务进入同一 `FileTaskQueue` 并配有 `ReentrantLock`。
2. 轮询取任务：先尝试获取文件锁、弹出任务；所有文件耗尽后转向 `RetryQueue`。
3. Worker 通过 `CompletableFuture.get(10s)` 加超时包装 AI 调用；插入文档在 EDT 执行；更新统计与进度。
4. 429 会标记服务商为 `RATE_LIMITED` 并关闭其线程池；其他异常进入重试队列（最多 3 次）。
5. `waitForCompletion` 轮询队列为空→等待 2s 缓冲→再轮询→关闭线程池并 await termination。

## 已发现的风险与可优化点

- **P0 锁未完全释放导致文件队列长时间占用**
  `TaskDispatcher.getNextTask()` 先 `tryLock()`，随后 `pollTask()` 内再次 `lock()`，`TaskWrapper.releaseLock()`
  仅解一次。持有计数递增后不会归零，同文件队列将持续被同一线程占用，其他线程无法获取锁，严重影响并行度并可能导致队列“假空转”。
- **P0 跳过已有注释的任务被当作失败重试**
  `executeTask()` 检测到已有 Javadoc 时标记为 `SKIPPED` 并抛出 `AIServiceException(UNKNOWN)`，后续在 `handleAIServiceException`
  中被当作普通错误，重新入重试队列，直至“失败”。跳过逻辑应直接完成/计数，而非走失败路径。
- **P1 超时包装线程池使用不当**
  `CompletableFuture.supplyAsync()` 使用 `ForkJoinPool.commonPool`，与提供商线程池解耦：
    - 实际执行线程数可能高于分配的 provider 并发；
    - `future.cancel(true)` 不一定能打断底层 AI 请求；
    - executor shutdown 后，commonPool 仍可继续执行任务。
      建议复用 provider 线程池或自建可控的 scheduler，并在 AIService 侧支持可取消请求。
- **P1 429/错误后的任务去向不明确**
  429 仅关闭该服务商线程池并记失败，未将正在处理或等待的任务转交其他服务商；若唯一可用服务商触发 429，整体会早停，用户缺少“未处理/可重试”提示。需设计降级/回滚策略（如切换可用
  provider、回灌重试队列并提示）。
- **P2 队列空检查未考虑在途任务**
  `waitForQueuesEmpty()` 仅看队列是否为空，未跟踪 in-flight 计数。若请求耗时 > 2s 缓冲，可能在任务仍执行时进入 executor shutdown 流程。建议维护全局
  in-flight 计数或使用 `CountDownLatch`/`CompletionService` 来阻塞到真正完成。
- **P3 重试包装冗余与信息丢失**
  首次失败分支同时创建 `RetryableTask retryableTask = new RetryableTask(task)`，又调用 `addToRetryQueue(task, error)` 新建实例；前者未入队且
  retryCount 始终从 1 开始，浪费对象且未记录首次错误。可以复用单个实例并补充首次错误信息。
- **P3 线程命名/日志颗粒度**
  线程名带 `System.currentTimeMillis()`，批量线程同名难以 grep；建议使用递增序号。错误/重试日志可附带 retryCount/elapsed，用于排障。

## 优化路径

1. **修正文件锁释放**：`getNextTask` 只在获取到任务时记录“已持锁”状态，并确保完成后完全释放；或让 `pollTask` 在已持锁情况下不再
   lock。补充针对同文件多任务的回归测试（多线程 + 多重任务）。
2. **调整跳过分支**：跳过时直接返回并更新统计，避免抛异常；或在异常分支识别 `SKIPPED` 状态后立即视为完成。
3. **收敛异步执行模型**：使用 provider 专属 executor 运行 AI 调用；超时采用 `Future#get(timeout)` 或 `CompletableFuture.orTimeout` 搭配协作式取消，避免
   commonPool 外溢。
4. **设计限流/错误的回退策略**：可选模式：
    - 429 后将未完成任务放回重试队列并尝试其他可用 provider；
    - 若无可用 provider，及时中断并抛出汇总错误，提醒用户重试。
      补充统计：限流次数、被丢弃任务列表。
5. **补强完成条件**：新增 in-flight 计数器或 `CompletionService`，直至所有已派发任务完成/失败后再关闭线程池，避免因请求长尾而过早 shutdown。
6. **重试记录与日志**：合并 `RetryableTask` 创建路径，记录首次错误；统一线程名/日志上下文，便于调试并行问题。

> 当前进展：已修复文件队列锁重复获取（单文件多服务商无法轮转）、跳过任务进入重试、请求执行落在 commonPool、429
> 不回灌重试、重试对象重复创建、线程命名不易排查，以及收尾时未计入在途任务的问题。
