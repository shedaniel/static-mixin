# Static Mixin
Static Mixin is a Gradle plugin to statically apply mixins to jars
___
## Usage
### Adding the plugin
Since the plugin is not hosted on Gradle Plugin Portal, you will need to add my maven repository:

`settings.gradle`:
```gradle
pluginManagement {
    repositories {
        maven { url "https://maven.shedaniel.me" }
        gradlePluginPortal()
    }
}

[...]
```

Now, we can add the plugin to our project:
`build.gradle`:
```gradle
plugins {
    id "me.shedaniel.static-mixin" version "1.0.+"
}
```

### Adding the dependency
We need to use the mixin annotations, and static mixin itself relocates mixin at our comfort.
We will be adding static mixin as a compile only dependency here.
```
dependencies {
    compileOnly "me.shedaniel:static-mixin:1.0.+"
}
```

### Declaring the patch task
The patch task is where all the magic happens, there are a few properties to take note.
- `mixinConfigs`: Direct FileCollection to set where the mixin configs are
- `classpath`: The classpath of our patches, the classpath of the program we are patching, and our patches
```gradle
dependencies {
    compileOnly "we.patch:this:1.0.0"
}

import me.shedaniel.staticmixin.tasks.MixinPatchTask

// for demo purposes, you can write all of these in a better way
task patchMixin(type: MixinPatchTask, dependsOn: "jar") {
    classpath.from(sourceSets.main.output + sourceSets.main.compileClasspath)
    from(sourceSets.main.output + configurations.detachedConfiguration(dependencies.create("we.patch:this:1.0.0")).files.collect { zipTree(it) })

    mixinConfig "our_patch.mixin.json" // we can declare our mixins like this
}
```

### Shadowing Static Mixin into the patched jar
The patched jar now requires static mixin to run, this is because Mixin adds its classes into the classes.

You can fix this in two ways:
1. Add static mixin to your runtime
2. Shadow static mixin to your jar

I am not going to detail how to shadow things into your jar, I am assuming that you know all of this better than me :p