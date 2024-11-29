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
		dataprobeiBootPDUCommunicator.setHost("");
		dataprobeiBootPDUCommunicator.setPort(80);
		dataprobeiBootPDUCommunicator.setProtocol("http");
		dataprobeiBootPDUCommunicator.setLogin("");
		dataprobeiBootPDUCommunicator.setPassword("");
		dataprobeiBootPDUCommunicator.init();
		dataprobeiBootPDUCommunicator.connect();
		dataprobeiBootPDUCommunicator.setSequenceProperties("01, 02, 03");
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
		Assert.assertEquals("1",stats.get("1: Outlet_1"));
		Assert.assertEquals("1",stats.get("2: Outlet_2"));
		Assert.assertEquals("1",stats.get("3: Outlet_3"));
		Assert.assertEquals("1",stats.get("4: Outlet_4"));
		Assert.assertEquals("1",stats.get("5: Outlet_5"));
		Assert.assertEquals("1",stats.get("6: Outlet_6"));
		Assert.assertEquals("1",stats.get("7: Outlet_7"));
		Assert.assertEquals("1",stats.get("8: Outlet_8"));
		System.out.println(stats);
	}

	@Test
	public void testConfigSequence() throws Exception {
		dataprobeiBootPDUCommunicator.getMultipleStatistics();
		Thread.sleep(2000);
		Map<String,String> stats = ((ExtendedStatistics)dataprobeiBootPDUCommunicator.getMultipleStatistics().get(0)).getStatistics();
		System.out.println("Test: " + stats);
	}

	@Test
	public void testSequence() throws Exception {
		dataprobeiBootPDUCommunicator.getMultipleStatistics();
		String updateSequence = "Sequence_01#Control";
		ControllableProperty cp = new ControllableProperty();
		cp.setProperty(updateSequence);
		cp.setValue("run");
		dataprobeiBootPDUCommunicator.controlProperty(cp);
		Thread.sleep(8500);
	}

	@Test
	public void testCycle() throws Exception {
		dataprobeiBootPDUCommunicator.getMultipleStatistics();
		String updateSequence = "Group_group01#Cycle";
		ControllableProperty cp = new ControllableProperty();
		cp.setProperty(updateSequence);
		dataprobeiBootPDUCommunicator.controlProperty(cp);
		Thread.sleep(10000);
	}

	@Test
	public void testControls() throws Exception {
		dataprobeiBootPDUCommunicator.getMultipleStatistics();
		String fieldUpdate = "Group_group01#Status";
		Map<String,String> stats = ((ExtendedStatistics)dataprobeiBootPDUCommunicator.getMultipleStatistics().get(0)).getStatistics();
		System.out.println("Test: " + stats);

		ControllableProperty cp = new ControllableProperty();
		cp.setProperty(fieldUpdate);
		cp.setValue("1");
		dataprobeiBootPDUCommunicator.controlProperty(cp);

		Thread.sleep(8500);

		stats = ((ExtendedStatistics)dataprobeiBootPDUCommunicator.getMultipleStatistics().get(0)).getStatistics();
		Assert.assertEquals("On",stats.get(fieldUpdate));
	}
}
