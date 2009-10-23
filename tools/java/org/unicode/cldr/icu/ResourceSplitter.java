// Copyright 2009 Google Inc. All Rights Reserved.

package org.unicode.cldr.icu;

import org.unicode.cldr.icu.ICUResourceWriter.Resource;
import org.unicode.cldr.icu.ICUResourceWriter.ResourceInt;
import org.unicode.cldr.icu.ICUResourceWriter.ResourceString;
import org.unicode.cldr.icu.ICUResourceWriter.ResourceTable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ResourceSplitter {
  private final List<SplitInfo> splitInfos;
  private final Map<String, File> targetDirs;

  public static class SplitInfo {
    final String srcNodePath;
    final String targetNodePath;
    final String targetDirPath;
    
    public SplitInfo(String srcNodePath, String targetDirPath) {
      this(srcNodePath, targetDirPath, null);
    }
    
    public SplitInfo(String srcNodePath, String targetDirPath, String targetNodePath) {
      // normalize
      if (!srcNodePath.endsWith("/")) {
        srcNodePath += "/";
      }
      
      if (targetNodePath == null) {
        targetNodePath = srcNodePath;
      } else if (!targetNodePath.endsWith("/")) {
        targetNodePath += "/";
      }
      
      this.srcNodePath = srcNodePath;
      this.targetNodePath = targetNodePath;
      this.targetDirPath = targetDirPath;
    }
  }
  
  static class ResultInfo {
    final File directory;
    final ResourceTable root;
    
    public ResultInfo(File directory, ResourceTable root) {
      this.directory = directory;
      this.root = root;
    }
  }
  
  static class Path {
    private StringBuilder sb = new StringBuilder();
    private int[] indices = new int[10]; // default length should be enough
    private int depth;
    
    Path(String s) {
      sb.append(s);
    }
    
    String fullPath() {
      return sb.toString();
    }
    
    void push(String pathSegment) {
      if (depth == indices.length) {
        int[] temp = new int[depth * 2];
        System.arraycopy(indices, 0, temp, 0, depth);
        indices = temp;
      }
      indices[depth++] = sb.length();
      sb.append(pathSegment).append("/");
    }
    
    void pop() {
      if (depth == 0) {
        throw new IndexOutOfBoundsException("can't pop past start of path");
      }
      sb.setLength(indices[--depth]);
    }
  }
  
  ResourceSplitter(String baseDirPath, List<SplitInfo> splitInfos) {
    this.splitInfos = splitInfos;
    
    File baseDir = new File(baseDirPath);
    
    int resultLength = 0;
    this.targetDirs = new HashMap<String, File>();
    for (SplitInfo si : splitInfos) {
      String dirPath = si.targetDirPath;
      if (!targetDirs.containsKey(dirPath)) {
        File dir = new File(dirPath);
        if (!dir.isAbsolute()) {
          dir = new File(baseDir, dirPath);
        }
        if (dir.exists()) {
          if (!dir.isDirectory()) {
            throw new IllegalArgumentException(
                "File \"" + dirPath + "\" exists and is not a directory");
          }
          if (!dir.canWrite()) {
            throw new IllegalArgumentException(
                "Cannot write to directory \"" + dirPath + "\"");
          }
        } else {
          if (!dir.mkdirs()) {
            throw new IllegalArgumentException(
                "Unable to create directory path \"" + dirPath + "\"");
          }
        }
        this.targetDirs.put(dirPath, dir);
      }
    }
  }
  
  public List<ResultInfo> split(File targetDir, ResourceTable root) {
    return new SplitProcessor(new ResultInfo(targetDir, root)).split();
  }
  
  // Does the actual work of splitting the resource, based on the ResourceSplitter's specs.
  private class SplitProcessor {
    private final ResultInfo source;

    private final Path path;
    private final Map<String, ResourceTable> resultMap;
    private final List<SplitInfo> remainingInfos;

    private SplitProcessor(ResultInfo source) {
      this.source = source;

      this.path = new Path("/");
      this.resultMap = new HashMap<String, ResourceTable>();
      this.remainingInfos = new ArrayList<SplitInfo>();
      this.remainingInfos.addAll(splitInfos);
    }

    private List<ResultInfo> split() {
      // start split below the root, so we don't match against the locale name
      process(source.root, source.root.first);

      List<ResultInfo> results = new ArrayList<ResultInfo>();
      results.add(source);
      for (Map.Entry<String, ResourceTable> e : resultMap.entrySet()) {
        File dir = targetDirs.get(e.getKey());
        results.add(new ResultInfo(dir, e.getValue()));
      }
      return results;
    }

    private void process(Resource parent, Resource res) {
      while (true) {
        Resource next = res.next;

        path.push(res.name);
        String fullPath = path.fullPath();
        for (Iterator<SplitInfo> iter = splitInfos.iterator(); iter.hasNext();) {
          SplitInfo si = iter.next();
          if (si.srcNodePath.startsWith(fullPath)) {
            if (si.srcNodePath.equals(fullPath)) {
              handleSplit(parent, res, si);
              iter.remove(); // don't need to look for this path anymore
            } else {
              if (res.first != null) {
                process(res, res.first);
              }
            }
            break;
          }
        }
        path.pop();

        if (next == null) {
          break;
        }

        res = next;
      }
    }

    private void handleSplit(Resource parent, Resource res, SplitInfo si) {
      ResourceTable root = getResultRoot(si);

      removeChildFromParent(res, parent);
      
      placeResourceAtPath(root, si.targetNodePath, res);
    }

    private ResourceTable getResultRoot(SplitInfo si) {
      ResourceTable root = resultMap.get(si.targetDirPath);
      if (root == null) {
        root = createRoot();
        resultMap.put(si.targetDirPath, root);
      }
      return root;
    }

    /**
     * Creates a new ResourceTable root.  It is a copy of the top of the source resource.
     * It includes the Version and %%ParentIsRoot resources from the source resource, if present.
     */
    private ResourceTable createRoot() {
      ResourceTable src = source.root;
      ResourceTable root = new ResourceTable();
      root.annotation = src.annotation;
      root.comment = src.comment;
      root.name = src.name;
      
      // if the src contains a version element, copy that element
      final String versionKey = "Version";
      for (Resource child = src.first; child != null; child = child.next) {
        if (versionKey.equals(child.name)) {
          String value = ((ResourceString) child).val;
          root.appendContents(ICUResourceWriter.createString(versionKey, value));
          break;
        }
      }
      
      // if the src contains a "%%ParentIsRoot" element, copy that element
      final String parentRootKey = "%%ParentIsRoot";
      for (Resource child = src.first; child != null; child = child.next) {
        if (parentRootKey.equals(child.name)) {
          ResourceInt parentIsRoot = new ResourceInt();
          parentIsRoot.name = parentRootKey;
          parentIsRoot.val = ((ResourceInt) child).val;
          root.appendContents(parentIsRoot);
          break;
        }
      }
      
      return root;
    }

    /**
     * Ensures that targetNodePath exists rooted at res, and returns the resource at that
     * path.
     */
    private void placeResourceAtPath(Resource root, String targetNodePath, Resource res) {
      String[] nodeNames = targetNodePath.split("/");
      
      // rename the resource with the last name in the path, and shorten the path
      int len = nodeNames.length;
      res.name = nodeNames[--len];
      
      // find or build nodes corresponding to remaining path
      // Skip initial empty node name (because of leading slash in target path)
      for (int i = 1; i < len; ++i) {
        root = findOrCreateNode(root, nodeNames[i]);
      }
      
      // put the renamed node at the end of the new parent
      root.appendContents(res);
    }
    
    private Resource findOrCreateNode(Resource parent, String nodeName) {
      // if no children, just create one, set it as the first child, and return it
      if (parent.first == null) {
        ResourceTable newNode = new ResourceTable();
        newNode.name = nodeName;
        parent.first = newNode;
        return newNode;
      }
      
      // if the first child is the one we want, return it
      if (nodeName.equals(parent.first.name)) {
        return parent.first;
      }
      
      // search for the node we want, remembering its 'elder' sibling, and if we find the
      // one we want, return it
      Resource child = parent.first;
      for (; child.next != null; child = child.next) {
        if (nodeName.equals(child.next.name)) {
          return child.next;
        }
      }
      
      // didn't find it, so create the node, make it the sibling of the youngest child,
      // and return it
      ResourceTable newNode = new ResourceTable();
      newNode.name = nodeName;
      child.next = newNode;
      return newNode;
    }

    /**
     * Removes this single child resource from parent, leaving other children
     * of parent undisturbed.
     * 
     * @return the child resource (with no siblings)
     */
    private Resource removeChildFromParent(Resource child, Resource parent) {
      Resource first = parent.first;
      if (first == child) {
        parent.first = child.next;
      } else {
        while (first.next != null && first.next != child) {
          first = first.next;
        }
        if (first.next == null) {
          throw new IllegalArgumentException("Resource " + child + " is not a child of " + 
              parent);
        }
        first.next = child.next;
      }
      child.next = null;

      return child;
    }
  }
}