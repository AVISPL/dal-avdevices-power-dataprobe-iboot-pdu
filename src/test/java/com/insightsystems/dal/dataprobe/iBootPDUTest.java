package com.insightsystems.dal.dataprobe;

import com.avispl.symphony.api.dal.dto.control.ControllableProperty;
import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.insightsystems.symphony.dal.dataprobe.iBootPDU;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

public class iBootPDUTest {
    iBootPDU ibootPDU = new iBootPDU();

    @Before
    public void before() throws Exception {
        ibootPDU.setHost("10.152.67.70");
        ibootPDU.setPort(80);
        ibootPDU.setProtocol("http");
        ibootPDU.setLogin("admin");
        ibootPDU.setPassword("admin");
        ibootPDU.init();
    }

    @Test
    public void testPortControls() throws Exception {
        Map<String,String> stats = ((ExtendedStatistics)ibootPDU.getMultipleStatistics().get(0)).getStatistics();
        Assert.assertEquals("1",stats.get("8: Outlet-8"));

        ControllableProperty cp = new ControllableProperty();
        cp.setProperty("8: Outlet-8");
        cp.setValue("0");
        ibootPDU.controlProperty(cp);

        Thread.sleep(8500);

        stats = ((ExtendedStatistics)ibootPDU.getMultipleStatistics().get(0)).getStatistics();
        Assert.assertEquals("0",stats.get("8: Outlet-8"));

        cp.setValue("1");
        ibootPDU.controlProperty(cp);

        Thread.sleep(8500);

        stats = ((ExtendedStatistics)ibootPDU.getMultipleStatistics().get(0)).getStatistics();
        Assert.assertEquals("1",stats.get("8: Outlet-8"));
    }

    @Test
    public void checkExtendedStatistics() throws Exception {
        Map<String,String> stats = ((ExtendedStatistics)ibootPDU.getMultipleStatistics().get(0)).getStatistics();

        Assert.assertEquals("1",stats.get("1: Outlet-1"));
        Assert.assertEquals("1",stats.get("2: Outlet-2"));
        Assert.assertEquals("1",stats.get("3: Outlet-3"));
        Assert.assertEquals("1",stats.get("4: Outlet-4"));
        Assert.assertEquals("1",stats.get("5: Outlet-5"));
        Assert.assertEquals("1",stats.get("6: Outlet-6"));
        Assert.assertEquals("1",stats.get("7: Outlet-7"));
        Assert.assertEquals("1",stats.get("8: Outlet-8"));

        Assert.assertEquals("0",stats.get("_AllOn"));
        Assert.assertEquals("0",stats.get("_AllOff"));

        //Device Info
        Assert.assertEquals("iBoot-PDU-836d6e",stats.get("DeviceInfo#Name"));
        Assert.assertEquals("8",stats.get("DeviceInfo#NumOutlets"));
        Assert.assertEquals("iBoot-PDU8-C10",stats.get("DeviceInfo#Model"));
        Assert.assertEquals("true",stats.get("DeviceInfo#DeviceCalibrated"));
        Assert.assertEquals("C",stats.get("DeviceInfo#TempUnit"));

        //Power Lines
        Assert.assertEquals("true",stats.get("DeviceInfo#Line1Connected"));
        Assert.assertNotEquals("0.0000",stats.get("DeviceInfo#Line1Voltage"));
        Assert.assertNotEquals("0.0000",stats.get("DeviceInfo#Line1Current"));
        Assert.assertEquals("false",stats.get("DeviceInfo#Line2Connected"));
        Assert.assertEquals("0.0000",stats.get("DeviceInfo#Line2Voltage"));
        Assert.assertEquals("0.0000",stats.get("DeviceInfo#Line2Current"));
        Assert.assertEquals("false",stats.get("DeviceInfo#TempProbe0Connected"));
        Assert.assertEquals("false",stats.get("DeviceInfo#TempProbe1Connected"));
        Assert.assertEquals("0",stats.get("DeviceInfo#TempProbe0Temp"));
        Assert.assertEquals("0",stats.get("DeviceInfo#TempProbe1Temp"));

        //Network Settings
        Assert.assertEquals("dhcp",stats.get("DeviceInfo#networkIpMode"));
        Assert.assertEquals("10.152.67.254",stats.get("DeviceInfo#networkGateway"));
        Assert.assertEquals("68:47:49:83:6d:6e",stats.get("DeviceInfo#networkMacAddress"));
    }

}
