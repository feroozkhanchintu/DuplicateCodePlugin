package eclipse.commandline;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

public class TestModelProvider extends ModelProvider {

	private static class Sorter implements Comparator {
		public int compare(Object o1, Object o2) {
			IResourceDelta d1 = (IResourceDelta) o1;
			IResourceDelta d2= (IResourceDelta) o2;
			return d1.getResource().getFullPath().toPortableString().compareTo(
				d2.getResource().getFullPath().toPortableString());
		}
	}

	private static final Comparator COMPARATOR= new Sorter();

	public static IResourceDelta LAST_DELTA;
	public static boolean IS_COPY_TEST;

	private static final int PRE_DELTA_FLAGS= IResourceDelta.CONTENT | IResourceDelta.MOVED_TO |
		IResourceDelta.MOVED_FROM | IResourceDelta.OPEN;

	public static void clearDelta() {
		LAST_DELTA= null;
	}

	public IStatus validateChange(IResourceDelta delta, IProgressMonitor pm) {
		LAST_DELTA= delta;
		return super.validateChange(delta, pm);
	}

	

	private static String printDelta(IResourceDelta delta) {
		StringBuffer buf= new StringBuffer();
		appendDelta(delta, 0, buf);
		return buf.toString();
	}

	private static StringBuffer appendDelta(IResourceDelta delta, int indent, StringBuffer buf) {
		for (int i= 0; i < indent; i++) {
			buf.append("  ");
		}
		buf.append(delta.getResource().toString());
		buf.append("-").append(getKindString(delta.getKind()));
		int flags= delta.getKind();
		if (flags != 0) {
			buf.append("-").append(getFlagString(flags)).append('\n');
		}

		IResourceDelta[] affectedChildren= delta.getAffectedChildren();
		Arrays.sort(affectedChildren, COMPARATOR);

		for (int i= 0; i < affectedChildren.length; i++) {
			appendDelta(affectedChildren[i], indent + 1, buf);
		}
		return buf;
	}



	private static String getKindString(int kind) {
		switch (kind) {
			case IResourceDelta.CHANGED:
				return "CHANGED";
			case IResourceDelta.ADDED:
				return "ADDED";
			case IResourceDelta.REMOVED:
				return "REMOVED";
			case IResourceDelta.ADDED_PHANTOM:
				return "ADDED_PHANTOM";
			case IResourceDelta.REMOVED_PHANTOM:
				return "REMOVED_PHANTOM";
			default:
				return "NULL";
		}
	}

	private static String getFlagString(int flags) {
		StringBuffer buf= new StringBuffer();
		appendFlag(flags, IResourceDelta.CONTENT, "CONTENT", buf);
		appendFlag(flags, IResourceDelta.DESCRIPTION, "DESCRIPTION", buf);
		appendFlag(flags, IResourceDelta.ENCODING, "ENCODING", buf);
		appendFlag(flags, IResourceDelta.OPEN, "OPEN", buf);
		appendFlag(flags, IResourceDelta.MOVED_TO, "MOVED_TO", buf);
		appendFlag(flags, IResourceDelta.MOVED_FROM, "MOVED_FROM", buf);
		appendFlag(flags, IResourceDelta.TYPE, "TYPE", buf);
		appendFlag(flags, IResourceDelta.SYNC, "SYNC", buf);
		appendFlag(flags, IResourceDelta.MARKERS, "MARKERS", buf);
		appendFlag(flags, IResourceDelta.REPLACED, "REPLACED", buf);
		return buf.toString();
	}

	private static void appendFlag(int flags, int flag, String name, StringBuffer res) {
		if ((flags & flag) != 0) {
			if (res.length() > 0) {
				res.append("-");
			}
			res.append(name);
		}
	}

	private static IResourceDelta[] getExpectedChildren(IResourceDelta delta) {
		List result= new ArrayList();
		IResourceDelta[] children= delta.getAffectedChildren();
		for (int i= 0; i < children.length; i++) {
			IResourceDelta child= children[i];
			IResource resource= child.getResource();
			if (resource != null && isIgnorable(resource))
				continue;
			if (child.getAffectedChildren().length > 0) {
				result.add(child);
			} else {
				int flags= child.getFlags();
				if (flags == 0 || (flags & PRE_DELTA_FLAGS) != 0) {
					result.add(child);
				}
			}
		}
		return (IResourceDelta[]) result.toArray(new IResourceDelta[result.size()]);
	}

	private static boolean isIgnorable(IResource resource) {
		final String name= resource.getName();
		if (resource.getType() != IResource.FOLDER)
			return false;
		return name.startsWith(".");
	}


	private static boolean contains(IResourceDelta[] expectedChildren, IResourceDelta actualDelta) {
		IResource actualResource= actualDelta.getResource();
		for (int i= 0; i < expectedChildren.length; i++) {
			if (isSameResourceInCopy(expectedChildren[i].getResource(), actualResource))
				return true;
		}
		return false;
	}

	private static boolean isSameResourceInCopy(IResource expected, IResource actual) {
		IPath expectedPath= expected.getFullPath();
		IPath actualPath= actual.getFullPath();
		if (expectedPath.segmentCount()!= actualPath.segmentCount())
			return false;
		for(int i= 0; i < expectedPath.segmentCount(); i++) {
			String expectedSegment= expectedPath.segment(i);
			if (expectedSegment.startsWith("UnusedName") || expectedSegment.equals("unusedName"))
				continue;
			if (!expectedSegment.equals(actualPath.segment(i)))
				return false;
		}
		return true;
	}

	
}
