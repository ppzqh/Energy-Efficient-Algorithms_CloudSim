package org.cloudbus.cloudsim.power;
import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;

import java.util.ArrayList;
import java.util.List;

public class GlobalBrokerUncertain extends SimEntity {
    private int delay;
    private String name;
    private DatacenterBroker broker = null;
    private int interval;
    private int numCloudlet;

    private static int submittedCount = 0;

    private List<Cloudlet> cloudletList = null;
    private List<Cloudlet> urgentCloudletList = new ArrayList<>();
    private List<Cloudlet> waitingCloudletList = new ArrayList<>();

    private List<Double> laxity = new ArrayList<>();

    public GlobalBrokerUncertain(DatacenterBroker broker, String name, int num, int delay) {
        super(name);
        setBroker(broker);
        setDelay(delay);
        setNumCloudlet(num);

        int interval = 50;
        setInterval(interval);

        setCloudletList(Helper.createCloudlet(getBroker().getId(), getNumCloudlet(), delay));

        for(int i = 0; i < getNumCloudlet(); i ++){
            laxity.add(Math.random()*100);
        }

        urgentCloudletList = new ArrayList<>();
        waitingCloudletList = new ArrayList<>();
    }

    @Override
    public void processEvent(SimEvent ev) {
        switch (ev.getTag()) {
            case CloudSimTags.PRS:
                PRS();
                sendNow(getId(), CloudSimTags.Search_Queue);
                break;

            case CloudSimTags.Search_Queue:
                scheduleUrgentTask();
                updateWaitingQueue();
                if(getBroker().getCloudletReceivedList().size() < getCloudletList().size())
                    send(getId(), getInterval(), CloudSimTags.Search_Queue);
                break;

            default:
                System.out.println(ev.getTag());
                Log.printLine(getName() + ": unknown event type");
                break;
        }
    }

    private void PRS(){
        for(Cloudlet cl:getCloudletList()){
            if(isUrgentTask(cl)) getUrgentCloudletList().add(cl);
            else {
                getWaitingCloudletList().add(cl);
                if(inequality5()){
                    Vm vm = ScaleUpComputingResource();
                    if(vm != null){
                        getBroker().getVmList().add(vm);
                        //这里不直接分配vm了，而是将cl加入到urgent中
                        getUrgentCloudletList().add(cl);
                    }
                }
            }
        }

    }

    private void updateWaitingQueue(){
        List<Cloudlet> toUrgent = new ArrayList<>();
        for(Cloudlet cl:getWaitingCloudletList()){
            if(isUrgentTask(cl)){
                getUrgentCloudletList().add(cl);
                toUrgent.add(cl);
            }
        }
        getWaitingCloudletList().removeAll(toUrgent);
    }

    private void scheduleUrgentTask(){
        getBroker().submitCloudletList(getUrgentCloudletList());
        getBroker().submitCloudlets();
        submittedCount += getUrgentCloudletList().size();

        getUrgentCloudletList().clear();
        CloudSim.resumeSimulation();
    }


    private Vm ScaleUpComputingResource(){
        return null;
    }

    //为VM找一个task放在waitingQueue中
    private void searchWaitingTask(Vm vm){
        for(Cloudlet task : getWaitingCloudletList()){
//            //需要判断时间关系
//            double estimateExecTimeLower = getExecLower(vm, task);
//            double estimateExecTimeUpper = getExecUpper(vm, task);
//            double startTimeUpper = 0;
//            double startTimeLower = 0;
//            double estimateFinishTimeUpper = getUpper(startTimeLower, startTimeUpper, estimateExecTimeLower, estimateExecTimeUpper);
//            double DDL = 0;
//            if(inTime(DDL, estimateFinishTimeUpper)){
//                task2waitingList(vm, task);
//                waitingQueue.remove(task);
//                break;
//            }

            getWaitingCloudletList().remove(task);
        }
    }

    //不等式5是否满足，满足的话进行scaleUp
    private boolean inequality5(){
        return false;
    }

    //判断是否是urgent task
    private boolean isUrgentTask(Cloudlet task){
//        return task.getCloudletId() % 2 == 0;
//        return Math.random() > 0.5;
        int laxityIndex = task.getCloudletId() % 50;
        double laxity = this.laxity.get(laxityIndex);
        return CloudSim.clock() - getDelay() > laxity;
//        return true;
    }


    @Override
    public void startEntity() {
//        Log.printLine(super.getName()+" is starting...");
        schedule(getId(), delay+100, CloudSimTags.PRS);
    }

    @Override
    public void shutdownEntity() {
    }

    private void setCloudletList(List<Cloudlet> list){
        this.cloudletList = list;
    }

    private void setDelay(int delay){
        this.delay = delay;
    }

    private void setBroker(DatacenterBroker broker){
        this.broker = broker;
    }

    private void setInterval(int interval){
        this.interval = interval;
    }

    private void setNumCloudlet(int num){
        this.numCloudlet = num;
    }

    private int getNumCloudlet(){
        return this.numCloudlet;
    }

    private int getInterval(){
        return this.interval;
    }

    private List<Cloudlet> getCloudletList(){
        return this.cloudletList;
    }

    private List<Cloudlet> getUrgentCloudletList(){
        return this.urgentCloudletList;
    }

    private List<Cloudlet> getWaitingCloudletList(){
        return this.waitingCloudletList;
    }


    private int getDelay(){
        return this.delay;
    }

    private DatacenterBroker getBroker(){
        return this.broker;
    }
}
