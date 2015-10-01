// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.workspace

import org.nlogo.api.ExtensionException

import org.scalatest.{ BeforeAndAfter, FunSuite }

import scala.collection.JavaConversions._

class ExtensionManagerTests extends FunSuite with BeforeAndAfter {
  before {
    AbstractWorkspace.isApplet(false)
  }

  after {
    AbstractWorkspace.isApplet(true)
  }

  val dummyWorkspace = new DummyWorkspace
  val emptyManager = new ExtensionManager(dummyWorkspace)

  class ErrorSourceException extends Exception("problem")

  trait LoadingExtensionTest {
    var errorMessage: String = null
    val errorSource = new org.nlogo.api.ErrorSource(null) {
      override def signalError(message: String): Nothing = {
        errorMessage = message
        throw new ErrorSourceException()
      }
    }
    val loadingManager = new ExtensionManager(dummyWorkspace)
  }

  trait WithLoadedArrayExtension extends LoadingExtensionTest {
    val loadedManager = loadingManager
    loadedManager.importExtension("array", errorSource)
  }

  test("loadedExtensions returns empty list when no extensions loaded") {
    assert(emptyManager.loadedExtensions.isEmpty)
  }

  test("loadedExtensions returns a list of extensions when extensions loaded") {
    new WithLoadedArrayExtension {
      assert(loadedManager.loadedExtensions.nonEmpty)
      assert(loadedManager.loadedExtensions.head.getClass.getCanonicalName == "org.nlogo.extensions.array.ArrayExtension")
    }
  }

  test("anyExtensionsLoaded returns false with no extensions loaded") {
    assert(! emptyManager.anyExtensionsLoaded)
  }

  test("anyExtensionsLoaded returns true when one extension is loaded") {
    new WithLoadedArrayExtension {
      assert(loadedManager.anyExtensionsLoaded)
    }
  }

  test("getFile gets the source from the provided workspace") {
    assert(emptyManager.getSource("foobar") == dummyWorkspace.getSource("foobar"))
  }

  test("getFile retrieves files from the workspaces model directory") {
    assert(emptyManager.getFile("foobar") == dummyWorkspace.fileManager.getFile("foobar"))
  }

  test("getFile returns files in the extensions directory") {
    assert(emptyManager.getFile("array") != null)
  }

  test("getFile raises an exception if the file doesn't exist") {
    intercept[ExtensionException] {
      emptyManager.getFile("notfound")
    }
  }

  test("retrieveObject returns null when no object has been stored") {
    assert(emptyManager.retrieveObject == null)
  }

  test("retrieveObject reads objects set by storeObject") {
    emptyManager.storeObject("foo")
    assert(emptyManager.retrieveObject == "foo")
  }

  test("importExtension signals an error when extension doesn't exist") {
    new LoadingExtensionTest {
      intercept[ErrorSourceException] {
        loadingManager.importExtension("notfound", errorSource)
      }
      assert(errorMessage == ExtensionManager.EXTENSION_NOT_FOUND + "notfound")
    }
  }

  test("importExtension succeeds if the extension is located and valid") {
    new LoadingExtensionTest {
      loadingManager.importExtension("array", errorSource)
      assert(errorMessage == null)
      assert(loadingManager.anyExtensionsLoaded)
    }
  }

  test("resolvePathAsURL resolves URLs as URLs ") {
    assert(emptyManager.resolvePathAsURL("file:///tmp") == "file:/tmp")
  }

  test("resolvePathAsURL resolves paths with slashes relative to the model location") {
    val expectedURL = dummyWorkspace.dummyFileManager.fooExt.toURI.toURL.toString
    assert(fixWonkyURI(emptyManager.resolvePathAsURL("extensions/foo")) == expectedURL)
  }

  test("resolvePathAsURL resolves paths relative to the model location") {
    val expectedURL = dummyWorkspace.dummyFileManager.foobarFile.toURI.toURL.toString
    assert(fixWonkyURI(emptyManager.resolvePathAsURL("foobar")) == expectedURL)
  }

  test("resolvePathAsURL resolves extensions relative to the working directory") {
    val expectedURL = new java.io.File("extensions" + java.io.File.separator + "array").toURI.toURL.toString
    assert(emptyManager.resolvePathAsURL("array") == expectedURL)
  }

  test("resolvePathAsURL throws an exception if the file cannot be found") {
    intercept[IllegalStateException] {
      emptyManager.resolvePathAsURL("notfound")
    }
  }

  test("getFullPath returns the full path of files in the models directory") {
    val expectedPath = dummyWorkspace.attachModelDir("foobar")
    assert(emptyManager.getFullPath("foobar") == expectedPath)
  }

  test("getFullPath returns the path of files in cwd's extensions directory") {
    val expectedPath = new java.io.File("extensions/array").getPath
    assert(emptyManager.getFullPath("array") == expectedPath)
  }

  test("getFullPath throws an ExtensionException when the file cannot be found") {
    intercept[ExtensionException] {
      emptyManager.getFullPath("notfound")
    }
  }

  test("readFromString proxies through to workspace") {
    assert(emptyManager.readFromString("foobar") == "foobar")
  }

  test("clearAll runs clearAll on all jars") {
    pending
  }

  test("dumpExtensions prints an empty table when no extensions have been loaded") {
    assert(emptyManager.dumpExtensions ==
      """|EXTENSION	LOADED	MODIFIED	JARPATH
         |---------	------	--------	-------
         |""".stripMargin)
  }

  test("dumpExtensions prints a table with all loaded extensions") {
    new WithLoadedArrayExtension {
      val arrayJar = new java.io.File("extensions/array/array.jar")
      val modified = arrayJar.lastModified()
      val path = arrayJar.toURI.toURL.toString
      assert(loadedManager.dumpExtensions ==
        s"""|EXTENSION	LOADED	MODIFIED	JARPATH
            |---------	------	--------	-------
            |array	true	$modified	$path
            |""".stripMargin)
    }
  }

  test("getJarPaths returns empty list if no extensions loaded") {
    assert(emptyManager.getJarPaths.isEmpty)
  }

  test("getJarPaths returns a list of paths when jars are loaded") {
    new WithLoadedArrayExtension {
      assert(loadedManager.getJarPaths.head == "array/array.jar")
    }
  }

  test("getExtensionNames returns empty list if no extensions loaded") {
    assert(emptyManager.getExtensionNames.isEmpty)
  }

  test("getExtensionNames lists loaded extensions") {
    new WithLoadedArrayExtension {
      assert(loadedManager.getExtensionNames.head == "array")
    }
  }

  test("dumpExtensionPrimitives prints an empty table when no extensions are loaded") {
    assert(emptyManager.dumpExtensionPrimitives ==
      """|EXTENSION	PRIMITIVE	TYPE
         |---------	---------	----
         |""".stripMargin)
  }

  test("dumpExtensionPrimitives prints a table with all loaded primitives") {
    new WithLoadedArrayExtension {
      assert(loadedManager.dumpExtensionPrimitives ==
        """|EXTENSION	PRIMITIVE	TYPE
           |---------	---------	----
           |array	TO-LIST	Reporter
           |array	SET	Command
           |array	ITEM	Reporter
           |array	LENGTH	Reporter
           |array	FROM-LIST	Reporter
           |""".stripMargin)
    }
  }

  test("reset unloads and clears all jars") {
    new WithLoadedArrayExtension {
      loadedManager.reset()
      assert(! loadedManager.anyExtensionsLoaded)
      assert(loadedManager.loadedExtensions.isEmpty)
    }
  }

  test("startFullCompilation can be called without error") {
    emptyManager.startFullCompilation()
  }

  test("finishFullCompilation doesn't error when specified extension went unused in compilation") {
    new WithLoadedArrayExtension {
      loadedManager.startFullCompilation()
      loadedManager.finishFullCompilation()
      assert(loadedManager.loadedExtensions.isEmpty)
    }
  }

  test("finishFullCompilation does not remove live jars if they are imported during compilation") {
    new WithLoadedArrayExtension {
      loadedManager.startFullCompilation()
      loadedManager.importExtension("array", errorSource)
      loadedManager.finishFullCompilation()
      assert(loadedManager.loadedExtensions.toSeq.length == 1)
    }
  }

  test("importExtensionData takes an extension name, a bunch of data, and an importHandler, and imports the world for an extension") {
    new WithLoadedArrayExtension {
      loadedManager.importExtensionData("array", List(Array("{{array: 0: 0 0 0 0 0}}")), null)
    }
  }

  test("importExtensionData errors with ExtensionException if the named extension can't be loaded") {
    intercept[ExtensionException] {
      emptyManager.importExtensionData("notfound", List[Array[String]](), null)
    }
  }

  test("isExtensionName returns false when no extension of that name is loaded") {
    assert(! emptyManager.isExtensionName("array"))
  }

  test("isExtensionName returns true when the extension is loaded") {
    new WithLoadedArrayExtension {
      assert(loadedManager.isExtensionName("array"))
    }
  }

  //TODO: this needs to be addressed
  test("finishFullCompilation doesn't catch exceptions thrown by the jar on unloading") {
    pending
  }

  // TODO: this shouldn't be needed
  def fixWonkyURI(uri: String) =
    uri.replaceFirst("/private", "")
}
