package org.cloudbus.cloudsim;

public class CloudletSchedulerDynamicWorkloadUncertain extends CloudletSchedulerDynamicWorkload{

    public CloudletSchedulerDynamicWorkloadUncertain(double mips, int numberOfPes) {
        super(mips, numberOfPes);

    }

    public double getEstimateTimeUncertain(Cloudlet cl){
        ResCloudlet rcl = new ResCloudlet(cl);
        rcl.setCloudletStatus(Cloudlet.INEXEC);

        for (int i = 0; i < cl.getNumberOfPes(); i++) {
            rcl.setMachineAndPeId(0, i);
        }

        double time = getEstimatedFinishTime(rcl, getPreviousTime());
        return time;
    }
}
