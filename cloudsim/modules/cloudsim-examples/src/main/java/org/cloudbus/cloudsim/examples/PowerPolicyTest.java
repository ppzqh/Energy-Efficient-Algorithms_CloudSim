package org.cloudbus.cloudsim.examples;

import java.io.*;
import java.io.File;
import java.text.DecimalFormat;
import java.util.*;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.*;
import org.cloudbus.cloudsim.examples.power.Helper;

public class PowerPolicyTest {
    /** The cloudlet list. */
    private static List<Cloudlet> cloudletList;

    /** The vmlist. */
    private static List<Vm> vmlist;

    private static List<Cloudlet> createCloudlet(int userId, int cloudlets){
        // Creates a container to store Cloudlets
        LinkedList<Cloudlet> list = new LinkedList<Cloudlet>();

        //cloudlet parameters
        long length = 150000;
        long fileSize = 300;
        long outputSize = 300;
        int pesNumber = 1;
        UtilizationModel utilizationModel = new UtilizationModelFull();

        Cloudlet[] cloudlet = new Cloudlet[cloudlets];

        for(int i=0;i<cloudlets;i++){
            cloudlet[i] = new Cloudlet(i, length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
            // setting the owner of these Cloudlets
            cloudlet[i].setUserId(userId);
            list.add(cloudlet[i]);
        }

        return list;
    }

    ////////////////////////// STATIC METHODS ///////////////////////


    /**
     * Creates main() to run this example
     */
    public static void main(String[] args) throws IOException{
        /*
        ArrayList<ArrayList<String>> result = new ArrayList<>();
        for(double interval = 0.4; interval <= 1; interval += 0.1) {
            System.out.println("interval = "+interval);
            ArrayList<String> tempResult = new ArrayList<>();
            for (double i = 0.0; i <= 1 - interval; i += 0.1) {
//                System.out.println(testCase(i + interval, i));
                System.out.println("upper: "+(i+interval)+" lower: "+i);
                double power = testCase(i + interval, i);
                tempResult.add(interval+" "+i+" "+power);
            }
            result.add(tempResult);
            System.out.println();
        }
        File file = new File("/Users/pp/Desktop/probabilistic.txt");
        FileWriter out = new FileWriter(file);
        for(ArrayList<String> temp: result) {
            for(String i : temp) {
                out.write(i);
                out.write(System.getProperty("line.separator"));
            }
        }
        out.close();
         */
        String pathName = "/Users/pp/Desktop/SIAT/cloud_computing/result/";
//        String[] policyNames = {"BFD", "Probabilistic","LearningAutomata"};
        String[] policyNames = {"BFD"};
        for(String policyName : policyNames) {
            HashMap<String, String> simulationResult = testCase(policyName, 0.9, 0.5, 0.95);
            //System.out.println(simulationResult);
            saveResult(simulationResult, pathName + policyName + ".txt");
        }
    }

    protected static void saveResult(HashMap<String,String> simulationResult, String fileName) throws IOException{
        try{
            File file = new File(fileName);
            FileWriter out = new FileWriter(file);
            for(Map.Entry<String,String> result : simulationResult.entrySet()){
                out.write(result.getKey() + ':' + result.getValue());
                out.write(System.getProperty("line.separator"));
            }
            out.close();
        } catch(IOException i){
            System.out.println("save result failed");
        }
    }

    private static HashMap<String, String> testCase(String policyName, double utilizationThreshlod, double lowerUtilizationThreshold,
                                                    double higherUtilizationThreshold){
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

            // Second step: Create Datacenters
            List<PowerHost> hostList = Helper.createHostList(50);
            VmAllocationPolicy vmAllocationPolicy = null;
            PowerVmSelectionPolicy vmSelectionPolicy = new PowerVmSelectionPolicyMinimumMigrationTime();

            if(policyName.equals("BFD")){
                vmAllocationPolicy = new PowerVmAllocationPolicyMBFD(
                        hostList,
                        vmSelectionPolicy,
                        utilizationThreshlod,
                        lowerUtilizationThreshold);
            }
            else if(policyName.equals("LearningAutomata")){
                vmAllocationPolicy = new PowerVmAllocationLearningAutomata(
                        hostList,
                        vmSelectionPolicy,
                        utilizationThreshlod,
                        lowerUtilizationThreshold);
            }
            //换个适用的写法
            PowerDatacenter datacenter_0 = Helper.createDatacenter("datacenter0", PowerDatacenterLearningAutomata.class,
            hostList, vmAllocationPolicy);

            //Third step: Create Broker
            DatacenterBroker broker = Helper.createBroker();
            int brokerId = broker.getId();
            //Fourth step: Create VMs and Cloudlets and send them to broker
            vmlist = Helper.createVmList(brokerId, 100);
            cloudletList = createCloudlet(brokerId,100); // one application for each VM

            for(Vm vm:vmlist){
                System.out.println("UID:"+vm.getUid());
            }
            broker.submitVmList(vmlist);
            broker.submitCloudletList(cloudletList);

            // Fifth step: Starts the simulation
            CloudSim.startSimulation();

            // Final step: Print results when simulation is over
            List<Cloudlet> newList = broker.getCloudletReceivedList();

            CloudSim.stopSimulation();

            HashMap<String, String> simulationResult = Helper.printResults(
                    datacenter_0,
                    vmlist,
                    0,
                    policyName,
                    false,
                    "no");
            printCloudletList(newList);

            //print final result
            Log.printLine("PowerPolicyTest finished!");
            return simulationResult;//printFinalStatistics(datacenter_0.getPower());
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
