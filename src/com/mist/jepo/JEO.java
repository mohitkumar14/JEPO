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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

public class JEO extends AbstractHandler {

	static protected boolean JEOEnabled = false;

	@Override
	/*
	 * Activate view for energy suggestions for current editor
	 */
	public Object execute(ExecutionEvent event) {

		try {

			JEOEnabled = true;
			JEP.JEPEnabled = false;
			JEOAll.JEOAllEnabled = false;

			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView("com.mist.jepo.JEPOView");

			if (JEOEnabled) {

				JEPOView.tableView.removeAll();
				JEPOView.createTableJEO();

			}

		} catch (PartInitException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

}
