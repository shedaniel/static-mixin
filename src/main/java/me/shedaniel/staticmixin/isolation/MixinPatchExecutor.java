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

package me.shedaniel.staticmixin.isolation;

import com.google.common.base.Strings;
import dev.architectury.transformer.Transform;
import dev.architectury.transformer.input.OpenedFileAccess;
import dev.architectury.transformer.util.Logger;
import me.shedaniel.staticmixin.isolation.service.MixinPatchPropertyService;
import me.shedaniel.staticmixin.isolation.service.MixinPatchService;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.transformer.Config;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MixinPatchExecutor {
    public static void main(String[] args) throws Exception {
        for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
            Logger.debug("Properties: %s = %s", entry.getKey(), entry.getValue());
        }
        List<File> mixinConfigs = Arrays.stream(System.getProperty("mixinConfigs").split(File.pathSeparator))
                .filter(s -> s != null && !s.trim().isEmpty())
                .map(File::new)
                .collect(Collectors.toList());
        List<File> classpath = Arrays.stream(System.getProperty("patchClasspath").split(File.pathSeparator))
                .filter(s -> s != null && !s.trim().isEmpty())
                .map(File::new)
                .collect(Collectors.toList());
        Set<String> simpleMixinConfigs = Arrays.stream(System.getProperty("simpleMixinConfigs").split(File.pathSeparator))
                .filter(s -> s != null && !s.trim().isEmpty())
                .collect(Collectors.toSet());
        Path path = Paths.get(System.getProperty("path"));
        patch(mixinConfigs, classpath, simpleMixinConfigs, path);
    }
    
    public static void patch(List<File> mixinConfigs, List<File> classpath, Set<String> simpleMixinConfigs, Path path)
            throws Exception {
        for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
            MixinPatchPropertyService.PROPERTIES.put((String) entry.getKey(), entry.getValue());
        }
        MixinBootstrap.init();
        MixinPatchService.instance.resources.clear();
        for (File file : mixinConfigs) {
            Logger.debug("Found mixin config " + file.getAbsolutePath());
            MixinPatchService.instance.resources.put(file.getAbsolutePath(), Files.readAllBytes(file.toPath()));
            Mixins.addConfiguration(file.getAbsolutePath());
        }
        MixinPatchService.instance.classes.clear();
        Map<String, Map.Entry<ClassNode, byte[]>> toTransform = new HashMap<>();
        
        BiConsumer<String, Supplier<byte[]>> fileConsumer = (name, bytes) -> {
            if (name.endsWith(".class")) {
                ClassReader tmpReader = new ClassReader(bytes.get());
                if ((tmpReader.getAccess() & Opcodes.ACC_MODULE) == 0) {
                    String className = tmpReader.getClassName();
                    Logger.debug("Found class " + className);
                    MixinPatchService.instance.classes.put(className, () -> {
                        ClassReader reader = new ClassReader(bytes.get());
                        ClassNode node = new ClassNode(Opcodes.ASM8);
                        reader.accept(node, ClassReader.EXPAND_FRAMES);
                        return node;
                    });
                }
            } else {
                String[] split = name.split("/");
                String fileName = split[split.length - 1];
                if (simpleMixinConfigs.contains(fileName)) {
                    Logger.debug("Found mixin config " + name);
                    MixinPatchService.instance.resources.put(name, bytes.get());
                    Mixins.addConfiguration(name);
                }
            }
        };
        
        for (File file : classpath) {
            String absolutePath = file.getAbsolutePath();
            Logger.debug("Classpath Supplied: " + absolutePath);
            if (absolutePath.endsWith(".jar") || absolutePath.endsWith(".zip")) {
                try (OpenedFileAccess access = OpenedFileAccess.ofJar(file.toPath())) {
                    access.handle((name, bytes) -> fileConsumer.accept(name.replace(File.separatorChar, '/'), () -> bytes));
                }
            } else {
                fileConsumer.accept(absolutePath.replace(File.separatorChar, '/'), () -> {
                    try {
                        return Files.readAllBytes(file.toPath());
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            }
        }
        
        try (OpenedFileAccess access = OpenedFileAccess.ofJar(path)) {
            access.handle(p -> p.endsWith(".class"), (p, bytes) -> {
                ClassReader reader = new ClassReader(bytes);
                if ((reader.getAccess() & Opcodes.ACC_MODULE) == 0) {
                    ClassNode node = new ClassNode(Opcodes.ASM8);
                    reader.accept(node, ClassReader.EXPAND_FRAMES);
                    Logger.debug("Found input class " + node.name);
                    MixinPatchService.instance.classes.put(node.name, () -> node);
                    toTransform.put(node.name, new AbstractMap.SimpleEntry<>(node, bytes));
                }
            });
            
            try {
                Method m = MixinEnvironment.class.getDeclaredMethod("gotoPhase", MixinEnvironment.Phase.class);
                m.setAccessible(true);
                m.invoke(null, MixinEnvironment.Phase.INIT);
                m.invoke(null, MixinEnvironment.Phase.DEFAULT);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            
            List<String> packages = Mixins.getConfigs().stream().map(Config::getConfig).map(IMixinConfig::getMixinPackage)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            for (String mixinPackage : packages) {
                Logger.debug("Mixin package: " + mixinPackage);
            }
            List<String> toDelete = new ArrayList<>();
            
            out:
            for (Map.Entry<String, Map.Entry<ClassNode, byte[]>> entry : toTransform.entrySet()) {
                for (String mixinPackage : packages) {
                    if (packageMatch(mixinPackage, entry.getKey().replace('/', '.'))) {
                        toDelete.add(entry.getKey() + ".class");
                        Logger.debug("Cleaning mixin class %s as defined with mixin package %s ", entry.getKey().replace('/', '.'), mixinPackage);
                        continue out;
                    }
                }
                
                Logger.debug("Transforming " + entry.getKey());
                byte[] bytes = MixinPatchService.instance.getTransformer().transformClassBytes(entry.getKey().replace('/', '.'), entry.getKey().replace('/', '.'), entry.getValue().getValue());
                if (!Arrays.equals(bytes, entry.getValue().getValue())) {
                    Logger.debug("Transformed " + entry.getKey());
                    access.addClass(entry.getKey(), bytes);
                } else {
                    Logger.debug("No Transformation " + entry.getKey());
                }
            }
            
            access.deleteFiles(s -> {
                String[] split = s.replace(File.separatorChar, '/').split("/");
                String fileName = split[split.length - 1];
                return simpleMixinConfigs.contains(fileName) || toDelete.contains(Transform.trimSlashes(s));
            });
        }
        
        MixinPatchService.instance.resources.clear();
        MixinPatchService.instance.classes.clear();
    }
    
    public static boolean packageMatch(String mixinPackage, String className) {
        return !Strings.isNullOrEmpty(mixinPackage) && className.startsWith(mixinPackage);
    }
}
