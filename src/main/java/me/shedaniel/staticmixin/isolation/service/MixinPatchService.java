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

package me.shedaniel.staticmixin.isolation.service;

import dev.architectury.transformer.shadowed.impl.org.zeroturnaround.zip.commons.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.launch.platform.container.ContainerHandleURI;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.IMixinTransformerFactory;
import org.spongepowered.asm.service.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.function.Supplier;

public class MixinPatchService extends MixinServiceAbstract implements IClassProvider, IClassBytecodeProvider {
    public static MixinPatchService instance;
    public final LoggerAdapterGradle logger = new LoggerAdapterGradle("gradle");
    public final Map<String, byte[]> resources = new HashMap<>();
    public final Map<String, Supplier<ClassNode>> classes = new HashMap<>();
    public IMixinTransformerFactory transformerFactory;
    public IMixinTransformer transformer;
    
    public MixinPatchService() {
        instance = this;
    }
    
    @Override
    public String getName() {
        return "MixinPatchService";
    }
    
    @Override
    public boolean isValid() {
        return true;
    }
    
    @Override
    public void offer(IMixinInternal internal) {
        if (internal instanceof IMixinTransformerFactory) {
            this.transformerFactory = (IMixinTransformerFactory) internal;
        }
        super.offer(internal);
    }
    
    public IMixinTransformer getTransformer() {
        Objects.requireNonNull(transformerFactory, "transformerFactory");
        if (transformer == null) transformer = transformerFactory.createTransformer();
        return transformer;
    }
    
    @Override
    public IClassProvider getClassProvider() {
        return this;
    }
    
    @Override
    public IClassBytecodeProvider getBytecodeProvider() {
        return this;
    }
    
    @Override
    public ITransformerProvider getTransformerProvider() {
        return null;
    }
    
    @Override
    public IClassTracker getClassTracker() {
        return null;
    }
    
    @Override
    public IMixinAuditTrail getAuditTrail() {
        return null;
    }
    
    @Override
    public Collection<String> getPlatformAgents() {
        return Collections.emptyList();
    }
    
    @Override
    public IContainerHandle getPrimaryContainer() {
        try {
            return new ContainerHandleURI(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public InputStream getResourceAsStream(String name) {
        byte[] bytes = resources.get(name);
        if (bytes != null) return new ByteArrayInputStream(bytes);
        return MixinPatchService.class.getClassLoader().getResourceAsStream(name);
    }
    
    @Override
    protected ILogger createLogger(String name) {
        return logger;
    }
    
    @Override
    public ClassNode getClassNode(String name) throws ClassNotFoundException, IOException {
        Supplier<ClassNode> supplier = classes.get(name.replace('.', '/'));
        if (supplier != null) {
            ClassNode node = supplier.get();
            if (node != null) return node;
        }
        ClassReader reader = new ClassReader(getClassBytes(name));
        ClassNode node = new ClassNode();
        reader.accept(node, 0);
        classes.put(name.replace('.', '/'), () -> node);
        return node;
    }
    
    @Override
    public ClassNode getClassNode(String name, boolean runTransformers) throws ClassNotFoundException, IOException {
        return getClassNode(name);
    }
    
    public byte[] _getClassBytes(String name) throws IOException {
        InputStream stream = getResourceAsStream(name.replace('.', '/') + ".class");
        if (stream == null) return null;
        return IOUtils.toByteArray(stream);
    }
    
    public byte[] getClassBytes(String name) throws ClassNotFoundException, IOException {
        byte[] classBytes = _getClassBytes(name);
        
        if (classBytes != null) {
            return classBytes;
        } else {
            throw new ClassNotFoundException(name);
        }
    }
    
    @Override
    public URL[] getClassPath() {
        return new URL[0];
    }
    
    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        return findClass(name, true);
    }
    
    @Override
    public Class<?> findClass(String name, boolean initialize) throws ClassNotFoundException {
        throw new ClassNotFoundException(name);
    }
    
    @Override
    public Class<?> findAgentClass(String name, boolean initialize) throws ClassNotFoundException {
        throw new ClassNotFoundException(name);
    }
}
