package cn.edu.kpi.ruoyi;

public final class KpiUnauthenticatedException extends RuntimeException {
    public KpiUnauthenticatedException() { super("KPI request requires an authenticated user"); }
}
