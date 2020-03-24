package net.quantum6.camerafps;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.os.Debug;

public final class SystemKit
{
    /**
     * 
     */
    public final static int CPU_APP     = 0;
    public final static int CPU_WORK    = 1;
    public final static int CPU_IDLE    = 2;

    private static long[] mCpuTimes = new long[3];
    
    /**
     * 
     * @param context
     * @return
     */
    public static String getText(Context context)
    {
        long[] cpuTimes = SystemKit.getCpuState();
        long idlecpu    = cpuTimes[SystemKit.CPU_IDLE] - mCpuTimes[SystemKit.CPU_IDLE];
        long workcpu    = cpuTimes[SystemKit.CPU_WORK] - mCpuTimes[SystemKit.CPU_WORK];
        long appcpu     = cpuTimes[SystemKit.CPU_APP]  - mCpuTimes[SystemKit.CPU_APP];
        
        long totalcpu   = idlecpu + workcpu;
        int  apprate    = (int)(1.0*appcpu /totalcpu*100+0.5);
        int  workrate   = (int)(1.0*workcpu/totalcpu*100+0.5);
        float memory    = (SystemKit.getAppMemory(context)/100)/10.0f;
        mCpuTimes = cpuTimes;
        return ("cpu="+apprate+"%/"+workrate+"%" +"; mem="+memory+"M");
    }

    /**
     * 
     * @param context
     * @return
     */
    public static int getAppMemory(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
     
        List<RunningAppProcessInfo> runningAppProcessesList = am.getRunningAppProcesses();
     
        int currentPid = android.os.Process.myPid();
        for (RunningAppProcessInfo runningAppProcessInfo : runningAppProcessesList) {
            if (runningAppProcessInfo.pid == currentPid)
            {
                //String processName = runningAppProcessInfo.processName;
                //int uid = runningAppProcessInfo.uid;
                int[] pids = new int[] {currentPid};
                Debug.MemoryInfo memoryInfo = am.getProcessMemoryInfo(pids)[0];
                return memoryInfo.getTotalPss();
            }
        }
        return 0;
    }
    
    /**
     * 
     * @return
     */
    public static long[] getCpuState()
    {
        long[] cpu = new long[3];
        getAppCpuTime(cpu);
        getTotalCpuTime(cpu);
        return cpu;
    }
    
    private static void getTotalCpuTime(long[] cpu)
    {
        String[] cpuInfos = null;
        try
        {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream("/proc/stat")), 1000);
            String load = reader.readLine();
            reader.close();
            cpuInfos = load.split(" ");
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
        cpu[CPU_WORK] =
                ( Long.parseLong(cpuInfos[2]) + Long.parseLong(cpuInfos[3])
                + Long.parseLong(cpuInfos[4]) + Long.parseLong(cpuInfos[6])
                + Long.parseLong(cpuInfos[7]) + Long.parseLong(cpuInfos[8]));
        cpu[CPU_IDLE] = Long.parseLong(cpuInfos[5]);
    }
    
    private static void getAppCpuTime(long[] cpu)
    {
        String[] cpuInfos = null;
        try
        {
            int pid = android.os.Process.myPid();
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream("/proc/" + pid + "/stat")), 1000);
            String load = reader.readLine();
            reader.close();
            cpuInfos = load.split(" ");
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
        cpu[CPU_APP] =
                 (Long.parseLong(cpuInfos[13])
                + Long.parseLong(cpuInfos[14])
                + Long.parseLong(cpuInfos[15])
                + Long.parseLong(cpuInfos[16]));
    }
    
}
