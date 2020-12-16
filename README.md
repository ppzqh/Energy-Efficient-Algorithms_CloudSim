# Energy Efficient Algorithms based on VM Consolidation for Cloud Computing: Comparisons and Evaluations

Source code for several energy efficient algorithms in Cloud Computing implemented by CloudSim.



## Algorithm

Base path: modules/cloudsim/src/main/java/org/cloudbus/cloudsim/power/

- MBFD
  - path: PowerVmAllocationPolicyMBFD.java
- LOAD
  - path: PowerVmAllocationLearningAutomata.java
- GRANITE
  - path: PowerVmAllocationPolicyGRANITE.java
- ACS
  - path: PowerVmAllocationPolicyACS_VMC.java
- THR
  - path: PowerVmAllocationPolicyMigrationStaticThreshold.java
- IQR
  - path: PowerVmAllocationPolicyMigrationInterQuartileRange.java



## Setup and Run

Clone the repo and open the *CloudSim* in Idea IntelliJ IDEA.

- **PowerPolicyComparison.java**: modules/cloudsim/src/main/java/org/cloudbus/cloudsim/power/PowerPolicyComparison.java

- **main function:**
- `void SyntheticWorkload(double ratio);`
  - `void PlanetLab(int hostNum);`




## References

**Qiheng Zhou, Minxian Xu, Sukhpal Singh Gill, Chengxi Gao, Wenhong Tian, Chengzhong Xu, Rajkumar Buyya: Energy Efficient Algorithms based on VM Consolidation for Cloud Computing: Comparisons and Evaluations. [CCGRID 2020](https://dblp.org/db/conf/ccgrid/ccgrid2020.html#ZhouXGGT0B20): 489-498**



