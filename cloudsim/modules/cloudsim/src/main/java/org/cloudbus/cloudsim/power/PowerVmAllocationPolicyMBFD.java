/*
 MM: 修改原本的方案，换成固定的下阈值
*/

package org.cloudbus.cloudsim.power;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Vm;

public class PowerVmAllocationPolicyMBFD extends PowerVmAllocationPolicyMigrationAbstract {

    private double utilizationThreshold = 0.8;
    private double lowerUtilizationThreshold = 0.2;
    /**
     * Instantiates a new PowerVmAllocationPolicyMigrationStaticThreshold.
     *
     * @param hostList the host list
     * @param vmSelectionPolicy the vm selection policy
     * @param utilizationThreshold the utilization threshold
     */
    public PowerVmAllocationPolicyMBFD(
            List<? extends Host> hostList,
            PowerVmSelectionPolicy vmSelectionPolicy,
            double higherUtilizationThreshold,
            double lowerUtilizationThreshold) {
        super(hostList, vmSelectionPolicy);
        setHigherUtilizationThreshold(higherUtilizationThreshold);
        setLowerUtilizationThreshold(lowerUtilizationThreshold);
    }

    /**
     * Checks if a host is over utilized, based on CPU usage.
     *
     * @param host the host
     * @return true, if the host is over utilized; false otherwise
     */
    @Override
    protected boolean isHostOverUtilized(PowerHost host) {
        addHistoryEntry(host, getHigherUtilizationThreshold());
        double totalRequestedMips = 0;
        for (Vm vm : host.getVmList()) {
            totalRequestedMips += vm.getCurrentRequestedTotalMips();
        }
        double utilization = totalRequestedMips / host.getTotalMips();
        return utilization > getHigherUtilizationThreshold();
    }

    /**
     * Sets the utilization threshold.
     *
     * @param utilizationThreshold the new utilization threshold
     */
    protected void setHigherUtilizationThreshold(double utilizationThreshold) {
        this.utilizationThreshold = utilizationThreshold;
    }

    protected void setLowerUtilizationThreshold(double lowerUtilizationThreshold) {
        this.lowerUtilizationThreshold = lowerUtilizationThreshold;
    }

    /**
     * Gets the utilization threshold.
     *
     * @return the utilization threshold
     */
    protected double getHigherUtilizationThreshold() {
        return utilizationThreshold;
    }

    protected double getLowerUtilizationThreshold() {
        return lowerUtilizationThreshold;
    }

    /**
     * 修改原本的方案，换成固定的下阈值
     * @param excludedHosts the excluded hosts
     * @return the most under utilized host
     */
    protected PowerHost getUnderUtilizedHost(Set<? extends Host> excludedHosts) {
        PowerHost underUtilizedHost = null;
        for (PowerHost host : this.<PowerHost> getHostList()) {
            if (excludedHosts.contains(host)) {
                continue;
            }
            double utilization = host.getUtilizationOfCpu();
            if (utilization > 0 && utilization < getLowerUtilizationThreshold())
                return host;
        }
        return underUtilizedHost;
    }

}
