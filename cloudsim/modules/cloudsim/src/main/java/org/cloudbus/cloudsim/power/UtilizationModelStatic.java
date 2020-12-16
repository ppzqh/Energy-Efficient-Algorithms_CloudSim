package org.cloudbus.cloudsim.power;
import org.cloudbus.cloudsim.UtilizationModel;

public class UtilizationModelStatic implements UtilizationModel{
    /**
     * @return Always return a static utilization, independent of the time.
     */

    private double utilization;

    private void setUtilization(double utilization) {
        this.utilization = utilization;
    }

    private double getUtilization() {
        return utilization;
    }

    public UtilizationModelStatic(double utilization){
        setUtilization(utilization);
    }
    @Override
    public double getUtilization(double time) {
        return getUtilization();
    }
}


