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

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.internal.file.copy.CopyAction;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.WorkResults;
import org.gradle.api.tasks.bundling.Jar;

import java.nio.file.Path;

public abstract class TransformingTask extends Jar {
    private final ConfigurableFileCollection classpath = getProject().getObjects().fileCollection();
    
    @InputFiles
    public ConfigurableFileCollection getClasspath() {
        return classpath;
    }
    
    @Override
    protected CopyAction createCopyAction() {
        CopyAction action = super.createCopyAction();
        return stream -> {
            action.execute(stream);
            try {
                modifyJar(getArchiveFile().get().getAsFile().toPath());
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
            
            return WorkResults.didWork(true);
        };
    }
    
    protected abstract void modifyJar(Path path) throws Exception;
}
