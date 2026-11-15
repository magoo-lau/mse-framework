package vip.mseapp.rpc.fixtures.scan;

import vip.mseapp.core.annotation.RpcService;

/** 兜底注册候选：显式 bean 名（local().value），TreeMap 类名排序先于 ScanContractImpl2 被处理 */
@RpcService(value = ScanContract.class, local = @RpcService.Local("scanFallbackImpl"))
public class ScanContractImpl implements ScanContract {

    @Override
    public String name() {
        return "scan-impl";
    }
}
