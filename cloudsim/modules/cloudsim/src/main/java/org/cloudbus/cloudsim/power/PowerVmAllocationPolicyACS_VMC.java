package org.cloudbus.cloudsim.power;

import org.cloudbus.cloudsim.HarddriveStorage;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.util.ExecutionTimeMeasurer;

import java.lang.reflect.Array;
import java.util.*;

public class PowerVmAllocationPolicyACS_VMC extends PowerVmAllocationPolicyMigrationAbstract{
    private double higherUtilizationThreshold;
    private double lowerUtilizationThreshold;

    public PowerVmAllocationPolicyACS_VMC(
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


    private boolean isHostUnderUtilized(PowerHost host){
//        addHistoryEntry(host, getHigherUtilizationThreshold());
        double totalRequestedMips = 0;
        for (Vm vm : host.getVmList()) {
            totalRequestedMips += vm.getCurrentRequestedTotalMips();
        }
        double utilization = totalRequestedMips / host.getTotalMips();
        return utilization < getLowerUtilizationThreshold();
    }


    /**
     * Gets the over utilized hosts.
     *
     * @return the over utilized hosts
     */
    protected List<PowerHostUtilizationHistory> getUnderUtilizedHosts() {
        List<PowerHostUtilizationHistory> overUtilizedHosts = new LinkedList<PowerHostUtilizationHistory>();
        for (PowerHostUtilizationHistory host : this.<PowerHostUtilizationHistory> getHostList()) {
            if (isHostUnderUtilized(host)) {
                overUtilizedHosts.add(host);
            }
        }
        return overUtilizedHosts;
    }



    private boolean checkContains(List<? extends Host> hostList, int hostId){
        for(Host host:hostList){
            if(hostId == host.getId()) return true;
        }
        return false;
    }

    double calculateLoss(List<Double> yHatList, List<Double> yList){
        double loss = 0;
        for(int i = 0; i < yList.size(); i ++){
            loss += Math.pow(yList.get(i) - yHatList.get(i), 2);
        }
        return loss;
    }

    double getMean(List<Double> xList){
        double totalX = 0, totalY = 0;
        for(int i = 0; i < xList.size(); i ++){
            totalX += xList.get(i);
        }
        return totalX / xList.size();
    }

    double getBeta1(List<Double> xList, List<Double> yList, double xMean, double yMean){
        double beta_1 = 0;
        double upper = 0;
        double lower = 0;
        for(int i = 0; i < xList.size(); i ++){
            upper += (xList.get(i) - xMean) * (yList.get(i) - yMean);
            lower += (xList.get(i) - xMean);
        }
        return upper/lower;
    }

    boolean predictedOverUtilized(PowerHostUtilizationHistory host){
        int k = 12;
        if(host.getUtilizationHistory().length < 12) return false;
        //initialize
        double beta_0 = Math.random() * 0.1, beta_1 = Math.random() * 0.1;
        List<Double> yHatList = new ArrayList<>();
        List<Double> yList = new ArrayList<>();
        List<Double> xList = new ArrayList<>();

        int currentRound = host.getUtilizationHistory().length;
        for(int i = currentRound - k; i < currentRound - 1; i ++){
            double util_curr = host.getUtilizationHistory()[i];
            double util_next = host.getUtilizationHistory()[i+1];
            double yHat = beta_0 + beta_1 * util_curr;

            xList.add(util_curr);
            yList.add(util_next);
            yHatList.add(yHat);

            //line 8
            double loss = calculateLoss(yHatList, yList);
            double xMean = getMean(xList), yMean = getMean(yList);
            beta_0 = yMean - beta_1 * xMean;
            beta_1 = getBeta1(xList, yList, xMean, yMean);
        }

        double totalRequestedMips = 0;
        for (Vm vm : host.getVmList()) {
            totalRequestedMips += vm.getCurrentRequestedTotalMips();
        }
        double utilization = totalRequestedMips / host.getTotalMips();

        return beta_0 + beta_1 * utilization > getHigherUtilizationThreshold();
    }

    /**
     * Optimize allocation of the VMs according to current utilization.
     *
     * @param vmList the vm list
     *
     * @return the array list< hash map< string, object>>
     */
    @Override
    public List<Map<String, Object>> optimizeAllocation(List<? extends Vm> vmList) {
        ExecutionTimeMeasurer.start("optimizeAllocationTotal");
        ExecutionTimeMeasurer.start("optimizeAllocationHostSelection");

        //store tuples
        //vm_tomigrate, List<>host_destination
        Map<Integer, List<Integer> > allTuples = new HashMap<>();
        Map<Integer, Vm> allVmsForACS = new HashMap<>();
        Map<Integer, PowerHost> allDestinationHostsForACS = new HashMap<>();

        List<Integer> overUtilizedHostId = new LinkedList<>();
        //vm's old host
        Map<Integer, Integer> mapForOldMap = new HashMap<>();
        saveAllocation();

        for(PowerHostUtilizationHistory sourceHost:this.<PowerHostUtilizationHistory>getHostList()) {
            //get source host(over-utilized)

            if (isHostOverUtilized(sourceHost)) {
                overUtilizedHostId.add(sourceHost.getId());
                //get vms to be migrated from current source host(over-utilized)
                while (true) {
                    Vm vm = getVmSelectionPolicy().getVmToMigrate(sourceHost);
                    if (vm == null) {
                        break;
                    }
                    int vmIndex = 0;
                    for (int i = 0; i < sourceHost.getVmList().size(); i++) {
                        if (sourceHost.getVmList().get(i).getId() == vm.getId()) {
                            vmIndex = i;
                            break;
                        }
                    }
                    vm = sourceHost.getVmList().get(vmIndex);
                    allVmsForACS.put(vm.getId(), vm);
//                    mapForOldMap.put(vm.getId(), sourceHost.getId());
                    mapForOldMap.put(vm.getId(), vm.getHost().getId()); //store old host for this vm
                    sourceHost.vmDestroy(vm);
                    if (!isHostOverUtilized(sourceHost)) {
                        break;
                    }
                }
            }
//            } else if(predictedOverUtilized(sourceHost)){
//                //predict utilization by linear regression
//                overUtilizedHostId.add(sourceHost.getId());
//            }
            else {
                //add a destination host
                allDestinationHostsForACS.put(sourceHost.getId(), sourceHost);

                //get source host(under-utilized)
                if (isHostUnderUtilized(sourceHost)) {
                    //get vms to be migrated from current source host(under-utilized)
                    //remove all vms on under-utilized host
                    while (sourceHost.getVmList().size() > 0) {
                        Vm vm = sourceHost.getVmList().get(0);
                        allVmsForACS.put(vm.getId(), vm);   //store old host for this vm
//                        mapForOldMap.put(vm.getId(), sourceHost.getId());
                        mapForOldMap.put(vm.getId(), vm.getHost().getId());
                        sourceHost.vmDestroy(vm);
                    }
                }
            }
        }
        // set potential host for each vm to be migrated
        for(Map.Entry<Integer,Vm> entry:allVmsForACS.entrySet()){
            Vm vm = entry.getValue();
            List<Integer> destHostList = new LinkedList<>();
            for(PowerHostUtilizationHistory destHost:this.<PowerHostUtilizationHistory>getHostList()){
                //destination host can not be an overutilized one
                if( !destHost.isSuitableForVm(vm) ||
                    overUtilizedHostId.contains(destHost.getId()) ) continue;
                destHostList.add(destHost.getId());
            }
            if(destHostList.size() == 0){
                System.out.println("NO suitable host");
            }
            allTuples.put(vm.getId(), destHostList);
        }


        //ACS to get the best migration map
        ACS_VMC acs_vmc = new ACS_VMC(allTuples, allDestinationHostsForACS, allVmsForACS, mapForOldMap, getHigherUtilizationThreshold());
        //ACO之后返回一个migrate的map，<vmId, destHostId>
        Map<Integer, Integer> migrateMapInID = acs_vmc.VMC();

        //getMigrationMap(migrateMapInID);
        List<Map<String, Object>> migrationMap =
                constructMigrationMap(migrateMapInID, mapForOldMap, allVmsForACS);

        restoreAllocation();
        return migrationMap;
    }

    private List<Map<String, Object>> constructMigrationMap(
            Map<Integer, Integer> migrateMapInID,
            Map<Integer, Integer> mapForOldMap,
            Map<Integer, Vm> allVmsForACS){

        List<Map<String, Object>> migrationMap = new LinkedList<>();

        //search all vms in the input vmList of ACS
        for(int vmIdForAll:allVmsForACS.keySet()){
            int destinationHostId = migrateMapInID.get(vmIdForAll);
            //if not migrated, put the vm back to its source host
            if(destinationHostId == mapForOldMap.get(vmIdForAll)){
//                getHostList().get(destinationHostId).vmCreate(allVmsForACS.get(vmIdForAll));
                continue;
            }
            //if migrated, set the destination host for vm
            else {
                Map<String, Object> singleMigration = new HashMap<>();
                singleMigration.put("vm", allVmsForACS.get(vmIdForAll));
                singleMigration.put("host", getHostList().get(destinationHostId));
                //create vm on the selected host
                getHostList().get(destinationHostId).vmCreate(allVmsForACS.get(vmIdForAll));
                migrationMap.add(singleMigration);
            }
        }
        return migrationMap;
    }
    private void setHigherUtilizationThreshold(double higherUtilizationThreshold) {
        this.higherUtilizationThreshold = higherUtilizationThreshold;
    }

    private void setLowerUtilizationThreshold(double lowerUtilizationThreshold) {
        this.lowerUtilizationThreshold = lowerUtilizationThreshold;
    }

    private double getHigherUtilizationThreshold() {
        return higherUtilizationThreshold;
    }

    private double getLowerUtilizationThreshold() {
        return lowerUtilizationThreshold;
    }
}
