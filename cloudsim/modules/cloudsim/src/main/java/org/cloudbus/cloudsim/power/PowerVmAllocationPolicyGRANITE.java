package org.cloudbus.cloudsim.power;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;

import java.util.List;
import java.util.Set;

public class PowerVmAllocationPolicyGRANITE extends PowerVmAllocationPolicyMigrationAbstract {

    private double higherUtilizationThreshold;
    private double lowerUtilizationThreshold;
    private double thermalResistance = 0.34;
    private double heatCapacity = 340;
    private double CPUcriticalTemperature = 343.15;//343.15K = 70;
    private double c = 0.0018;
    private double T_0 = 310;
    private double T0_sup = 290;
    private double R_k = 12.8;

    public PowerVmAllocationPolicyGRANITE(
            List<? extends Host> hostList,
            PowerVmSelectionPolicy vmSelectionPolicy,
            double higherUtilizationTreshold,
            double lowerUtilizationThreshold) {

        super(hostList, vmSelectionPolicy);
        setHigherUtilizationThreshold(higherUtilizationTreshold);
        setLowerUtilizationThreshold(lowerUtilizationThreshold);
    }

    //迁移之后是否过载
    protected boolean isHostOverUtilized(PowerHost host, PowerVm vmToMigrate) {
        double totalRequestedMips = 0;
        for (Vm vm : host.getVmList()) {
            totalRequestedMips += vm.getCurrentRequestedTotalMips();
        }
        //将要迁移的vm的mips减掉
        totalRequestedMips -= vmToMigrate.getCurrentRequestedTotalMips();

        double utilization = totalRequestedMips / host.getTotalMips();
        return utilization > getHighUtilizationThreshold();
    }

    protected boolean isHostOverUtilized(PowerHost host) {
        addHistoryEntry(host, getHighUtilizationThreshold());
        double totalRequestedMips = 0;
        for (Vm vm : host.getVmList()) {
            totalRequestedMips += vm.getCurrentRequestedTotalMips();
        }
        double utilization = totalRequestedMips / host.getTotalMips();
        return utilization > getHighUtilizationThreshold();
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

    //重载这个函数：修改选择目的主机的算法（将新的或被迁移的VM放到哪个server上）
    public PowerHost findHostForVm(Vm vm, Set<? extends Host> excludedHosts) {
        double minPower = Double.MAX_VALUE;
        PowerHost allocatedHost = null;

        for (PowerHost host : this.<PowerHost> getHostList()) {
            if (excludedHosts.contains(host)) {
                continue;
            }
            if (host.isSuitableForVm(vm)) {
                if (getUtilizationOfCpuMips(host) != 0 && isHostOverUtilizedAfterAllocation(host, vm)) {
//                    double CPUafter = getMaxUtilizationAfterAllocation(host, vm);
                    continue;
                }
//                double TcpuAfter = getCPUtemperature(CloudSim.clock());
//                if(TcpuAfter > getCPUcriticalTemperature())
//                    continue;

                try {
                    //选择一个能耗变化最小的host
                    double powerAfterAllocation = getPowerAfterAllocation(host, vm);
                    if (powerAfterAllocation != -1) {
                        double powerDiff = powerAfterAllocation - host.getPower();
                        if (powerDiff < minPower) {
                            minPower = powerDiff;
                            allocatedHost = host;
                        }
                    }
                } catch (Exception e) {
                }
            }
        }
        return allocatedHost;
    }

    protected double getPowerAfterAllocation(PowerHost host, Vm vm) {
        double power = 0;
        try {
            power = host.getPowerModel().getPower(getMaxUtilizationAfterAllocation(host, vm));
            //添加cooling的能耗
            power += getCoolingPower(power);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
        return power;
    }

    private double getCoolingPower(double energy){
        //TODO T_sup怎么得到
        return energy * 1000 / getCoP(T0_sup);
    }

    private double getCoP(double T_sup){
        return 0.0068 * T_sup * T_sup + 0.008 * T_sup + 0.458;
    }

    private double getStableT_inlet(){
        return T0_sup + R_k;
    }

    private double getT_inlet(){
        return getStableT_inlet();
    }
    private double getPR(){
        return getThermalResistance() * getHeatCapacity();
    }
    private double getCPUtemperature(double time){
        double e = 2.718;
        double t = getPR() + getT_inlet();
        double cooler =  (T_0 - getPR() - getT_inlet()) * Math.pow(e,-time/getPR());
        t -= cooler;
        return t;
    }

    private double getThermalResistance(){
        return thermalResistance;
    }

    private double getHeatCapacity(){
        return heatCapacity;
    }

    private double getCPUcriticalTemperature(){
        return CPUcriticalTemperature;
    }

    private void setHigherUtilizationThreshold(double higher){
        this.higherUtilizationThreshold = higher;
    }

    private void setLowerUtilizationThreshold(double lower){
        this.lowerUtilizationThreshold = lower;
    }

    protected double getLowerUtilizationThreshold() {
        return lowerUtilizationThreshold;
    }

    protected double getHighUtilizationThreshold(){
        return higherUtilizationThreshold;
    }
}
