/*******************************************************************************
 * Copyright (c) 2019 MIST Lab, Wayne State University
 *
 *  This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     MIST Lab
 *******************************************************************************/

package com.mist.jepo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;

import com.google.common.collect.LinkedHashMultimap;

public class CalculatePower {

	private static double start;
	private static double stop;
	private static double before;
	private static double after;
	private static LinkedHashMultimap<String, ArrayList<Double>> recordStart = LinkedHashMultimap.create();
	private static LinkedHashMultimap<String, ArrayList<Double>> recordStop = LinkedHashMultimap.create();
	private static LinkedHashMultimap<String, ArrayList<Double>> res = LinkedHashMultimap.create();

	/*
	 * Record MSR value at start of method execution
	 */
	public static void start(String name) {

		before = readMSR();
		start = System.currentTimeMillis();
		recordStart.put(name, new ArrayList<Double>(Arrays.asList(start, before)));

	}

	/*
	 * Record MSR value at end of method execution
	 */
	public static void stop(String name) {

		after = readMSR();
		stop = System.currentTimeMillis();
		recordStop.put(name, new ArrayList<Double>(Arrays.asList(stop, after)));

	}

	/*
	 * Read MSR value
	 */
	private static Double readMSR() {

		try {

			Process p = Runtime.getRuntime()
					.exec(new String[] { "/bin/sh", "-c", "sudo modprobe msr |sudo rdmsr -d 0x611" });
			p.waitFor();
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String value=reader.readLine();
			
			if (value == null) {

				System.out.println("Make sure to edit visudo to avoid sudo password for modprobe and rdmsr command!!!");
				return 0.0;

			}

			return Double.parseDouble(value);

		} catch (InterruptedException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return -1.00;
	}

	/*
	 * Calculate and store energy consumption values of each function
	 */
	public static void result() {

		//System.out.println("Started storing results");

		if (recordStart.size() == recordStop.size()) {

			for (String keyMName : recordStart.keySet()) {

				if (recordStart.get(keyMName).size() == 1) {

					Double execStartTime = recordStart.get(keyMName).iterator().next().get(0);
					Double execStartEnergy = recordStart.get(keyMName).iterator().next().get(1);
					Double execStopTime = recordStop.get(keyMName).iterator().next().get(0);
					Double execStopEnergy = recordStop.get(keyMName).iterator().next().get(1);

					res.put(keyMName, new ArrayList<Double>(Arrays.asList((execStopTime - execStartTime) / 1000,
							Math.pow(0.5, (659459 >> 8) & 0x1f) * (execStopEnergy - execStartEnergy))));

				}

				if (recordStart.get(keyMName).size() > 1) {

					Double execStartTime = recordStart.get(keyMName).stream().skip(1).iterator().next().get(0);
					Double execStopTime = recordStop.get(keyMName).iterator().next().get(0);

					if (execStartTime > execStopTime) {

						Iterator<ArrayList<Double>> iterStart = recordStart.get(keyMName).iterator();
						Iterator<ArrayList<Double>> iterStop = recordStop.get(keyMName).iterator();

						while (iterStart.hasNext() && iterStop.hasNext()) {

							ArrayList<Double> startR = iterStart.next();
							ArrayList<Double> stopR = iterStop.next();

							res.put(keyMName, new ArrayList<Double>(Arrays.asList((stopR.get(0) - startR.get(0)) / 1000,
									Math.pow(0.5, (659459 >> 8) & 0x1f) * (stopR.get(1) - startR.get(1)))));

						}

					} else {

						Iterator<ArrayList<Double>> iterStart = recordStart.get(keyMName).iterator();
						LinkedList<ArrayList<Double>> list = new LinkedList<ArrayList<Double>>(
								recordStop.get(keyMName));
						Iterator<ArrayList<Double>> iterStop = list.descendingIterator();

						while (iterStart.hasNext() && iterStop.hasNext()) {

							ArrayList<Double> startR = iterStart.next();
							ArrayList<Double> stopR = iterStop.next();

							res.put(keyMName, new ArrayList<Double>(Arrays.asList((stopR.get(0) - startR.get(0)) / 1000,
									Math.pow(0.5, (659459 >> 8) & 0x1f) * (stopR.get(1) - startR.get(1)))));

						}
					}
				}
			}
		}

		//System.out.println("Stopped storing results");

		writeToFile();

	}

	/*
	 * Write method energy consumption values to a file
	 */
	public static void writeToFile() {

		BufferedWriter writer;

		//System.out.println("Started Writing to file");

		try {

			writer = new BufferedWriter(new FileWriter("result.txt"));

			for (String key : res.keySet()) {

				Iterator<ArrayList<Double>> recordIterator = res.get(key).iterator();

				while (recordIterator.hasNext()) {

					ArrayList<Double> record = recordIterator.next();
					writer.write(key + " " + record.get(0) + " " + record.get(1) + "\n");
					//System.out.println(key + " " + record.get(0) + " " + record.get(1) + "\n");

				}
			}

			writer.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//System.out.println("Finished Writing to file");

	}

	public static LinkedHashMultimap<String, ArrayList<Double>> getRes() {

		return res;

	}

}
