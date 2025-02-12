package eclipse.commandline;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.SourceRange;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.internal.corext.refactoring.ParameterInfo;
import org.eclipse.jdt.internal.corext.refactoring.changes.CreateCompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.code.ExtractMethodRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.util.TextEditUtil;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ltk.core.refactoring.Change;

@SuppressWarnings("restriction")
public class Application implements IApplication {

	public static final String CONTAINER = "src";
	private static IPackageFragmentRoot fgRoot;
	private static IPackageFragment fgPackageP;
	private static IJavaProject fgJavaTestProject;
	private static IPackageFragmentRoot fgJRELibrary;
	private IPackageFragment fSelectionPackage;

	static String readFile(String path, Charset encoding) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}

	public ISourceRange getSelection(ICompilationUnit cu, int startLine, int startColumn, int endLine, int endColumn)
			throws Exception {
		IDocument document = new Document(cu.getSource());
		System.out.println(document.get(15270, 282));
		int offset = getOffset(document, startLine, startColumn);
		int end = getOffset(document, endLine, endColumn);
		return new SourceRange(offset, end - offset);
	}

	private static int getOffset(IDocument document, int line, int column) throws BadLocationException {
		int r = document.getLineInformation(line - 1).getOffset();
		IRegion region = document.getLineInformation(line - 1);
		int lineTabCount = calculateTabCountInLine(document.get(region.getOffset(), region.getLength()), column);
		r += (column - 1) - (lineTabCount * getTabWidth()) + lineTabCount;
		return r;
	}

	private static final int getTabWidth() {
		return 4;
	}

	public static int calculateTabCountInLine(String lineSource, int lastCharOffset) {
		int acc = 0;
		int charCount = 0;
		for (int i = 0; charCount < lastCharOffset - 1; i++) {
			if ('\t' == lineSource.charAt(i)) {
				acc++;
				charCount += getTabWidth();
			} else
				charCount += 1;
		}
		return acc;
	}

	public static IPackageFragmentRoot getDefaultSourceFolder() throws Exception {
		if (fgRoot != null)
			return fgRoot;
		throw new FileNotFoundException();
	}

	@Override
	public Object start(IApplicationContext arg0) throws Exception {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		System.out.println(workspace);
		IWorkspaceRoot root = workspace.getRoot();
		IProject[] projects = root.getProjects();
		IProject project = root.getProject("apps/");
		System.out.println(project);
		IPackageFragment[] packages = JavaCore.create(project).getPackageFragments();

		for (int i = 0; i < packages.length; i++) {
			if (packages[i].getElementName().equals("com.clickability.security")) {
				System.out.println(packages[i]);
				System.out.println(i);
				if (i == 609) {
					System.out.println(packages[i]);
					ICompilationUnit[] units = packages[i].getCompilationUnits();
					ASTParser astParser = ASTParser.newParser(8);
					astParser.setSource(units[2]);
					astParser.setResolveBindings(true);
					ASTNode result = astParser.createAST((IProgressMonitor) null);
					CompilationUnit astRoot = (CompilationUnit) result;

					ISourceRange sourceRange = getSelection(units[2], 272, 1, 276, 1);
					System.out.println(sourceRange.getLength());
					System.out.println(sourceRange.getOffset());
					ExtractMethodRefactoring refactoring = new ExtractMethodRefactoring(astRoot,
							sourceRange.getOffset(), sourceRange.getLength());

					refactoring.setMethodName("extracted");
					refactoring.setVisibility(Modifier.PRIVATE);

					refactoring.checkInitialConditions(new NullProgressMonitor());
					// refactoring.setDestination(0);
					// List parameters= refactoring.getParameterInfos();
					// System.out.println(parameters.size());
					refactoring.checkFinalConditions(new NullProgressMonitor());
					Change change = refactoring.createChange(new NullProgressMonitor());
					change.perform(new NullProgressMonitor());
					change.dispose();
					System.out.println(change.getModifiedElement().toString());
				}
			}
		}
		// ICompilationUnit[] units = packages[0].getCompilationUnits();
		// ASTParser astParser = ASTParser.newParser(8);
		// astParser.setSource(units[0]);
		// astParser.setResolveBindings(true);
		// ASTNode result = astParser.createAST((IProgressMonitor) null);
		// CompilationUnit astRoot = (CompilationUnit) result;
		// ISourceRange sourceRange = getSelection(units[0], 4, 9, 4, 31);
		//
		// ExtractMethodRefactoring refactoring = new
		// ExtractMethodRefactoring(astRoot, sourceRange.getOffset(),
		// sourceRange.getLength());
		//
		// refactoring.setMethodName("extracted");
		// refactoring.setVisibility(Modifier.PRIVATE);
		//
		// refactoring.checkInitialConditions(new NullProgressMonitor());
		// //refactoring.setDestination(0);
		// //List parameters= refactoring.getParameterInfos();
		// //System.out.println(parameters.size());
		// refactoring.checkFinalConditions(new NullProgressMonitor());
		// Change change = refactoring.createChange(new NullProgressMonitor());
		// change.perform(new NullProgressMonitor());
		// change.dispose();
		// System.out.println(change.getModifiedElement().toString());
		workspace.save(true, null);
		return IApplication.EXIT_OK;
	}

	@Override
	public void stop() {

	}

}
