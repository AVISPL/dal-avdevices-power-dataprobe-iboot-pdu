/*
 *  Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */

package com.insightsystems.dal.dataprobe;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.insightsystems.symphony.dal.dataprobe.DataprobeiBootPDUCommunicator;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.avispl.symphony.api.dal.dto.control.ControllableProperty;
import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.api.dal.dto.monitor.Statistics;

/**
 * dataprobeiBootPDUCommunicatorTest
 *
 * @author Harry / Symphony Dev Team<br>
 * Created on 25/10/2024
 * @since 1.0.0
 */
public class DataprobeiBootPDUCommunicatorTest {
	private ExtendedStatistics extendedStatistic;
	private DataprobeiBootPDUCommunicator dataprobeiBootPDUCommunicator;

	@BeforeEach
	void setUp() throws Exception {
		dataprobeiBootPDUCommunicator = new DataprobeiBootPDUCommunicator();
		dataprobeiBootPDUCommunicator.setHost("***REMOVED***");
		dataprobeiBootPDUCommunicator.setPort(80);
		dataprobeiBootPDUCommunicator.setProtocol("http");
		dataprobeiBootPDUCommunicator.setLogin("");
		dataprobeiBootPDUCommunicator.setPassword("");
		dataprobeiBootPDUCommunicator.init();
		dataprobeiBootPDUCommunicator.connect();
	}

	@AfterEach
	void destroy() throws Exception {
		dataprobeiBootPDUCommunicator.disconnect();
		dataprobeiBootPDUCommunicator.destroy();
	}

	@Test
	void testLoginSuccess() throws Exception {
		dataprobeiBootPDUCommunicator.getMultipleStatistics();
	}

	@Test
	public void testGetMultipleStatistics() throws Exception {
		dataprobeiBootPDUCommunicator.getMultipleStatistics();
		List<Statistics> statistics = dataprobeiBootPDUCommunicator.getMultipleStatistics();
		ExtendedStatistics es = (ExtendedStatistics) statistics.get(0);

		Map<String,String> stats = ((ExtendedStatistics)dataprobeiBootPDUCommunicator.getMultipleStatistics().get(0)).getStatistics();
//		Assert.assertEquals("1",stats.get("1: Outlet-1"));
//		Assert.assertEquals("1",stats.get("2: Outlet-2"));
//		Assert.assertEquals("1",stats.get("3: Outlet-3"));
//		Assert.assertEquals("1",stats.get("4: Outlet-4"));
//		Assert.assertEquals("1",stats.get("5: Outlet-5"));
//		Assert.assertEquals("1",stats.get("6: Outlet-6"));
//		Assert.assertEquals("1",stats.get("7: Outlet-7"));
//		Assert.assertEquals("1",stats.get("8: Outlet-8"));
		System.out.println(stats);
	}

	@Test
	public void testControls() throws Exception {
		dataprobeiBootPDUCommunicator.getMultipleStatistics();
		Map<String,String> stats = ((ExtendedStatistics)dataprobeiBootPDUCommunicator.getMultipleStatistics().get(0)).getStatistics();
		System.out.println("Test: " + stats);
		Assert.assertEquals("Off",stats.get("Outlet-8#Status"));

		ControllableProperty cp = new ControllableProperty();
		cp.setProperty("Outlet-8#Status");
		cp.setValue("Off");
		dataprobeiBootPDUCommunicator.controlProperty(cp);

		Thread.sleep(8500);

		stats = ((ExtendedStatistics)dataprobeiBootPDUCommunicator.getMultipleStatistics().get(0)).getStatistics();
		Assert.assertEquals("Off",stats.get("Outlet-8#Status"));

		cp.setValue("On");
		dataprobeiBootPDUCommunicator.controlProperty(cp);

		Thread.sleep(8500);

		stats = ((ExtendedStatistics)dataprobeiBootPDUCommunicator.getMultipleStatistics().get(0)).getStatistics();
		Assert.assertEquals("On",stats.get("Outlet-8#Status"));
	}

	@Test
	void testTimeLogin() throws Exception {
		long exp = System.currentTimeMillis() / 1000 + 3600;
		long iat = System.currentTimeMillis() / 1000;
		System.out.println(exp);
		System.out.println(iat);
	}
}
