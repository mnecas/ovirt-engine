package org.ovirt.engine.core.vdsbroker.monitoring;

import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.utils.Pair;
import org.ovirt.engine.core.common.vdscommands.VDSCommandType;
import org.ovirt.engine.core.common.vdscommands.VDSReturnValue;
import org.ovirt.engine.core.common.vdscommands.VdsIdAndVdsVDSCommandParametersBase;
import org.ovirt.engine.core.vdsbroker.VdsManager;
import org.ovirt.engine.core.vdsbroker.vdsbroker.entities.VmInternalData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VmsStatisticsFetcher extends VmsListFetcher {

    private static final Logger log = LoggerFactory.getLogger(VmsStatisticsFetcher.class);
    private StringBuilder logBuilder;

    public VmsStatisticsFetcher(VdsManager vdsManager) {
        super(vdsManager);
    }

    @Override
    protected VDSReturnValue poll() {
        return getResourceManager().runVdsCommand(
                VDSCommandType.GetAllVmStats,
                new VdsIdAndVdsVDSCommandParametersBase(vdsManager.getCopyVds()));
    }

    @Override
    protected void onFetchVms() {
        logBuilder = new StringBuilder(String.format("Poll %s:", vdsManager.getVdsId()));
        super.onFetchVms();
        logBuilder.append(String.format("(%d VMs)", changedVms.size()));
        log.info(logBuilder.toString());
    }

    @Override
    protected void gatherChangedVms(VM dbVm, VmInternalData vdsmVm) {
        changedVms.add(new Pair<>(dbVm, vdsmVm));
        logBuilder.append(String.format(" %s:%s",
                vdsmVm.getVmDynamic().getId().toString().substring(0, 8),
                vdsmVm.getVmDynamic().getStatus()));
    }
}
