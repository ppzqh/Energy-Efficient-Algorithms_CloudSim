package org.cloudbus.cloudsim.power.models;

/**
 * implement a quadratic polynomial model from "Power-Aware and Performance-Guaranteed Virtual Machine Placement in the Cloud"
 */
public class PowerModelQuadratic implements PowerModel{

    private double idlePower;
    private double omega1;
    private double omega2;

    public PowerModelQuadratic(){
        setOmega1(1.30447);
        setOmega2(0.02867);
        setIdlePower(459.95821);
    }

    // power = idlePower + omega1 * utilization + omega2 * utilization^2
    @Override
    public double getPower(double utilization) throws IllegalArgumentException {
        return idlePower + omega1 * utilization * 100 + omega2 * Math.pow(utilization * 100, 2);
    }

    private void setOmega1(double omega1){
        this.omega1 = omega1;
    }

    private void setOmega2(double omega2){
        this.omega2 = omega2;
    }

    private void setIdlePower(double idlePower){
        this.idlePower = idlePower;
    }

}
