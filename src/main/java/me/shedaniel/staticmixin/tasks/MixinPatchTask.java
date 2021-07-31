/*
 * Copyright (C) 2021 shedaniel
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package me.shedaniel.staticmixin.tasks;

import dev.architectury.transformer.shadowed.impl.com.google.common.collect.ImmutableMap;
import dev.architectury.transformer.transformers.BuiltinProperties;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.logging.configuration.ShowStacktrace;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MixinPatchTask extends TransformingTask {
    @InputFiles
    private final ConfigurableFileCollection mixinConfigs;
    @InputFiles
    private final ConfigurableFileCollection classpath;
    @Input
    private final SetProperty<String> simpleMixinConfigs;
    
    public MixinPatchTask() {
        this.mixinConfigs = getProject().getObjects().fileCollection();
        this.classpath = getProject().getObjects().fileCollection();
        this.simpleMixinConfigs = getProject().getObjects().setProperty(String.class)
                .empty();
    }
    
    public ConfigurableFileCollection getMixinConfigs() {
        return mixinConfigs;
    }
    
    @Override
    public ConfigurableFileCollection getClasspath() {
        return classpath;
    }
    
    public SetProperty<String> getSimpleMixinConfigs() {
        return simpleMixinConfigs;
    }
    
    public void mixinConfig(String path) {
        simpleMixinConfigs.add(path);
    }
    
    @Override
    protected void modifyJar(Path path) {
        System.setProperty(BuiltinProperties.LOCATION, getProject().getGradle().getRootProject().getBuildDir().toPath().resolve("static-mixin").toAbsolutePath().toString());
        FileCollection cp = getProject().getBuildscript().getConfigurations().getByName("classpath")
                .plus(getProject().getRootProject().getBuildscript().getConfigurations().getByName("classpath"))
                .plus(getProject().getBuildscript().getConfigurations().detachedConfiguration(getProject().getDependencies().gradleApi(), getProject().getDependencies().localGroovy()));
        getProject().javaexec(spec -> {
            spec.setSystemProperties(ImmutableMap.copyOf((Set<Map.Entry<String, Object>>) (Set) System.getProperties().entrySet()));
            spec.systemProperty("mixinConfigs", mixinConfigs.getAsFileTree().getFiles().stream()
                    .map(File::getAbsolutePath)
                    .collect(Collectors.joining(File.pathSeparator)));
            spec.systemProperty("patchClasspath", classpath.getAsFileTree().getFiles().stream()
                    .map(File::getAbsolutePath)
                    .collect(Collectors.joining(File.pathSeparator)));
            spec.systemProperty("simpleMixinConfigs", String.join(File.pathSeparator, simpleMixinConfigs.get()));
            spec.systemProperty("path", path.toAbsolutePath().toString());
            spec.setClasspath(cp);
            spec.workingDir(getProject().getBuildDir());
            spec.getMainClass().set("me.shedaniel.staticmixin.isolation.MixinPatchExecutor");
            
            // if running with INFO or DEBUG logging
            if (getProject().getGradle().getStartParameter().getShowStacktrace() != ShowStacktrace.INTERNAL_EXCEPTIONS
                || getProject().getGradle().getStartParameter().getLogLevel().compareTo(LogLevel.LIFECYCLE) < 0) {
                spec.setStandardOutput(System.out);
                spec.setErrorOutput(System.err);
            } else {
                spec.setStandardOutput(new NullOutputStream());
                spec.setErrorOutput(new NullOutputStream());
            }
        }).rethrowFailure().assertNormalExitValue();
    }
    
    private static class NullOutputStream extends OutputStream {
        @Override
        public void write(int b) throws IOException {}
    }
}
