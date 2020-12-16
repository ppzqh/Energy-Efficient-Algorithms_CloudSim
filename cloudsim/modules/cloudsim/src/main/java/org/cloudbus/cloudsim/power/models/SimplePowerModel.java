/*
The power model used in:
Energy-aware resource allocation heuristics for efficient management of data centers for Cloud computing
 */

package org.cloudbus.cloudsim.power.models;

/*
model from paper ??? added by PP
 */
public class SimplePowerModel extends PowerModelSpecPower{
    private final double K = 0.7;
    private final double pMax = 250;

    protected double getPowerData(int index) {
        return K*pMax + (1-K)*pMax*index/100;
    }

}
