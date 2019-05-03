/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * ClassCache.java
 * Copyright (C) 2010-2019 University of Waikato, Hamilton, New Zealand
 */
package nz.ac.waikato.cms.locator;

import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * A class that stores all classes on the classpath.
 * <br>
 * Use "nz.ac.waikato.cms.locator.ClassCache.LOGLEVEL" with a value of
 * "{OFF|SEVERE|WARNING|INFO|CONFIG|FINE|FINER|FINEST}" to set custom
 * logging level.
 *
 * @author  fracpete (fracpete at waikato dot ac dot nz)
 */
public class ClassCache
  implements Serializable {

  /** for serialization. */
  private static final long serialVersionUID = -2973185784363491578L;

  /**
   * For listening to the class traversal and populating the caches.
   *
   * @author  fracpete (fracpete at waikato dot ac dot nz)
   */
  public static class Listener
    implements TraversalListener {

    /** for caching all classes on the class path (package-name &lt;-&gt; Set with classnames). */
    protected Map<String,Set<String>> m_NameCache;

    /** caches the classnames per classpath part (path &lt;-&gt; Set with classnames). */
    protected Map<URL,Set<String>> m_ClasspathPartCache;

    /** the logger in use. */
    protected transient Logger m_Logger;

    /**
     * Initializes the listener.
     */
    public Listener() {
      m_NameCache          = new HashMap<>();
      m_ClasspathPartCache = new HashMap<>();
    }

    /**
     * Returns the logger in use.
     *
     * @return		the logger
     */
    public synchronized Logger getLogger() {
      if (m_Logger == null) {
	m_Logger = Logger.getLogger(getClass().getName());
	m_Logger.setLevel(LoggingHelper.getLevel(getClass()));
      }
      return m_Logger;
    }

    /**
     * Gets called when a class is being traversed.
     *
     * @param classname		the current classname
     * @param classPathPart	the current classpath part this classname is
     *                          located in
     */
    @Override
    public void traversing(String classname, URL classPathPart) {
      String		pkgname;

      // classname and package
      pkgname = ClassPathTraversal.extractPackage(classname);

      // add to package cache
      if (!m_NameCache.containsKey(pkgname))
        m_NameCache.put(pkgname, new HashSet<>());
      m_NameCache.get(pkgname).add(classname);

      // add to classpath part cache
      if (!m_ClasspathPartCache.containsKey(classPathPart))
	m_ClasspathPartCache.put(classPathPart, new HashSet<>());
      m_ClasspathPartCache.get(classPathPart).add(classname);
    }

    /**
     * Returns the name cache.
     *
     * @return		the cache
     */
    public Map<String,Set<String>> getNameCache() {
      return m_NameCache;
    }

    /**
     * Returns the classpath path cache.
     *
     * @return		the cache
     */
    public Map<URL,Set<String>> getClasspathPartCache() {
      return m_ClasspathPartCache;
    }
  }

  /** the logger in use. */
  protected transient Logger m_Logger;

  /** for caching all classes on the class path (package-name &lt;-&gt; Set with classnames). */
  protected Map<String,Set<String>> m_NameCache;

  /** the classpath path part cache (classpath part &lt;-&gt; set with classnames). */
  protected Map<URL,Set<String>> m_ClasspathPartCache;

  /**
   * Initializes the cache.
   * Uses the {@link ClassPathTraversal} class.
   */
  public ClassCache() {
    super();
    initialize(new ClassPathTraversal());
  }

  /**
   * Initializes the cache.
   *
   * @param traversal the traversal instance to use, can be null
   */
  public ClassCache(ClassTraversal traversal) {
    super();
    initialize((traversal == null) ? new ClassPathTraversal() : traversal);
  }

  /**
   * Returns whether logging is enabled.
   *
   * @return		true if enabled
   */
  public boolean isLoggingEnabled() {
    return true;
  }

  /**
   * Returns the logger in use.
   *
   * @return		the logger
   */
  public synchronized Logger getLogger() {
    if (m_Logger == null) {
      m_Logger = Logger.getLogger(getClass().getName());
      m_Logger.setLevel(LoggingHelper.getLevel(getClass()));
    }
    return m_Logger;
  }

  /**
   * Removes the classname from the cache.
   *
   * @param classname	the classname to remove
   * @return		true if the removal changed the cache
   */
  public boolean remove(String classname) {
    String		pkgname;
    Set<String>		names;

    for (URL part: m_ClasspathPartCache.keySet())
      m_ClasspathPartCache.get(part).remove(classname);

    classname = ClassPathTraversal.cleanUp(classname);
    pkgname   = ClassPathTraversal.extractPackage(classname);
    names     = m_NameCache.get(pkgname);
    if (names != null)
      return names.remove(classname);
    else
      return false;
  }

  /**
   * Returns all the stored packages.
   *
   * @return		the package names
   */
  public Iterator<String> packages() {
    return m_NameCache.keySet().iterator();
  }

  /**
   * Returns all the classes for the given package.
   *
   * @param pkgname	the package to get the classes for
   * @return		the classes
   */
  public Set<String> getClassnames(String pkgname) {
    if (m_NameCache.containsKey(pkgname))
      return m_NameCache.get(pkgname);
    else
      return new HashSet<>();
  }

  /**
   * Returns all the stored classpath parts.
   *
   * @return		the classpath parts
   */
  public Iterator<URL> classpathParts() {
    return m_ClasspathPartCache.keySet().iterator();
  }

  /**
   * Returns all the classes for the given classpath part.
   *
   * @param part	the classpath part to get the classes for
   * @return		the classes
   */
  public Set<String> getClassnames(URL part) {
    if (m_ClasspathPartCache.containsKey(part))
      return m_ClasspathPartCache.get(part);
    else
      return new HashSet<>();
  }

  /**
   * Initializes the cache.
   */
  protected void initialize(ClassTraversal traversal) {
    Listener 	listener;

    listener = new Listener();
    traversal.traverse(listener);

    m_NameCache          = listener.getNameCache();
    m_ClasspathPartCache = listener.getClasspathPartCache();
  }

  /**
   * Returns whether the cache is empty.
   *
   * @return		true if empty
   */
  public boolean isEmpty() {
    return (m_NameCache == null) || m_NameCache.isEmpty();
  }

  /**
   * For testing only.
   *
   * @param args	ignored
   */
  public static void main(String[] args) {
    ClassCache cache = new ClassCache();

    // packages
    System.out.println("--> Packages");
    Iterator<String> pkgs = cache.packages();
    List<String> pkgsSorted = new ArrayList<>();
    while (pkgs.hasNext())
      pkgsSorted.add(pkgs.next());
    Collections.sort(pkgsSorted);
    for (String pkg : pkgsSorted)
      System.out.println(pkg + ": " + cache.getClassnames(pkg).size());

    // packages
    System.out.println("--> Classpath parts");
    Iterator<URL> parts = cache.classpathParts();
    List<URL> partsSorted = new ArrayList<>();
    while (parts.hasNext())
      partsSorted.add(parts.next());
    Collections.sort(partsSorted, new Comparator<URL>() {
      @Override
      public int compare(URL o1, URL o2) {
	return o1.toExternalForm().compareTo(o2.toExternalForm());
      }
    });
    for (URL part : partsSorted)
      System.out.println(part + ": " + cache.getClassnames(part).size());
  }
}
