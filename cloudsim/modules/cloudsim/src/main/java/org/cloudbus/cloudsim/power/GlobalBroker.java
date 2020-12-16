package org.cloudbus.cloudsim.power;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;

import javax.xml.crypto.Data;
import java.util.ArrayList;
import java.util.List;

public class GlobalBroker extends SimEntity {
    private List<Vm> vmList;
    private List<Cloudlet> cloudletList;
    private DatacenterBroker broker = null;
    private String name;
    private int delay;
    public GlobalBroker(DatacenterBroker broker, String name, int delay) {
        super(name);
        setName(name);
        setBroker(broker);
        this.delay = delay;
    }

    @Override
    public void processEvent(SimEvent ev) {
        switch (ev.getTag()) {
            case CloudSimTags.Submit_New_Cloudlet:
                Log.printLine("new cloudlets are coming...");
                getBroker().submitCloudletList(Helper.createCloudlet(getBroker().getId(), 50, delay));
                getBroker().submitCloudlets();
                CloudSim.resumeSimulation();
                break;

            default:
                System.out.println(ev.getTag());
                Log.printLine(getName() + ": unknown event type");
                break;
        }
    }

    @Override
    public void startEntity() {
//        Log.printLine(super.getName()+" is starting...");
        schedule(getId(), delay, CloudSimTags.Submit_New_Cloudlet);
    }

    @Override
    public void shutdownEntity() {
    }

    public List<Vm> getVmList() {
        return vmList;
    }

    protected void setVmList(List<Vm> vmList) {
        this.vmList = vmList;
    }

    public List<Cloudlet> getCloudletList() {
        return cloudletList;
    }

    protected void setCloudletList(List<Cloudlet> cloudletList) {
        this.cloudletList = cloudletList;
    }

    public DatacenterBroker getBroker() {
        return this.broker;
    }

    protected void setBroker(DatacenterBroker broker) {
        this.broker = broker;
    }

    protected void setName(String name){this.name = name;}
}