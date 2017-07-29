package org.ovirt.engine.core.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.ovirt.engine.core.common.businessentities.ExternalStatus;
import org.ovirt.engine.core.common.businessentities.NonOperationalReason;
import org.ovirt.engine.core.common.businessentities.VDSStatus;
import org.ovirt.engine.core.common.businessentities.VdsDynamic;
import org.ovirt.engine.core.common.businessentities.VdsStatic;
import org.ovirt.engine.core.common.businessentities.network.DnsResolverConfiguration;
import org.ovirt.engine.core.common.businessentities.network.NameServer;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.RpmVersion;
import org.ovirt.engine.core.dao.network.DnsResolverConfigurationDao;
import org.ovirt.engine.core.utils.RandomUtils;

public class VdsDynamicDaoTest extends BaseDaoTestCase {
    private VdsDynamicDao dao;
    private VdsStaticDao staticDao;
    private DnsResolverConfigurationDao dnsResolverConfigurationDao;
    private VdsStatisticsDao statisticsDao;
    private VdsStatic existingVds;
    private VdsStatic newStaticVds;
    private VdsDynamic newDynamicVds;

    private static final List<Guid> HOSTS_WITH_UP_STATUS =
            Arrays.asList(FixturesTool.VDS_RHEL6_NFS_SPM,
                    FixturesTool.HOST_ID,
                    FixturesTool.HOST_WITH_NO_VFS_CONFIGS_ID,
                    FixturesTool.GLUSTER_BRICK_SERVER1,
                    FixturesTool.VDS_GLUSTER_SERVER2);

    @Override
    public void setUp() throws Exception {
        super.setUp();

        dao = dbFacade.getVdsDynamicDao();
        staticDao = dbFacade.getVdsStaticDao();
        dnsResolverConfigurationDao = dbFacade.getDnsResolverConfigurationDao();
        statisticsDao = dbFacade.getVdsStatisticsDao();
        existingVds = staticDao.get(FixturesTool.VDS_GLUSTER_SERVER2);

        newStaticVds = new VdsStatic();
        newStaticVds.setHostName("farkle.redhat.com");
        newStaticVds.setClusterId(existingVds.getClusterId());
        newDynamicVds = new VdsDynamic();
        newDynamicVds.setReportedDnsResolverConfiguration(new DnsResolverConfiguration());
        newDynamicVds.getReportedDnsResolverConfiguration().setNameServers(
                Collections.singletonList(new NameServer("1.1.1.1")));
    }

    /**
     * Ensures that an invalid id returns null.
     */
    @Test
    public void testGetWithInvalidId() {
        VdsDynamic result = dao.get(Guid.newGuid());

        assertNull(result);
    }

    /**
     * Ensures that the right object is returned.
     */
    @Test
    public void testGet() {
        VdsDynamic result = dao.get(existingVds.getId());

        assertNotNull(result);
        assertEquals(existingVds.getId(), result.getId());
    }

    /**
     * Ensures saving a VDS instance works.
     */
    @Test
    public void testSave() {
        staticDao.save(newStaticVds);
        newDynamicVds.setId(newStaticVds.getId());
        newDynamicVds.setUpdateAvailable(true);
        dao.save(newDynamicVds);

        VdsStatic staticResult = staticDao.get(newStaticVds.getId());
        VdsDynamic dynamicResult = dao.get(newDynamicVds.getId());

        assertNotNull(staticResult);
        assertEquals(newStaticVds, staticResult);
        assertNotNull(dynamicResult);
        assertEquals(newDynamicVds, dynamicResult);
        assertEquals(newDynamicVds.isUpdateAvailable(), dynamicResult.isUpdateAvailable());
        assertEquals(newDynamicVds.getReportedDnsResolverConfiguration(),
                dynamicResult.getReportedDnsResolverConfiguration());
    }

    /**
     * Ensures removing a VDS instance works.
     */
    @Test
    public void testRemove() {
        dao.remove(existingVds.getId());
        statisticsDao.remove(existingVds.getId());
        staticDao.remove(existingVds.getId());

        VdsStatic resultStatic = staticDao.get(existingVds.getId());
        assertNull(resultStatic);
        VdsDynamic resultDynamic = dao.get(existingVds.getId());
        assertNull(resultDynamic);
        assertNull(dnsResolverConfigurationDao.get(FixturesTool.EXISTING_DNS_RESOLVER_CONFIGURATION));
    }

    @Test
    public void testUpdateStatus() {
        VdsDynamic before = dao.get(existingVds.getId());
        before.setStatus(VDSStatus.Down);
        dao.updateStatus(before.getId(), before.getStatus());
        VdsDynamic after = dao.get(existingVds.getId());
        assertEquals(before, after);
    }

    @Test
    public void testUpdateStatusAndReasons() {
        VdsDynamic before = dao.get(existingVds.getId());
        before.setStatus(RandomUtils.instance().nextEnum(VDSStatus.class));
        before.setNonOperationalReason(RandomUtils.instance().nextEnum(NonOperationalReason.class));
        before.setMaintenanceReason(RandomUtils.instance().nextString(50));
        dao.updateStatusAndReasons(before);
        VdsDynamic after = dao.get(existingVds.getId());
        assertEquals(before, after);
        assertEquals(before.getStatus(), after.getStatus());
        assertEquals(before.getNonOperationalReason(), after.getNonOperationalReason());
        assertEquals(before.getMaintenanceReason(), after.getMaintenanceReason());
    }

    @Test
    public void testUpdateHostExternalStatus() {
        VdsDynamic before = dao.get(existingVds.getId());
        before.setExternalStatus(ExternalStatus.Error);
        dao.updateExternalStatus(before.getId(), before.getExternalStatus());
        VdsDynamic after = dao.get(existingVds.getId());
        assertEquals(before.getExternalStatus(), after.getExternalStatus());
    }

    @Test
    public void testUpdateNetConfigDirty() {
        VdsDynamic before = dao.get(existingVds.getId());
        Boolean netConfigDirty = before.getNetConfigDirty();
        netConfigDirty = Boolean.FALSE.equals(netConfigDirty);
        before.setNetConfigDirty(netConfigDirty);
        dao.updateNetConfigDirty(before.getId(), netConfigDirty);
        VdsDynamic after = dao.get(existingVds.getId());
        assertEquals(before, after);
    }

    @Test
    public void testGlusterVersion() {
        RpmVersion glusterVersion = new RpmVersion("glusterfs-3.4.0.34.1u2rhs-1.el6rhs");
        VdsDynamic before = dao.get(existingVds.getId());
        before.setGlusterVersion(glusterVersion);
        dao.update(before);
        VdsDynamic after = dao.get(existingVds.getId());
        assertEquals(glusterVersion, after.getGlusterVersion());
    }

    @Test
    public void testUpdateLibrbdVersion() {
        RpmVersion librbdVersion = new RpmVersion("librbd1-0.80.9-1.fc21.x86_64_updated");
        VdsDynamic before = dao.get(existingVds.getId());
        assertNotEquals(librbdVersion, before.getLibrbdVersion());
        before.setLibrbdVersion(librbdVersion);
        dao.update(before);
        VdsDynamic after = dao.get(existingVds.getId());
        assertEquals(librbdVersion, after.getLibrbdVersion());
    }

    @Test
    public void testGetIdsOfHostsWithStatus() {
        List<Guid> hostIds = dao.getIdsOfHostsWithStatus(VDSStatus.Up);
        assertEquals(5, hostIds.size());
        assertTrue(hostIds.containsAll(HOSTS_WITH_UP_STATUS));

        hostIds = dao.getIdsOfHostsWithStatus(VDSStatus.Maintenance);
        assertEquals(0, hostIds.size());
    }

    @Test
    public void testUpdateAvailableUpdates() {
        VdsDynamic before = dao.get(existingVds.getId());
        assertFalse(before.isUpdateAvailable());
        before.setUpdateAvailable(true);
        dao.updateUpdateAvailable(before.getId(), before.isUpdateAvailable());
        VdsDynamic after = dao.get(existingVds.getId());
        assertEquals(before.isUpdateAvailable(), after.isUpdateAvailable());
    }

    @Test
    public void testCheckIfExistsHostWithStatusInCluster() {
        Guid clusterId = existingVds.getClusterId();
        VdsDynamic existingVdsDynamic = dao.get(existingVds.getId());
        VDSStatus existingHostStatus = existingVdsDynamic.getStatus();

        boolean resultBeforeUpdateStatus = dao.checkIfExistsHostWithStatusInCluster(clusterId, existingHostStatus);
        assertTrue(resultBeforeUpdateStatus);

        updateStatusForAllHostsInCluster(clusterId, VDSStatus.Connecting);
        boolean resultAfterUpdateStatus = dao.checkIfExistsHostWithStatusInCluster(clusterId, existingHostStatus);
        assertFalse(resultAfterUpdateStatus);
    }

    private void updateStatusForAllHostsInCluster(Guid clusterId, VDSStatus hostStatus) {
        for (VdsStatic host : staticDao.getAllForCluster(clusterId)) {
            dao.updateStatus(host.getId(), hostStatus);
        }
    }

    @Test
    public void testUpdateDnsResolverConfiguration() {
        VdsDynamic before = dao.get(existingVds.getId());
        before.getReportedDnsResolverConfiguration().getNameServers().add(new NameServer("1.1.1.1"));
        dao.update(before);
        VdsDynamic after = dao.get(existingVds.getId());
        assertEquals(before.getReportedDnsResolverConfiguration(), after.getReportedDnsResolverConfiguration());
    }
}
