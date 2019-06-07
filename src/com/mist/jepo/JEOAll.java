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

public class JEOAll extends AbstractHandler {

	protected static boolean JEOAllEnabled = false;

	@Override
	/*
	 * Activate view for energy suggestions for whole project
	 */
	public Object execute(ExecutionEvent event) {

		try {

			JEO.JEOEnabled = false;
			JEP.JEPEnabled = false;
			JEOAll.JEOAllEnabled = true;

			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView("com.mist.jepo.JEPOView");

			if (JEOAllEnabled) {

				JEPOView.tableView.removeAll();
				JEPOView.createTableJEOAll();

			}

		} catch (PartInitException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

}
