package org.cloudbus.cloudsim.power;

import org.apache.commons.math3.analysis.function.Power;
import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.lists.PowerVmList;
import org.cloudbus.cloudsim.power.planetlab.PlanetLabHelper;
import org.cloudbus.cloudsim.power.planetlab.PlanetLabConstants;

import java.util.*;

/*
只用来得到最优解并储存在文件中，之后在主测试程序中读取文件并正式分配。
 */
public class PowerVmAllocationPolicyACO {

    private List<Vm> vmList = new ArrayList<>();
    //    private List<? extends Host> hostList;
    private List<PowerHost> hostList = new ArrayList<>();
    private List<Cloudlet> cloudletList = new ArrayList<>();

    private int vmNum;
    private int hostNum;
    private int cloudletNum;

    private int numAnt;
    private int numIter;
    private double rhoLocal;
    private double rhoGlobal;
    private double q0;
    private double alpha;
    private double beta;

    private double tauPower0;
    private double tauPerformance0;

    private double higherUtilizationThreshold;
    private double lowerUtilizationThreshold;

    private Map< Integer, Map<Integer, Double> > powerPheromone;
    private Map< Integer, Map<Integer, Double> > performancePheromone;

    private List< Map<Integer, Integer> > Sopt = new ArrayList<>();
    private List<Double[]> SoptData = new ArrayList<>();
    private List<Integer> SoptTime = new ArrayList<>();

    private Random randomSeed;


    //*******Added for paper in 2013 *******************************************************/
    private Map< Integer, Double> cpuUtilizationForVM;
    private Map< Integer, Double> memoryUtilizationForVM;
    private Map< Integer, Map<Integer, Double>> newPheromone;
    private double newTau0;

    private void setNewPheromone(Map<Integer, Map<Integer, Double>> newPheromone) {
        this.newPheromone = newPheromone;
    }
    private void setNewTau0(double newTau0) {
        this.newTau0 = newTau0;
    }
    private double getNewTau0() {
        return newTau0;
    }
    private Map<Integer, Map<Integer, Double>> getNewPheromone() {
        return newPheromone;
    }
    private void setCpuUtilizationForVM(Map<Integer, Double> cpuUtilizationForVM) {
        this.cpuUtilizationForVM = cpuUtilizationForVM;
    }
    private Map<Integer, Double> getCpuUtilizationForVM() {
        return cpuUtilizationForVM;
    }
    private void setMemoryUtilizationForVM(Map<Integer, Double> memoryUtilizationForVM) {
        this.memoryUtilizationForVM = memoryUtilizationForVM;
    }
    private Map<Integer, Double> getMemoryUtilizationForVM() {
        return memoryUtilizationForVM;
    }

    private double epsilon = 0.0001;

    private double getResourceWastageForHost(PowerHost host){
        if(host.getVmList().size() == 0) return 0;

        double cpuUtil = 0, memoryUtil = 0;
        for(Vm vm:host.getVmList()){
            cpuUtil += getCpuUtilizationForVM().get(vm.getId()) * vm.getMips();
            memoryUtil += getMemoryUtilizationForVM().get(vm.getId()) * vm.getRam();
        }
        cpuUtil /= host.getTotalMips();
        memoryUtil /= host.getRam();

        double remainCpu = 1 - cpuUtil;
        double remainMemory = 1 - memoryUtil;
        return (Math.abs(remainCpu - remainMemory) + epsilon) / (cpuUtil + memoryUtil);
    }

    private double getResourceWastageForAllHost(){
        double totalResoruceWastage = 0;
        for(PowerHost host:getHostList()){
            totalResoruceWastage += getResourceWastageForHost(host);
        }
        return totalResoruceWastage;
    }


    /******************************************************************************************************/

    /*
            需要提前知道vm和host，在构造函数里实现算法，迭代得到最优解
            之后按照结果进行分配
             */
    public PowerVmAllocationPolicyACO(int numAnt, int numIter, double rhoLocal, double rhoGlobal,
                                      double q0, double alpha, double beta, double lowerUtilization, double higherUtilization) {

        setNumAnt(numAnt);
        setNumIter(numIter);
        setRhoGlobal(rhoGlobal);
        setRhoLocal(rhoLocal);
        setQ0(q0);
        setAlpha(alpha);
        setBeta(beta);

        setUtilization(lowerUtilization, higherUtilization);

        String inputFolder = "/Users/pp/Downloads/cloud_computing/cloudsim/modules/cloudsim-examples/src/main/resources/workload/planetlab/";
        String workload = "20110303";

        int brokerId = 0;
        try {
            int num = 200;

//            setCloudletList(PlanetLabHelper.createCloudletListPlanetLab(brokerId, inputFolder + workload));
//            setHostNum(PlanetLabConstants.NUMBER_OF_HOSTS);
//            setVmNum(getCloudletList().size());

            setHostNum(num);
            setVmNum(num);
            setVmList(Helper.createVmList(brokerId, getVmNum()));
            setHostList(Helper.createHostList(getHostNum()));

            //set initial utilization for VM
            setCpuUtilizationForVM(new HashMap<>());
            setMemoryUtilizationForVM(new HashMap<>());

            //set utilization from cloudlet
            bindCloudletToVm();

        }catch (Exception e){

        }
        randomSeed = new Random();
    }

    private void setBrokerIdForCloudletList(int brokerId){
        for(Cloudlet cl:getCloudletList()){
            cl.setUserId(brokerId);
        }
    }
    private void setCloudletList(List<Cloudlet> cloudletList) {
        this.cloudletList = cloudletList;
    }

    private List<Cloudlet> getCloudletList() {
        return cloudletList;
    }

    /**
     * bind cloudlet to a selected VM
     */
    private void bindCloudletToVm(){
//        int tempCloudletNum = getVmNum();
//        for(int cloudletId = 0; cloudletId < tempCloudletNum; cloudletId ++){
//            int toBindVmId = cloudletId % getVmNum();
////            getCloudletList().get(cloudletId).setVmId(toBindVmId);
//
//            //更新当前vm的初始util
//            double utilToAdd = getUtilMeanOfCloudlet(cloudletId);//getCloudletList().get(cloudletId).getUtilizationOfCpu(0);
//            double utilBefore = getCpuUtilizationForVM().getOrDefault(toBindVmId, 0.0);//get(toBindVmId);
//            getCpuUtilizationForVM().put(toBindVmId, utilBefore + utilToAdd);
//        }

        double _util = 0.45;
        double P = 0.5;
        for(int i = 0; i < getVmNum(); i ++){
            double cpuUtil = Math.random() * 2 * _util;
            double memoryUtil = Math.random() * _util;
            getCpuUtilizationForVM().put(i, cpuUtil);
            double r = Math.random();
            if((r < P && cpuUtil >= _util) || (r >= P && cpuUtil < _util))
                memoryUtil += _util;
            getMemoryUtilizationForVM().put(i, memoryUtil);
        }

        setCloudletList(Helper.createCloudletStaticUtilization(
                getCpuUtilizationForVM(), getMemoryUtilizationForVM()));

    }

    private double getUtilMeanOfCloudlet(int cloudletId){
        Cloudlet c = getCloudletList().get(cloudletId);
        double util = 0;
        int itervalNum = 288;
        for(int i = 0; i < itervalNum; i ++){
            util += c.getUtilizationOfCpu(i * 300);
        }
        return util / itervalNum;
    }


    /**
     * main process of PPVMP
     */
    public void PPVMP(){
        //initialize S0, pheromone(power, performance)
        initializePheromone();
        startSimulation(null, "S0");
        int iterCount = 0;
        //iterative loop
        while(iterCount < getNumIter()){
            //eac h ant constructs a solution
            for(int antCount = 0; antCount < getNumAnt(); antCount ++){
                //initialize vm and host for each ant
                initializeVmAndHost();

                //construct
                antConstructSolution(iterCount);

                //clear vm and host after each ant constructing a solution
                clearVmAndHost();
            }

            //Remove non-dominated sulutions in S opt;
            removeNonDominatedSolution();

            //global update
            for(int index = 0; index < getSopt().size(); index ++){
                globalUpdate(index, iterCount, iterCount - getSoptTime().get(index));
            }
            System.out.println("Iteration: "+ iterCount);
            System.out.println("Power: "+getSoptData().get(0)[0]+ " Resource:" + getSoptData().get(0)[1] + "\n");

            ++ iterCount;
        }

        double minPower = Double.MIN_VALUE;
        for(int i = 0; i < getSopt().size(); i ++) {
            HashMap<String, String> result = startSimulation(getSopt().get(i), "ACO");
            double tempPower = Double.valueOf(result.get("Energy consumption"));
            if(tempPower < minPower)
                minPower = tempPower;
        }
        setMinPower(minPower);
    }

    private double minPower;

    private void setMinPower(double minPower) {
        this.minPower = minPower;
    }

    public double getMinPower() {
        return minPower;
    }

    /**
     * An ant constructs a solution
     * @param iterCount indicates the round of current iteration
     */
    private void antConstructSolution(int iterCount){
        //store solution
        Map<Integer, Integer> vmTable = new HashMap<>();

        while(getVmList().size() > 0){
            Collections.shuffle(getVmList()); //Sort VMs in vmList in random order;

            Vm vm = getVmList().get(0); //get a VM_i in vmList
            int vmId = vm.getId();

            int hostId = chooseHostForVm(vm); //Choose a PM M_j with Eq. (21) to place V i ;

            getHostList().get(hostId).vmCreate(vm); //put VM_i on Host_hostID
            vmTable.put(vmId, hostId);

            getVmList().remove(vm); //Remove VM_i from vmList

            localUpdate(vmId, hostId); //local update
        }
        getSopt().add(vmTable);
        //add data(unchecked)
        Double []newData = new Double[2];
        newData[0] = getPowerForAllHost();
        newData[1] = getPerformanceDegradForAllHost();
        //**************added for paper in 2013***************
        newData[1] = getResourceWastageForAllHost();

        getSoptData().add(newData);
        getSoptTime().add(iterCount);
    }


    /**
     * initialize two pheromone trails
     * power: 1/W(S0)
     * performance degradation: 1/F(S0)
     */
    private void initializePheromone(){
        getS0();
        setPowerPheromone(new HashMap<>());
        setPerformancePheromone(new HashMap<>());

        //**************added for paper in 2013***************
        setNewPheromone(new HashMap<>());
        for(int vmId = 0; vmId < getVmNum(); vmId ++){
            Map<Integer, Double> newPheromone = new HashMap<>();
            for(int hostId = 0; hostId < getHostNum(); hostId ++){
                //new tau
                newPheromone.put(hostId, getNewTau0());
            }
            getNewPheromone().put(vmId, newPheromone);
        }
        //**************added for paper in 2013***************


        //初始化信息素，都为1/tau0
        for(int vmId = 0; vmId < getVmNum(); vmId ++){
            Map<Integer, Double> tempPheromonePower = new HashMap<>();
            Map<Integer, Double> tempPheromonePerformance = new HashMap<>();
            for(int hostId = 0; hostId < getHostNum(); hostId ++){
                //power
                tempPheromonePower.put(hostId, getTauPower0());
                //performace
                tempPheromonePerformance.put(hostId, getTauPerformance0());
            }
            getPowerPheromone().put(vmId, tempPheromonePower);
            getPerformancePheromone().put(vmId, tempPheromonePerformance);
        }
    }

    /**
     * Calculate power consumption of a host from power model by the CPU utilization
     * @param host
     * @return power consumption of host
     */
    private double getPowerFromHost(PowerHost host){
//        double mipsRequestedByAllVms = 0;
//        for(Vm vm:host.getVmList()){
//            mipsRequestedByAllVms += vm.getCurrentRequestedMaxMips();
//        }
//        double mipsUtil = mipsRequestedByAllVms / host.getTotalMips();
//        if(mipsUtil > 1) mipsUtil = 1;
//        return host.getPower(mipsUtil) / host.getMaxPower();


        //**************added for paper in 2013***************
        double mipsRequestedByAllVms = 0;
        for(Vm vm:host.getVmList()){
            mipsRequestedByAllVms += vm.getMips() * getCpuUtilizationForVM().get(vm.getId());
        }
        double mipsUtil = mipsRequestedByAllVms / host.getTotalMips();
//        if(mipsUtil > 1) mipsUtil = 1;
        return host.getPower(mipsUtil) / host.getMaxPower();
    }

    /**
     * Calculate power consumption of all hosts
     * @return total power consumption
     */
    private double getPowerForAllHost(){
        double power = 0;
        for(PowerHost host:getHostList()){
            power += getPowerFromHost(host);
        }
        return power;
    }

    /**
     * Get data of S0(FFD)
     */
    private void getS0(){
        //FFD
        initializeVmAndHost();
        PowerVmList.sortByMips(getVmList());
        Map<Integer, Integer> VmTable = new HashMap<>();
        for(Vm vm:getVmList()){
            boolean is = false;
            for (PowerHost host : getHostList()) {
                if (host.isSuitableForVm(vm)) {
                    host.vmCreate(vm);
                    VmTable.put(vm.getId(), host.getId());
                    is = true;
                    break;
                }
            }
            if(!is){
                System.out.println("WHAT?");
            }
        }
        //get power and performance
        double power = getPowerForAllHost();
        double performanceDegrad = getPerformanceDegradForAllHost();
        setTauPerformance0(1 / performanceDegrad);
        setTauPower0(1 / power);

        //**************added for paper in 2013***************
        double resourceWastage = getResourceWastageForAllHost();
        setNewTau0(1/( getVmNum() * (power + resourceWastage) ));
        //**************added for paper in 2013***************
        clearVmAndHost();
    }


    /**
     * Calculate performance degradation of all hosts
     * @return total performance degradation
     */
    private double getPerformanceDegradForAllHost(){
        double performanceDegrad = 0;
        for(PowerHost host:getHostList()){
            performanceDegrad += host.getPerformanceDegradation();
        }
        return performanceDegrad;
    }


    /*
    If at least one objective of the solution S i is better than S j ,
    and the other objectives of S i is not worse than S j , then S i is said to dominate S j .
     */
    private void removeNonDominatedSolution(){
        //mark Solution to remove(all False at first)
        boolean[] markForToRemove = new boolean[getSopt().size()];
        for (int i = 0; i < getSopt().size(); i ++) {
            if (markForToRemove[i]) continue;
            for (int j = i + 1; j < getSopt().size(); j++) {
                //
                if (dominate(getSoptData().get(i), getSoptData().get(j)))
                    markForToRemove[j] = true; //i dominate j, remove j
                else if (dominate(getSoptData().get(j), getSoptData().get(i)))
                    markForToRemove[i] = true; //j dominate i, remove i
            }
        }

        int j = 0;
        //remove
        for (int i = 0; i < getSopt().size(); i ++) {
            if (markForToRemove[j ++]) {
                getSopt().remove(i);
                getSoptData().remove(i);
                getSoptTime().remove(i);
                -- i;
            }
        }

    }

    /**
     * To determine which solution is better
     * @param data1
     * @param data2
     * @return
     */
    private boolean dominate(Double[] data1, Double[] data2){
//        return data1[0] <= data2[0];
        for(int i = 0; i < data1.length; i ++){
            if(data1[i] > data2[i]) return false;
        }
        return true;
    }


    /**
     * Local pheromone update based on Eqs.(16),(17)
     * @param vmId
     * @param hostId
     */
    private void localUpdate(int vmId, int hostId){
        //power
        double powerPheromone= getPowerPheromone().get(vmId).get(hostId);
        powerPheromone *= (1 - getRhoLocal());
        powerPheromone += getRhoLocal() * getTauPower0();
        getPowerPheromone().get(vmId).put(hostId, powerPheromone);

        //performance
        double performancePheromone= getPerformancePheromone().get(vmId).get(hostId);
        performancePheromone *= (1 - getRhoLocal());
        performancePheromone += getRhoLocal() * getTauPerformance0();
        getPerformancePheromone().get(vmId).put(hostId, performancePheromone);

        //**************added for paper in 2013***************
        //new tau
        double pheromone = getNewPheromone().get(vmId).get(hostId);
        pheromone *= (1 - getRhoLocal());
        pheromone += getNewTau0();
        getNewPheromone().get(vmId).put(hostId, pheromone);
        //**************added for paper in 2013***************
    }

    /**
     * Global pheromone update based on Eqs.(18),(19)
     * @param solutionIndex
     * @param iterCount
     * @param timeReside
     */
    private void globalUpdate(int solutionIndex, int iterCount, int timeReside){
        for(int vmId = 0; vmId < getVmNum(); vmId ++){
            int hostId = getSopt().get(solutionIndex).get(vmId);
            //power
            double powerPheromone = getPowerPheromone().get(vmId).get(hostId);
            powerPheromone *= (1 - getRhoGlobal());
            powerPheromone += getRhoGlobal() * getLambda(getNumAnt(), iterCount, timeReside)
                    / getSoptData().get(solutionIndex)[0];
            getPowerPheromone().get(vmId).put(hostId, powerPheromone);

            //performance
            double performancePheromone = getPerformancePheromone().get(vmId).get(hostId);
            performancePheromone *= (1 - getRhoGlobal());
            performancePheromone += getRhoGlobal() * getLambda(getNumAnt(), iterCount, timeReside)
                    / getSoptData().get(solutionIndex)[1];
            getPerformancePheromone().get(vmId).put(hostId, performancePheromone);

            //**************added for paper in 2013***************
            //new pheromone
            double newPheromone = getNewPheromone().get(vmId).get(hostId);
            newPheromone *= (1 - getRhoGlobal());
            newPheromone += getRhoGlobal() * getLambda(getNumAnt(), iterCount, timeReside)
                    / (getSoptData().get(solutionIndex)[0] + getSoptData().get(solutionIndex)[1]);
            getNewPheromone().get(vmId).put(hostId, newPheromone);
            //**************added for paper in 2013***************
        }
    }

    /**
     * Lambda in Eq(20)
     * @param numAnt
     * @param iterCount
     * @param timeReside
     * @return
     */
    private double getLambda(int numAnt, int iterCount, int timeReside){
        return numAnt/(iterCount - timeReside + 1);
    }


    /**
     * Choose a host for the vm with Eq.(21)
     * @param vm
     * @return
     */
    private int chooseHostForVm(Vm vm){
        //q is it is determined by the relative importance of exploitation of accumulated
        // knowledge about the problem versus exploration of new movements
        double q = randomSeed.nextDouble();//Math.random();

        //power and performance before the choice
        double totalPower = getPowerForAllHost();
        double totalPerformanceDegrad = getPerformanceDegradForAllHost();

        //store the value of each choice
        Map<Integer, Double> allValueList = new HashMap<>();
        Map<Integer, Double> allProbList = new HashMap<>();
        double sumValue = 0;

        //**************added for paper in 2013***************
        for(PowerHost host: getHostList()){
            if(host.isSuitableForVm(vm)){
                host.vmCreate(vm); //create
                double powerAfter = getPowerForAllHost();
                double resourceWastageAfter = getResourceWastageForAllHost();
                host.vmDestroy(vm); //destroy

                //Calculate tau and eta
                double tau = getNewPheromone().get(vm.getId()).get(host.getId());
                double eta = 1/(epsilon + powerAfter) + 1/(epsilon + resourceWastageAfter);
                //get value for this choice: tau^alpha * eta^beta
                double ALPHA = 0.45;
                double value = ALPHA * tau + (1 - ALPHA) * eta;
                allValueList.put(host.getId(), value);
                sumValue += value;
            }
        }
        //**************added for paper in 2013***************


        /* PPVMP
        //get value for each choice
        for(PowerHost host: getHostList()){
            if(host.isSuitableForVm(vm)){
                double powerBefore = getPowerFromHost(host);
                double performaceDegradBefore = host.getPerformanceDegradation();
                host.vmCreate(vm); //create
                double powerAfter = getPowerFromHost(host);
                double performanceDegradAfter = host.getPerformanceDegradation();
                host.vmDestroy(vm); //destroy

                //Calculate tau and eta

                //theta: random number to control importance between power and performance
                //we only consider power at first
                double theta = 1;

                // Get tau
                double tauPower = getPowerPheromone().get(vm.getId()).get(host.getId());
                double tauPerformance = getPerformancePheromone().get(vm.getId()).get(host.getId());
                double tau = theta * tauPower + (1-theta) * tauPerformance;

                // Get eta
                double currentPower = totalPower - powerBefore + powerAfter;
                double currentPerformanceDegrad = totalPerformanceDegrad
                        - performaceDegradBefore + performanceDegradAfter;
                //we only consider power at first
                currentPerformanceDegrad = 1;
                double eta = 1 / (currentPower * currentPerformanceDegrad);

                //get value for this choice: tau^alpha * eta^beta
                double value = Math.pow(tau, getAlpha()) * Math.pow(eta, getBeta());
                allValueList.put(host.getId(), value);
                sumValue += value;
            }
        }
        */

        //get max value
        double maxValue = Double.MIN_VALUE;
        int bestHostId = -1;
        for(int hostId = 0; hostId < getHostNum(); hostId ++){
            if(allValueList.containsKey(hostId)){
                double value = allValueList.get(hostId);
                //update max value
                if(q <= getQ0()) {
                    if (value > maxValue) {
                        maxValue = value;
                        bestHostId = hostId;
                    }
                }
                //get probability
                else allProbList.put(hostId, value/sumValue);
            }
        }

        if(q <= getQ0()) return bestHostId;
        else{
            return selectFromProbDistribution(allProbList);
        }
    }

    /**
     * Randomly select a host acording to the Probability distribution in Eq.(22)
     * @param allProbList
     * @return
     */
    private int selectFromProbDistribution(Map<Integer, Double> allProbList){
        //get all hostId from probability map
        ArrayList<Integer> hostIdList = new ArrayList<>(allProbList.keySet());
        int []freq = new int[hostIdList.size()];
        int sum = 0;
        for(int i = 0; i < hostIdList.size(); i ++){
            int hostId = hostIdList.get(i);
            freq[i] = (int)(allProbList.get(hostId)*100000.0);
            sum += freq[i];
        }
        int n = 1 + randomSeed.nextInt(sum);
        if(n < freq[0])
            return hostIdList.get(0);
        for(int i = 0; i < hostIdList.size() - 1; i ++){
            freq[i+1] += freq[i];
            if(n > freq[i] && n <= freq[i+1]){
                return hostIdList.get(i+1);
            }
        }
        return 0;
    }


    private void setPowerPheromone(Map<Integer, Map<Integer, Double>> powerPheromone) {
        this.powerPheromone = powerPheromone;
    }

    private void setPerformancePheromone(Map<Integer, Map<Integer, Double>> performancePheromone) {
        this.performancePheromone = performancePheromone;
    }

    /*
    get(vmId).get(hostId) is the Performance Degradation Pheromone between Vm_vmID and Host_hostId
     */
    private Map<Integer, Map<Integer, Double>> getPerformancePheromone() {
        return performancePheromone;
    }

    /*
    get(vmId).get(hostId) is the Power Consumption Pheromone between Vm_vmID and Host_hostId
     */
    private Map<Integer, Map<Integer, Double>> getPowerPheromone() {
        return powerPheromone;
    }

    private List<Double[]> getSoptData() {
        return SoptData;
    }

    private List<Integer> getSoptTime() {
        return SoptTime;
    }

    private List<Map<Integer, Integer>> getSopt() {
        return Sopt;
    }

    /**
     * initialize vmList and hostList
     */
    private void initializeVmAndHost(){
        int brokerId = 0;
//        setHostList(Helper.createHostList(getHostNum()));
//        setVmList(Helper.createVmList(brokerId, getVmNum()));

        try {
            setVmList(Helper.createVmList(brokerId, getVmNum()));
            setHostList(Helper.createHostList(getHostNum()));
        }catch (Exception e){

        }
    }

    private void initializeVmAndHost(int brokerId){
        try {
            setVmList(Helper.createVmList(brokerId, getVmNum()));
            setHostList(Helper.createHostList(getHostNum()));
        }catch (Exception e){

        }
    }

    private double getRhoGlobal() {
        return rhoGlobal;
    }

    private double getRhoLocal() {
        return rhoLocal;
    }

    private void clearVmAndHost(){
        getHostList().clear();
        getVmList().clear();
    }

    public double getQ0() {
        return q0;
    }

    private double getAlpha() {
        return alpha;
    }

    private double getBeta() {
        return beta;
    }

    private int getNumAnt() {
        return numAnt;
    }

    private int getNumIter() {
        return numIter;
    }

    private int getVmNum() {
        return vmNum;
    }

    private int getHostNum() {
        return hostNum;
    }

    public void setHostList(List<PowerHost> hostList) {
        this.hostList = hostList;
    }

    public void setVmList(List<Vm> vmList) {
        this.vmList = vmList;
    }

    public List<PowerHost> getHostList() {
        return hostList;
    }

    public List<Vm> getVmList() {
        return vmList;
    }

    private void setVmNum(int vmNum) {
        this.vmNum = vmNum;
    }

    private void setHostNum(int hostNum) {
        this.hostNum = hostNum;
    }

    private void setBeta(double beta) {
        this.beta = beta;
    }

    public void setQ0(double q0) {
        this.q0 = q0;
    }

    private void setNumAnt(int numAnt) {
        this.numAnt = numAnt;
    }

    private void setNumIter(int numIter) {
        this.numIter = numIter;
    }

    private void setRhoGlobal(double rhoGlobal) {
        this.rhoGlobal = rhoGlobal;
    }

    private void setRhoLocal(double rhoLocal) {
        this.rhoLocal = rhoLocal;
    }

    private void setAlpha(double alpha) {
        this.alpha = alpha;
    }

    private void setCloudletNum(int cloudletNum) {
        this.cloudletNum = cloudletNum;
    }

    private void setUtilization(double lower, double higher) {
        this.higherUtilizationThreshold = higher;
        this.lowerUtilizationThreshold = lower;
    }

    private double getHigherUtilizationThreshold() {
        return higherUtilizationThreshold;
    }

    private double getLowerUtilizationThreshold() {
        return lowerUtilizationThreshold;
    }

    private void setTauPerformance0(double tauPerformance0) {
        this.tauPerformance0 = tauPerformance0;
    }

    private void setTauPower0(double tauPower0) {
        this.tauPower0 = tauPower0;
    }

    /*
    Performance degration for S0
     */
    private double getTauPerformance0() {
        return tauPerformance0;
    }

    /*
    Power Consumption for S0
     */
    private double getTauPower0() {
        return tauPower0;
    }

//    /**
//     *
//     * @param brokerId
//     * @param solution
//     * @return
//     */
//    private Map<String, Integer> setBrokerIdForCurrentS(int brokerId, Map<Integer, Integer> solution){
//        Map<String, Integer> vmTable = new HashMap<>();
//        for(int vmId = 0; vmId < getVmNum(); vmId ++){
//            vmTable.put(brokerId+"-"+vmId, solution.get(vmId));
//        }
//        return vmTable;
//    }

    /*
        start simulation for a current solution and save data
            data[0]：energy consumption
            data[1]: performance degradation
    */
    public HashMap<String, String> startSimulation(Map<Integer, Integer> solution, String mode) {
        Log.disable();
        Log.printLine("Starting PowerPolicyTest...");
        try {
            // First step: Initialize the CloudSim package. It should be called
            // before creating any entities.
            int num_user = 1;   // number of grid users
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;  // mean trace events

            // Initialize the CloudSim library
            CloudSim.init(num_user, calendar, trace_flag);

            DatacenterBroker broker = Helper.createBroker();
            int brokerId = broker.getId();

            //需要更改一下brokerID
            setBrokerIdForCloudletList(brokerId);
            initializeVmAndHost(brokerId);

            //select policy
            PowerVmSelectionPolicy vmSelectionPolicy = new PowerVmSelectionPolicyMinimumMigrationTime();
            VmAllocationPolicy vmAllocationPolicy = null;
            if (mode.equals("S0")) {
                vmAllocationPolicy = new PowerVmAllocationPolicySimple(hostList);
            } else {
                vmAllocationPolicy = new PowerVmAllocationPolicyForACO(hostList, vmSelectionPolicy,
                        getHigherUtilizationThreshold(), getLowerUtilizationThreshold(), solution);
            }

            PowerDatacenter datacenter_0 = Helper.createDatacenter("datacenter0", PowerDatacenter.class,
                    getHostList(), vmAllocationPolicy);

            broker.submitVmList(getVmList());
            broker.submitCloudletList(getCloudletList());
            for(int i = 0; i < getVmNum(); i ++) {
                broker.bindCloudletToVm(getCloudletList().get(i).getCloudletId(), getVmList().get(i).getId());
            }
            datacenter_0.setDisableMigrations(true);

            CloudSim.terminateSimulation(Constants.SIMULATION_LIMIT);

            // Fifth step: Starts the simulation
            CloudSim.startSimulation();

            // Final step: Print results when simulation is over
            List<Cloudlet> newList = broker.getCloudletReceivedList();

            CloudSim.stopSimulation();

            String policyName = "ACO_LOG";
            HashMap<String, String> simulationResult = Helper.printResults(
                    datacenter_0,
                    getVmList(),
                    0,
                    policyName,
                    false,
                    "no");

            return simulationResult;
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("The simulation has been terminated due to an unexpected error");
        }
        return null;
    }

}