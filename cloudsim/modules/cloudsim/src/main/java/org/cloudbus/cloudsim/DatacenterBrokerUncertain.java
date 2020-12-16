package org.cloudbus.cloudsim;

import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.lists.VmList;
import org.cloudbus.cloudsim.power.PowerDatacenterBroker;

import java.util.ArrayList;
import java.util.List;

public class DatacenterBrokerUncertain extends PowerDatacenterBroker {
    /**
     * Instantiates a new PowerDatacenterBroker.
     *
     * @param name the name of the broker
     * @throws Exception the exception
     */
    public DatacenterBrokerUncertain(String name) throws Exception {
        super(name);
    }

    /**
     * 负责将cloudlet交给datacenter，之后再进行分发
     */
    public void submitCloudlets(){
        for (Cloudlet cloudlet : getCloudletList()) {
//            sendNow(getVmsToDatacentersMap().get(vm.getId()), CloudSimTags.CLOUDLET_SUBMIT, cloudlet);
            //send过去怎么接收啊
            sendNow(getDatacenterIdsList().get(0), CloudSimTags.CLOUDLET_SUBMIT, cloudlet);
        }
    }
}
