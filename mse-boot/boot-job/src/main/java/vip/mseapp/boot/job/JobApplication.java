package vip.mseapp.boot.job;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 薄调度中心微服务启动器（mse-job）：只做「什么时候该跑」——到期判定后置 trigger_pending + 写踢键，
 * 由既有链路接管执行（各服务 sync 乐观锁抢占恰一实例 → 本地执行池跑体 → 运行态上报）。
 *
 * <p>服务侧配套：任务所属服务配 {@code mse.scheduling.mode=central} 关本地排程（灰度按服务切换，
 * local 缺省不变）；mse-job 多实例可直接部署（置位幂等 + 执行端乐观锁单飞）。
 */
@SpringBootApplication
public class JobApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobApplication.class, args);
    }
}
