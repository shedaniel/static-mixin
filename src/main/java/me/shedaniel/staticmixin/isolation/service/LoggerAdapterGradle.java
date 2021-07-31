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

import dev.architectury.transformer.util.Logger;
import org.slf4j.helpers.MessageFormatter;
import org.spongepowered.asm.logging.Level;
import org.spongepowered.asm.logging.LoggerAdapterAbstract;

import java.io.PrintWriter;
import java.io.StringWriter;

public class LoggerAdapterGradle extends LoggerAdapterAbstract {
    public LoggerAdapterGradle(String id) {
        super(id);
    }
    
    @Override
    public String getType() {
        return "Gradle";
    }
    
    @Override
    public void catching(Level level, Throwable t) {
        log(level, "Caught error", t);
    }
    
    @Override
    public void catching(Throwable t) {
        catching(Level.ERROR, t);
    }
    
    public String throwable(Throwable t) {
        StringWriter writer = new StringWriter();
        t.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }
    
    @Override
    public void debug(String message, Object... params) {
        Logger.debug(MessageFormatter.arrayFormat(message, params).getMessage());
    }
    
    @Override
    public void debug(String message, Throwable t) {
        Logger.debug(message);
        Logger.debug(throwable(t));
    }
    
    @Override
    public void error(String message, Object... params) {
        Logger.error(MessageFormatter.arrayFormat(message, params).getMessage());
    }
    
    @Override
    public void error(String message, Throwable t) {
        Logger.error(message);
        Logger.error(throwable(t));
    }
    
    @Override
    public void fatal(String message, Object... params) {
        Logger.error(MessageFormatter.arrayFormat(message, params).getMessage());
    }
    
    @Override
    public void fatal(String message, Throwable t) {
        Logger.error(message);
        Logger.error(throwable(t));
    }
    
    @Override
    public void info(String message, Object... params) {
        Logger.info(MessageFormatter.arrayFormat(message, params).getMessage());
    }
    
    @Override
    public void info(String message, Throwable t) {
        Logger.info(message);
        Logger.info(throwable(t));
    }
    
    @Override
    public void log(Level level, String message, Object... params) {
        switch (level) {
            case FATAL:
            case ERROR:
                error(message, params);
                break;
            case WARN:
                warn(message, params);
                break;
            case INFO:
                info(message, params);
                break;
            case DEBUG:
                debug(message, params);
                break;
            case TRACE:
                trace(message, params);
                break;
        }
    }
    
    @Override
    public void log(Level level, String message, Throwable t) {
        switch (level) {
            case FATAL:
            case ERROR:
                error(message, t);
                break;
            case WARN:
                warn(message, t);
                break;
            case INFO:
                info(message, t);
                break;
            case DEBUG:
                debug(message, t);
                break;
            case TRACE:
                trace(message, t);
                break;
        }
    }
    
    @Override
    public <T extends Throwable> T throwing(T t) {
        Logger.error(throwable(t));
        return t;
    }
    
    @Override
    public void trace(String message, Object... params) {
        Logger.debug(MessageFormatter.arrayFormat(message, params).getMessage());
    }
    
    @Override
    public void trace(String message, Throwable t) {
        Logger.debug(message);
        Logger.debug(throwable(t));
    }
    
    @Override
    public void warn(String message, Object... params) {
        Logger.error(MessageFormatter.arrayFormat(message, params).getMessage());
    }
    
    @Override
    public void warn(String message, Throwable t) {
        Logger.error(message);
        Logger.error(throwable(t));
    }
    
}
