Test / scalaSource       := (baseDirectory.value / "test")
Test / resourceDirectory := (baseDirectory.value / "test" / "resources")

Compile / scalaSource := { (Compile / baseDirectory)(_ / "src") }.value
