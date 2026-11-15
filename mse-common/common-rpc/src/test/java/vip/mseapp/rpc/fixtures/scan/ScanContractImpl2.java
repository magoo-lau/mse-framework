package vip.mseapp.rpc.fixtures.scan;

import vip.mseapp.core.annotation.RpcService;

/** 兜底注册候选：默认 bean 名路径；同契约排序在后，ScanContractImpl 注册后本类应被让位跳过 */
@RpcService(ScanContract.class)
public class ScanContractImpl2 implements ScanContract {

    @Override
    public String name() {
        return "scan-impl-2";
    }
}
