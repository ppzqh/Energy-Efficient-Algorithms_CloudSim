package org.cloudbus.cloudsim.power;

import org.apache.commons.math3.analysis.function.Pow;
import org.apache.commons.math3.analysis.function.Power;
import org.cloudbus.cloudsim.Vm;

import java.util.*;

public class ACS_VMC {
    private Map<Integer, PowerHost> hostList;
    private Map<Integer, Vm> vmList;

    private Map<Integer, Integer> oldMap;
    private Map< Integer, List<Integer> > allTuples;
    private Map< Integer, Map<Integer, Double> > pheromone;

    private int numAnt;
    private int numIter;
    private double rhoLocal;
    private double rhoGlobal;
    private double q0;
    private double alpha;
    private double beta;
    private double tau0;
    private Random randomSeed;
    private double gamma;
    private double higherUtilizationThreshold;
    private double lowerUtlizationThreshold;

    private int idleNum;

    private List< Map<Integer, Integer> > Sopt = new ArrayList<>();
    private List< Double > SoptData = new ArrayList<>();

    public void setHigherUtilizationThreshold(double higherUtilizationThreshold) {
        this.higherUtilizationThreshold = higherUtilizationThreshold;
    }

    public double getHigherUtilizationThreshold() {
        return higherUtilizationThreshold;
    }

    public void setLowerUtlizationThreshold(double lowerUtlizationThreshold) {
        this.lowerUtlizationThreshold = lowerUtlizationThreshold;
    }

    public void setOldMap(Map<Integer, Integer> oldMap) {
        this.oldMap = oldMap;
    }

    public Map<Integer, Integer> getOldMap() {
        return oldMap;
    }

    public ACS_VMC(Map<Integer, List<Integer> > tuples,
                   Map<Integer, PowerHost> hostList,
                   Map<Integer, Vm> vmList,
                   Map<Integer, Integer> oldMap,
                   double higherUtilizationThreshold){
        setHostList(hostList);
        setVmList(vmList);
        setTupleList(tuples);
        setOldMap(oldMap);

        setNumAnt(10);
        setNumIter(2);
        setRhoGlobal(0.1);
        setRhoLocal(0.1);
        setGamma(5);
        setBeta(0.9);
        setQ0(0.9);
        setQ0(1);

        setHigherUtilizationThreshold(higherUtilizationThreshold);
        randomSeed = new Random();
    }

    public Map<Integer, Integer> VMC(){
        //initialize S0, pheromone(power, performance)
        initializePheromone();
        int iterCount = 0;
        //iterative loop
        while(iterCount < getNumIter()){
            //each ant constructs a solution
            for(int antCount = 0; antCount < getNumAnt(); antCount ++){
                antConstructSolution();
            }

//            Remove non-dominated sulutions in S opt;
            removeNonDominatedSolution();

            //global update
            for(int index = 0; index < getSopt().size(); index ++){
                globalUpdate(index);
            }

            ++ iterCount;
        }

        int bestSoptIndex = getBestPlan();
        return getSopt().get(bestSoptIndex);
    }

    private int getBestPlan(){
        int bestIndex = -1;
        double bestValue = Double.MIN_VALUE;
        for(int i = 0; i < getSoptData().size(); i ++){
            if(getSoptData().get(i) > bestValue){
                bestIndex = i;
                bestValue = getSoptData().get(i);
            }
        }
        return bestIndex;
    }

    private void initializePheromone(){
        getS0();
        setPheromone(new HashMap<>());

        for (Map.Entry<Integer, List<Integer> > entry:getAllTuples().entrySet()) {
            int vmId = entry.getKey();
            List<Integer> hostListForcurrentVm = entry.getValue();
            Map<Integer, Double> pheromoneForcurrentVm = new HashMap<>();
            for(int hostId:hostListForcurrentVm){
                pheromoneForcurrentVm.put(hostId, getTau0());
            }
            getPheromone().put(vmId, pheromoneForcurrentVm);
        }

    }

    private void getS0(){
        //set migration0 to the number of vm
        double migration0 = getVmList().size();
        double idle0 = 0;

        for(PowerHost host:getHostList().values()){
            if(host.getVmList().size() == 0)
                ++ idle0;
        }

        setTau0( 1 / (migration0 * idle0) );
    }

    /**
     * An ant constructs a solution
     */
    private void antConstructSolution(){
        Map<Integer, Integer> vmTable = new HashMap<>();
//        Collections.shuffle(getVmList());
        Map<Integer, Integer> finalVmTable = new HashMap<>();
        double finalValue = 0;

        List<Integer> vmIndexList = new LinkedList<>(getAllTuples().keySet());
        Collections.shuffle(vmIndexList);

        for(int vmId:vmIndexList){
//            int vmId = getVmList().get(vmCount).getId();
            int hostId = chooseHostForVm(getVmList().get(vmId), vmTable);

            if(!getHostList().containsKey(hostId)){
                System.out.println("SB?");
            }
            //getHostList().get(hostId).vmCreate(getVmList().get(vmId)); //put VM_i on Host_hostID

            //add the migration to current plan
            vmTable.put(vmId, hostId);
            getHostList().get(hostId).vmCreate(getVmList().get(vmId));

            //update final(line 15-20)
//            double currentValue = getValueForCurrentPlan(vmTable);
//            if(currentValue > finalValue) {
//                finalVmTable.put(vmId, hostId);
//                finalValue = currentValue;
//            }
//            else{
//                vmTable.remove(vmId, hostId);
//            }

            localUpdate(vmId, hostId); //local update
        }
//        getSopt().add(finalVmTable);
//        //add data(unchecked)
//        double newData = getValueForCurrentPlan(finalVmTable);
        getSopt().add(vmTable);
//        double newData = getValueForCurrentPlan(vmTable);
        double newData = getValueForCurrentStatus();
        getSoptData().add(newData);

        restoreInitialStatus(vmTable);

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
    private boolean dominate(Double data1, Double data2){
        return data1 <= data2;
    }

    private void exeCurrentPlan(Map<Integer, Integer> vmTable){
        for (Map.Entry<Integer, Integer> entry : vmTable.entrySet()) {
            int vmId = entry.getKey();
            int hostId = entry.getValue();
            //place vm on the host
            getHostList().get(hostId).vmCreate(getVmList().get(vmId));
        }
    }

    /**
     * restore the vm and host status before execute the vmTable
     * @param vmTable
     */
    private void restoreInitialStatus(Map<Integer, Integer> vmTable){
        for (Map.Entry<Integer, Integer> entry : vmTable.entrySet()) {
            int vmId = entry.getKey();
            int hostId = entry.getValue();
            //remove vm from the host
            getHostList().get(hostId).vmDestroy(getVmList().get(vmId));
        }
    }


    private double getValueForCurrentStatus(){
        int idle = 0;
        int migrationTime = 0;

        for(PowerHost host:getHostList().values()){
            if(host.getVmList().size() == 0){
                ++ idle;
            }
            for(Vm vm:host.getVmList()){
                if( getVmList().containsKey(vm.getId()) ){
                    if(getOldMap().get(vm.getId()) != host.getId())
                        ++ migrationTime;
                }
            }
        }

        return Math.pow(idle, gamma) + 1.0 / migrationTime;
    }


    private double getValueForCurrentPlan(Map<Integer, Integer> vmTable){
//        exeCurrentPlan(vmTable);
//        double value = getValueForCurrentStatus();
//        restoreInitialStatus(vmTable);
        int idle = 0;
        int migrationTime = 0;

        for(Map.Entry<Integer, Integer> entry:vmTable.entrySet()){
            int vmId = entry.getKey();
            int hostId = entry.getValue();
            if(getOldMap().get(vmId) != hostId){
                ++ migrationTime;
            }
        }

        for(PowerHost host:getHostList().values()) {
            if (host.getVmList().size() == 0) {
                ++idle;
            }
        }

        return Math.pow(idle, gamma) + 1.0 / migrationTime;
    }

    private double getUsedCapacityForHost(PowerHost host){
        double used = 0;
        for(Vm vm:host.getVmList()){
            used += vm.getCurrentRequestedTotalMips();
        }
        return used;
    }

    private double getEta(int vmId, int hostId){

        //place a new vm on the host selected
//        getHostList().get(hostId).vmCreate(getVmList().get(vmId));
        //TODO: We only consider CPU here.
        double hostCapacity = getHostList().get(hostId).getTotalMips();
        double hostUsedCapacity = getUsedCapacityForHost(getHostList().get(hostId));
        double vmUsedCapacity = getVmList().get(vmId).getCurrentRequestedTotalMips();

        //remove the new vm from the host selected
//        getHostList().get(hostId).vmCreate(getVmList().get(vmId));

        return 1 / Math.abs( hostCapacity - (hostUsedCapacity + vmUsedCapacity) );
    }

    /**
     * Gets the utilization of the CPU in MIPS for the current potentially allocated VMs.
     *
     * @param host the host
     *
     * @return the utilization of the CPU in MIPS
     */
    protected double getUtilizationOfCpuMips(PowerHost host) {
        double hostUtilizationMips = 0;
        for (Vm vm2 : host.getVmList()) {
            if (host.getVmsMigratingIn().contains(vm2)) {
                // calculate additional potential CPU usage of a migrating in VM
                hostUtilizationMips += host.getTotalAllocatedMipsForVm(vm2) * 0.9 / 0.1;
            }
            hostUtilizationMips += host.getTotalAllocatedMipsForVm(vm2);
        }
        return hostUtilizationMips;
    }

    /**
     * Checks if a host will be over utilized after placing of a candidate VM.
     *
     * @param host the host to verify
     * @param vm the candidate vm
     * @return true, if the host will be over utilized after VM placement; false otherwise
     */
    protected boolean isHostOverUtilizedAfterAllocation(PowerHost host, Vm vm) {
        boolean isHostOverUtilizedAfterAllocation = true;
        if (host.vmCreate(vm)) {
            isHostOverUtilizedAfterAllocation = isHostOverUtilized(host);
            host.vmDestroy(vm);
        }
        return isHostOverUtilizedAfterAllocation;
    }

    /**
     * Checks if a host is over utilized, based on CPU usage.
     *
     * @param host the host
     * @return true, if the host is over utilized; false otherwise
     */
    protected boolean isHostOverUtilized(PowerHost host) {
        double totalRequestedMips = 0;
        for (Vm vm : host.getVmList()) {
            totalRequestedMips += vm.getCurrentRequestedTotalMips();
        }
        double utilization = totalRequestedMips / host.getTotalMips();
        return utilization > getHigherUtilizationThreshold();
    }


    private int chooseHostForVm(Vm vm, Map<Integer, Integer> vmTable){
        //update status with current plan
//        exeCurrentPlan(vmTable);


        //q is it is determined by the relative importance of exploitation of accumulated
        // knowledge about the problem versus exploration of new movements
        double q = randomSeed.nextDouble();//Math.random();

        //apply vmTable to the Host and Vm, calculating a value
        int vmId = vm.getId();

        //store the value of each choice
        Map<Integer, Double> allValueList = new HashMap<>();
        Map<Integer, Double> allProbList = new HashMap<>();
        double sumValue = 0;
        //calculate value for each host and select one
        List<Integer> hostIdListForVm = getAllTuples().get(vmId);
        for(int hostId:hostIdListForVm){
            PowerHost host = getHostList().get(hostId);
            if(host.isSuitableForVm(vm)) {
                if (getUtilizationOfCpuMips(host) != 0 && isHostOverUtilizedAfterAllocation(host, vm)) {
                    continue;
                }
                double tau = getPheromone().get(vmId).get(hostId);
                double eta = getEta(vmId, hostId);//getValueForCurrentPlan(vmTable);
                double value = tau * Math.pow(eta, getBeta());
                allValueList.put(hostId, value);
                sumValue += value;
            }
        }

        if(sumValue < 0) {
            System.out.println("WHY < 0");
        }

        //restore status with current plan
//        restoreInitialStatus(vmTable);

        //get max value
        double maxValue = Double.MIN_VALUE;
        int bestHostId = -1;
        for(int hostId:hostIdListForVm){
            if(allValueList.containsKey(hostId)) {
                double value = allValueList.get(hostId);
                //update max value
                if (q <= getQ0()) {
                    if (value > maxValue) {
                        maxValue = value;
                        bestHostId = hostId;
                    }
                }
                //get probability
                else {
                    double prob = value / sumValue;
                    allProbList.put(hostId, value / sumValue);
                }
            }
        }

        if(q <= getQ0()) {
            return bestHostId;
        }
        else{
            return selectFromProbDistribution(allProbList);
        }
    }

    boolean sameVmCondition(PowerHost host1, PowerHost host2){
        return host1.getTotalMips() == host2.getTotalMips() &&
                host1.getAvailableMips() == host2.getAvailableMips();
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
        if(sum <= 0){
            System.out.println("WHAT?");
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


    /**
     * Local pheromone update based on Eqs.(11)
     * @param vmId
     * @param hostId
     */
    private void localUpdate(int vmId, int hostId){
        //power
        double powerPheromone= getPheromone().get(vmId).get(hostId);
        powerPheromone *= (1 - getRhoLocal());
        powerPheromone += getRhoLocal() * getTau0();
        getPheromone().get(vmId).put(hostId, powerPheromone);

    }

    /**
     * Global pheromone update based on Eqs.(9)
     * @param solutionIndex
     */
    private void globalUpdate(int solutionIndex){
//        double value = getValueForCurrentPlan(getSopt().get(solutionIndex));
        Map<Integer, Integer> tempSopt = getSopt().get(solutionIndex);
        for(Map.Entry<Integer, Integer> entry:tempSopt.entrySet()){
            int vmId = entry.getKey();
            int hostId = entry.getValue();
            //power
            double pheromone = getPheromone().get(vmId).get(hostId);
            pheromone *= (1 - getRhoGlobal());
            pheromone += getRhoGlobal() * getSoptData().get(solutionIndex);
            getPheromone().get(vmId).put(hostId, pheromone);
        }
    }


    public Map<Integer, Map<Integer, Double>> getPheromone() {
        return pheromone;
    }

    public double getTau0() {
        return tau0;
    }

    public void setTau0(double tau0) {
        this.tau0 = tau0;
    }

    public void setPheromone(Map<Integer, Map<Integer, Double>> pheromone) {
        this.pheromone = pheromone;
    }

    public void setNumIter(int numIter) {
        this.numIter = numIter;
    }

    public void setBeta(double beta) {
        this.beta = beta;
    }

    public void setQ0(double q0) {
        this.q0 = q0;
    }

    public void setRhoLocal(double rhoLocal) {
        this.rhoLocal = rhoLocal;
    }

    public void setRhoGlobal(double rhoGlobal) {
        this.rhoGlobal = rhoGlobal;
    }

    public void setNumAnt(int numAnt) {
        this.numAnt = numAnt;
    }


    public double getRhoLocal() {
        return rhoLocal;
    }

    public double getRhoGlobal() {
        return rhoGlobal;
    }

    public int getNumAnt() {
        return numAnt;
    }

    public int getNumIter() {
        return numIter;
    }

    public double getQ0() {
        return q0;
    }

    public double getBeta() {
        return beta;
    }

    public void setVmList(Map<Integer, Vm> vmList) {
        this.vmList = vmList;
    }

    public void setHostList(Map<Integer, PowerHost> hostList) {
        this.hostList = hostList;
    }

    public void setTupleList(Map<Integer, List<Integer> > tupleList) {
        this.allTuples = tupleList;
    }

    public Map<Integer, Vm> getVmList() {
        return vmList;
    }

    public Map<Integer, PowerHost> getHostList() {
        return hostList;
    }

    public Map<Integer, List<Integer>> getAllTuples() {
        return allTuples;
    }

    public void setGamma(double gamma) {
        this.gamma = gamma;
    }

    public double getGamma() {
        return gamma;
    }

    public void setSopt(List<Map<Integer, Integer>> sopt) {
        Sopt = sopt;
    }

    public List<Map<Integer, Integer>> getSopt() {
        return Sopt;
    }

    public List<Double> getSoptData() {
        return SoptData;
    }
}
