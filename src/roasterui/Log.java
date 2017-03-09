/*
 *      Log class
 */
package roasterui;


public class Log {
    public      long timestamp;
    public      int exhaustTemp;
    public      int drumTemp;
    public      int chamberTemp;
    public      boolean firstCrack;
    public      boolean secondCrack;
    public      boolean ignitorStatus;
    public      boolean gasStatus;
    public      boolean exhaustStatus;
    public      boolean coolingStatus;
    public      int     proValve;
    
    @Override
  public String toString() {
    return "timestamp: "+timestamp+" exhaustTemp: "+exhaustTemp;
  }
    
}
