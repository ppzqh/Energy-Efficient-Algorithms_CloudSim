package org.cloudbus.cloudsim.power;
import org.cloudbus.cloudsim.Host;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ACOtest {
    public static void main(String[] args) throws IOException{
        int antNum = 10;
        int numIter = 100;
        double rhoLocal = 0.35, rhoGlobal = 0.35;
        String file_path = "/Users/pp/Downloads/cloud_computing/result/ACO/train/";
        double q0 = 0.8;//0.9;
        double alpha = 1, beta = 1;
        double higherUtilizationThreshold = 0.9;
        double lowerUtilizationThreshold = 0.5;
        //iterate
        int instanceNum = 1;
        double totalPower = 0;
        for(int i = 0; i < instanceNum; i ++) {
            PowerVmAllocationPolicyACO ACO = new PowerVmAllocationPolicyACO(antNum,
                    numIter, rhoLocal, rhoGlobal, q0, alpha, beta,
                    lowerUtilizationThreshold, higherUtilizationThreshold);
            ACO.PPVMP();
            double power = ACO.getMinPower();
            totalPower += power;
        }
//        totalPower /= instanceNum;
//        System.out.println("Mean min power:"+totalPower);


    }
/*
    private void search(String file_path) throws IOException{
        int antNum = 10;
        int numIter = 200;
        double higherUtilizationThreshold = 0.9;
        double lowerUtilizationThreshold = 0.5;

        double rhoLocal = 0.1, rhoGlobal = 0.1;
        double base_q0 = 0.4;
        double base_alpha = 1;
        int base_beta = 1;
        //调q0
        for(int i_q0 = 0; base_q0 + i_q0 * 0.1 < 1; i_q0 ++) {
            double q0 = base_q0 + i_q0 * 0.1;
            //调alpha
            for(int i_alpha = 0; i_alpha < 3 ; i_alpha ++) {
                double alpha = Math.pow(0.1, i_alpha) * base_alpha;
                //调beta
                double bestResultforCurrent = Double.MAX_VALUE;
                HashMap<String, String> bestResultMapforCurrent = new HashMap<>();
                HashMap<String, Host> bestVmTable = new HashMap<>();
                double bestBeta = 0;
                for (int i_beta = 0; i_beta < 10; i_beta++) {
                    double beta = base_beta + i_beta * 2;
                    System.out.println("TRAINING_"+q0+"_"+alpha+"_"+beta+" STARTS");
                    PowerVmAllocationPolicyACO ACO = new PowerVmAllocationPolicyACO(antNum,
                            numIter, rhoLocal, rhoGlobal, q0, alpha, beta,
                            lowerUtilizationThreshold, higherUtilizationThreshold);

                    try {
//                        ACO.startSimulation(null, "S0");
                        ACO.ACO();
                        if(ACO.getBestResult() < bestResultforCurrent){
                            bestResultforCurrent = ACO.getBestResult();
                            bestResultMapforCurrent = ACO.getBestResultMap();
                            bestVmTable = ACO.getFinalVmTable();
                            bestBeta = beta;
                        }
//                        ACO.getBestResultMap().put("POWER_ACO", Double.toString(ACO.getBestResult()));
                        Helper.saveResult(ACO.getBestResultMap(), file_path + q0 + "_" + alpha + "_" + beta + ".txt");

                    } catch (IOException e) {
                        System.out.println("CAO!!!!!");
                        break;
                    }
                    System.out.println("TRAINING:"+q0+"_"+alpha+"_"+beta+"END");
                    System.out.println();
                }
//                bestResultMapforCurrent.put("POWER_ACO", Double.toString(bestResultforCurrent));
                Helper.saveResult(bestResultMapforCurrent, file_path + q0 + "_" + alpha + "_best_beta" + bestBeta + ".txt");
            }
        }

    }

 */
}
