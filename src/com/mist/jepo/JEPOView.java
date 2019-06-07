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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.Scanner;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.ITextEditor;

import com.google.common.collect.LinkedHashMultimap;

public class JEPOView extends ViewPart {

	protected static Table tableView;

	private static String record = "", lastRecord = "", lastCode = "";
	private static TableItem addRow[];
	private static IEditorPart editor;
	private static ITextEditor ite;
	private static IDocument doc;
	private IRegion lineInfo;
	private static TableColumn c1, c2, c3;
	private static LinkedHashMultimap<String, ArrayList<String>> allSuggestions = LinkedHashMultimap.create();

	/*
	 * Return java source code in current editor
	 */
	private static String getCurrentEditorContent() {

		editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();

		if (!(editor instanceof ITextEditor))
			return "No editor!!!";

		if ((editor.getTitle()).contains("java")) {
			ite = (ITextEditor) editor;
			doc = ite.getDocumentProvider().getDocument(ite.getEditorInput());
			return doc.get();
		} else {
			return "No Java file open!!!";
		}

	}

	/*
	 * Store energy savings suggestions for current editor
	 */
	private static LinkedHashMap<Integer, String> suggestionLocations(String code) {

		String[] lines = code.split("\\r?\\n");
		LinkedHashMap<Integer, String> listCode = new LinkedHashMap<Integer, String>();

		for (int i = 0; i < lines.length; i++) {
			String suggestion = energyTableMessage(lines[i]);
			if (suggestion.equals(""))
				continue;
			else
				listCode.put(i + 1, suggestion);
		}

		return listCode;

	}

	/*
	 * Store energy savings suggestions for selected project
	 */
	private static void suggestionLocationsAll(String id, String code) {

		String[] lines = code.split("\\r?\\n");

		for (int i = 0; i < lines.length; i++) {

			String suggestion = energyTableMessage(lines[i]);

			if (suggestion.equals(""))
				continue;
			else {
				allSuggestions.put(id, new ArrayList<String>(Arrays.asList(String.valueOf(i + 1), suggestion)));
			}
		}

	}

	/*
	 * Return selected project from package explorer
	 */
	private static IJavaProject selectedProject() {

		ISelectionService selectService = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService();
		ISelection selection = selectService.getSelection("org.eclipse.jdt.ui.PackageExplorer");
		IJavaProject jProject = null;

		if (selection instanceof IStructuredSelection) {
			Object element = ((IStructuredSelection) selection).getFirstElement();

			if (element instanceof IJavaElement) {
				jProject = ((IJavaElement) element).getJavaProject();

			}
		}

		return jProject;

	}

	/*
	 * Return the energy measurement of methods
	 */
	private static String readFile() {

		IJavaProject jProject = selectedProject();

		try {

			IProject project = jProject.getProject();
			jProject.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
			IFile file = project.getFile("result.txt");

			if (!file.exists())
				return record;

			InputStream fileContent = file.getContents();
			Scanner scanner = new Scanner(fileContent);
			record = "";

			while (scanner.hasNextLine()) {

				record += scanner.nextLine() + "\n";

			}
			scanner.close();

		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return record;

	}

	/*
	 * Return energy suggestions for a given String
	 */
	private static String energyTableMessage(String msg) {

		String res = "";

		if (msg.contains(" double ") || msg.contains(" byte ") || msg.contains(" short ") || msg.contains(" float ")
				|| msg.contains(" char ") || msg.contains("\tdouble ") || msg.contains("\tbyte ")
				|| msg.contains("\tshort ") || msg.contains("\tfloat ") || msg.contains("\tchar ")) {
			res += "int is the most energy efficient primitive data type. Replace if possible. ";
		}

		if (msg.contains(" double ") || msg.contains(" float ") || msg.contains("\tdouble ")
				|| msg.contains("\tfloat ")) {
			res += "Scientific notation results in lower energy consumption of decimal numbers. ";
		}

		if (msg.contains(" Double ") || msg.contains(" Byte ") || msg.contains(" Short ") || msg.contains(" Float ")
				|| msg.contains(" Character ") || msg.contains(" Integer ") || msg.contains(" Long ")
				|| msg.contains("\tDouble ") || msg.contains("\tByte ") || msg.contains("\tShort ")
				|| msg.contains("\tFloat ") || msg.contains("\tCharacter ") || msg.contains("\tInteger ")
				|| msg.contains("\tLong ")) {
			res += "Integer Wrapper class object is the most energy efficient. Replace if possible. ";
		}

		if (msg.matches(".*static.*=.*;") && !msg.contains(" main(")) {
			res += "static keyword consumes up to 17,700% more energy. Avoid if possible. ";
		}

		if (msg.matches(".*\\d+%\\d+.*")) {
			res += "Modulus arithmetic operator consumes up to 1,620% more energy than other arithmetic operators. ";
		}

		if (msg.matches(".*=.*?.*:.*;")) {
			res += "Ternary operator consume up to 37% more energy than if-then-else statement. ";
		}

		if (msg.matches(".*\\(+.*\\|\\|.*\\)+.*") || msg.matches(".*\\(+.*\\&\\&.*\\)+.*")) {
			res += "Put most common case first for lower energy consumption. ";
		}

		if (msg.matches(".*\"[ ]*\\+[ ]*\".*") || msg.matches(".*\"[ ]*\\+.*") || msg.matches(".*\\+[ ]*\".*")) {
			res += "StringBuilder append method consumes much lower energy than String concatenation operator. ";
		}

		if (msg.contains(".compareTo(")) {
			res += "String compareTo method consumes up to 33% more energy than String equals method. ";
		}

		if (msg.contains("Arrays.copyOf(") || msg.contains(".clone(")) {
			res += "System.arraycopy() is the most energy efficient way to copy Arrays. ";
		}

		if (msg.matches(".*\\[\\]\\[\\].*=.*\\[.*\\]\\[.*\\].*;") || msg.matches(".*\\[\\]\\[\\].*;")) {
			res += "Two-dimensional Array column traversal result in up to 793% more energy. ";
		}

		return res;
	}

	/*
	 * Create table of energy suggestions for current editor
	 */
	public static void createTableJEO() {

		c1.dispose();
		c2.dispose();
		c3.dispose();

		c1 = new TableColumn(tableView, SWT.NONE);
		c2 = new TableColumn(tableView, SWT.NONE);
		c3 = new TableColumn(tableView, SWT.NONE);

		c1.setText("ID");
		c2.setText("Line Number");
		c3.setText("Suggestion");

		addRowsJEO();

	}

	/*
	 * Store energy suggestions as rows in the table for current editor
	 */
	public static void addRowsJEO() {

		int rowCount = 0;
		lastCode = getCurrentEditorContent();
		LinkedHashMap<Integer, String> codeView = suggestionLocations(lastCode);
		addRow = new TableItem[codeView.size()];

		for (int i : codeView.keySet()) {
			addRow[rowCount] = new TableItem(tableView, SWT.NONE);
			addRow[rowCount].setText(new String[] { String.valueOf(rowCount + 1), String.valueOf(i), codeView.get(i) });
			++rowCount;
		}

		c1.pack();
		c2.pack();
		c3.pack();

	}

	/*
	 * Create table of energy suggestions for whole project
	 */
	public static void createTableJEOAll() {
		c1.dispose();
		c2.dispose();
		c3.dispose();

		c1 = new TableColumn(tableView, SWT.NONE);
		c2 = new TableColumn(tableView, SWT.NONE);
		c3 = new TableColumn(tableView, SWT.NONE);

		c1.setText("ID");
		c2.setText("Line Number");
		c3.setText("Suggestion");

		addRowsJEOAll();

	}

	/*
	 * Store energy suggestions as rows in the table for whole project
	 */
	public static void addRowsJEOAll() {

		int rowCount = 0;
		IJavaProject jProject = selectedProject();

		allSuggestions.clear();

		if (jProject != null) {
			try {

				IPackageFragment[] pkgs = jProject.getPackageFragments();

				for (IPackageFragment pkg : pkgs) {

					if (pkg.getKind() == IPackageFragmentRoot.K_SOURCE) {

						ICompilationUnit[] javaFiles = pkg.getCompilationUnits();

						for (ICompilationUnit javaFile : javaFiles) {

							suggestionLocationsAll(pkg.getElementName() + "." + javaFile.getElementName(),
									javaFile.getSource());
						}
					}
				}

			} catch (JavaModelException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		addRow = new TableItem[allSuggestions.size()];

		for (String i : allSuggestions.keySet()) {

			Iterator<ArrayList<String>> iterSuggest = allSuggestions.get(i).iterator();

			while (iterSuggest.hasNext()) {

				ArrayList<String> record = iterSuggest.next();
				addRow[rowCount] = new TableItem(tableView, SWT.NONE);
				addRow[rowCount].setText(new String[] { i, record.get(0), record.get(1) });
				++rowCount;
			}

		}

		c1.pack();
		c2.pack();
		c3.pack();

	}

	/*
	 * Create table of energy measurements
	 */
	public static void createTableJEP() {

		c1.dispose();
		c2.dispose();
		c3.dispose();

		c1 = new TableColumn(tableView, SWT.NONE);
		c2 = new TableColumn(tableView, SWT.NONE);
		c3 = new TableColumn(tableView, SWT.NONE);

		c1.setText("Method Name");
		c2.setText("Execution Time (s)");
		c3.setText("Execution Energy (J)");

		addRowsJEP();

	}

	/*
	 * Store energy measurements as rows in the table
	 */
	public static void addRowsJEP() {

		lastRecord = readFile();

		if (record != "") {

			String[] lines = record.split("\\n");
			addRow = new TableItem[lines.length];

			for (int i = 0; i < lines.length; i++) {

				String[] splitStr = lines[i].split(" ");
				addRow[i] = new TableItem(tableView, SWT.NONE);
				addRow[i].setText(splitStr);

			}
		}

		c1.pack();
		c2.pack();
		c3.pack();

	}

	/*
	 * Create view using table
	 */
	@Override
	public void createPartControl(Composite parent) {

		tableView = new Table(parent, 0);
		tableView.setHeaderVisible(true);
		tableView.setLinesVisible(true);

		c1 = new TableColumn(tableView, SWT.NONE);
		c2 = new TableColumn(tableView, SWT.NONE);
		c3 = new TableColumn(tableView, SWT.NONE);

		if (JEP.JEPEnabled)
			createTableJEP();
		else if (JEO.JEOEnabled)
			createTableJEO();
		else if (JEOAll.JEOAllEnabled)
			createTableJEOAll();

		/*
		 * Update view on mouse movement
		 */
		parent.getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {

				Display dis = Display.getDefault();
				dis.addFilter(SWT.MouseMove, new Listener() {

					@Override
					public void handleEvent(Event event) {

						if (JEP.JEPEnabled) {

							if (!readFile().equals(lastRecord)) {

								tableView.removeAll();
								createTableJEP();

							}

						} else if (JEO.JEOEnabled) {

							if (!getCurrentEditorContent().equals(lastCode)) {

								lastCode = getCurrentEditorContent();
								tableView.removeAll();
								createTableJEO();

							}
						}

					}
				});
			}
		});

		/*
		 * Update view on key movement
		 */
		parent.getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {

				Display dis = Display.getDefault();
				dis.addFilter(SWT.KeyUp, new Listener() {

					@Override
					public void handleEvent(Event event) {

						if (JEP.JEPEnabled) {

							if (!readFile().equals(lastRecord)) {

								tableView.removeAll();
								createTableJEP();

							}

						} else if (JEO.JEOEnabled) {

							if (!getCurrentEditorContent().equals(lastCode)) {

								lastCode = getCurrentEditorContent();
								tableView.removeAll();
								createTableJEO();

							}
						}
					}
				});
			}
		});

		/*
		 * Select line in the current editor for the suggestion selected in the view
		 */
		tableView.getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {

				tableView.addListener(SWT.Selection, new Listener() {

					@Override
					public void handleEvent(Event event) {

						if (JEO.JEOEnabled) {

							try {

								lineInfo = doc.getLineInformation(
										Integer.parseInt(addRow[tableView.getSelectionIndex()].getText(1)) - 1);

							} catch (NumberFormatException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (BadLocationException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

							ite.selectAndReveal(lineInfo.getOffset(), lineInfo.getLength());

						}
					}
				});
			}
		});

	}

	public static Table getTableView() {

		return tableView;

	}

	public static void setTableView(Table tableView) {

		JEPOView.tableView = tableView;

	}

	@Override
	public void setFocus() {
	}

}