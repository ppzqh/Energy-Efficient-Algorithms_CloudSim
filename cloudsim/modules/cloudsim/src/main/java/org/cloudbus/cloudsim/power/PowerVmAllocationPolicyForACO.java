package org.cloudbus.cloudsim.power;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Vm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PowerVmAllocationPolicyForACO extends PowerVmAllocationPolicyMBFD {
    private Map<Integer, Integer> currentS = new HashMap<>();
    public PowerVmAllocationPolicyForACO(List<? extends Host> list,
                                         PowerVmSelectionPolicy vmSelectionPolicy,
                                         double utilizationThreshold,
                                         double lowerUtilizationThreshold,
                                         Map<Integer, Integer> currentS) {
        super(list, vmSelectionPolicy, utilizationThreshold, lowerUtilizationThreshold);
        //TODO：尝试将currentS赋给vmTable，即分配好
        setCurrentS(currentS);
    }

    private void setCurrentS(Map<Integer, Integer> S){
        getCurrentS().putAll(S);
    }

    private Map<Integer, Integer> getCurrentS(){
        return currentS;
    }

    /*
    提前更新好vmTable后，直接分配
     */
    @Override
    public boolean allocateHostForVm(Vm vm) {
        if(vm.getHost() == null){
            try {
                int hostId = getCurrentS().get(vm.getId());
                return allocateHostForVm(vm, getHostList().get(hostId));
            }catch (Exception a){
                System.out.println(vm.getUid());
                return false;
            }
        }
        else{
            return allocateHostForVm(vm, findHostForVm(vm));
        }
    }

//
//    public boolean allocateHostForVm(Vm vm, Host host){
//        Host host = getVmTable().get(vm.getUid());
//        return false;
//    }

//    /*
//    Nothing
//     */
//    @Override
//    public List<Map<String, Object>> optimizeAllocation(List<? extends Vm> vmList){
//        return null;
//    }
}
