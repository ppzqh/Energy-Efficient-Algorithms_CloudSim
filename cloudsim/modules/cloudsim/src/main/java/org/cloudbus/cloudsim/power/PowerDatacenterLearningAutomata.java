package org.cloudbus.cloudsim.power;

import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.VmAllocationPolicy;

import java.util.List;

public class PowerDatacenterLearningAutomata extends PowerDatacenter{
    public PowerDatacenterLearningAutomata(
        String name,
        DatacenterCharacteristics characteristics,
        VmAllocationPolicy vmAllocationPolicy,
        List<Storage> storageList,
        double schedulingInterval) throws Exception {
        super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval);
    }

    private void updateLA(){
        for(PowerHost host: this.<PowerHost>getHostList()){
            for(PowerVmLearningAutomata vm : host.<PowerVmLearningAutomata>getVmList()){
                //先更新策略: update policy
                //再选择新的action: update action
                vm.updateLA();
                vm.updateAction();
            }
        }
    }

    protected void updateCloudletProcessing(){
        super.updateCloudletProcessing();
        updateLA();
    }
//    protected double updateCloudetProcessingWithoutSchedulingFutureEventsForce(){
//        updateLA();
//        return super.updateCloudetProcessingWithoutSchedulingFutureEventsForce();
//    }

}
