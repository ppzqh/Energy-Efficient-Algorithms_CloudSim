package org.cloudbus.cloudsim.power;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.core.predicates.PredicateType;
import org.cloudbus.cloudsim.lists.CloudletList;
import org.cloudbus.cloudsim.power.PowerDatacenter;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerHostUtilizationHistory;
import org.cloudbus.cloudsim.power.PowerVm;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PowerDatacenterUncertain extends PowerDatacenter {
    //用于储存所有的cloudlets
    private List<Cloudlet> allCloudlets;
    private List<Cloudlet> waitingQueue;
    private List<Cloudlet> urgentTaskQueue;
    private static int count = 0;
    private static int prsCount = 0;
    /*
    将datacenter broker加进来
     */
    public PowerDatacenterUncertain(String name, DatacenterCharacteristics characteristics, VmAllocationPolicy vmAllocationPolicy,
                                    List<Storage> storageList, double schedulingInterval) throws Exception {
        super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval);
        allCloudlets = new ArrayList<>();
        waitingQueue = new ArrayList<>();
        urgentTaskQueue = new ArrayList<>();
    }

//    private void setDatacenterBroker(DatacenterBroker broker){
//        this.datacenterBroker = broker;
//    }
//
//    private DatacenterBroker getDatacenterBroker(){
//        return datacenterBroker;
//    }

    private List<Cloudlet> getAllCloudlets(){
        return allCloudlets;
    }

    private List<Cloudlet> getUrgentTaskQueue(){
        return urgentTaskQueue;
    }

    private List<Cloudlet> getWaitingQueue(){
        return waitingQueue;
    }

    //判断是否是urgent task
    private boolean isUrgentTask(Cloudlet task){
        return true;
    }

    //不等式5是否满足，满足的话进行scaleUp
    private boolean inequality5(Cloudlet task){
        return false;
    }

    //创建特定类型的VM
    private Vm createVM(int vmType){
        int brokerId = 0;
        return new PowerVm(
                getVmList().size(),
                brokerId,
                Constants.VM_MIPS[vmType],
                Constants.VM_PES[vmType],
                Constants.VM_RAM[vmType],
                Constants.VM_BW,
                Constants.VM_SIZE,
                1,
                "Xen",
                new CloudletSchedulerDynamicWorkload(Constants.VM_MIPS[vmType], Constants.VM_PES[vmType]),
                Constants.SCHEDULING_INTERVAL);
    }

    private PowerHost createHost(int hostType){
        List<Pe> peList = new ArrayList<Pe>();
        for (int j = 0; j < Constants.HOST_PES[hostType]; j++) {
            peList.add(new Pe(j, new PeProvisionerSimple(Constants.HOST_MIPS[hostType])));
        }
        return new PowerHostUtilizationHistory(
                getHostList().size(),
                new RamProvisionerSimple(Constants.HOST_RAM[hostType]),
                new BwProvisionerSimple(Constants.HOST_BW),
                Constants.HOST_STORAGE,
                peList,
                new VmSchedulerTimeSharedOverSubscription(peList),
                Constants.HOST_POWER[hostType]);
    }

    //为指定的task选择一个可以完成task且mips最小的vm类型
    private int selectVmForTask(Cloudlet task){
        double taskLength = task.getCloudletLength();
        double DDL = 0;
        int vmType = -1;
        double minTime = Double.MAX_VALUE;
        for(int i = 0; i < Constants.VM_TYPES; i ++){
            double startTime = 0;
            double estimateExecTime = Constants.VM_MIPS[i] / taskLength;
            double estimateFinishTime = estimateExecTime + startTime;
            if(inTime(DDL, estimateFinishTime) && estimateFinishTime < minTime){
                minTime = estimateFinishTime;
                vmType = i;
            }
        }
        return vmType;
    }

    //启动一个新的host，在其上新建一个vm并放置一个指定的task
    private Vm createHostForTask(Cloudlet task){
        //这个怎么定？也是规模最小的？
        int hostType = 0;
        PowerHost host = createHost(hostType);
        Vm vm = createVM(selectVmForTask(task));
        host.vmCreate(vm);
        getHostList().add(host);
        return vm;
    }

    //scaleUp
    private Vm scaleUpComputingResource(Cloudlet task){
        int vmType = selectVmForTask(task);
        if(vmType != -1) {
            Vm vm = createVM(vmType);
            for(Host host : getHostList()){
                if(host.vmCreate(vm)) return vm;
            }
        }
        if(vmType != -1) {
            return createHostForTask(task);
        }
        return null;
    }

    //将task放到vm上直接开始执行
    private void executeTask(Vm vm, Cloudlet task){
        double estimatedFinishTime = getExecUpper(vm, task);
        send(getId(), estimatedFinishTime, CloudSimTags.VM_DATACENTER_EVENT);
        task.setVmId(vm.getId());
        //vm.getCloudletScheduler().getCloudletExecList().add(new ResCloudlet(task));
        vm.getCloudletScheduler().cloudletSubmit(task);
    }

    //将task放到vm的waitingQueue里
    private void task2waitingList(Vm vm, Cloudlet task){
        task.setVmId(vm.getId());
        vm.getCloudletScheduler().getCloudletWaitingList().add(new ResCloudlet(task));
    }

    //为VM找一个task放在waitingQueue中
    private void searchWaitingTask(Vm vm){
        for(Cloudlet task : waitingQueue){
            //需要判断时间关系
            double estimateExecTimeLower = getExecLower(vm, task);
            double estimateExecTimeUpper = getExecUpper(vm, task);
            double startTimeUpper = 0;
            double startTimeLower = 0;
            double estimateFinishTimeUpper = getUpper(startTimeLower, startTimeUpper, estimateExecTimeLower, estimateExecTimeUpper);
            double DDL = 0;
            if(inTime(DDL, estimateFinishTimeUpper)){
                task2waitingList(vm, task);
                waitingQueue.remove(task);
                break;
            }
        }
    }

    //将新的task分配到queue中
    protected void PRS(Cloudlet cl){
        //PRS
//        if(isUrgentTask(task)){
//            getUrgentTaskQueue().add(task);
//            //delete task from allCloudlet
//        }
//        else{
//            getWaitingQueue().add(task);
//        }
        if (cl.getCloudletId() % 2 != 0)//% 2 == 0)
            getUrgentTaskQueue().add(cl);
        else getWaitingQueue().add(cl);
        prsCount ++;
        System.out.println("PRS:"+prsCount);
    }

    /*
    将waiting list中变为urgent的task加到urgentList中
     */
    private void waitingQueue2urgent(){
        if(getUrgentTaskQueue().size() != 0 || getWaitingQueue().size() != 0)
            send(getId(), 100, CloudSimTags.Search_Queue);


        if(getWaitingQueue().size() != 0) {
            getUrgentTaskQueue().addAll(getWaitingQueue());
            getWaitingQueue().clear();
            scheduleUrgentTasks();
        }
        //if(getVmList().size() != 0)
//        if(count == 0 || count < prsCount)
//        if(finishedCloudletCount != prsCount)
    }

    /*
    用于判断是否在ddl前完成任务
     */
    private boolean inTime(double ddl, double estimateFinishTimeUpper){
        return true;
    }

    //执行时间下界
    private double getExecLower(Vm vm, Cloudlet task){
        return task.getCloudletLength() / vm.getMips();
    }

    //执行时间上界
    private double getExecUpper(Vm vm, Cloudlet task){
        return task.getCloudletLength() / vm.getMips();
    }

    //上界和
    private double getUpper(double lower1, double upper1, double lower2, double upper2){
        return upper1 + upper2;
    }

    //下界和
    private double getLower(double lower1, double upper1, double lower2, double upper2){
        return lower1 + lower2;
    }

    private void rejectTask(Cloudlet task){
    }

    private void scheduleUrgentTasks(){
        List<Cloudlet> submitted = new ArrayList<>();
        for(Cloudlet task:getUrgentTaskQueue()){
            if(submitCloudletToVM(task)) {
                submitted.add(task);
                ++ count;
                System.out.println("SUBMITTED COUNT"+count);
            }
        }
        getUrgentTaskQueue().removeAll(submitted);
//        ArrayList<Cloudlet> toMove = new ArrayList<>();
//
//        for(Cloudlet task: urgentTaskQueue){
//            //double fileTransferTime = predictFileTransferTime(task.getRequiredFiles());
//            double minFinishTimeUpper = Double.MAX_VALUE;
//            double minFinishTimeLower = Double.MAX_VALUE;
//            int vmUpper = -1;
//            int vmLower = -1;
//            //找已有的VM中完成时间最近的
//            for(int i = 0; i < getVmList().size(); i ++){
//                try{
//                    Vm vm = getVmList().get(i);
//                    CloudletScheduler scheduler = vm.getCloudletScheduler();
//
//                    int execTaskListLength = scheduler.getCloudletExecList().size();
//                    int waitingTaskListLength = scheduler.getCloudletWaitingList().size();
//                    if(execTaskListLength == 0 || waitingTaskListLength == 0 || (waitingTaskListLength == 1
//                            && !isUrgentTask(scheduler.getCloudletWaitingList().get(0).getCloudlet()))){
////                        System.out.println("waiting"+waitingTaskListLength);
////                        System.out.println("execute"+execTaskListLength);
//                        //崩溃了，不知道怎么定这些数值
//                        double estimateExecTimeLower = getExecLower(vm, task);
//                        double estimateExecTimeUpper = getExecUpper(vm, task);
//                        double startTimeUpper = 0;
//                        double startTimeLower = 0;
//                        double estimateFinishTimeUpper = getUpper(startTimeLower, startTimeUpper, estimateExecTimeLower, estimateExecTimeUpper);
//                        double estimateFinishTimeLower = getLower(startTimeLower, startTimeUpper, estimateExecTimeLower, estimateExecTimeUpper);
//                        double DDL = 0;
//                        if(inTime(DDL, estimateExecTimeUpper) && estimateFinishTimeUpper < minFinishTimeUpper){
//                            minFinishTimeUpper =  estimateFinishTimeUpper;
//                            vmUpper = i;
//
//                        }
//                        else if(vmUpper == -1 && estimateExecTimeLower < minFinishTimeLower){
//                            minFinishTimeLower = estimateFinishTimeLower;
//                            vmLower = i;
//                        }
//                    }
//                } catch (Exception e){
//                    e.printStackTrace();
//                }
//            }
//            System.out.println("VM INDEX:"+vmUpper);
//            //找到vm，将task放到其中并开始执行
//            if(vmUpper != -1){
//                System.out.println("BEFORE:"+getVmList().get(vmUpper).getCloudletScheduler().getCloudletExecList().size());
//                if(getVmList().get(vmUpper).getCloudletScheduler().getCloudletExecList().size() == 1){
//                    task2waitingList(getVmList().get(vmUpper), task);
//                }
//                else executeTask(getVmList().get(vmUpper), task);
//                System.out.println("AFTER:"+getVmList().get(vmUpper).getCloudletScheduler().getCloudletExecList().size());
//                toMove.add(task);
//            }
//            else{
////                System.out.println("BEFORE:"+getVmList().get(vmUpper).getCloudletScheduler().getCloudletExecList().size());
//                Vm vm = scaleUpComputingResource(task);
//                if(vm != null){
//                    executeTask(vm, task);
//                    toMove.add(task);
//                    searchWaitingTask(vm);
//                }
//                else if(vmLower != -1){
//                    if(getVmList().get(vmLower).getCloudletScheduler().getCloudletExecList().size() == 1){
//                        task2waitingList(getVmList().get(vmLower), task);
//                    }
//                    else executeTask(getVmList().get(vmLower), task);
//                    toMove.add(task);
//                }
//                else{
//                    rejectTask(task);
//                }
//            }
//        }
//        urgentTaskQueue.removeAll(toMove);
    }

    protected void processCloudletSubmit(SimEvent ev, boolean ack) {
        super.updateCloudletProcessing();

        try {
            // gets the Cloudlet object
            Cloudlet cl = (Cloudlet) ev.getData();
            // checks whether this Cloudlet has finished or not
            if (cl.isFinished()) {
                String name = CloudSim.getEntityName(cl.getUserId());
                Log.printConcatLine(getName(), ": Warning - Cloudlet #", cl.getCloudletId(), " owned by ", name,
                        " is already completed/finished.");
                Log.printLine("Therefore, it is not being executed again");
                Log.printLine();

                // NOTE: If a Cloudlet has finished, then it won't be processed.
                if (ack) {
                    int[] data = new int[3];
                    data[0] = getId();
                    data[1] = cl.getCloudletId();
                    data[2] = CloudSimTags.FALSE;

                    // unique tag = operation tag
                    int tag = CloudSimTags.CLOUDLET_SUBMIT_ACK;
                    sendNow(cl.getUserId(), tag, data);
                }
                sendNow(cl.getUserId(), CloudSimTags.CLOUDLET_RETURN, cl);
                return;
            }

            // process this Cloudlet to this CloudResource
            cl.setResourceParameter(
                    getId(), getCharacteristics().getCostPerSecond(),
                    getCharacteristics().getCostPerBw());
            //这里不将cloudlet直接分配给vm，而是存在datacenter的waiting queue里
//            waitingQueue2urgent();
            submitCloudletToVM(cl);

            if (ack) {
                int[] data = new int[3];
                data[0] = getId();
                data[1] = cl.getCloudletId();
                data[2] = CloudSimTags.TRUE;

                // unique tag = operation tag
                int tag = CloudSimTags.CLOUDLET_SUBMIT_ACK;
                sendNow(cl.getUserId(), tag, data);
            }
        } catch (ClassCastException c) {
            Log.printLine(getName() + ".processCloudletSubmit(): " + "ClassCastException error.");
            c.printStackTrace();
        } catch (Exception e) {
            Log.printLine(getName() + ".processCloudletSubmit(): " + "Exception error.");
            e.printStackTrace();
        }
        checkCloudletCompletion();
//        setCloudletSubmitted(CloudSim.clock());
    }

    private Vm searchForBestVm(Cloudlet cl){
        double maxMips = Double.MIN_VALUE;
        Vm bestVm = null;
        for(Host host:getVmAllocationPolicy().getHostList()) {
            for (Vm vm : host.getVmList()) {
                if (vm.getCloudletScheduler().getCloudletExecList().size() < 1){
                    bestVm = vm;
                    break;
//                    double tempMaxMips = host.getTotalAllocatedMipsForVm(vm);
//                    tempMaxMips -= vm.getTotalUtilizationOfCpuMips(CloudSim.clock());
//                    if (maxMips < tempMaxMips) {
//                        maxMips = tempMaxMips;
//                        bestVm = vm;
//                    }
                }
                else if(vm.getCloudletScheduler().getCloudletWaitingList().size() < 1){
//                    bestVm = vm;
                    vm.getCloudletScheduler().getCloudletWaitingList().add(new ResCloudlet(cl));
                    bestVm = vm;
                    break;
                }
            }
        }
        if(bestVm == null){
            System.out.println("NOT FOUND");
        }
        return bestVm;
    }

    private boolean submitCloudletToVM(Cloudlet cl){
        int userId = cl.getUserId();
        int vmId = cl.getVmId();
        // time to transfer the files
        double fileTransferTime = predictFileTransferTime(cl.getRequiredFiles());

        Host host = getVmAllocationPolicy().getHost(vmId, userId);
//        Vm bestVm = host.getVm(vmId, userId);
        Vm bestVm = searchForBestVm(cl);
        //分配成功
//        if(bestVm == null) bestVm = host.getVm(vmId, userId);

        if(bestVm != null) {
            CloudletScheduler scheduler = bestVm.getCloudletScheduler();
            double estimatedFinishTime = scheduler.cloudletSubmit(cl, fileTransferTime);
            // if this cloudlet is in the exec queue
            if (estimatedFinishTime > 0.0 && !Double.isInfinite(estimatedFinishTime)) {
                estimatedFinishTime += fileTransferTime;
                send(getId(), estimatedFinishTime, CloudSimTags.VM_DATACENTER_EVENT);
            }
            setCloudletSubmitted(CloudSim.clock());
            return true;
        }
        return false;
    }

    private void submitUrgentCloudletToVM(){
        if(getUrgentTaskQueue().size() == 0) return;

        for(Cloudlet cl:getUrgentTaskQueue()) {
            //submit
            int userId = cl.getUserId();
            int vmId = cl.getVmId();
            // time to transfer the files
            double fileTransferTime = predictFileTransferTime(cl.getRequiredFiles());

            Host host = getVmAllocationPolicy().getHost(vmId, userId);
            Vm vm = host.getVm(vmId, userId);
            CloudletScheduler scheduler = vm.getCloudletScheduler();
            double estimatedFinishTime = scheduler.cloudletSubmit(cl, fileTransferTime);
            // if this cloudlet is in the exec queue
            if (estimatedFinishTime > 0.0 && !Double.isInfinite(estimatedFinishTime)) {
                estimatedFinishTime += fileTransferTime;
                send(getId(), estimatedFinishTime, CloudSimTags.VM_DATACENTER_EVENT);
            }
            setCloudletSubmitted(CloudSim.clock());
        }
        getUrgentTaskQueue().clear();
    }

    public void processEvent(SimEvent ev){
//        int srcId = -1;
        if(ev.getTag() == CloudSimTags.Search_Queue) {
            waitingQueue2urgent();
        }
        else{
            super.processEvent(ev);
        }
    }


    protected double updateCloudetProcessingWithoutSchedulingFutureEventsForce() {
        double currentTime = CloudSim.clock();
        double minTime = Double.MAX_VALUE;
        double timeDiff = currentTime - getLastProcessTime();
        double timeFrameDatacenterEnergy = 0.0;

        Log.printLine("\n\n--------------------------------------------------------------\n\n");
        Log.formatLine("New resource usage for the time frame starting at %.2f:", currentTime);

        addUtilizationCount();

        for (PowerHost host : this.<PowerHost> getHostList()) {
            Log.printLine();

            double time = host.updateVmsProcessing(currentTime); // inform VMs to update processing
            if (time < minTime) {
                minTime = time;
            }

            int hostId = host.getId();
            if(hostId >= getHostUtilizationList().size()) continue;
            double current = getHostUtilizationList().get(hostId);
            getHostUtilizationList().set(hostId, host.getUtilizationOfCpu() + current);


            Log.formatLine(
                    "%.2f: [Host #%d] utilization is %.2f%%",
                    currentTime,
                    host.getId(),
                    host.getUtilizationOfCpu() * 100);
        }

        if (timeDiff > 0) {
            Log.formatLine(
                    "\nEnergy consumption for the last time frame from %.2f to %.2f:",
                    getLastProcessTime(),
                    currentTime);

            for (PowerHost host : this.<PowerHost> getHostList()) {
                double previousUtilizationOfCpu = host.getPreviousUtilizationOfCpu();
                double utilizationOfCpu = host.getUtilizationOfCpu();
                double timeFrameHostEnergy = host.getEnergyLinearInterpolation(
                        previousUtilizationOfCpu,
                        utilizationOfCpu,
                        timeDiff);
                timeFrameDatacenterEnergy += timeFrameHostEnergy;

                Log.printLine();
                Log.formatLine(
                        "%.2f: [Host #%d] utilization at %.2f was %.2f%%, now is %.2f%%",
                        currentTime,
                        host.getId(),
                        getLastProcessTime(),
                        previousUtilizationOfCpu * 100,
                        utilizationOfCpu * 100);
                Log.formatLine(
                        "%.2f: [Host #%d] energy is %.2f W*sec",
                        currentTime,
                        host.getId(),
                        timeFrameHostEnergy);
            }

            Log.formatLine(
                    "\n%.2f: Data center's energy is %.2f W*sec\n",
                    currentTime,
                    timeFrameDatacenterEnergy);
        }

        setPower(getPower() + timeFrameDatacenterEnergy);

        checkCloudletCompletion();
        /** Remove completed VMs **/
//        for (PowerHost host : this.<PowerHost> getHostList()) {
//            for (Vm vm : host.getCompletedVms()) {
//                getVmAllocationPolicy().deallocateHostForVm(vm);
//                getVmList().remove(vm);
//                Log.printLine("VM #" + vm.getId() + " has been deallocated from host #" + host.getId());
//            }
//        }

        Log.printLine();

        setLastProcessTime(currentTime);
        return minTime;
    }

}
