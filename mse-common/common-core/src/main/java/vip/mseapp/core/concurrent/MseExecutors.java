package vip.mseapp.core.concurrent;

import io.micrometer.context.ContextExecutorService;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 统一命名线程池工厂：自建后台线程池一律经本工厂创建，收口四类约定
 * （规范背景见 docs/tech/context-propagation.md——新池禁裸建是结构性约定，不是可选项）：
 *
 * <ul>
 *   <li><b>命名</b>：线程名 {@code <prefix>-N}，日志/线程 dump 可归属；</li>
 *   <li><b>有界</b>：队列容量必填（防无界堆积拖垮内存）；缺省拒绝策略 CallerRuns（后台任务背压，
 *       提交线程自己跑，不静默丢任务）；</li>
 *   <li><b>上下文</b>：缺省经 {@code ContextExecutorService.wrap} 包装——池内任务自动恢复提交线程的
 *       租户上下文（{@link vip.mseapp.core.context.TenantContextPropagation} 注册的租户号；
 *       IGNORE 刻意不传播）。任务内显式 runAs 仍建议保留，wrap 是结构化兜底而非替代；
 *       <b>刻意无租户</b>的池（跨租户扫描、调度中心类按行 runAs 的执行域）用
 *       {@code contextWrap(false)} 并在注释说明理由，防租户上下文随提交线程泄漏进扫描任务；</li>
 *   <li><b>生命周期</b>：daemon 缺省 true（不阻 JVM 退出；需要优雅排空的池显式 false + 自行
 *       shutdown 钩子/@PreDestroy）。</li>
 * </ul>
 *
 * <p>纯 JDK + micrometer context-propagation（common-core 无 Spring 依赖）；
 * Spring {@code ThreadPoolTaskExecutor}（@Async 池）不适用本工厂——用 TaskDecorator
 * {@code ContextSnapshot.captureAll().wrap(task)} 范式（先例：mse-open OpenWebConfig）。
 * 生命周期关闭由使用方自持（@PreDestroy / shutdown 钩子），工厂不代管注册。
 */
public final class MseExecutors {

    private MseExecutors() {
    }

    /** @param prefix 线程名前缀（如 mse-ai-kb-parse；实际线程名 {@code <prefix>-N}） */
    public static Builder builder(String prefix) {
        return new Builder(prefix);
    }

    /** 有界命名线程池构建器（容量/拒绝策略/上下文包装见类注释四约定） */
    public static final class Builder {

        private final String prefix;
        private int core = 1;
        private int max = 1;
        private int queueCapacity = -1;
        private boolean daemon = true;
        private boolean contextWrap = true;
        private RejectedExecutionHandler rejection = new ThreadPoolExecutor.CallerRunsPolicy();

        private Builder(String prefix) {
            if (prefix == null || prefix.isBlank()) {
                throw new IllegalArgumentException("线程名前缀不能为空（日志/线程 dump 归属依赖命名）");
            }
            this.prefix = prefix.trim();
        }

        /** 核心线程数（缺省 1：后台任务多数为低频串行） */
        public Builder core(int corePoolSize) {
            this.core = corePoolSize;
            return this;
        }

        /** 最大线程数（缺省同 core：后台任务极少需要弹性扩容，需要时显式声明） */
        public Builder max(int maxPoolSize) {
            this.max = maxPoolSize;
            return this;
        }

        /** 有界队列容量（<b>必填</b>：未显式声明 build() 拒绝创建，防无界堆积） */
        public Builder queueCapacity(int capacity) {
            this.queueCapacity = capacity;
            return this;
        }

        /** daemon 线程（缺省 true；需优雅排空的池显式 false 并自行管理关闭） */
        public Builder daemon(boolean daemonThreads) {
            this.daemon = daemonThreads;
            return this;
        }

        /**
         * 是否包装租户上下文传播（缺省 true）。false 仅限「刻意无租户」执行域
         * （跨租户扫描/调度中心类任务内按行 runAs），须在调用处注释理由。
         */
        public Builder contextWrap(boolean wrap) {
            this.contextWrap = wrap;
            return this;
        }

        /** 拒绝策略（缺省 CallerRunsPolicy 背压；换成 AbortPolicy/DiscardPolicy 须注释理由） */
        public Builder rejection(RejectedExecutionHandler handler) {
            this.rejection = handler == null ? new ThreadPoolExecutor.CallerRunsPolicy() : handler;
            return this;
        }

        public ExecutorService build() {
            if (queueCapacity <= 0) {
                throw new IllegalArgumentException(prefix + ": 有界队列容量必填（builder.queueCapacity，防无界堆积）");
            }
            if (max < core) {
                throw new IllegalArgumentException(prefix + ": max(" + max + ") 不得小于 core(" + core + ")");
            }
            ThreadFactory factory = new NamedFactory(prefix, daemon);
            ThreadPoolExecutor raw = new ThreadPoolExecutor(core, max, 60L, java.util.concurrent.TimeUnit.SECONDS,
                    new ArrayBlockingQueue<>(queueCapacity), factory, rejection);
            raw.allowCoreThreadTimeOut(true);
            return contextWrap ? ContextExecutorService.wrap(raw) : raw;
        }
    }

    /** 命名线程工厂：{@code <prefix>-N}（复用已有空闲线程不新建） */
    private static final class NamedFactory implements ThreadFactory {

        private final String prefix;
        private final boolean daemon;
        private final AtomicInteger seq = new AtomicInteger();

        private NamedFactory(String prefix, boolean daemon) {
            this.prefix = prefix;
            this.daemon = daemon;
        }

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, prefix + "-" + seq.incrementAndGet());
            thread.setDaemon(daemon);
            return thread;
        }
    }

    /** 便捷式：单线程有界池（低频串行后台任务的常见形态，如管理动作执行器） */
    public static ExecutorService singleThread(String prefix, int queueCapacity) {
        return builder(prefix).core(1).max(1).queueCapacity(queueCapacity).build();
    }

    /** 便捷式：固定大小有界池（并行处理型后台任务；容量语义同 {@link Builder#queueCapacity}） */
    public static ExecutorService fixedPool(String prefix, int threads, int queueCapacity) {
        return builder(prefix).core(threads).max(threads).queueCapacity(queueCapacity).build();
    }

    /** 便捷式：单线程有界池 + 刻意无租户（跨租户扫描/按行 runAs 的执行域，注释理由后使用） */
    public static ExecutorService singleThreadNoContext(String prefix, int queueCapacity) {
        return builder(prefix).core(1).max(1).queueCapacity(queueCapacity).contextWrap(false).build();
    }

    /** 便捷式：固定大小有界池 + 刻意无租户（同上语义） */
    public static ExecutorService fixedPoolNoContext(String prefix, int threads, int queueCapacity) {
        return builder(prefix).core(threads).max(threads).queueCapacity(queueCapacity).contextWrap(false).build();
    }
}
