package org.cloudbus.cloudsim.power;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import org.cloudbus.cloudsim.power.planetlab.PlanetLabHelper;
import org.cloudbus.cloudsim.power.random.RandomConstants;
import org.cloudbus.cloudsim.power.random.RandomHelper;

public class PowerPolicyComparison {
    /** The cloudlet list. */
    private static List<Cloudlet> cloudletList;

    /** The vmlist. */
    private static List<Vm> vmList;
    //
    private static List<PowerHost> hostList;

//    ////////////////////////// STATIC METHODS ///////////////////////
    /**
     * Creates main() to run this example
     */
    private static String saveDir;

    public static void main(String[] args) throws IOException{
        // Set the path for saving the result
        saveDir = "./result/";

        // synthetic
        try{
            // run a single simulation on the PlanetLab Workload
            SyntheticWorkload(1.0);

            // varying the ratio of VM to Host
//            double[] ratioList = {1.0, 1.25, 1.5, 1.75};
//            varyingRatioForSyntheticWorkload(ratioList);

        }catch (Exception e){
            System.out.println("ERROR");
        }

        // planetlab
        try{
            // run a single simulation on the PlanetLab Workload
            PlanetLab(800);

            // varying the number of hosts
//            int[] hostNumList = {800, 900, 1000, 1100};
//            varyingHostNumForPlanetlabWorkload(hostNumList);

        }catch (Exception e){
            System.out.println("ERROR");
        }
    }

    /**
     * Adjust the ratio of VM to Host and run several rounds of simulation.
     * @param ratioList: For example, double[] ratioList = {1.0, 1.25, 1.5, 1.75};
     * @throws Exception
     */
    private static void varyingRatioForSyntheticWorkload(double[] ratioList) throws Exception {
        for(int i = 0; i < ratioList.length; i ++) {
            SyntheticWorkload(ratioList[i]);
        }
    }

    /**
     * Run several rounds of simulations with different number of hosts.
     * @param hostNumList: For example, int[] hostNumList = {800, 900, 1000, 1100};
     * @throws Exception
     */
    private static void varyingHostNumForPlanetlabWorkload(int[] hostNumList) throws Exception {
            for(int i = 0; i < hostNumList.length; i ++) {
                PlanetLab(hostNumList[i]);
            }
    }
    /**
     * Single simulation on the PlanetLab Workload
     * @param hostNum: specify the number of hosts
     * The lowerUtilization (firstLower) is set to 0.1 and the higherUtilization is set to 0.1 + thresholdInterval(0.4)
     * @throws Exception
     */
    private static void PlanetLab(int hostNum) throws Exception{
        //specify the output folder path
        String savePath = saveDir+"planetLab/"+hostNum+"/"; // cloudsim/result/planetLab/
        File saveFile = new File(savePath);
        if (!saveFile.exists()) saveFile.mkdirs();

        String[] policyNames = {"BFD", "LearningAutomata", "GRANITE", "ACS", "THR", "IQR"};
        String inputFolder = System.getProperty("user.dir") + "/modules/cloudsim-examples/src/main/resources/workload/planetlab/";
        File file=new File(inputFolder);
        File[] tempList = file.listFiles();
        if(tempList == null) {
            System.out.println("Wrong folder path");
        };

        double thresholdInterval = 0.4;
        double firstLower = 0.1;
        for(int i = 0; firstLower + 0.1 * i + thresholdInterval < 1; i ++) {
            double lowerUtilization = firstLower + i * 0.1;
            double utilization = lowerUtilization + thresholdInterval;
            for (String policyName : policyNames) {
                //10 workload
                for(int j = 0; j < tempList.length; j ++) {
                    String workload = tempList[j].getName();
                    if(!workload.substring(0,4).equals("2011")) continue;
                    HashMap<String, String> simulationResult = testCase(inputFolder+workload, policyName, utilization, lowerUtilization, hostNum);
                    Helper.saveResult(simulationResult, savePath + policyName + "_" + String.format("%.1f", lowerUtilization) + ".txt");
                }
            }
        }
    }

    /**
     * Single simulation on the Synthetic Workload
     * @param ratio: specify the ratio of vmNum to hostNum. (Default hostNum is set to 50)
     * The lowerUtilization (firstLower) is set to 0.1 and the higherUtilization is set to 0.1 + thresholdInterval(0.4)
     * @throws Exception
     */
    private static void SyntheticWorkload(double ratio) throws Exception {
        //specify the output folder path
        String savePath = saveDir+"synthetic/"+ratio+"/"; // cloudsim/result/random/
        File file = new File(savePath);
        if (!file.exists()) file.mkdirs();

        String[] policyNames = {"BFD", "ACS", "LearningAutomata", "GRANITE", "THR", "IQR"};
        double thresholdInterval = 0.4;
        double firstLower = 0.1;
        for (int j = 0; j < 10; j++) {
            int brokerId = 0;
            for (int i = 0; firstLower + 0.1 * i + thresholdInterval < 1; i++) {
                cloudletList = RandomHelper.createCloudletList(brokerId, (int) (RandomConstants.NUMBER_OF_HOSTS * ratio), j);
                double lowerUtilization = firstLower + i * 0.1;
                double utilization = lowerUtilization + thresholdInterval;
                for (String policyName : policyNames) {
                    //10 workload
                    HashMap<String, String> simulationResult = syntheticTest(policyName, utilization, lowerUtilization, ratio);
                    Helper.saveResult(simulationResult, savePath + policyName + "_" + String.format("%.1f", lowerUtilization) + ".txt");

                }
            }
        }
    }
    private static HashMap<String, String> syntheticTest(String policyName, double utilizationThreshlod, double lowerUtilizationThreshold, double ratioVMandHost){
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
            // modify brokderID
            for(Cloudlet cl: cloudletList){
                cl.setUserId(brokerId);
            }
            vmList = Helper.createVmList(brokerId, cloudletList.size());
            hostList = Helper.createHostList(RandomConstants.NUMBER_OF_HOSTS);


            VmAllocationPolicy vmAllocationPolicy = null;
            PowerVmSelectionPolicy vmSelectionPolicy = new PowerVmSelectionPolicyMinimumMigrationTime();
            PowerDatacenter datacenter_0 = null;

            //select algorithm
            if(policyName.equals("BFD")){
                vmAllocationPolicy = new PowerVmAllocationPolicyMBFD(
                        hostList,
                        vmSelectionPolicy,
                        utilizationThreshlod,
                        lowerUtilizationThreshold);
                datacenter_0 = Helper.createDatacenter("datacenter0", PowerDatacenter.class,
                        hostList, vmAllocationPolicy);
            }
            else if(policyName.equals("LearningAutomata")){
                vmAllocationPolicy = new PowerVmAllocationLearningAutomata(
                        hostList,
                        vmSelectionPolicy,
                        utilizationThreshlod,
                        lowerUtilizationThreshold);
                datacenter_0 = Helper.createDatacenter("datacenter0", PowerDatacenterLearningAutomata.class,
                        hostList, vmAllocationPolicy);
            }
            else if(policyName.equals("GRANITE")){
                vmAllocationPolicy = new PowerVmAllocationPolicyGRANITE(
                        hostList,
                        vmSelectionPolicy,
                        utilizationThreshlod,
                        lowerUtilizationThreshold);
                datacenter_0 = Helper.createDatacenter("datacenter0", PowerDatacenter.class,
                        hostList, vmAllocationPolicy);
            }
            else if(policyName.equals("THR")){
                vmAllocationPolicy = new PowerVmAllocationPolicyMigrationStaticThreshold(
                        hostList,
                        vmSelectionPolicy,
                        utilizationThreshlod
                );
                datacenter_0 = Helper.createDatacenter("datacenter0", PowerDatacenter.class,
                        hostList, vmAllocationPolicy);
            }
            else if(policyName.equals("IQR")){
                PowerVmAllocationPolicyMigrationAbstract fallbackVmSelectionPolicy = new PowerVmAllocationPolicyMigrationStaticThreshold(
                        hostList,
                        vmSelectionPolicy,
                        utilizationThreshlod);
                vmAllocationPolicy = new PowerVmAllocationPolicyMigrationInterQuartileRange(
                        hostList,
                        vmSelectionPolicy,
                        1.5,
                        fallbackVmSelectionPolicy);
                datacenter_0 = Helper.createDatacenter("datacenter0", PowerDatacenter.class,
                        hostList, vmAllocationPolicy);
            }
            else if(policyName.equals("ACO")){
                vmAllocationPolicy = new PowerVmAllocationPolicyACS_VMC(
                        hostList,
                        vmSelectionPolicy,
                        utilizationThreshlod,
                        lowerUtilizationThreshold
                );
                datacenter_0 = Helper.createDatacenter("datacenter0", PowerDatacenter.class,
                        hostList, vmAllocationPolicy);
            }
            else{
                vmAllocationPolicy = new PowerVmAllocationPolicyMBFD(
                        hostList,
                        vmSelectionPolicy,
                        utilizationThreshlod,
                        lowerUtilizationThreshold);
                datacenter_0 = Helper.createDatacenter("datacenter0", PowerDatacenterUncertain.class,
                        hostList, vmAllocationPolicy);
            }
            // Fifth step: Starts the simulation
            datacenter_0.setDisableMigrations(false);

            broker.submitVmList(vmList);
            broker.submitCloudletList(cloudletList);

            CloudSim.terminateSimulation(Constants.SIMULATION_LIMIT);
            CloudSim.startSimulation();

            // Final step: Print results when simulation is over
            List<Cloudlet> newList = broker.getCloudletReceivedList();
            CloudSim.stopSimulation();

            HashMap<String, String> simulationResult = Helper.printResults(
                    datacenter_0,
                    vmList,
                    0,
                    policyName,
                    false,
                    "no");

            printCloudletList(newList);
            Log.printLine("Simulation finished!");
            return simulationResult;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Log.printLine("The simulation has been terminated due to an unexpected error");
            return null;
        }
    }


    private static HashMap<String, String> testCase(String inputFolder, String policyName, double utilizationThreshlod,
                                                    double lowerUtilizationThreshold, int hostNum){
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

            cloudletList = PlanetLabHelper.createCloudletListPlanetLab(brokerId, inputFolder);
            vmList = Helper.createVmList(brokerId, cloudletList.size());
            hostList = Helper.createHostList(hostNum);


            VmAllocationPolicy vmAllocationPolicy = null;
            PowerVmSelectionPolicy vmSelectionPolicy = new PowerVmSelectionPolicyMinimumMigrationTime();
            // data center
            PowerDatacenter datacenter_0 = null;

            // select algorithm
            if(policyName.equals("BFD")){
                vmAllocationPolicy = new PowerVmAllocationPolicyMBFD(
                        hostList,
                        vmSelectionPolicy,
                        utilizationThreshlod,
                        lowerUtilizationThreshold);
                datacenter_0 = Helper.createDatacenter("datacenter0", PowerDatacenter.class,
                        hostList, vmAllocationPolicy);
            }
            else if(policyName.equals("LearningAutomata")){
                vmAllocationPolicy = new PowerVmAllocationLearningAutomata(
                        hostList,
                        vmSelectionPolicy,
                        utilizationThreshlod,
                        lowerUtilizationThreshold);
                datacenter_0 = Helper.createDatacenter("datacenter0", PowerDatacenterLearningAutomata.class,
                        hostList, vmAllocationPolicy);
            }
            else if(policyName.equals("GRANITE")){
                vmAllocationPolicy = new PowerVmAllocationPolicyGRANITE(
                        hostList,
                        vmSelectionPolicy,
                        utilizationThreshlod,
                        lowerUtilizationThreshold);
                datacenter_0 = Helper.createDatacenter("datacenter0", PowerDatacenter.class,
                        hostList, vmAllocationPolicy);
            }
            else if(policyName.equals("THR")){
                vmAllocationPolicy = new PowerVmAllocationPolicyMigrationStaticThreshold(
                        hostList,
                        vmSelectionPolicy,
                        utilizationThreshlod
                );
                datacenter_0 = Helper.createDatacenter("datacenter0", PowerDatacenter.class,
                        hostList, vmAllocationPolicy);
            }
            else if(policyName.equals("IQR")){
                PowerVmAllocationPolicyMigrationAbstract fallbackVmSelectionPolicy = new PowerVmAllocationPolicyMigrationStaticThreshold(
                        hostList,
                        vmSelectionPolicy,
                        utilizationThreshlod);
                vmAllocationPolicy = new PowerVmAllocationPolicyMigrationInterQuartileRange(
                        hostList,
                        vmSelectionPolicy,
                        1.5,
                        fallbackVmSelectionPolicy);
                datacenter_0 = Helper.createDatacenter("datacenter0", PowerDatacenter.class,
                        hostList, vmAllocationPolicy);
            }
            else if(policyName.equals("ACS")){
                vmAllocationPolicy = new PowerVmAllocationPolicyACS_VMC(
                        hostList,
                        vmSelectionPolicy,
                        utilizationThreshlod,
                        lowerUtilizationThreshold
                );
                datacenter_0 = Helper.createDatacenter("datacenter0", PowerDatacenter.class,
                        hostList, vmAllocationPolicy);
            }
            else{
                vmAllocationPolicy = new PowerVmAllocationPolicyMBFD(
                        hostList,
                        vmSelectionPolicy,
                        utilizationThreshlod,
                        lowerUtilizationThreshold);
                datacenter_0 = Helper.createDatacenter("datacenter0", PowerDatacenterUncertain.class,
                        hostList, vmAllocationPolicy);
            }

            // Fifth step: Starts the simulation
            datacenter_0.setDisableMigrations(false);

            broker.submitVmList(vmList);
            broker.submitCloudletList(cloudletList);

            CloudSim.terminateSimulation(Constants.SIMULATION_LIMIT);
            CloudSim.startSimulation();

            // Final step: Print results when simulation is over
            List<Cloudlet> newList = broker.getCloudletReceivedList();
            CloudSim.stopSimulation();

            HashMap<String, String> simulationResult = Helper.printResults(
                    datacenter_0,
                    vmList,
                    0,
                    policyName,
                    false,
                    "no");
//            printCloudletList(newList);

            //print final result
            Log.printLine("Simulation finished!");
            return simulationResult;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Log.printLine("The simulation has been terminated due to an unexpected error");
            return null;
        }
    }



    private static void printCloudletList(List<Cloudlet> list) {
        int size = list.size();
        Cloudlet cloudlet;

        String indent = "    ";
        Log.printLine();
        Log.printLine("========== OUTPUT ==========");
        Log.printLine("Cloudlet count:"+size);
        Log.printLine("Cloudlet ID" + indent + "STATUS" + indent +
                "Data center ID" + indent + "VM ID" + indent + indent + "Time" + indent + "Start Time" + indent + "Finish Time");

        DecimalFormat dft = new DecimalFormat("###.##");
        for (int i = 0; i < size; i++) {
            cloudlet = list.get(i);
            Log.print(indent + cloudlet.getCloudletId() + indent + indent);

            if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS){
                Log.print("SUCCESS");

                Log.printLine( indent + indent + cloudlet.getResourceId() + indent + indent + indent + cloudlet.getVmId() +
                        indent + indent + indent + dft.format(cloudlet.getActualCPUTime()) +
                        indent + indent + dft.format(cloudlet.getExecStartTime())+ indent + indent + indent + dft.format(cloudlet.getFinishTime()));
            }
        }

    }

}
