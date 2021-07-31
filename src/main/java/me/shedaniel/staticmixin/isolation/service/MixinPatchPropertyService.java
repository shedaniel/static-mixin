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

import org.spongepowered.asm.service.IGlobalPropertyService;
import org.spongepowered.asm.service.IPropertyKey;

import java.util.HashMap;
import java.util.Map;

public class MixinPatchPropertyService implements IGlobalPropertyService {
    public static final Map<String, Object> PROPERTIES = new HashMap<>();
    
    @Override
    public IPropertyKey resolveKey(String name) {
        return new Property(name);
    }
    
    @Override
    public <T> T getProperty(IPropertyKey key) {
        return (T) PROPERTIES.get(((Property) key).key);
    }
    
    @Override
    public void setProperty(IPropertyKey key, Object value) {
        PROPERTIES.put(((Property) key).key, value);
    }
    
    @Override
    public <T> T getProperty(IPropertyKey key, T defaultValue) {
        return (T) PROPERTIES.getOrDefault(((Property) key).key, defaultValue);
    }
    
    @Override
    public String getPropertyString(IPropertyKey key, String defaultValue) {
        Object get = PROPERTIES.get(((Property) key).key);
        return get == null ? defaultValue : get.toString();
    }
    
    private static class Property implements IPropertyKey {
        private final String key;
        
        public Property(String key) {
            this.key = key;
        }
        
        @Override
        public String toString() {
            return key;
        }
    }
}
