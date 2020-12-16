/*
Algorithm from the paper
A learning automata-based algorithm for energy and SLA efficient consolidation of virtual machines in cloud data centers
 */
package org.cloudbus.cloudsim.power;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class PowerVmAllocationLearningAutomata extends PowerVmAllocationPolicyMigrationAbstract {
    private double higherUtilizationThreshold = 0.8;
    private double lowerUtilizationThreshold = 0.2;

    public PowerVmAllocationLearningAutomata(
            List<? extends Host> hostList,
            PowerVmSelectionPolicy vmSelectionPolicy,
            double utilizationThreshold,
            double lowerUtilizationThreshold) {
        super(hostList, vmSelectionPolicy);

        setUtilizationThreshold(utilizationThreshold);
        setLowerUtilizationThreshold(lowerUtilizationThreshold);
    }

    protected double getPowerAfterAllocation(PowerHost host, Vm vm) {
        double power = 0;
        try {
            double util = getMaxUtilizationAfterAllocation(host, vm);
            if(util > 1) util = 1;
            power = host.getPowerModel().getPower(util);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
        return power;
    }

    /**
     * Checks if a host is over utilized, based on CPU usage.
     *
     * @param host the host
     * @return true, if the host is over utilized; false otherwise
     */
    @Override
    protected boolean isHostOverUtilized(PowerHost host) {
        addHistoryEntry(host, getUtilizationThreshold());
        double totalRequestedMips = 0;
        double predictedUtilization = 0;

        for (PowerVmLearningAutomata vm : host.<PowerVmLearningAutomata>getVmList()) {
            totalRequestedMips += vm.getCurrentRequestedTotalMips();
            //均值,标准差
            double avg = vm.getUtilizationMean() / host.getTotalMips();//vm.getMips();
            //TODO: check
//            double standardDeviation = Math.sqrt(vm.getUtilizationVariance()) / vm.getMips();//host.getTotalMips();
            double standardDeviation = Math.sqrt(vm.getUtilizationVariance()) / host.getTotalMips();
            if(vm.getAction().equals("ASC")){
                predictedUtilization += (avg + standardDeviation);
            }
            else if(vm.getAction().equals("DESC")){
                predictedUtilization -= (avg - standardDeviation);
            }
        }
        double utilization = totalRequestedMips / host.getTotalMips();
        //满足两个条件 statisfy both
        return utilization > getUtilizationThreshold() && predictedUtilization > 1 - getUtilizationThreshold();
    }

    /**
     * Sets the utilization threshold.
     *
     * @param utilizationThreshold the new utilization threshold
     */
    protected void setUtilizationThreshold(double utilizationThreshold) {
        this.higherUtilizationThreshold = utilizationThreshold;
    }

    protected void setLowerUtilizationThreshold(double lowerUtilizationThreshold) {
        this.lowerUtilizationThreshold = lowerUtilizationThreshold;
    }

    /**
     * Gets the utilization threshold.
     *
     * @return the utilization threshold
     */
    protected double getUtilizationThreshold() {
        return higherUtilizationThreshold;
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
            if (utilization > 0 && utilization <= getLowerUtilizationThreshold())
                return host;
        }
        return underUtilizedHost;
    }

}
