package org.cloudbus.cloudsim.power;

import org.cloudbus.cloudsim.CloudletScheduler;
import org.cloudbus.cloudsim.core.CloudSim;

import java.util.List;

public class PowerVmLearningAutomata extends PowerVm{
    //action num
    private int r = 3;
    //添加learning automata
    private Double[] learningAutomataProbability = {1.0/3, 1.0/3, 1.0/3};
    private String[] action = {"ASC", "DESC", "NONE"};
    private int currentAction = 0;
    //reward, penalty parameter
    private double a;
    private double b;

    public PowerVmLearningAutomata(
            final int id,
            final int userId,
            final double mips,
            final int pesNumber,
            final int ram,
            final long bw,
            final long size,
            final int priority,
            final String vmm,
            final CloudletScheduler cloudletScheduler,
            final double schedulingInterval) {
        super(id, userId, mips, pesNumber, ram, bw, size, priority, vmm, cloudletScheduler, schedulingInterval);
        setA(0.1);
        setB(0.1);
    }

    /*
    update action
     */
    public void updateAction(){
        int action = 0;
        double value = 0;
        //update
        //randomly select
        for(int i = 0; i < r; i ++){
            double randomValue = Math.random() * learningAutomataProbability[i];
            if(randomValue > value) {
                action = i;
                value = randomValue;
            }
        }
        setAction(action);
    }

    /*
    set action by int
     */
    private void setAction(int action){
        currentAction = action;
    }

    /*
    get current action in String
     */
    public String getAction(){
        return action[currentAction];
    }

    /*
    action is rewarded
     */
    private void rewardAction(int action){
        //reward
        learningAutomataProbability[action] *= (1-getA());
        learningAutomataProbability[action] += getA();

        for(int i = 0; i < r; i ++){
            if(i != action) learningAutomataProbability[i] *= (1-getA());
        }
    }

    /*
    action is penalized
     */
    private void penalizeAction(int action){
        //penalize
        learningAutomataProbability[action] *= (1-getB());

        for(int i = 0; i < r; i ++){
            if(i != action) {
                learningAutomataProbability[i] *= (1-getB());
                learningAutomataProbability[i] += getB()/(r-1);
            }
        }
    }

    /*
    update learning automata
     */
    public void updateLA(){
        double mean = getUtilizationMean();
        double current = getTotalUtilizationOfCpuMips(CloudSim.clock());//
        double temp =  getCurrentRequestedTotalMips();
        //判断不同的动作
        switch (getAction()) {
            case "NONE":
                if (mean == current)
                    rewardAction(currentAction);
                else
                    penalizeAction(currentAction);
                break;
            case "ASC":
                if (mean < current)
                    rewardAction(currentAction);
                else
                    penalizeAction(currentAction);
                break;
            case "DESC":
                if (mean > current)
                    rewardAction(currentAction);
                else
                    penalizeAction(currentAction);
                break;
        }
    }


    @Override
    public double updateVmProcessing(final double currentTime, final List<Double> mipsShare) {
        double time = super.updateVmProcessing(currentTime, mipsShare);
        if (currentTime > getPreviousTime()) {
            double utilization = getTotalUtilizationOfCpu(getCloudletScheduler().getPreviousTime());
            //为1
            if(utilization > 1) utilization = 1;
            //等于0的时候也得算
            if (CloudSim.clock() != 0){
                addUtilizationHistoryValue(utilization);
            }
            setPreviousTime(currentTime);
        }
        return time;
    }

    private void setA(double a){
        this.a = a;
    }
    private void setB(double b){
        this.b = b;
    }

    private double getA(){
        return this.a;
    }

    private double getB(){
        return this.b;
    }
}
