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

import java.io.FileNotFoundException;
import java.util.ArrayList;

import javax.swing.JOptionPane;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

public class JEP extends AbstractHandler implements IJavaLaunchConfigurationConstants {

	protected static boolean JEPEnabled = false;
	private String mainClass = "", JEPOClass = "";

	/*
	 * Activate view for energy measurements for methods
	 */
	@Override
	public Object execute(ExecutionEvent event) {

		try {

			JEPEnabled = true;
			JEO.JEOEnabled = false;
			JEOAll.JEOAllEnabled = false;

			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView("com.mist.jepo.JEPOView");

		} catch (PartInitException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		ISelectionService selectService = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService();
		ISelection selection = selectService.getSelection("org.eclipse.jdt.ui.PackageExplorer");

		if (selection instanceof IStructuredSelection) {

			Object element = ((IStructuredSelection) selection).getFirstElement();

			if (element instanceof IJavaElement) {

				IJavaProject jProject = ((IJavaElement) element).getJavaProject();

				try {

					findMainClass(jProject);

					String projectPath = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();

					if (!containLibrary(jProject, "javassist.jar"))
						addLibrary(jProject, new Path(projectPath + "lib/javassist.jar"));
					if (!containLibrary(jProject, "CP.jar"))
						addLibrary(jProject, new Path(projectPath + "lib/CP.jar"));
					if (!containLibrary(jProject, "guava-23.0.jar"))
						addLibrary(jProject, new Path(projectPath + "lib/guava-23.0.jar"));

					addEnergyCode(jProject);

					ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
					ILaunchConfigurationType type = manager
							.getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION);
					ILaunchConfigurationWorkingCopy wc;

					wc = type.newInstance(null, "JEPO");

					wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, jProject.getElementName());
					wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, JEPOClass);
					ILaunchConfiguration config = wc.doSave();
					config.launch(ILaunchManager.RUN_MODE, null);

					// removeLibrary(jProject);
				} catch (CoreException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}

		return null;
	}

	/*
	 * Check required library in selected project
	 */
	private boolean containLibrary(IJavaProject jProject, String jarName) throws JavaModelException {

		IClasspathEntry[] oldEntries = jProject.getRawClasspath();

		for (int i = 0; i < oldEntries.length; i++) {

			if (oldEntries[i].toString().contains(jarName)) {
				return true;
			}

		}
		return false;
	}

	/*
	 * Add required library in selected project
	 */
	private void addLibrary(IJavaProject jProject, IPath jarPath) throws CoreException, FileNotFoundException {

		IClasspathEntry javaEntry = JavaCore.newLibraryEntry(jarPath, null, null);
		IClasspathEntry[] oldEntries = jProject.getRawClasspath();
		IClasspathEntry[] newEntries = new IClasspathEntry[oldEntries.length + 1];
		System.arraycopy(oldEntries, 0, newEntries, 0, oldEntries.length);
		newEntries[oldEntries.length] = javaEntry;
		jProject.setRawClasspath(newEntries, null);

	}

	/*
	 * Find main classes in selected project
	 */
	private void findMainClass(IJavaProject jProject) throws JavaModelException {

		String mainC = "";
		ArrayList<String> mainClasses = new ArrayList<>();
		IPackageFragment[] pkgs = jProject.getPackageFragments();

		for (IPackageFragment pkg : pkgs) {

			if (pkg.getKind() == IPackageFragmentRoot.K_SOURCE) {

				if (pkg.getElementName().equals("com.mist.jepo")) {

					pkg.delete(true, null);
				}
			}
		}

		pkgs = jProject.getPackageFragments();

		for (IPackageFragment pkg : pkgs) {

			if (pkg.getKind() == IPackageFragmentRoot.K_SOURCE) {

				ICompilationUnit[] javaFiles = pkg.getCompilationUnits();

				for (ICompilationUnit javaFile : javaFiles) {

					if (javaFile.getSource().contains("public static void main")) {

						if (pkg.getElementName() == "") {

							mainC = javaFile.getElementName().replace(".java", "");
							mainClasses.add(mainC);

						} else {

							mainC = pkg.getElementName() + "." + javaFile.getElementName().replaceAll(".java", "");
							mainClasses.add(mainC);

						}

					}
				}
			}

		}

		if (mainClasses.size() == 1) {

			mainClass = mainClasses.get(0);

		} else {

			String input = (String) JOptionPane.showInputDialog(null, "Choices", "Select main class",
					JOptionPane.INFORMATION_MESSAGE, null, mainClasses.toArray(), mainClasses.get(0));
			mainClass = input;

		}

	}

	/*
	 * Create package and class to measure energy consumption of methods in selected
	 * project
	 */
	private void addEnergyCode(IJavaProject jProject) throws JavaModelException, PartInitException {

		int objCount = 0, mCount = 0;
		String originalCode = "";

		IPackageFragment[] pkgs = jProject.getPackageFragments();
		IFolder srcFolder = jProject.getProject().getFolder("src");
		IPackageFragment pack = jProject.getPackageFragmentRoot(srcFolder).createPackageFragment("com.mist.jepo", false,
				null);

		originalCode += "package " + pack.getElementName() + ";\n\n" + "import java.io.IOException;\n" + "\n"
				+ "import javassist.CannotCompileException;\n" + "import javassist.ClassPool;\n"
				+ "import javassist.CtClass;\n" + "import javassist.CtMethod;\n"
				+ "import javassist.NotFoundException;\n" + "\n" + "import " + mainClass + ";\n" + "\n"
				+ "public class JEPOInsert {\n"
				+ "	public static void main(String[] args) throws NotFoundException, CannotCompileException, IOException {\n"
				+ "		ClassPool pool = ClassPool.getDefault();\n";

		for (IPackageFragment pkg : pkgs) {

			if (pkg.getKind() == IPackageFragmentRoot.K_SOURCE) {

				ICompilationUnit[] javaFiles = pkg.getCompilationUnits();

				for (ICompilationUnit javaFile : javaFiles) {

					if (javaFile.getElementName() == "JEPOInsert.java") {

						continue;

					}

					originalCode += "\nCtClass cc" + (++objCount) + " = pool.get(\"" + pkg.getElementName() + "."
							+ javaFile.getElementName().substring(0, javaFile.getElementName().indexOf('.')) + "\");\n";

					IType[] itype = javaFile.getAllTypes();

					for (IType it : itype) {

						IMethod[] methods = it.getMethods();

						for (IMethod method : methods) {

							originalCode += "CtMethod m" + (++mCount) + " = cc" + objCount + ".getDeclaredMethod(\""
									+ method.getElementName() + "\");\n";
							originalCode += "m" + mCount + ".insertBefore(\"{com.mist.jepo.CalculatePower.start(\\\""
									+ pkg.getElementName() + "."
									+ javaFile.getElementName().substring(0, javaFile.getElementName().indexOf('.'))
									+ "." + method.getElementName() + "()\\\");}\");\n";

							if (!method.getElementName().equals("main")) {

								originalCode += "m" + mCount + ".insertAfter(\"{com.mist.jepo.CalculatePower.stop(\\\""
										+ pkg.getElementName() + "."
										+ javaFile.getElementName().substring(0, javaFile.getElementName().indexOf('.'))
										+ "." + method.getElementName() + "()\\\");}\");\n";

							} else {

								originalCode += "m" + mCount + ".insertAfter(\"{com.mist.jepo.CalculatePower.stop(\\\""
										+ pkg.getElementName() + "."
										+ javaFile.getElementName().substring(0, javaFile.getElementName().indexOf('.'))
										+ "." + method.getElementName()
										+ "()\\\");com.mist.jepo.CalculatePower.result();}\");\n";

							}
						}
					}

					originalCode += "cc" + objCount + ".toClass();\n";

				}
			}
		}

		originalCode += mainClass.substring(mainClass.lastIndexOf('.') + 1) + ".main(args);\n}\n}\n";
		ICompilationUnit cu = pack.createCompilationUnit("JEPOInsert.java", originalCode, false, null);
		JEPOClass = pack.getElementName() + "."
				+ cu.getElementName().substring(0, cu.getElementName().lastIndexOf('.'));
		originalCode = "";

		PlatformUI.getWorkbench().saveAllEditors(true);

	}

}
